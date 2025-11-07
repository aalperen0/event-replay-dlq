package com.example.event_replay_dlq_system.service;


import com.example.event_replay_dlq_system.dto.DLQEventDTO;
import com.example.event_replay_dlq_system.entity.DeadLetterQueue;
import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventProcessingLog;
import com.example.event_replay_dlq_system.enums.DLQStatus;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import com.example.event_replay_dlq_system.exception.DLQNotFoundException;
import com.example.event_replay_dlq_system.exception.EventNotFoundException;
import com.example.event_replay_dlq_system.exception.EventProcessingLogNotFoundException;
import com.example.event_replay_dlq_system.exception.ProcessingException;
import com.example.event_replay_dlq_system.mapper.EventMapper;
import com.example.event_replay_dlq_system.repository.DeadLetterQueueRepository;
import com.example.event_replay_dlq_system.repository.EventProcessingLogRepository;
import com.example.event_replay_dlq_system.repository.EventRepository;
import kafka.coordinator.group.Dead;
import org.apache.catalina.connector.Response;
import org.apache.zookeeper.Op;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
public class DLQServiceTest {

    @Mock
    private DeadLetterQueueRepository deadLetterQueueRepository;

    @Mock
    private EventProcessingLogRepository eventProcessingLogRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;


    @InjectMocks
    private DLQService dlqService;


    private DeadLetterQueue mockDLQ;
    private Event mockEvent;
    private EventProcessingLog mockLog;

    private static final String TEST_EVENT_ID = UUID.randomUUID().toString();
    private static final String ARCHIVE_REASON = "Manually archived due to data issue";


    @BeforeEach
    void setUp() {
        mockDLQ = DeadLetterQueue.builder()
                .eventId(TEST_EVENT_ID)
                .processorName("OrderProcessor")
                .originalPayload("{\"orderId\": \"ORD-12345\", \"customerId\": \"CUST-001\", \"totalAmount\": -150.50}")
                .failureReason("Amount must be greater than or equal to 0")
                .totalAttempts(3)
                .lastFailureTime(LocalDateTime.now())
                .dlqStatus(DLQStatus.ACTIVE)
                .build();

        mockEvent = Event.builder()
                .eventId("uuid-2")
                .eventType("PaymentCompleted")
                .payload("mock-payload")
                .sourceSystem("PaymentService")
                .correlationId("payment-002")
                .version(1)
                .build();

        mockLog = EventProcessingLog.builder()
                .eventId(TEST_EVENT_ID)
                .processorName("OrderProcessor")
                .status(ProcessingStatus.FAILED)
                .attemptCount(3)
                .errorMessage("Some error")
                .nextRetryTime(LocalDateTime.now())
                .build();

    }


    @Test
    void getActiveDLQEntries_Success() {
        DeadLetterQueue dlq = DeadLetterQueue.builder()
                .eventId("uuid-2")
                .processorName("PaymentProcessor")
                .originalPayload("{\"orderId\": \"PAY-12345\", \"customerId\": \"CUST-002\", \"totalAmount\": -250.50}")
                .failureReason("Amount must be greater than or equal to 0")
                .totalAttempts(3)
                .lastFailureTime(LocalDateTime.now())
                .dlqStatus(DLQStatus.ACTIVE)
                .build();

        when(deadLetterQueueRepository.findByDlqStatus(DLQStatus.ACTIVE)).thenReturn(Arrays.asList(mockDLQ, dlq));

        List<DeadLetterQueue> response = dlqService.getActiveDLQEntries();

        assertNotNull(response);
        assertEquals(2, response.size());

        DeadLetterQueue firstResponse = response.get(1);
        assertEquals("uuid-2", firstResponse.getEventId());
        assertEquals("PaymentProcessor", firstResponse.getProcessorName());
        assertEquals(3, firstResponse.getTotalAttempts());
        assertEquals(DLQStatus.ACTIVE, firstResponse.getDlqStatus());

        verify(deadLetterQueueRepository, times(1)).findByDlqStatus(DLQStatus.ACTIVE);
    }


