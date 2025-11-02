package com.example.event_replay_dlq_system.consumer;

import com.example.event_replay_dlq_system.entity.DeadLetterQueue;
import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.ReplayEvent;
import com.example.event_replay_dlq_system.entity.ReplaySession;
import com.example.event_replay_dlq_system.enums.DLQStatus;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import com.example.event_replay_dlq_system.enums.ReplaySessionStatus;
import com.example.event_replay_dlq_system.exception.DLQNotFoundException;
import com.example.event_replay_dlq_system.exception.ProcessingException;
import com.example.event_replay_dlq_system.exception.ReplaySessionNotFoundException;
import com.example.event_replay_dlq_system.processor.EventProcessor;
import com.example.event_replay_dlq_system.repository.DeadLetterQueueRepository;
import com.example.event_replay_dlq_system.repository.ReplayEventRepository;
import com.example.event_replay_dlq_system.repository.ReplaySessionRepository;
import com.example.event_replay_dlq_system.service.RedisLockService;
import com.example.event_replay_dlq_system.service.ReplayService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.sql.Update;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@Slf4j
public class RepConsumer {

    private final List<EventProcessor> processors;
    private final ReplayEventRepository replayEventRepository;
    private final ReplaySessionRepository replaySessionRepository;
    private final ReplayService replayService;
    private final RedisLockService redisLockService;
    private final DeadLetterQueueRepository deadLetterQueueRepository;

    public RepConsumer(List<EventProcessor> processors, ReplayEventRepository replayEventRepository, ReplaySessionRepository replaySessionRepository, ReplayService replayService, RedisLockService redisLockService, DeadLetterQueueRepository deadLetterQueueRepository) {
        this.processors = processors;
        this.replayEventRepository = replayEventRepository;
        this.replaySessionRepository = replaySessionRepository;
        this.replayService = replayService;
        this.redisLockService = redisLockService;
        this.deadLetterQueueRepository = deadLetterQueueRepository;
    }

