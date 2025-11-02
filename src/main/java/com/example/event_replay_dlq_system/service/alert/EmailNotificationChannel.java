package com.example.event_replay_dlq_system.service.alert;

import com.example.event_replay_dlq_system.entity.Alert;
import com.example.event_replay_dlq_system.enums.AlertSeverity;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EmailNotificationChannel implements AlertChannel {

    @Value("${alert.channels.email.enabled}")
    private boolean enabled;

    @Value("${alert.channels.email.from}")
    private String fromEmail;

    @Value("${alert.channels.email.to}")
    private String toEmail;

    private final JavaMailSender mailSender;

    @Autowired
    public EmailNotificationChannel(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }


    @Override
    @Async
    public void send(Alert alert) {


        if (!enabled) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(formatSubject(alert));
            helper.setText(formatEmailBody(alert), true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email alert", e);
        }

    }

    private String formatSubject(Alert alert) {
        return String.format("[%s] %s - %s",
                alert.getAlertSeverity(),
                alert.getAlertType(),
                alert.getTitle());
    }

    private String formatEmailBody(Alert alert) {
        return String.format("""
                        <html>
                               <body style="font-family: Arial, sans-serif;">
                                        <h2 style="color: %s;">%s</h2>
                                        <p><strong>Severity:</strong> %s</p>
                                        <p><strong>Type:</strong> %s</p>
                                        <p><strong>Source:</strong> %s</p>
                                        <p><strong>Timestamp:</strong> %s</p>
                                        <hr>
                                        <h3>Message:</h3>
                                        <p>%s</p>
                                        <hr>
                                        <h3>Details:</h3>
                                        <table border="1" cellpadding="5" cellspacing="0">
                                            %s
                                        </table>
                               </body>
                        </html>
                        """,
                getSeverityColor(alert.getAlertSeverity()),
                alert.getTitle(),
                alert.getAlertSeverity(),
                alert.getAlertType(),
                alert.getSource(),
                alert.getTimestamp(),
                alert.getMessage().replace("\n", "<br/>"),
                formatMetadata(alert.getMetadata())
        );
    }

    private String formatMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "<tr><td colspan='2'><i>No metadata</i></td></tr>";
        }


        return metadata.entrySet().stream()
                .map(entry -> String.format(
                        "<tr><td><strong>%s</strong></td><td>%s</td></tr>",
                        entry.getKey(),
                        entry.getValue()
                ))
                .collect(Collectors.joining("\n"));
    }


    private String getSeverityColor(AlertSeverity severity) {
        return switch (severity) {
            case INFO -> "#dc3545";
            case WARNING -> "#ffc107";
            case ERROR, CRITICAL -> "#d9534f";
        };
    }


    @Override
    public String getChannelName() {
        return "Email";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}