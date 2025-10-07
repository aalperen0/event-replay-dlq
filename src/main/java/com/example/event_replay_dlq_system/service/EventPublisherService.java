package com.example.event_replay_dlq_system.service;


import com.example.event_replay_dlq_system.dto.EventDetailResponse;
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class EventPublisherService {

    private final EventRepository eventRepository;
    // private final KafkaProducer kafkaProducer;
    private final EventProcessingLogRepository eventProcessingLogRepository;

    @Autowired
    public EventPublisherService(EventRepository eventRepository, EventProcessingLogRepository eventProcessingLogRepository) {
        this.eventRepository = eventRepository;
        //       this.kafkaProducer = kafkaProducer;
        this.eventProcessingLogRepository = eventProcessingLogRepository;
    }

    /**
     *  Creates an event and save to the database
     *  Later it will be published to kafka
     *  Update status of Event
     *  Add to the log
     *
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


            // publish kafka later
            log.info("Creating event with id {}", event.getEventId());


            ProcessingStatus processingStatus = ProcessingStatus.PENDING;
            EventProcessingLog eventProcessingLog = EventMapper.processEventLog(event, processingStatus);
            eventProcessingLogRepository.save(eventProcessingLog);

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

    public EventDetailResponse getEventByID(UUID eventId) {
        return eventRepository.getEventByEventId(eventId.toString())
                .map(EventMapper::toEventDetailResponse)
                .orElseThrow(() -> new EventNotFoundException("event not found with id " + eventId.toString()));

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


    public EventPublishResponseDTO getEventProcessingStatus(UUID eventId) {
        return eventRepository.getEventByEventId(eventId.toString())
                .map(EventMapper::mapToEventPublishResponseDTO)
                .orElseThrow(() -> new EventNotFoundException("event not found with id " + eventId.toString()));
    }

}
