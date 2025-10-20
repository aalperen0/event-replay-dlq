package com.example.event_replay_dlq_system.service;

import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventProcessingLog;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import com.example.event_replay_dlq_system.exception.ProcessingException;
import com.example.event_replay_dlq_system.mapper.EventMapper;
import com.example.event_replay_dlq_system.processor.EventProcessor;
import com.example.event_replay_dlq_system.repository.EventProcessingLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EventProcessingService {
    private final EventProcessingLogRepository eventProcessingLogRepository;
    private final RedisLockService redisLockService;
    private final List<EventProcessor> processors;
    private final RetrySchedulerService retrySchedulerService;
    private final DLQService dLQService;


    @Autowired
    public EventProcessingService(EventProcessingLogRepository eventProcessingLogRepository, RedisLockService redisLockService, List<EventProcessor> processors, RetrySchedulerService retrySchedulerService, DLQService dLQService) {
        this.eventProcessingLogRepository = eventProcessingLogRepository;
        this.redisLockService = redisLockService;
        this.processors = processors;
        this.retrySchedulerService = retrySchedulerService;
        this.dLQService = dLQService;
    }


    public void processEvent(Event event, Acknowledgment ack) {

        log.info("Received event: {} (type: {})", event.getEventId(), event.getEventType());

        try {
            EventProcessor processor = findProcessor(event.getEventType());

            if (processor == null) {
                log.error("No EventProcessor found for event type {}", event.getEventType());
                ack.acknowledge();
                return;
            }

            String processorName = processor.getProcessorName();
            log.info("Using Processor: {}", processorName);


            // Acquiring lock from redis to prevent duplicate processing
            // event:lock:{eventId}:{processorName}
            String lock = "event:lock:" + event.getEventId() + ":" + processorName;

            if (!acquireLock(event, ack, lock)) {
                return;
            }

            try {
                EventProcessingLog eventProcessingLog = findOrCreateProcessingLog(event, processorName);

                if (eventProcessingLog.getStatus() == ProcessingStatus.SUCCESS) {
                    log.info("Event already successfully processed");
                    ack.acknowledge();
                    return;
                }
                if (eventProcessingLog.getStatus() == ProcessingStatus.DLQ) {
                    log.info("Event already in DLQ");
                    ack.acknowledge();
                    return;
                }

                eventProcessingLog.setStatus(ProcessingStatus.PROCESSING);
                eventProcessingLog.setAttemptCount(eventProcessingLog.getAttemptCount() + 1);
                eventProcessingLog.setProcessingStartTime(LocalDateTime.now());
                eventProcessingLogRepository.save(eventProcessingLog);


                // Call processor
                processor.process(event);

                eventProcessingLog.setStatus(ProcessingStatus.SUCCESS);
                eventProcessingLog.setProcessingEndTime(LocalDateTime.now());
                eventProcessingLogRepository.save(eventProcessingLog);

                log.info("Event successfully processed {}", event.getEventId());
                ack.acknowledge();


            } catch (ProcessingException e) {
                handleProcessingFailure(event, processorName, e, ack);
                ack.acknowledge();
            } finally {
                redisLockService.releaseLock(lock);
            }


        } catch (Exception e) {
            log.error("Unexpected error while processing event {}", event.getEventId(), e);
            ack.acknowledge();
        }
    }

    /**
     *
     * @param event TYPE OF event
     * @param ack   Manually acknowledge
     * @param lock  which lock to proceed
     * @return true if redis acquire lock successfully, else false
     */

    private boolean acquireLock(Event event, Acknowledgment ack, String lock) {
        boolean lockAcquired = redisLockService.acquireLock(lock, 300);
        if (!lockAcquired) {
            log.info("Event {} has been locked", event.getEventId());
            ack.acknowledge();
            return false;
        }
        return true;
    }

    /**
     * find individual processor
     *
     * @param eventType type of event
     * @return processor or null in case not find it
     */
    private EventProcessor findProcessor(String eventType) {
        return processors.stream()
                .filter(p -> p.canProcess(eventType))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check existing log based on event and processor name
     * if it exists get the log
     * otherwise create a one.
     *
     * @param event         Event
     * @param processorName Determine processor name
     * @return save it  event processing log to db
     */
    public EventProcessingLog findOrCreateProcessingLog(Event event, String processorName) {
        Optional<EventProcessingLog> existingLog = eventProcessingLogRepository.getByEventIdAndProcessorName(event.getEventId(), processorName);
        if (existingLog.isPresent()) {
            return existingLog.get();
        }

        EventProcessingLog newLog = EventMapper.processEventLog(event, ProcessingStatus.PENDING, processorName);
        return eventProcessingLogRepository.save(newLog);
    }

    /**
     * If event not processed successfully, we deliver to the failure processing
     * Later, IT WILL SEND EVENTS TO THE RETRY
     * IF it exceeds max attempt, move event to the DLQ
     *
     * @param event         type of event
     * @param processorName specific processor to process event
     * @param e             which exception we get
     * @param ack           acknowledge event for reliable guarantee
     */
    private void handleProcessingFailure(Event event, String processorName, ProcessingException e, Acknowledgment ack) {
        EventProcessingLog eLog = eventProcessingLogRepository.getByEventIdAndProcessorName(event.getEventId(), processorName).orElseThrow();


        eLog.setErrorMessage(e.getMessage());
        eLog.setProcessingEndTime(LocalDateTime.now());


        if (eLog.getAttemptCount() < eLog.getMaxAttempts()) {
            log.info("Processing failed for event, will retry (attempt {}/{})", eLog.getAttemptCount(), eLog.getMaxAttempts());
            eLog.setStatus(ProcessingStatus.RETRY);


            retrySchedulerService.scheduleRetry(event, processorName, eLog.getAttemptCount());

        } else {
            log.warn("Max attempts reached, moving to DLQ {}", event.getEventId());

            dLQService.moveToDLQ(event, processorName, e.getMessage(), eLog.getAttemptCount());
        }


        eventProcessingLogRepository.save(eLog);


    }
}
