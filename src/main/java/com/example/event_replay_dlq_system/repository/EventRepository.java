package com.example.event_replay_dlq_system.repository;

import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventFilter;
import com.example.event_replay_dlq_system.specification.EventSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    Optional<Event> getEventByEventId(String eventId);


    List<Event> findAllByEventIdIn(Collection<String> eventIds);
}
