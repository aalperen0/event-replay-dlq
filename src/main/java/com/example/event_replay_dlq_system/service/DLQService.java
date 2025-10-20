package com.example.event_replay_dlq_system.service;


import com.example.event_replay_dlq_system.dto.DLQEventDTO;
import com.example.event_replay_dlq_system.entity.DeadLetterQueue;
import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventProcessingLog;
import com.example.event_replay_dlq_system.enums.DLQStatus;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import com.example.event_replay_dlq_system.exception.DLQNotFoundException;
import com.example.event_replay_dlq_system.exception.EventNotFoundException;
import com.example.event_replay_dlq_system.mapper.EventMapper;
import com.example.event_replay_dlq_system.repository.DeadLetterQueueRepository;
import com.example.event_replay_dlq_system.repository.EventProcessingLogRepository;
import com.example.event_replay_dlq_system.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DLQService {

    private final EventProcessingLogRepository eventProcessingLogRepository;
    private final KafkaProducerService kafkaProducerService;
    private final DeadLetterQueueRepository deadLetterQueueRepository;
    private final EventRepository eventRepository;

    @Autowired
    public DLQService(EventProcessingLogRepository eventProcessingLogRepository, KafkaProducerService kafkaProducerService, DeadLetterQueueRepository deadLetterQueueRepository, EventRepository eventRepository) {
        this.eventProcessingLogRepository = eventProcessingLogRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.deadLetterQueueRepository = deadLetterQueueRepository;
        this.eventRepository = eventRepository;
    }

    public void moveToDLQ(Event event, String processorName, String failureReason, int totalAttempts) {

        log.info("DLQ EVENT ALERT");

        EventProcessingLog eventLog = eventProcessingLogRepository.getByEventIdAndProcessorName(event.getEventId(), processorName).orElseThrow();


        DeadLetterQueue dlqEvent = DeadLetterQueue.builder()
                .eventId(event.getEventId())
                .processorName(processorName)
                .originalPayload(event.getPayload())
                .failureReason(failureReason)
                .totalAttempts(totalAttempts)
                .firstFailureTime(eventLog.getProcessingEndTime())
                .lastFailureTime(LocalDateTime.now())
                .dlqStatus(DLQStatus.ACTIVE).build();

        deadLetterQueueRepository.save(dlqEvent);


        eventLog.setStatus(ProcessingStatus.DLQ);
        eventProcessingLogRepository.save(eventLog);

        DLQEventDTO dlqEventDTO = EventMapper.toDLQEventDTO(dlqEvent);
        dlqEventDTO.setMovedToDLQAt(LocalDateTime.now());

        log.info("Event moved to DLQ: {}", event.getEventId());

        try {
            kafkaProducerService.sendDLQEvent(dlqEventDTO);
            log.info("DLQ event sent to kafka {}", dlqEvent.getEventId());
        } catch (Exception e) {
            log.error("Failed to sending DLQ", e);
        }

    }

    public void retryDLQEvent(String eventId) {
        DeadLetterQueue dlqEntry = deadLetterQueueRepository.findByEventId(eventId)
                .orElseThrow(() -> new DLQNotFoundException("DLQ NOT FOUND with eventId" + eventId));

        Event originalEvent = eventRepository.getEventByEventId(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found with eventId" + eventId));


        Event retryEvent = new Event();
        retryEvent.setEventId(originalEvent.getEventId());
        retryEvent.setEventType(originalEvent.getEventType());
        retryEvent.setPayload(dlqEntry.getOriginalPayload());
        retryEvent.setSourceSystem(originalEvent.getSourceSystem());
        retryEvent.setCorrelationId(originalEvent.getCorrelationId());
        retryEvent.setVersion(originalEvent.getVersion());

        EventProcessingLog eLog = eventProcessingLogRepository.getByEventIdAndProcessorName(eventId, dlqEntry.getProcessorName()).orElseThrow();
        eLog.setStatus(ProcessingStatus.PENDING);
        eLog.setAttemptCount(0);
        eLog.setErrorMessage(null);
        eLog.setNextRetryTime(null);
        eventProcessingLogRepository.save(eLog);

        dlqEntry.setDlqStatus(DLQStatus.RETRIED);

        try {
            kafkaProducerService.sendEvent(retryEvent);
            log.info("RETRY event sent to kafka {}", retryEvent.getEventId());
        } catch (Exception e) {
            log.error("Failed to sending retry for DLQ", e);
        }

        log.info("Manual retry triggered for DLQ event: {}", eventId);

    }


    /**
     * Find all the active dlq entries
     *
     * @return List of dead letter queues
     */

    public List<DeadLetterQueue> getActiveDLQEntries() {
        return deadLetterQueueRepository.findByDlqStatus(DLQStatus.ACTIVE);
    }

    /**
     * Find specific dlq by eventId
     *
     * @param eventId type of event
     *
     */

    public DeadLetterQueue getDLQByEventId(String eventId) {
        return deadLetterQueueRepository.findByEventId(eventId).orElseThrow
                (() -> new DLQNotFoundException("DLQ NOT FOUND with eventId" + eventId));

    }

    /**
     * Mark events that can't be retried
     *
     * @param eventId       type of event
     * @param archiveReason reason for archiving
     */
    public void archiveDLQEvent(String eventId, String archiveReason) {
        DeadLetterQueue deadLetterQueue = deadLetterQueueRepository.findByEventId(eventId).orElseThrow
                (() -> new DLQNotFoundException("DLQ NOT FOUND with eventId" + eventId));

        if (deadLetterQueue.getDlqStatus() == DLQStatus.ARCHIVED) {
            log.warn("DLQ Event already archived {}", eventId);
            return;
        }

        deadLetterQueue.setDlqStatus(DLQStatus.ARCHIVED);
        deadLetterQueue.setArchiveReason(archiveReason);

        deadLetterQueueRepository.save(deadLetterQueue);

        log.info("DLQ event archived, can not be retried: {}", eventId);
    }

}
