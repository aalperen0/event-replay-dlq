package com.example.event_replay_dlq_system.dto;

import com.example.event_replay_dlq_system.entity.EventFilter;
import io.netty.util.internal.AppendableCharSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReplaySessionRequestDTO {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private EventFilter eventFilter;

    private String createdBy;

}
