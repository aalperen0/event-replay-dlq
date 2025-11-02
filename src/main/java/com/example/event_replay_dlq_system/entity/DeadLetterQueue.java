package com.example.event_replay_dlq_system.entity;


import com.example.event_replay_dlq_system.enums.DLQStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letter_queue", indexes = {
        @Index(name = "idx_dlq_status", columnList = "dlqStatus"),
        @Index(name = "idx_processor_name", columnList = "processor_name"),
        @Index(name = "idx_last_failure_time", columnList = "last_failure_time")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeadLetterQueue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "processor_name", nullable = false, length = 100)
    private String processorName;

    @Column(name = "original_payload", columnDefinition = "TEXT", nullable = false)
    private String originalPayload;

    @Column(name = "failure_reason", columnDefinition = "TEXT", nullable = false)
    private String failureReason;

    @Column(name = "total_attempts", nullable = false)
    private int totalAttempts;

    @Column(name = "first_failure_time", nullable = false)
    private LocalDateTime firstFailureTime;

    @Column(name = "last_failure_time", nullable = false)
    private LocalDateTime lastFailureTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "dlq_status", nullable = false, length = 20)
    private DLQStatus dlqStatus = DLQStatus.ACTIVE;

    @Column(name = "archive_reason", columnDefinition = "TEXT")
    private String archiveReason;

    @Column(name = "approximate_retention_time")
    private LocalDateTime approximateRetentionTime = LocalDateTime.now().plusDays(30);
}