    //    ======================== getDLQEventById Tests ==========================
    @Test
    void getDLQByEventId_Success() {
        when(deadLetterQueueRepository.findByEventId(TEST_EVENT_ID)).thenReturn(Optional.of(mockDLQ));

        DeadLetterQueue response = dlqService.getDLQByEventId(TEST_EVENT_ID);

        assertNotNull(response);
        assertEquals(mockDLQ.getEventId(), response.getEventId());
        assertEquals(mockDLQ.getProcessorName(), response.getProcessorName());
        assertEquals(mockDLQ.getTotalAttempts(), response.getTotalAttempts());
        assertEquals(DLQStatus.ACTIVE, response.getDlqStatus());

        verify(deadLetterQueueRepository, times(1)).findByEventId(TEST_EVENT_ID);
    }

    @Test
    void getDLQByEventId_NotFound_ThrowsNotFoundException() {
        when(deadLetterQueueRepository.findByEventId(TEST_EVENT_ID)).thenReturn(Optional.empty());

        DLQNotFoundException dlqNotFoundException = assertThrows(DLQNotFoundException.class, () -> {
            dlqService.getDLQByEventId(TEST_EVENT_ID);
        });

        assertTrue(dlqNotFoundException.getMessage().contains("DLQ NOT FOUND with eventId"));
        verify(deadLetterQueueRepository, times(1)).findByEventId(TEST_EVENT_ID);
    }


    //    ======================== Archive DLQ Event Tests ==========================
    @Test
    void archiveDLQEvent_Success() {
        when(deadLetterQueueRepository.findByEventId(TEST_EVENT_ID)).thenReturn(Optional.of(mockDLQ));

        dlqService.archiveDLQEvent(TEST_EVENT_ID, ARCHIVE_REASON);

        ArgumentCaptor<DeadLetterQueue> captor = ArgumentCaptor.forClass(DeadLetterQueue.class);
        verify(deadLetterQueueRepository).save(captor.capture());

        DeadLetterQueue savedDLQ = captor.getValue();
        assertEquals(DLQStatus.ARCHIVED, savedDLQ.getDlqStatus());
        assertEquals(ARCHIVE_REASON, savedDLQ.getArchiveReason());

        verify(deadLetterQueueRepository, times(1)).save(any(DeadLetterQueue.class));

    }

    @Test
    void archiveDLQEvent_NotFound_ThrowsNotFoundException() {
        when(deadLetterQueueRepository.findByEventId(TEST_EVENT_ID)).thenReturn(Optional.empty());

        DLQNotFoundException dlqNotFoundException = assertThrows(DLQNotFoundException.class, () -> {
            dlqService.archiveDLQEvent(TEST_EVENT_ID, ARCHIVE_REASON);
        });

        assertTrue(dlqNotFoundException.getMessage().contains("DLQ NOT FOUND with eventId"));
        verify(deadLetterQueueRepository, never()).save(any());

    }

    //    ======================== Retry Event Tests ==========================

    @Test
    void RetryDLQEvent_Success() {

        when(deadLetterQueueRepository.findByEventId(TEST_EVENT_ID)).thenReturn(Optional.of(mockDLQ));
        when(eventRepository.getEventByEventId(TEST_EVENT_ID)).thenReturn(Optional.of(mockEvent));
        when(eventProcessingLogRepository.getByEventIdAndProcessorName(TEST_EVENT_ID, "OrderProcessor")).thenReturn(Optional.of(mockLog));

        dlqService.retryDLQEvent(TEST_EVENT_ID);

        ArgumentCaptor<DeadLetterQueue> dlqCaptor = ArgumentCaptor.forClass(DeadLetterQueue.class);
        verify(deadLetterQueueRepository).save(dlqCaptor.capture());
        DeadLetterQueue dlq = dlqCaptor.getValue();
        assertEquals(DLQStatus.RETRIED, dlq.getDlqStatus());
        assertEquals("mock-payload", dlq.getOriginalPayload());

        ArgumentCaptor<EventProcessingLog> logCaptor = ArgumentCaptor.forClass(EventProcessingLog.class);
        verify(eventProcessingLogRepository, times(1)).save(logCaptor.capture());
        EventProcessingLog log = logCaptor.getValue();
        assertEquals(ProcessingStatus.PENDING, log.getStatus());
        assertEquals(0, log.getAttemptCount());
        assertNull(log.getErrorMessage());
        assertNull(log.getNextRetryTime());


        verify(kafkaProducerService, times(1)).sendEvent(mockEvent);

    }

