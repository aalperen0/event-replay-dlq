package com.example.event_replay_dlq_system.dto;

import com.example.event_replay_dlq_system.entity.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;


/**
 * Incoming request event DTO
 * Includes type of event,
 * Payload content
 * Which source published event
 * correlation
 */


@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventPublishRequestDTO {
    @NotBlank(message = "Event Type is required")
    @NotNull
    private String eventType;

    @NotNull
    @NotBlank(message = "Payload is required")
    private String payload;

    @NotBlank(message = "Source  is required")
    @Size(max = 100, message = "Must be at most 255 characters")
    private String sourceSystem;

    @NotBlank(message = "Correlation id is required")
    @Size(max = 255, message = "Must be at most 255 characters")
    private String correlationId;
}
