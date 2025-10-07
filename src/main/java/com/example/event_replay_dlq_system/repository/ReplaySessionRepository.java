package com.example.event_replay_dlq_system.repository;

import com.example.event_replay_dlq_system.entity.ReplaySession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplaySessionRepository extends JpaRepository<ReplaySession, Long> {
}
