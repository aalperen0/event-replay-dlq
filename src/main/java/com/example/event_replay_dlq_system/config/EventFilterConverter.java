package com.example.event_replay_dlq_system.config;

import com.example.event_replay_dlq_system.entity.EventFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.events.Attribute;

@Slf4j
@Converter
public class EventFilterConverter implements AttributeConverter<EventFilter, String> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(EventFilter eventFilter) {
        if (eventFilter == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(eventFilter);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert EventFilter to String!", e);
            throw new RuntimeException("Could not convert EventFilter to JSON", e);
        }
    }

    @Override
    public EventFilter convertToEntityAttribute(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return mapper.readValue(data, EventFilter.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert EventFilter to String!", e);
            throw new RuntimeException("Could not convert JSON to EventFilter", e);
        }
    }
}
