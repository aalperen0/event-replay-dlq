package com.example.event_replay_dlq_system.repository;

import com.example.event_replay_dlq_system.entity.DeadLetterQueue;
import com.example.event_replay_dlq_system.enums.DLQStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueue, Long> {
}
