package com.example.event_replay_dlq_system.exception;

public class EventProcessingLogNotFoundException extends RuntimeException {
    public EventProcessingLogNotFoundException(String message) {
        super(message);
    }
}