    @Test
    void retryDLQEvent_ThrowsException_WhenDLQNotFound() {
        when(deadLetterQueueRepository.findByEventId(TEST_EVENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(DLQNotFoundException.class,
                () -> dlqService.retryDLQEvent(TEST_EVENT_ID));

        verify(kafkaProducerService, never()).sendEvent(any());
    }


    @Test
    void retryEvent_ThrowsException_WhenEventNotFound() {
        when(deadLetterQueueRepository.findByEventId(TEST_EVENT_ID))
                .thenReturn(Optional.of(mockDLQ));
        when(eventRepository.getEventByEventId(TEST_EVENT_ID))
                .thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class,
                () -> dlqService.retryDLQEvent(TEST_EVENT_ID));
    }


    //    ======================== Move TO DLQ  Tests ==========================

    @Test
    void moveToDLQ_Success() {
        mockLog.setProcessingEndTime(LocalDateTime.of(2025, 1, 1, 10, 0));

        mockEvent = Event.builder()
                .eventId(TEST_EVENT_ID)
                .eventType("OrderCreated")
                .payload("random-payload")
                .sourceSystem("OrderService")
                .build();


        when(eventProcessingLogRepository.getByEventIdAndProcessorName(TEST_EVENT_ID, "OrderProcessor"))
                .thenReturn(Optional.of(mockLog));

        dlqService.moveToDLQ(mockEvent, "OrderProcessor",
                "Amount must be greater than or equal to 0", 3);

        ArgumentCaptor<EventProcessingLog> logCaptor = ArgumentCaptor.forClass(EventProcessingLog.class);
        verify(eventProcessingLogRepository).save(logCaptor.capture());
        EventProcessingLog savedLog = logCaptor.getValue();
        assertEquals(ProcessingStatus.DLQ, savedLog.getStatus());

        ArgumentCaptor<DeadLetterQueue> dlqCaptor = ArgumentCaptor.forClass(DeadLetterQueue.class);
        verify(deadLetterQueueRepository).save(dlqCaptor.capture());
        DeadLetterQueue savedDlq = dlqCaptor.getValue();

        assertEquals(TEST_EVENT_ID, savedDlq.getEventId());
        assertEquals("OrderProcessor", savedDlq.getProcessorName());
        assertEquals("random-payload", savedDlq.getOriginalPayload());
        assertEquals("Amount must be greater than or equal to 0", savedDlq.getFailureReason());
        assertEquals(3, savedDlq.getTotalAttempts());
        assertEquals(DLQStatus.ACTIVE, savedDlq.getDlqStatus());
        assertEquals(mockLog.getProcessingEndTime(), savedDlq.getFirstFailureTime());
        assertNotNull(savedDlq.getLastFailureTime());

        ArgumentCaptor<DLQEventDTO> dtoCaptor = ArgumentCaptor.forClass(DLQEventDTO.class);
        verify(kafkaProducerService).sendDLQEvent(dtoCaptor.capture());
        DLQEventDTO sentDto = dtoCaptor.getValue();

        assertEquals(TEST_EVENT_ID, sentDto.getEventId());
        assertEquals("OrderProcessor", sentDto.getProcessorName());
        assertNotNull(sentDto.getMovedToDLQAt());
    }

    @Test
    void moveToDLQ_ThrowsException_WhenEventLogNotFound() {

        Event testEvent = Event.builder()
                .eventId(TEST_EVENT_ID)
                .eventType("PaymentCompleted")
                .payload("test-payload")
                .sourceSystem("PaymentService")
                .version(1)
                .build();


        when(eventProcessingLogRepository.getByEventIdAndProcessorName(TEST_EVENT_ID, "OrderProcessor")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> dlqService.moveToDLQ(testEvent, "OrderProcessor", "Error", 3));

        verify(deadLetterQueueRepository, never()).save(any());
        verify(kafkaProducerService, never()).sendDLQEvent(any());

    }

}
