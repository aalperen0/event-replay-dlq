package com.example.event_replay_dlq_system.entity;


import com.example.event_replay_dlq_system.enums.AlertSeverity;
import com.example.event_replay_dlq_system.enums.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    private String alertId;
    private AlertType alertType;
    private String title;
    private String message;
    private Map<String, Object> metadata;
    private AlertSeverity alertSeverity;
    private LocalDateTime timestamp;
    private String source;
}
