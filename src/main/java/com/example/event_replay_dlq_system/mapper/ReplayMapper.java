package com.example.event_replay_dlq_system.mapper;

import com.example.event_replay_dlq_system.dto.ReplayProgressResponseDTO;
import com.example.event_replay_dlq_system.entity.ReplaySession;

public class ReplayMapper {

    public static ReplayProgressResponseDTO mapReplayProgressResponseDTO(ReplaySession replaySession) {
        return ReplayProgressResponseDTO.builder()
                .sessionId(replaySession.getSessionId())
                .name(replaySession.getName())
                .status(replaySession.getStatus())
                .totalEvents(replaySession.getTotalEvents())
                .processedEvents(replaySession.getProcessedEvents())
                .successfulEvents(replaySession.getSuccessfulEvents())
                .failedEvents(replaySession.getFailedEvents())
                .pendingEvents(replaySession.getTotalEvents())
                .progressPercentage(0.0)
                .startedAt(null)
                .endedAt(null)
                .build();
    }
}
