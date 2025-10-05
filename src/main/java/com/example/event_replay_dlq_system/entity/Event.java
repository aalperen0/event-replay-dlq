package com.example.event_replay_dlq_system.entity;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_correlation_id", columnList = "correlation_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, name = "event_id")
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(nullable = false)
    private int version = 1;


}
