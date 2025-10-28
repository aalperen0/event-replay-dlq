package com.example.event_replay_dlq_system.controller;


import com.example.event_replay_dlq_system.dto.ReplayProgressResponseDTO;
import com.example.event_replay_dlq_system.dto.ReplaySessionRequestDTO;
import com.example.event_replay_dlq_system.entity.ReplayEvent;
import com.example.event_replay_dlq_system.entity.ReplaySession;
import com.example.event_replay_dlq_system.mapper.ReplayMapper;
import com.example.event_replay_dlq_system.service.ReplayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReplayController {

    private final ReplayService replayService;

    public ReplayController(ReplayService replayService) {
        this.replayService = replayService;
    }

    @PostMapping("/replay/sessions")
    public ResponseEntity<ReplayProgressResponseDTO> createReplaySession(@Valid @RequestBody ReplaySessionRequestDTO replayDTO) {
        ReplaySession rs = replayService.createReplaySession(replayDTO);
        var responseDTO = ReplayMapper.mapReplayProgressResponseDTO(rs);

        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/replay/sessions/{sessionId}/start")
    public ResponseEntity<ReplayProgressResponseDTO> startReplaySession(@PathVariable String sessionId) {
        ReplaySession session = replayService.startReplaySession(sessionId);
        var responseDTO = ReplayMapper.mapReplayProgressResponseDTO(session);
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/replay/progress/{sessionId}")
    public ResponseEntity<ReplayProgressResponseDTO> getReplayProgress(@PathVariable String sessionId) {
        ReplayProgressResponseDTO progress = replayService.getReplayProgress(sessionId);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/replay/events")
    public List<ReplayEvent> getReplayEvents() {
       return replayService.getAllReplayEvents();
    }



    // TODO: Pause Replay Session Controller
    // TODO: Resume Replay Session Controller

}

