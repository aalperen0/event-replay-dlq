package com.example.event_replay_dlq_system.service.alert;

import com.example.event_replay_dlq_system.entity.Alert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class LogNotificationChannel implements AlertChannel {

    @Value("${alert.channels.email.enabled}")
    private boolean enabled;



    @Override
    public void send(Alert alert) {
        String logMessage = formatAlert(alert);

        switch(alert.getAlertSeverity()) {
            case INFO -> log.info(logMessage);
            case ERROR, CRITICAL -> log.error(logMessage);
            case WARNING -> log.warn(logMessage);
        }
    }


    private String formatAlert(Alert alert) {
        return String.format(
                "\n" +
                        "====================== ALERT ======================\n" +
                        "Type: %s\n" +
                        "Severity: %s\n" +
                        "Title: %s\n" +
                        "Message: %s\n" +
                        "Source: %s\n" +
                        "Time: %s\n" +
                        "Metadata: %s\n" +
                        "===================================================",
                alert.getAlertType(),
                alert.getAlertSeverity(),
                alert.getTitle(),
                alert.getMessage(),
                alert.getSource(),
                alert.getTimestamp(),
                alert.getMetadata()
        );
    }



    @Override
    public String getChannelName() {
        return "Log";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
