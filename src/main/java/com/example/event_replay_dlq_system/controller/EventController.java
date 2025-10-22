package com.example.event_replay_dlq_system.controller;


import com.example.event_replay_dlq_system.dto.*;
import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventProcessingLog;
import com.example.event_replay_dlq_system.repository.EventRepository;
import com.example.event_replay_dlq_system.service.EventPublisherService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EventController {

    private final EventPublisherService eventPublisherService;

    @Autowired
    public EventController(EventPublisherService eventPublisherService) {
        this.eventPublisherService = eventPublisherService;
    }

    @PostMapping("/events")
    public ResponseEntity<EventPublishResponseDTO> publishEvent(@Valid @RequestBody EventPublishRequestDTO eventPublishRequestDTO) {
        EventPublishResponseDTO eventPublishResponseDTO = eventPublisherService.publishEvent(eventPublishRequestDTO);
        return new ResponseEntity<>(eventPublishResponseDTO, HttpStatus.CREATED);
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventDetailResponse>> getAllEvents() {
        List<EventDetailResponse> events = eventPublisherService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<EventDetailResponse> getEvent(@PathVariable String eventId) {
        EventDetailResponse event = eventPublisherService.getEventByID(eventId);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/events/{eventId}/status")
    public ResponseEntity<List<EventProcessingLogResponse>> getEventStatus(@PathVariable String eventId) {
        List<EventProcessingLogResponse> logs = eventPublisherService.getEventProcessingStatus(eventId);
        return ResponseEntity.ok(logs);
    }

    @PatchMapping("/events/{eventId}")
    public ResponseEntity<Void> updateEventManually(@PathVariable String eventId, @RequestBody EventUpdateDTO dto) {
        eventPublisherService.updateEvent(eventId, dto.getPayload());

        return ResponseEntity.noContent().build();
    }
}
