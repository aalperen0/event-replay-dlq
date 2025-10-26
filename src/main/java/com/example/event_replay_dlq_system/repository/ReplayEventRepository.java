package com.example.event_replay_dlq_system.repository;

import com.example.event_replay_dlq_system.entity.ReplayEvent;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReplayEventRepository extends JpaRepository<ReplayEvent, Long> {
    ReplayEvent findBySessionId(String sessionId);

    List<ReplayEvent> findBySessionIdAndStatus(String sessionId, ProcessingStatus processingStatus);

    long countBySessionIdAndStatus(String sessionId, ProcessingStatus status);
}
