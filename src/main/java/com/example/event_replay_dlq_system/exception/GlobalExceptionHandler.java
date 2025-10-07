package com.example.event_replay_dlq_system.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({EventProcessingLogNotFoundException.class})
    public ResponseEntity<Object> handleEventProcessingLogNotFoundException(EventProcessingLogNotFoundException e, WebRequest request) {

        log.warn("EVENT_PROCESSING_LOG_NOT_FOUND_EXCEPTION {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

    }
}
