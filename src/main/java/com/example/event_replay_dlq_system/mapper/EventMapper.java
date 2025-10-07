package com.example.event_replay_dlq_system.mapper;

import com.example.event_replay_dlq_system.dto.EventDetailResponse;
import com.example.event_replay_dlq_system.dto.EventPublishRequestDTO;
import com.example.event_replay_dlq_system.dto.EventPublishResponseDTO;
import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventProcessingLog;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;


public class EventMapper {

    public static EventDetailResponse toEventDetailResponse(Event event) {
        return EventDetailResponse.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .sourceSystem(event.getSourceSystem())
                .correlationId(event.getCorrelationId())
                .version(event.getVersion())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    public static Event mapToEventEntity(EventPublishRequestDTO eventpublishRequestDTO) {
        Event event = new Event();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventpublishRequestDTO.getEventType());
        event.setPayload(eventpublishRequestDTO.getPayload());
        event.setSourceSystem(eventpublishRequestDTO.getSourceSystem());
        event.setCorrelationId(eventpublishRequestDTO.getCorrelationId());
        event.setVersion(1);
        return event;
    }

    public static EventProcessingLog processEventLog(Event event, ProcessingStatus processingStatus) {
        EventProcessingLog eventProcessingLog = new EventProcessingLog();
        String processorName = event.getSourceSystem() != null ? eventProcessingLog.getProcessorName() + "-processor" : "default-processor";

        eventProcessingLog.setEventId(event.getEventId());
        eventProcessingLog.setProcessorName(processorName);
        eventProcessingLog.setStatus(processingStatus);
        eventProcessingLog.setAttemptCount(0);
        eventProcessingLog.setMaxAttempts(3);

        return eventProcessingLog;

    }

    /**
     * Convert Event to the response
     *
     * @param event
     * @return EventPublishResponseDTO
     */
    public static EventPublishResponseDTO mapToEventPublishResponseDTO(Event event) {
        return EventPublishResponseDTO.builder()
                .eventId(event.getEventId())
                .status("PUBLISHED")
                .message("Event published successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

}
