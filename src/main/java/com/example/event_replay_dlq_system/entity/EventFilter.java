package com.example.event_replay_dlq_system.entity;


import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import com.example.event_replay_dlq_system.enums.ReplaySessionStatus;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventFilter {
    private String eventType;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String sourceSystem;
    private String correlationId;
    private ProcessingStatus status;
    private List<String> eventIds;
}
