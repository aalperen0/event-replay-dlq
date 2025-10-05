package com.example.event_replay_dlq_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EventReplayDlqSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventReplayDlqSystemApplication.class, args);
	}

}
