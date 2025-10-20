package com.example.event_replay_dlq_system.dto;


import com.example.event_replay_dlq_system.enums.DLQStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DLQEventDTO {
    private String eventId;
    private String processorName;
    private String payload;
    private String failureReason;
    private int totalAttempts;
    private DLQStatus dlqStatus;
    private LocalDateTime movedToDLQAt;
}
