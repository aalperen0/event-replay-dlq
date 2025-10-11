package com.example.event_replay_dlq_system.processor.impl;

import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.exception.ProcessingException;
import com.example.event_replay_dlq_system.processor.EventProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentEventProcessor implements EventProcessor {
    private final ObjectMapper objectMapper;

    public PaymentEventProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Event event) throws ProcessingException {
        if (!canProcess(event.getEventType())) {
            log.warn("Unsupported event type {}", event.getEventType());
            return;
        }
        try {

            JsonNode paymentJson = objectMapper.readTree(event.getPayload());
            double paymentAmount = paymentJson.get("amount").asDouble();
            String paymentMethod = paymentJson.get("payment_method").asText();
            String paymentId = paymentJson.get("paymentId").asText();

            if (paymentMethod.equals("expired_card")) {
                throw new ProcessingException("Payment method is expired");
            }
            if (paymentAmount > 5000) {
                throw new ProcessingException("Payment amount exceeds 5000");
            }

            /*
                SIMULATING PAYMENT PROCESS
             */
            log.info("Payment status updated for payment {} with an amount of {}", paymentId, paymentAmount);
            Thread.sleep(200);
            log.info("Successfully processed payment Event {}", paymentId);

        } catch (ProcessingException ex) {
            log.error("Processing payment event failed {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error for Payment Event {}", event, ex);
            throw new ProcessingException("Unexpected error for Payment Event");
        }
    }

    @Override
    public String getProcessorName() {
        return "PaymentEventProcessor";
    }

    @Override
    public boolean canProcess(String eventType) {
        return eventType.equals("PaymentProcessed") ||
                eventType.equals("PaymentCancelled");
    }
}

//{
//        "eventType": "PaymentProcessed",
//        "payload": "{\"paymentId\": \"PAY-001\", \"orderId\": \"ORD-001\", \"amount\": 99.99, \"paymentMethod\": \"retry_test\"}",
//        "sourceSystem": "PaymentService",
//        "correlationId": "order-session-123"
//        }