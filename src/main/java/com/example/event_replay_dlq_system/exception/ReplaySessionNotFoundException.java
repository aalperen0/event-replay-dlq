package com.example.event_replay_dlq_system.exception;

public class ReplaySessionNotFoundException extends RuntimeException {
    public ReplaySessionNotFoundException(String message) {
        super(message);
    }
}
