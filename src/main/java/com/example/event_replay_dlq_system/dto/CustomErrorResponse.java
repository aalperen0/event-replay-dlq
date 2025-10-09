package com.example.event_replay_dlq_system.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CustomErrorResponse {
    private String message;
    private List<String> errors;
    private LocalDateTime timestamp;
    private int status;
}
