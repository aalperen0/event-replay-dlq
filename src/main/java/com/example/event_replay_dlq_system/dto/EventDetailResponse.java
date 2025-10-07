package com.example.event_replay_dlq_system.dto;

import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailResponse {
    private String eventId;
    private String eventType;
    private String payload;
    private String sourceSystem;
    private String correlationId;
    private ProcessingStatus processingStatus;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
