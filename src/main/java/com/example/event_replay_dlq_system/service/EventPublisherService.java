package com.example.event_replay_dlq_system.service;


import com.example.event_replay_dlq_system.dto.EventDetailResponse;
import com.example.event_replay_dlq_system.dto.EventProcessingLogResponse;
import com.example.event_replay_dlq_system.dto.EventPublishRequestDTO;
import com.example.event_replay_dlq_system.dto.EventPublishResponseDTO;
import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventProcessingLog;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import com.example.event_replay_dlq_system.exception.EventNotFoundException;
import com.example.event_replay_dlq_system.exception.EventProcessingLogNotFoundException;
import com.example.event_replay_dlq_system.mapper.EventMapper;
import com.example.event_replay_dlq_system.repository.EventProcessingLogRepository;
import com.example.event_replay_dlq_system.repository.EventRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class EventPublisherService {

    private final EventRepository eventRepository;
    private final KafkaProducerService kafkaProducerService;
    private final EventProcessingLogRepository eventProcessingLogRepository;

    @Autowired
    public EventPublisherService(EventRepository eventRepository, EventProcessingLogRepository eventProcessingLogRepository,  KafkaProducerService kafkaProducerService) {
        this.eventRepository = eventRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.eventProcessingLogRepository = eventProcessingLogRepository;
    }

    /**
     * Creates an event and save to the database
     * Later it will be published to kafka
     * Update status of Event
     * Add to the log
     *
     * @param
     * @return eventID, status of event, success/error message, time
     */


    @Transactional
    public EventPublishResponseDTO publishEvent(EventPublishRequestDTO eventPublishRequestDTO) {
        // save event
        try {
            Event event = EventMapper.mapToEventEntity(eventPublishRequestDTO);
            event = eventRepository.save(event);





            kafkaProducerService.sendEvent(event);



            return EventMapper.mapToEventPublishResponseDTO(event);


        } catch (Exception e) {
            log.error("Failed to publish event {}", e.getMessage());
            throw new RuntimeException("Failed to publish event " + e.getMessage());
        }

    }

    /***
     * Search for an event based on given id
     * if it fails, return not found
     * else return an event
     *
     * @param eventId
     * @return event
     */

    public EventDetailResponse getEventByID(String eventId) {
        return eventRepository.getEventByEventId(eventId)
                .map(EventMapper::toEventDetailResponse)
                .orElseThrow(() -> new EventNotFoundException("event not found with id " + eventId));

    }

    /**
     * finds the all events
     * return it
     *
     * @return List<Event>
     */
    public List<EventDetailResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(EventMapper::toEventDetailResponse)
                .toList();

    }

    /**
     * return current
     *
     * @param eventId
     * @return
     */

    public List<EventProcessingLogResponse> getEventProcessingStatus(String eventId) {
        List<EventProcessingLog> logs = eventProcessingLogRepository.getByEventId(eventId);

        if (logs.isEmpty()) {
            throw new EventNotFoundException("event not found with id " + eventId);
        }
        return logs.stream()
                .map(EventMapper::toProcessingLogResponse)
                .toList();
    }

}
