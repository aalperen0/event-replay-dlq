package com.example.event_replay_dlq_system.dto;

import com.example.event_replay_dlq_system.enums.ReplaySessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@Builder
public class ReplayProgressResponseDTO {
    private String sessionId;
    private String name;
    private ReplaySessionStatus status;
    private int totalEvents;
    private int processedEvents;
    private int successfulEvents;
    private int failedEvents;
    private int pendingEvents;
    private double progressPercentage;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
