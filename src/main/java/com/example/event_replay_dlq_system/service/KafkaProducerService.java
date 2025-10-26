package com.example.event_replay_dlq_system.service;

import com.example.event_replay_dlq_system.dto.DLQEventDTO;
import com.example.event_replay_dlq_system.entity.DeadLetterQueue;
import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.ReplayEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaProducerService {


    private final NewTopic eventsTopic;
    private final NewTopic dlqTopic;
    private final NewTopic retryTopic;
    private final NewTopic replayTopic;
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate, NewTopic eventsTopic, NewTopic dlqTopic, NewTopic retryTopic, NewTopic replayTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventsTopic = eventsTopic;
        this.dlqTopic = dlqTopic;
        this.retryTopic = retryTopic;
        this.replayTopic = replayTopic;
    }

    /**
     * -- Sending an event to the events-topics a
     * Partitioning by event-id
     * send it
     * logging whether its successful or failed after process finish.
     *
     * @param event Event
     *
     */

    public void sendEvent(Event event) {
        try {
            log.debug("Sending event to Kafka: {}", event.getEventId());
            kafkaTemplate.send(eventsTopic.name(), event.getEventId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Event published successfully to topic: '{}': eventId={}", eventsTopic.name(), event.getEventId());
                        } else {
                            log.error("Failed to publish event to Kafka: eventId:{}", event.getEventId(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing event to kafka: eventId:{}", event.getEventId(), e);
            throw new KafkaException("Error sending event " + event.getEventId(), e);
        }
    }

    /**
     * -- Sending an event to the dlq-topic
     * Partitioning by event-id
     * send it
     * logging whether its successful or failed after process finish.
     *
     * @param dlqEvent Event
     *
     */

    public void sendDLQEvent(DLQEventDTO dlqEvent) {
        try {
            log.debug("Publishing DLQ event to Kafka: {}", dlqEvent.getEventId());
            kafkaTemplate.send(dlqTopic.name(), dlqEvent.getEventId(), dlqEvent);
        } catch (Exception e) {
            log.error("Error publishing DLQ event to kafka: eventId:{}", dlqEvent.getEventId());
            throw new KafkaException("Error publishing DLQ event " + dlqEvent.getEventId(), e);
        }
    }

    /**
     * -- Sending an event to the replay-topic
     * Partitioning by event-id
     * send it
     * logging whether its successful or failed after process finish.
     *
     * @param event Event
     *
     */

    public void sendReplayEvent(Event event) {
        try {
            log.debug("Publishing replay event to Kafka: {}", event.getEventId());
            kafkaTemplate.send(replayTopic.name(), event.getEventId(), event);
        } catch (Exception e) {
            log.error("Error publishing replay event to kafka: eventId:{}", event.getEventId());
            throw new KafkaException("Error publishing Replay event " + event.getEventId(), e);
        }
    }

    /**
     * -- Sending an event to the retry-topic
     * Partitioning by event-id
     * send it
     * logging whether its successful or failed after process finish.
     *
     * @param event Event
     *
     */

    public void sendRetryEvent(Event event) {
        try {
            log.debug("Publishing Retry event to Kafka: {}", event.getEventId());
            kafkaTemplate.send(retryTopic.name(), event.getEventId(), event);
        } catch (Exception e) {
            log.error("Error publishing Retry event to kafka: eventId:{}", event.getEventId());
            throw new KafkaException("Error publishing Retry event " + event.getEventId(), e);
        }
    }
}
