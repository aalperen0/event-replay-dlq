package com.example.event_replay_dlq_system.repository;

import com.example.event_replay_dlq_system.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> getEventByEventId(String eventId);

    List<Event> findByCorrelationId(String correlationId);

    List<Event> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<Event> findByEventTypeAndCreatedAtBetween(String eventType, LocalDateTime from, LocalDateTime to);

    @Query(
            "SELECT e FROM Event e WHERE " +
                    "(:eventType IS NULL OR e.eventType = :eventType) AND " +
                    "(:fromDate IS NULL OR e.createdAt >= :fromDate) AND " +
                    "(:toDate IS NULL OR e.createdAt <= :toDate) AND " +
                    "(:sourceSystem IS NULL OR e.sourceSystem = :sourceSystem)"
    )
    List<Event> findByFilterCriteria(
            @Param("eventType") String eventType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("sourceSystem") String sourceSystem
    );

    List<Event> findAllByEventIdIn(Collection<String> eventIds);
}
