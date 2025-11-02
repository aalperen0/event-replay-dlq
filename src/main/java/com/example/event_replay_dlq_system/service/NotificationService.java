package com.example.event_replay_dlq_system.service;


import com.example.event_replay_dlq_system.entity.Alert;
import com.example.event_replay_dlq_system.entity.DeadLetterQueue;
import com.example.event_replay_dlq_system.entity.ReplaySession;
import com.example.event_replay_dlq_system.enums.AlertSeverity;
import com.example.event_replay_dlq_system.enums.AlertType;
import com.example.event_replay_dlq_system.service.alert.AlertChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {


    List<AlertChannel> alertChannels;

    @Autowired
    public NotificationService(List<AlertChannel> alertChannels) {
        this.alertChannels = alertChannels;
    }

    public void sendAlert(Alert alert) {

        if (alert.getTimestamp() == null) {
            alert.setTimestamp(LocalDateTime.now());
        }

        if (alert.getAlertId() == null) {
            alert.setAlertId(UUID.randomUUID().toString());
        }

        log.info("Sending Alert {} - {}", alert.getAlertId(), alert.getTitle());

        for (AlertChannel channel : alertChannels) {
            if (channel.isEnabled()) {
                try {
                    channel.send(alert);
                } catch (Exception e) {
                    log.error("Failed to send alert {} - {}", channel.getChannelName(), e.getMessage());
                }
            }
        }
    }

    public void sendDLQNotification(DeadLetterQueue dlqEntry) {
        Alert alert = Alert.builder()
                .alertType(AlertType.DLQ_EVENT)
                .title("Event Moved TO Dead Letter Queue")
                .alertSeverity(AlertSeverity.ERROR)
                .message(String.format(
                        "Event %s failed after %d attempts and moved to DLQ.\nReason: %s",
                        dlqEntry.getEventId(),
                        dlqEntry.getTotalAttempts(),
                        dlqEntry.getFailureReason()

                ))
                .metadata(Map.of(
                        "eventId", dlqEntry.getEventId(),
                        "processor", dlqEntry.getProcessorName(),
                        "totalAttempts", dlqEntry.getTotalAttempts(),
                        "firstFailure", dlqEntry.getFirstFailureTime(),
                        "lastFailure", dlqEntry.getLastFailureTime()
                ))
                .source("DLQService")
                .build();

        sendAlert(alert);
    }

    public void sendReplayCompletedNotification(ReplaySession session) {
        boolean hasFailures = session.getFailedEvents() > 0;

        Alert alert = Alert.builder()
                .alertType(AlertType.REPLAY_COMPLETED)
                .title("Replay Completed")
                .alertSeverity(hasFailures ? AlertSeverity.WARNING : AlertSeverity.INFO)
                .message(String.format(
                        "Replay session '%s' completed.\n" +
                                "Total: %d events\n" +
                                "Success: %d\n" +
                                "Failed: %d",
                        session.getName(),
                        session.getTotalEvents(),
                        session.getSuccessfulEvents(),
                        session.getFailedEvents()
                ))
                .metadata(Map.of(
                        "sessionId", session.getSessionId(),
                        "totalEvents", session.getTotalEvents(),
                        "successfulEvents", session.getSuccessfulEvents(),
                        "failedEvents", session.getFailedEvents(),
                        "duration", Duration.between(session.getStartedAt(), session.getCompletedAt())
                ))
                .source("DLQService")
                .build();

        sendAlert(alert);
    }

}
