package com.example.event_replay_dlq_system.exception;

public class DLQNotFoundException extends RuntimeException {
    public DLQNotFoundException(String message) {
        super(message);
    }
}
