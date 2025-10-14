package com.example.event_replay_dlq_system.consumer;


import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.service.EventProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RetryEventConsumer {

    private final EventProcessingService eventProcessingService;

    @Autowired
    public RetryEventConsumer(EventProcessingService eventProcessingService) {
        this.eventProcessingService = eventProcessingService;
    }

    @KafkaListener(topics = "${event-system.kafka.topics.retry}")
    public void consumeEventRetry(Event event, Acknowledgment ack) {
        log.info("Received RETRY event: {}", event.getEventId());
        eventProcessingService.processEvent(event, ack);
    }
}
