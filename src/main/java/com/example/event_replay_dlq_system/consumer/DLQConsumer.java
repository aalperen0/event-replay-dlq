package com.example.event_replay_dlq_system.consumer;


import com.example.event_replay_dlq_system.dto.DLQEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DLQConsumer {

    @KafkaListener(topics = "${event-system.kafka.topics.dlq}")
    public void consumeDLQEvent(DLQEventDTO dlqEvent, Acknowledgment ack){
        log.error(" DLQ EVENT ALERT!!! ");
        log.error("Event ID: {}", dlqEvent.getEventId());
        log.error("Processor: {}", dlqEvent.getProcessorName());
        log.error("Failure Reason: {}", dlqEvent.getFailureReason());
        log.error("Total Attempts: {}", dlqEvent.getTotalAttempts());
        log.error("Moved to DLQ at: {}", dlqEvent.getMovedToDLQAt());
        log.error("===============================================");

        // TODO: SEND ALERT TO ON-CALL
        // alertService

        ack.acknowledge();
    }
}
