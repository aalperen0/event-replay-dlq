package com.example.event_replay_dlq_system.repository;

import com.example.event_replay_dlq_system.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> getEventByEventId(String eventId);
}