    /**
     * consumes events from kafka & find all the replay events
     * send to the processReplay
     *
     * @param event
     * @param ack
     */
    @KafkaListener(topics = "${event-system.kafka.topics.replay}")
    public void consumeReplayEvent(Event event, Acknowledgment ack) {

        log.info("------------------------------------------------------------");
        log.info("Received REPLAY event: {}", event.getEventId());
        try {
            List<ReplayEvent> replayEvents = replayEventRepository.findByEventId(event.getEventId());

            if (replayEvents.isEmpty()) {
                log.warn("⚠️ No replay event entry found for: {}", event.getEventId());
                ack.acknowledge();
                return;
            }
            for (ReplayEvent replayEvent : replayEvents) {
                if (replayEvent.getStatus() == ProcessingStatus.PENDING) {
                    processReplayEvent(event, replayEvent);
                }
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Unexpected error processing replay event {}{}", e.getMessage(), event.getEventId());
            ack.acknowledge();
        }


        ack.acknowledge();
    }

    private void processReplayEvent(Event event, ReplayEvent replayEvent) {
        EventProcessor processor = processors.stream()
                .filter(p -> p.canProcess(event.getEventType()))
                .findFirst()
                .orElse(null);

        if (processor == null) {
            log.error("No processor found for event type: {}", event.getEventType());

            replayEvent.setStatus(ProcessingStatus.FAILED);
            replayEvent.setErrorMessage("No processor found for event type: " + event.getEventType());
            replayEvent.setProcessingTime(LocalDateTime.now());
            replayEventRepository.save(replayEvent);

        }

        assert processor != null;
        String processorName = processor.getProcessorName();
        log.info("Processor name: {}", processorName);

        String lockKey = "replay:lock: " + event.getEventId() + ":" + replayEvent.getSessionId();
        boolean lockAcquired = redisLockService.acquireLock(lockKey, 300);
        if (!lockAcquired) {
            log.info("Replay event already being processed {}", event.getEventId());
            return;
        }

        try {
            replayEvent.setStatus(ProcessingStatus.PROCESSING);
            replayEvent.setReplayAttemptCount(replayEvent.getReplayAttemptCount() + 1);
            replayEventRepository.save(replayEvent);

            log.info("Processing replay event (attempt {})", replayEvent.getReplayAttemptCount());
            processor.process(event);

            replayEvent.setStatus(ProcessingStatus.SUCCESS);
            replayEvent.setProcessingTime(LocalDateTime.now());
            replayEventRepository.save(replayEvent);

            log.info("Replay event processed successfully: {}", event.getEventId());
        } catch (ProcessingException e) {
            replayEvent.setStatus(ProcessingStatus.FAILED);
            replayEvent.setErrorMessage(e.getMessage());
            replayEvent.setProcessingTime(LocalDateTime.now());
            replayEventRepository.save(replayEvent);

            log.error("❌ Replay event processing failed: {}", event.getEventId(), e);
        } finally {
            redisLockService.releaseLock(lockKey);
        }


        updateReplaySessionStats(replayEvent.getSessionId());
        updateDLQ(replayEvent.getSessionId());
        checkAndCompleteSession(replayEvent.getSessionId());

    }

    /**
     * Update the session statistics based on success and failed events
     *
     * @param sessionId
     */
    private void updateReplaySessionStats(String sessionId) {
        ReplaySession session = replaySessionRepository.findBySessionId(sessionId).orElseThrow(() -> new ReplaySessionNotFoundException("Session not found" + sessionId));

        if (session == null) {
            return;
        }
        long success = replayEventRepository.countBySessionIdAndStatus(sessionId, ProcessingStatus.SUCCESS);
        long failed = replayEventRepository.countBySessionIdAndStatus(sessionId, ProcessingStatus.FAILED);

        session.setSuccessfulEvents((int) success);
        session.setFailedEvents((int) failed);
        session.setProcessedEvents((int) (success + failed));

        replaySessionRepository.save(session);


        log.debug("Session stats updated: {}/{} processed ({} success, {} failed)",
                session.getProcessedEvents(), session.getTotalEvents(),
                success, failed);
    }


    private void updateDLQ(String sessionId) {
        List<ReplayEvent> event = replayEventRepository.findAllBySessionId(sessionId);

        for (ReplayEvent re : event) {
            DeadLetterQueue dlqEntry = deadLetterQueueRepository.findByEventId(re.getEventId())
                    .orElseThrow(() -> new DLQNotFoundException("DLQ NOT FOUND with eventId" + re.getEventId()));

            if (re.getStatus().equals(ProcessingStatus.SUCCESS)) {
                deadLetterQueueRepository.deleteByEventId(re.getEventId());
            }

            if (re.getStatus().equals(ProcessingStatus.FAILED)) {
                dlqEntry.setDlqStatus(DLQStatus.ARCHIVED);
                dlqEntry.setLastFailureTime(LocalDateTime.now());
                dlqEntry.setTotalAttempts(dlqEntry.getTotalAttempts() + 1);
                dlqEntry.setArchiveReason("Can not be processed");
            }
        }

    }


    /**
     * Complete the replay session if processed events exceeds total events
     *
     * @param sessionId session
     */
    private void checkAndCompleteSession(String sessionId) {

        ReplaySession session = replaySessionRepository.findBySessionId(sessionId).orElseThrow(() -> new ReplaySessionNotFoundException("Session not found" + sessionId));

        if (session == null || session.getStatus() != ReplaySessionStatus.RUNNING) {
            return;
        }

        if (session.getProcessedEvents() >= session.getTotalEvents()) {
            replayService.completeReplaySession(sessionId);

            log.info("🎉 Replay session completed: {} ({}/{} events processed, {} success, {} failed)",
                    sessionId,
                    session.getProcessedEvents(),
                    session.getTotalEvents(),
                    session.getSuccessfulEvents(),
                    session.getFailedEvents()
            );
        }
    }


}
