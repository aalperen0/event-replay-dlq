package com.example.event_replay_dlq_system.enums;

public enum ProcessingStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    RETRY,
    DLQ
}
