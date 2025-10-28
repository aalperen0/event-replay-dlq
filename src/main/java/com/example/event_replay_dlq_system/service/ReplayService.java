package com.example.event_replay_dlq_system.service;

import com.example.event_replay_dlq_system.config.EventFilterConverter;
import com.example.event_replay_dlq_system.dto.ReplayProgressResponseDTO;
import com.example.event_replay_dlq_system.dto.ReplaySessionRequestDTO;
import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventFilter;
import com.example.event_replay_dlq_system.entity.ReplayEvent;
import com.example.event_replay_dlq_system.entity.ReplaySession;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import com.example.event_replay_dlq_system.enums.ReplaySessionStatus;
import com.example.event_replay_dlq_system.exception.ReplaySessionNotFoundException;
import com.example.event_replay_dlq_system.repository.EventRepository;
import com.example.event_replay_dlq_system.repository.ReplayEventRepository;
import com.example.event_replay_dlq_system.repository.ReplaySessionRepository;
import com.example.event_replay_dlq_system.specification.EventSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.filter.ApplicationContextHeaderFilter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ReplayService {

    private final ReplaySessionRepository replaySessionRepository;
    private final ReplayEventRepository replayEventRepository;
    private final KafkaProducerService kafkaProducerService;
    private final EventRepository eventRepository;


    @Autowired
    public ReplayService(ReplaySessionRepository replaySessionRepository, ReplayEventRepository replayEventRepository, KafkaProducerService kafkaProducerService, EventRepository eventRepository) {
        this.replaySessionRepository = replaySessionRepository;
        this.replayEventRepository = replayEventRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.eventRepository = eventRepository;
    }

    public List<ReplayEvent> getAllReplayEvents() {
        return replayEventRepository.findAll();
    }

    public ReplaySession createReplaySession(ReplaySessionRequestDTO request) {


        ReplaySession replaySession = new ReplaySession();
        replaySession.setSessionId(UUID.randomUUID().toString());
        replaySession.setName(request.getName());
        replaySession.setDescription(request.getDescription());
        replaySession.setCreatedBy(request.getCreatedBy());
        replaySession.setEventFilter(request.getEventFilter());

        replaySession.setStatus(ReplaySessionStatus.CREATED);
        replaySession.setTotalEvents(0);
        replaySession.setProcessedEvents(0);
        replaySession.setSuccessfulEvents(0);
        replaySession.setFailedEvents(0);


        return replaySessionRepository.save(replaySession);
    }

    /**
     * If session is created than find events related to query
     * Process them in batch
     *
     * @param sessionId take session id
     * @return Session
     */
    public ReplaySession startReplaySession(String sessionId) {

        ReplaySession session = replaySessionRepository.findBySessionId(sessionId).orElseThrow(
                () -> new ReplaySessionNotFoundException("Session not found with Id:" + sessionId)
        );

        if (session.getStatus() != ReplaySessionStatus.CREATED) {
            throw new IllegalStateException("Session can not started: " + session.getSessionId() + "ONLY CREATED session can be started");
        }


        EventFilter filter = session.getEventFilter();

        List<Event> events = eventRepository.findAll(EventSpecification.byFilter(filter));


        session.setStatus(ReplaySessionStatus.RUNNING);
        session.setTotalEvents(events.size());
        session.setStartedAt(LocalDateTime.now());
        replaySessionRepository.save(session);


        List<ReplayEvent> replayEvents = events.stream()
                .map(event -> {
                    ReplayEvent re = new ReplayEvent();
                    re.setSessionId(sessionId);
                    re.setEventId(event.getEventId());
                    re.setProcessorName("replay-processor");
                    re.setStatus(ProcessingStatus.PENDING);
                    re.setReplayAttemptCount(0);
                    return re;

                }).toList();

        replayEventRepository.saveAll(replayEvents);

        publishEventsInBatches(events, sessionId);


        return session;
    }

    public void pauseReplaySession(String sessionId) {
        ReplaySession session = replaySessionRepository.findBySessionId(sessionId).orElseThrow(
                () -> new ReplaySessionNotFoundException("Session not found with Id:" + sessionId)
        );
        session.setStatus(ReplaySessionStatus.PAUSED);

        replaySessionRepository.save(session);

        log.info("Replay session paused: {}", sessionId);
    }


    /**
     * find the pending events by session id and status
     * publish them in batches
     *
     * @param sessionId session id
     */
    public void resumeReplaySession(String sessionId) {

        ReplaySession session = replaySessionRepository.findBySessionId(sessionId).orElseThrow(
                () -> new ReplaySessionNotFoundException("Session not found with Id:" + sessionId)
        );

        if (session.getStatus() != ReplaySessionStatus.PAUSED) {
            throw new IllegalStateException("Can only resume PAUSED sessions");
        }

        session.setStatus(ReplaySessionStatus.RUNNING);
        replaySessionRepository.save(session);

        List<ReplayEvent> pendingEvents = replayEventRepository.findBySessionIdAndStatus(sessionId, ProcessingStatus.PENDING);

        if (!pendingEvents.isEmpty()) {
            log.info("Resuming replay session: {}", sessionId);

            List<String> eventIds = pendingEvents.stream()
                    .map(ReplayEvent::getEventId)
                    .toList();

            List<Event> events = eventRepository.findAllByEventIdIn(eventIds);

            publishEventsInBatches(events, sessionId);
        }
    }

    public void cancelReplaySession(String sessionId) {
        ReplaySession session = replaySessionRepository.findBySessionId(sessionId).orElseThrow(
                () -> new ReplaySessionNotFoundException("Session not found with Id:" + sessionId)
        );
        session.setStatus(ReplaySessionStatus.CANCELLED);
        replaySessionRepository.save(session);
        log.info("Replay session cancelled: {}", sessionId);

    }

    public void completeReplaySession(String sessionId) {
        ReplaySession session = replaySessionRepository.findBySessionId(sessionId).orElseThrow(
                () -> new ReplaySessionNotFoundException("Session not found with Id:" + sessionId)
        );
        session.setStatus(ReplaySessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());

        replaySessionRepository.save(session);

        log.info("Replay session completed: {}", sessionId);
    }


    public ReplayProgressResponseDTO getReplayProgress(String sessionId) {

        ReplaySession session = replaySessionRepository.findBySessionId(sessionId).orElseThrow(
                () -> new ReplaySessionNotFoundException("Session not found with Id:" + sessionId)
        );


        long pending = replayEventRepository.countBySessionIdAndStatus(sessionId, ProcessingStatus.PENDING);
        long success = replayEventRepository.countBySessionIdAndStatus(sessionId, ProcessingStatus.SUCCESS);
        long failed = replayEventRepository.countBySessionIdAndStatus(sessionId, ProcessingStatus.FAILED);


        int processed = (int) (success + failed);
        double progress = session.getTotalEvents() > 0 ? ((double) (processed / session.getTotalEvents()) * 100) : 0;

        return ReplayProgressResponseDTO.builder()
                .sessionId(sessionId)
                .name(session.getName())
                .status(session.getStatus())
                .totalEvents(session.getTotalEvents())
                .processedEvents(processed)
                .successfulEvents((int) success)
                .failedEvents((int) failed)
                .pendingEvents((int) pending)
                .progressPercentage(progress)
                .startedAt(session.getStartedAt())
                .endedAt(session.getCompletedAt())
                .build();
    }

    @Async
    protected void publishEventsInBatches(List<Event> events, String sessionId) {
        final int batchSize = 100;
        int totalBatch = (int) Math.ceil((double) events.size() / batchSize);

        int publishedCount = 0;
        log.info("Publishing {} events in {} batches for replay session: {}", events.size(), totalBatch, batchSize);

        for (int i = 0; i < events.size(); i += batchSize) {

            ReplaySession session = replaySessionRepository.findBySessionId(sessionId).orElseThrow();

            if (session.getStatus() != ReplaySessionStatus.RUNNING) {
                log.info("⏸️ Replay session paused/cancelled: {}", sessionId);
                break;
            }

            List<Event> batch = events.subList(i, Math.min(i + batchSize, events.size()));

            for (Event event : batch) {
                kafkaProducerService.sendReplayEvent(event);
                publishedCount++;
            }


            log.info("📤 Published batch {}/{} ({} events, total: {}/{})",
                    (i / batchSize) + 1, totalBatch, batch.size(),
                    publishedCount, events.size());


            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("⚠️ Publishing interrupted for session: {}", sessionId);
                break;
            }
        }

        log.info("All events published for replay session: {}", sessionId);
    }
}
