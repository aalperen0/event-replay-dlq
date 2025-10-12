package com.example.event_replay_dlq_system.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventProcessingLog;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventProcessingLogRepository extends JpaRepository<EventProcessingLog, Long> {
    List<EventProcessingLog> getByEventId(String eventID);

    Optional<EventProcessingLog> getByEventIdAndProcessorName(String eventId, String processorName);
}
