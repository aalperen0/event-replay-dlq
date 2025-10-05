package com.example.event_replay_dlq_system.entity;

import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_processing_log",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_processor",
                        columnNames = {"event_id", "processor_name"}
                )
        },
        indexes = {
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_next_retry_time", columnList = "next_retry_time")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventProcessingLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "processor_name", nullable = false, length = 100)
    private String processorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    @Column(name = "processing_start_time")
    private LocalDateTime processingStartTime;

    @Column(name = "processing_end_time")
    private LocalDateTime processingEndTime;

    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;

}
