package com.example.event_replay_dlq_system.service;

import com.example.event_replay_dlq_system.entity.Event;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaProducerService {


    private final NewTopic eventsTopic;
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate, NewTopic eventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventsTopic = eventsTopic;
    }

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
}
