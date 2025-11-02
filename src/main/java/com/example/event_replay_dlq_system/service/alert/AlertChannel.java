package com.example.event_replay_dlq_system.service.alert;

import com.example.event_replay_dlq_system.entity.Alert;

public interface AlertChannel {

    void send(Alert alert);
    String getChannelName();
    boolean isEnabled();
}
