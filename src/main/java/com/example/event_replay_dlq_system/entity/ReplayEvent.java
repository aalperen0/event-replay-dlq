package com.example.event_replay_dlq_system.entity;


import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import com.example.event_replay_dlq_system.enums.ReplaySessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Entity
@Table(name = "replay_events", indexes = {
        @Index(name = "idx_session_status", columnList = "session_id, status"),
        @Index(name = "idx_event_id", columnList = "event_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReplayEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;

    @Column(name = "processor_name", nullable = false, length = 100)
    private String processorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessingStatus status = ProcessingStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "replay_attempt_count", nullable = false)
    private int replayAttemptCount = 0;

    @Column(name = "processing_time")
    private LocalDateTime processingTime;
}