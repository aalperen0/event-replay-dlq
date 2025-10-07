package com.example.event_replay_dlq_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


/**
 * Returns that event response DTO
 * Includes eventId,
 * processing status information "PUBLISHED" or "FAILED"
 * success/error message
 * timestamp
 */



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPublishResponseDTO {
    private String eventId;
    private String status;
    private String message;
    private LocalDateTime timestamp;
}
