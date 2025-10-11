package com.example.event_replay_dlq_system.processor;


import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.exception.ProcessingException;

public interface EventProcessor {
    void process(Event event) throws ProcessingException;
    String getProcessorName();
    boolean canProcess(String eventType);
}
