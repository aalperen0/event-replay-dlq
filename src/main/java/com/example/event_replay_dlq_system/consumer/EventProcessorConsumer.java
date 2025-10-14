package com.example.event_replay_dlq_system.consumer;


import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventProcessingLog;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import com.example.event_replay_dlq_system.exception.ProcessingException;
import com.example.event_replay_dlq_system.mapper.EventMapper;
import com.example.event_replay_dlq_system.processor.EventProcessor;
import com.example.event_replay_dlq_system.repository.EventProcessingLogRepository;
import com.example.event_replay_dlq_system.service.EventProcessingService;
import com.example.event_replay_dlq_system.service.RedisLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class EventProcessorConsumer {
    private final EventProcessingService eventProcessingService;


    @Autowired
    public EventProcessorConsumer(EventProcessingService eventProcessingService) {
        this.eventProcessingService = eventProcessingService;
    }

    @KafkaListener(topics = "${event-system.kafka.topics.events}", groupId = "event-processor-group")
    public void consumeEvent(Event event, Acknowledgment ack) {
        log.info("------------------------------------------------------------");
        log.info("Received NEW event: {}", event.getEventId());
        eventProcessingService.processEvent(event, ack);
    }

}

