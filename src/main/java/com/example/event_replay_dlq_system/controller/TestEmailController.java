package com.example.event_replay_dlq_system.controller;


import com.example.event_replay_dlq_system.entity.Alert;
import com.example.event_replay_dlq_system.enums.AlertSeverity;
import com.example.event_replay_dlq_system.enums.AlertType;
import com.example.event_replay_dlq_system.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestEmailController {

    private final NotificationService notificationService;

    public TestEmailController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/email-test")
    public ResponseEntity<String> testEmail() {
        Alert alert = Alert.builder()
                .alertType(AlertType.DLQ_EVENT)
                .title("Test Email Alert")
                .alertSeverity(AlertSeverity.WARNING)
                .message("This is a test email from the alert system")
                .metadata(Map.of("test", "value"))
                .source("TestController")
                .build();

        notificationService.sendAlert(alert);
        return ResponseEntity.ok("Email alert sent");
    }
}

