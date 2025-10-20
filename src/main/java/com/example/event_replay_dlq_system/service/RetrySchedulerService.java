package com.example.event_replay_dlq_system.service;


import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventProcessingLog;
import com.example.event_replay_dlq_system.entity.ReplayEvent;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import com.example.event_replay_dlq_system.repository.EventProcessingLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RetrySchedulerService {


    private final EventProcessingLogRepository eventProcessingLogRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    @Autowired
    public RetrySchedulerService(EventProcessingLogRepository eventProcessingLogRepository, RedisTemplate<String, Object> redisTemplate, KafkaProducerService kafkaProducerService, ObjectMapper objectMapper) {
        this.eventProcessingLogRepository = eventProcessingLogRepository;
        this.redisTemplate = redisTemplate;
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = objectMapper;
    }


    /**
     * Scheduling exponential backoff for a specific based on attempt count
     * 2^n <= 60
     * If process fails -> calculate retry, with TTL = delay
     *
     * @param event         scheduling retry for specific event
     * @param processorName for a specific processor
     * @param attemptCount  current attempt
     */
    protected void scheduleRetry(Event event, String processorName, int attemptCount) {
        try {

            int delay = Math.min((int) (Math.pow(2, attemptCount)), 60);

            String retryKey = "event:retry:" + event.getEventId() + ":" + processorName;


            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.opsForValue().set(retryKey, eventJson, Duration.ofSeconds(delay));

            Long ttl = redisTemplate.getExpire(retryKey);
            log.info("Wrote retry key {} (ttl={}s) valuePresent={}", retryKey, ttl, redisTemplate.hasKey(retryKey));

            EventProcessingLog eLog = eventProcessingLogRepository.getByEventIdAndProcessorName(event.getEventId(), processorName).orElseThrow();
            LocalDateTime nextRetryTime = LocalDateTime.now().plusSeconds(delay);
            eLog.setNextRetryTime(nextRetryTime);

            eventProcessingLogRepository.save(eLog);

            log.info("Scheduler retry for event {} with delay {}s (attempt {})", event.getEventId(), delay, attemptCount);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for retry {}", event.getEventId());
            throw new RuntimeException("Failed to serialize event for retry ", e);
        }

    }

    @Scheduled(fixedRate = 1000)
    public void processExpiredRetries() {
        try {
            Set<String> redisRetryKeys = redisTemplate.keys("event:retry:*");
            if (redisRetryKeys.isEmpty()) {
                // log.debug("No retry keys found");
                return;
            }

            log.debug("Retry keys: {}", redisRetryKeys.size());

            for (String retryKey : redisRetryKeys) {
                processRetryKey(retryKey);
            }
        } catch (Exception e) {
            log.error("Failed to process expired retries", e);
        }
    }

    private void processRetryKey(String retryKey) {
        try {
            long ttl = redisTemplate.getExpire(retryKey);
            // key doesn't exist
            if (ttl == -2) {
                return;
            }


            if (ttl <= 1) {
                String eventJson = (String) redisTemplate.opsForValue().get(retryKey);

                if (eventJson == null) {
                    log.debug("Key already expired or deleted {}", retryKey);
                    return;
                }

                Event event = objectMapper.readValue(eventJson, Event.class);


                // Publish to retry topic
                kafkaProducerService.sendRetryEvent(event);

                redisTemplate.delete(retryKey);
                log.info("Republished retry event to kafka {}", event.getEventId());
            }
        } catch (Exception e) {
            log.error("Failed to process expired retries", e);
        }
    }


    public void cancelRetry(String eventId, String processorName) {
        String retryKey = "event:retry:" + eventId + ":" + processorName;

        Boolean deleted = redisTemplate.delete(retryKey);
        if (deleted) {
            log.info("Cancelled retry event {}", retryKey);
        }
    }

}
