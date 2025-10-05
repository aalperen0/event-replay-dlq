package com.example.event_replay_dlq_system.enums;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public enum DLQStatus {
    ACTIVE,
    RETRIED,
    ARCHIVED
}
