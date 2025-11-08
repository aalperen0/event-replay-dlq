package com.example.event_replay_dlq_system.service;

import com.example.event_replay_dlq_system.dto.EventPublishResponseDTO;
import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventProcessingLog;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import com.example.event_replay_dlq_system.repository.EventProcessingLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kafka.Kafka;
import org.apache.commons.digester.annotations.rules.SetProperty;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RetrySchedulerTest {

    @Mock
    private EventProcessingLogRepository eventProcessingLogRepository;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private RetrySchedulerService retrySchedulerService;



    private Event mockEvent;
    private EventProcessingLog mockLog;

    @BeforeEach
    void setUp() {
        mockEvent = Event.builder()
                .eventId("event-123")
                .eventType("OrderCreated")
                .payload("{\"eventId\":\"event-123\"}")
                .sourceSystem("OrderService")
                .correlationId("order-12345")
                .build();

        mockLog = EventProcessingLog.builder()
                .id(1L)
                .eventId("event-123")
                .processorName("OrderEventProcessor")
                .status(ProcessingStatus.PENDING)
                .attemptCount(1)
                .maxAttempts(3)
                .build();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        retrySchedulerService = spy(retrySchedulerService);
    }

    @Test
    void scheduleRetry_Success() throws JsonProcessingException {

        String processorName = "OrderEventProcessor";
        int attemptCount = 1;
        String expectedKey = "event:retry:event-123:OrderEventProcessor";
        String eventJson = "{\"eventId\":\"event-123\"}";
        int expectedDelay = 2;


        when(objectMapper.writeValueAsString(mockEvent)).thenReturn(eventJson);
        when(redisTemplate.getExpire(expectedKey)).thenReturn(2L);
        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);
        when(eventProcessingLogRepository.getByEventIdAndProcessorName(mockEvent.getEventId(), processorName)).thenReturn(Optional.of(mockLog));
        when(eventProcessingLogRepository.save(any(EventProcessingLog.class))).thenReturn(mockLog);


        retrySchedulerService.scheduleRetry(mockEvent, processorName, attemptCount);

        verify(valueOperations).set(expectedKey, eventJson, Duration.ofSeconds(expectedDelay));
        verify(eventProcessingLogRepository).save(any(EventProcessingLog.class));

        ArgumentCaptor<EventProcessingLog> logCaptor = ArgumentCaptor.forClass(EventProcessingLog.class);
        verify(eventProcessingLogRepository).save(logCaptor.capture());

        EventProcessingLog eventProcessingLog = logCaptor.getValue();
        assertEquals(processorName, eventProcessingLog.getProcessorName());
        assertEquals(attemptCount, eventProcessingLog.getAttemptCount());
        assertNotNull(eventProcessingLog.getNextRetryTime());
        assertTrue(eventProcessingLog.getNextRetryTime().isAfter(LocalDateTime.now().plusSeconds(1)));

    }


    @Test
    void scheduleRetry_WithMaxAttempts() throws JsonProcessingException {
        String processorName = "OrderEventProcessor";
        int attemptCount = 3;
        int maxAttempts = 3;
        String expectedKey = "event:retry:event-123:OrderEventProcessor";
        String eventJson = "{\"eventId\":\"event-123\"}";
        int expectedDelay = 8;

        when(objectMapper.writeValueAsString(mockEvent)).thenReturn(eventJson);
        when(redisTemplate.getExpire(expectedKey)).thenReturn(8L);
        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);
        when(eventProcessingLogRepository.getByEventIdAndProcessorName(mockEvent.getEventId(), processorName)).thenReturn(Optional.of(mockLog));

        retrySchedulerService.scheduleRetry(mockEvent, processorName, attemptCount);

        assertEquals(maxAttempts, mockLog.getMaxAttempts());

        verify(valueOperations).set(expectedKey, eventJson, Duration.ofSeconds(expectedDelay));

    }

    @Test
    void scheduleRetry_ThrowsException_WhenEventProcessingLogNotFound() throws JsonProcessingException {
        String processorName = "OrderEventProcessor";
        int attemptCount = 1;
        String eventJson = "{\"eventId\":\"event-123\"}";

        when(objectMapper.writeValueAsString(mockEvent)).thenReturn(eventJson);
        when(redisTemplate.getExpire(anyString())).thenReturn(2L);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        when(eventProcessingLogRepository.getByEventIdAndProcessorName(mockEvent.getEventId(), processorName)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> retrySchedulerService.scheduleRetry(mockEvent, processorName, attemptCount));
        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
        verify(eventProcessingLogRepository, never()).save(any(EventProcessingLog.class));

    }


    @Test
    void processRetryKey_Success() throws JsonProcessingException {
        String retryKey = "event:retry:event-123:OrderEventProcessor";
        String eventJson = "{\"eventId\":\"event-123\"}";

        when(redisTemplate.getExpire(retryKey)).thenReturn(0L); // TTL <= 1
        when(redisTemplate.opsForValue().get(retryKey)).thenReturn(eventJson);
        when(objectMapper.readValue(eventJson, Event.class)).thenReturn(mockEvent);
        when(redisTemplate.delete(retryKey)).thenReturn(true);

        retrySchedulerService.processRetryKey(retryKey);

        verify(valueOperations).get(retryKey);
        verify(redisTemplate).delete(retryKey);
        verify(redisTemplate).getExpire(retryKey);
        verify(objectMapper).readValue(eventJson, Event.class);
        verify(kafkaProducerService).sendRetryEvent(mockEvent);


    }

    @Test
    void processRetryKey_DoesNothing_WhenKeyDoesNotExist() {
        String retryKey = "event:retry:event-123:OrderEventProcessor";

        when(redisTemplate.getExpire(retryKey)).thenReturn(-2L);

        retrySchedulerService.processRetryKey(retryKey);

        verify(redisTemplate).getExpire(retryKey);
        verify(valueOperations, never()).get(anyString());
        verify(kafkaProducerService, never()).sendRetryEvent(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void processRetryKey_DoesNothing_WhenTTLGreaterThan1() {
        String retryKey = "event:retry:event-123:OrderEventProcessor";

        when(redisTemplate.getExpire(retryKey)).thenReturn(10L); // TTL > 1

        retrySchedulerService.processRetryKey(retryKey);

        verify(redisTemplate).getExpire(retryKey);
        verify(valueOperations, never()).get(anyString());
        verify(kafkaProducerService, never()).sendRetryEvent(any());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void processExpiredRetries() {

        Set<String> redisKeys = Set.of(
                "event:retry:event-123:OrderEventProcessor",
                "event:retry:event-123:PaymentEventProcessor"
        );



        when(redisTemplate.keys("event:retry:*")).thenReturn(redisKeys);

        retrySchedulerService.processExpiredRetries();

        verify(retrySchedulerService, times(redisKeys.size())).processRetryKey(anyString());

    }

    @Test
    void processExpiredRetries_DoesNothing_WhenKeyDoesNotExist() {
        when(redisTemplate.keys("event:retry:*")).thenReturn(Collections.emptySet());


        retrySchedulerService.processExpiredRetries();

        verify(retrySchedulerService, never()).processRetryKey(anyString());
    }

}
