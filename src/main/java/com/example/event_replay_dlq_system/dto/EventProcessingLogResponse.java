package com.example.event_replay_dlq_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventProcessingLogResponse {
    private String eventId;
    private String processorName;
    private String status;
    private int attemptCount;
    private int maxAttempts;
    private String errorMessage;
    private LocalDateTime processingStartTime;
    private LocalDateTime processingEndTime;
    private LocalDateTime nextRetryTime;
}