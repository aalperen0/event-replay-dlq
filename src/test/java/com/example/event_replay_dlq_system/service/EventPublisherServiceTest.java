package com.example.event_replay_dlq_system.service;


import com.example.event_replay_dlq_system.dto.EventDetailResponse;
import com.example.event_replay_dlq_system.dto.EventProcessingLogResponse;
import com.example.event_replay_dlq_system.dto.EventPublishRequestDTO;
import com.example.event_replay_dlq_system.dto.EventPublishResponseDTO;
import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventProcessingLog;
import com.example.event_replay_dlq_system.enums.ProcessingStatus;
import com.example.event_replay_dlq_system.exception.EventNotFoundException;
import com.example.event_replay_dlq_system.repository.EventProcessingLogRepository;
import com.example.event_replay_dlq_system.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EventPublisherServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventProcessingLogRepository eventProcessingLogRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private EventPublisherService eventPublisherService;

    private Event mockEvent;
    private EventPublishRequestDTO mockRequestDTO;
    private EventProcessingLog mockLog;

    private static final String TEST_EVENT_ID = UUID.randomUUID().toString();


    @BeforeEach
    void setUp() {
        mockEvent = Event.builder()
                .eventId(TEST_EVENT_ID)
                .eventType("OrderCreated")
                .payload("{\"orderId\": \"ORD-12345\", \"customerId\": \"CUST-001\", \"totalAmount\": 150.50}")
                .sourceSystem("OrderService")
                .correlationId("order-12345")
                .version(1)
                .build();


        mockRequestDTO = EventPublishRequestDTO.builder()
                .eventType("OrderCreated")
                .payload("{\"orderId\": \"ORD-12345\", \"customerId\": \"CUST-001\", \"totalAmount\": 150.50}")
                .sourceSystem("OrderService")
                .correlationId("order-12345")

                .build();

        mockLog = EventProcessingLog.builder()
                .id(1L)
                .eventId(TEST_EVENT_ID)
                .processorName("OrderEventProcessor")
                .status(ProcessingStatus.PENDING)
                .attemptCount(1)
                .maxAttempts(3)
                .build();

    }

    @Test
    void publishEvent_Success() {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);
        doNothing().when(kafkaProducerService).sendEvent(any(Event.class));

        EventPublishResponseDTO response = eventPublisherService.publishEvent(mockRequestDTO);

        verify(eventRepository).save(eventCaptor.capture());
        verify(kafkaProducerService).sendEvent(any(Event.class));

        Event capturedEvent = eventCaptor.getValue();

        assertNotNull(response);
        assertNotNull(capturedEvent.getEventId(), "Event ID should be generated");
        assertDoesNotThrow(() -> UUID.fromString(capturedEvent.getEventId()),
                "Event ID should be a valid UUID");
        assertEquals("OrderCreated", capturedEvent.getEventType());
        assertEquals("OrderService", capturedEvent.getSourceSystem());
    }

    //    ======================== getEventByEventID Tests ==========================

    @Test
    void getEventById_Success() {

        when(eventRepository.getEventByEventId(TEST_EVENT_ID)).thenReturn(Optional.of(mockEvent));


        EventDetailResponse response = eventPublisherService.getEventByID(TEST_EVENT_ID);

        assertNotNull(response);
        assertEquals(mockEvent.getEventId(), response.getEventId());
        verify(eventRepository, times(1)).getEventByEventId(TEST_EVENT_ID);
    }

    @Test
    void getEventById_NotFound_ThrowsNotFoundException() {
        when(eventRepository.getEventByEventId("evt-999")).thenReturn(Optional.empty());

        EventNotFoundException exception = assertThrows(EventNotFoundException.class, () -> {
            eventPublisherService.getEventByID("evt-999");
        });

        assertTrue(exception.getMessage().contains("event not found with id evt-999"));
        verify(eventRepository, times(1)).getEventByEventId("evt-999");
    }


    @Test
    void getAllEvents_Success() {
        Event event = Event.builder()
                .eventId("uuid-2")
                .eventType("PaymentCompleted")
                .payload("{\"paymentId\":\"PAY-002\"}")
                .sourceSystem("PaymentService")
                .correlationId("payment-002")
                .version(1)
                .build();

        when(eventRepository.findAll()).thenReturn(Arrays.asList(mockEvent, event));
        List<EventDetailResponse> responses = eventPublisherService.getAllEvents();

        assertNotNull(responses);
        assertEquals(2, responses.size());

        EventDetailResponse firstResponse = responses.get(1);
        assertEquals("uuid-2", firstResponse.getEventId());
        assertEquals("PaymentCompleted", firstResponse.getEventType());
        assertEquals("PaymentService", firstResponse.getSourceSystem());


        verify(eventRepository, times(1)).findAll();
    }

    //    ======================== getEventProcessingStatus Tests ==========================
    @Test
    void getEventProcessingStatus_Success() {

        EventProcessingLog log2 = EventProcessingLog.builder()
                .id(2L)
                .eventId("evt-123")
                .processorName("PaymentProcessor")
                .status(ProcessingStatus.SUCCESS)
                .attemptCount(1)
                .build();


        when(eventProcessingLogRepository.getByEventId("evt-123")).thenReturn(Arrays.asList(mockLog, log2));

        List<EventProcessingLogResponse> responses = eventPublisherService.getEventProcessingStatus("evt-123");

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("SUCCESS", responses.get(1).getStatus());
        verify(eventProcessingLogRepository, times(1)).getByEventId("evt-123");

    }

    @Test
    void getEventProcessingStatus_NotFound_ThrowsNotFoundException() {
        when(eventProcessingLogRepository.getByEventId("evt-999")).thenReturn(Collections.emptyList());

        EventNotFoundException exception = assertThrows(EventNotFoundException.class, () -> {
            eventPublisherService.getEventProcessingStatus("evt-999");
        });

        assertTrue(exception.getMessage().contains("event not found with id evt-999"));
        verify(eventProcessingLogRepository, times(1)).getByEventId("evt-999");

    }


    //    ======================== UpdateEvent Tests ==========================

    @Test
    void updateEvent_Success() {

        Object payload = new Object() {
            public String orderId = "ORDER-007";
            public String customerId = "CUSTOMER-007";
            public int amount = 100;
        };

        when(eventRepository.getEventByEventId(TEST_EVENT_ID)).thenReturn(Optional.of(mockEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);

        assertDoesNotThrow(() -> eventPublisherService.updateEvent(TEST_EVENT_ID, payload));

        verify(eventRepository, times(1)).getEventByEventId(TEST_EVENT_ID);
        verify(eventRepository, times(1)).save(any(Event.class));

    }

    @Test
    void updateEvent_NotFound_ThrowsNotFoundException() {
        when(eventRepository.getEventByEventId(TEST_EVENT_ID)).thenReturn(Optional.empty());

        Object payload = new Object();

        EventNotFoundException exception = assertThrows(EventNotFoundException.class, () -> {
            eventPublisherService.updateEvent(TEST_EVENT_ID, payload);
        });

        assertTrue(exception.getMessage().contains("event not found with id " + TEST_EVENT_ID));
        verify(eventRepository, times(1)).getEventByEventId(TEST_EVENT_ID);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEvent_JsonProcessingFailure_ThrowsJsonProcessingException() {
        when(eventRepository.getEventByEventId(TEST_EVENT_ID)).thenReturn(Optional.of(mockEvent));

        Object payload = new Object();
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventPublisherService.updateEvent(TEST_EVENT_ID, payload);
        });
        assertTrue(exception.getMessage().contains("Failed to update event payload"));
        verify(eventRepository, times(1)).getEventByEventId(TEST_EVENT_ID);
        verify(eventRepository, never()).save(any(Event.class));
    }


}
