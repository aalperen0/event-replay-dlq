package com.example.event_replay_dlq_system.controller;


import com.example.event_replay_dlq_system.dto.EventDetailResponse;
import com.example.event_replay_dlq_system.dto.EventPublishRequestDTO;
import com.example.event_replay_dlq_system.dto.EventPublishResponseDTO;
import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventProcessingLog;
import com.example.event_replay_dlq_system.repository.EventRepository;
import com.example.event_replay_dlq_system.service.EventPublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController("/api")
public class EventController {

    private final EventPublisherService eventPublisherService;

    @Autowired
    public EventController(EventPublisherService eventPublisherService) {
        this.eventPublisherService = eventPublisherService;
    }

    @PostMapping("/events")
    public ResponseEntity<EventPublishResponseDTO> publishEvent(@RequestBody EventPublishRequestDTO eventPublishRequestDTO) {
        EventPublishResponseDTO eventPublishResponseDTO = eventPublisherService.publishEvent(eventPublishRequestDTO);
        return new ResponseEntity<>(eventPublishResponseDTO, HttpStatus.OK);
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventDetailResponse>> getAllEvents() {
        List<EventDetailResponse> events = eventPublisherService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<EventDetailResponse> getEvent(@PathVariable UUID eventId) {
        EventDetailResponse event = eventPublisherService.getEventByID(eventId);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/events/{eventID}/status")
    public ResponseEntity<EventPublishResponseDTO>  getEventStatus(@PathVariable UUID eventID) {
        EventPublishResponseDTO event = eventPublisherService.getEventProcessingStatus(eventID);
        return ResponseEntity.ok(event);
    }
}
