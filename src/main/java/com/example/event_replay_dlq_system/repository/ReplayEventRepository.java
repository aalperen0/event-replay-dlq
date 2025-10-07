package com.example.event_replay_dlq_system.repository;

import com.example.event_replay_dlq_system.entity.ReplayEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplayEventRepository extends JpaRepository<ReplayEvent, Long> {
}
