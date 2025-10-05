package com.example.event_replay_dlq_system.entity;


import com.example.event_replay_dlq_system.enums.ReplaySessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "replay_sessions", indexes = {
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_by", columnList = "createdBy")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReplaySession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", unique = true, nullable = false, length = 255)
    private String sessionId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReplaySessionStatus status = ReplaySessionStatus.CREATED;

    @Column(name = "event_filter", columnDefinition = "TEXT")
    private String eventFilter;

    @Column(name = "total_events", nullable = false)
    private int totalEvents = 0;

    @Column(name = "processed_events", nullable = false)
    private int processedEvents = 0;

    @Column(name = "successful_events", nullable = false)
    private int successfulEvents = 0;

    @Column(name = "failed_events", nullable = false)
    private int failedEvents = 0;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}