package com.example.event_replay_dlq_system.controller;


import com.example.event_replay_dlq_system.entity.DeadLetterQueue;
import com.example.event_replay_dlq_system.service.DLQService;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DLQController {

    private final DLQService dlqService;

    @Autowired
    public DLQController(DLQService dlqService) {
        this.dlqService = dlqService;
    }

    @GetMapping("/dlq")
    public ResponseEntity<List<DeadLetterQueue>> getActiveDLQEntries() {
        return ResponseEntity.ok(dlqService.getActiveDLQEntries());
    }

    @GetMapping("/dlq/{eventId}")
    public ResponseEntity<DeadLetterQueue> getDLQEntry(@PathVariable String eventId) {
        DeadLetterQueue dlq = dlqService.getDLQByEventId(eventId);
        return ResponseEntity.ok(dlq);

    }

    @PostMapping("/dlq/{eventId}/archive")
    public ResponseEntity<String> archiveDLQ(@PathVariable String eventId, @RequestBody String archiveReason) {
        dlqService.archiveDLQEvent(eventId, archiveReason);
        return ResponseEntity.ok("DLQ Event archived: " + eventId);

    }

    @PostMapping("/dlq/{eventId}/retry")
    public ResponseEntity<String> retryDLQ(@PathVariable String eventId) {
        dlqService.retryDLQEvent(eventId);
        return ResponseEntity.ok("DLQ Event retry triggered " + eventId);
    }

}
