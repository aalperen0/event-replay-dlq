package com.example.event_replay_dlq_system.processor.impl;


import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.exception.ProcessingException;
import com.example.event_replay_dlq_system.processor.EventProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * ------ THIS PROCESSOR IS JUST FOR A SIMULATING PROCESS !
 * Processing events based on Orders
 * If order processor can process the job,
 * Check the json payload
 * If everything is valid simulate it.
 * Else
 */

@Slf4j
@Service
public class OrderEventProcessor implements EventProcessor {

    private final ObjectMapper objectMapper;
    private Map<String, Integer> attemptTracker = new ConcurrentHashMap<>();

    public OrderEventProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canProcess(String eventType) {
        return eventType.equals("OrderCreated") ||
                eventType.equals("OrderCancelled") ||
                eventType.equals("OrderUpdated");
    }

    @Override
    public void process(Event event) throws ProcessingException {
        if (!canProcess(event.getEventType())) {
            log.warn("Unsupported event type: {} ", event.getEventType());
            return;
        }

        try {
            log.info("Processing order event: {} with correlationID: {}", event, event.getCorrelationId());

            JsonNode orderJson = objectMapper.readTree(event.getPayload());
            String orderId = orderJson.get("orderId").asText();
            String customerId = orderJson.get("customerId").asText();
            double amount = orderJson.get("amount").asDouble();
            log.debug("--> Amount received: {}", amount);

            if (amount <= 0) {
                throw new ProcessingException("Amount must be greater than or equal to 0");
            }
            if (amount > 100000) {
                throw new ProcessingException("Order flagged as potential fraud(CHECK IT)");
            }
            if (orderId == null || orderId.isBlank()) {
                throw new ProcessingException("Invalid ORDER_ID: Order ID is null or blank");
            }
            if (customerId == null || customerId.isBlank()) {
                throw new ProcessingException("Invalid CUSTOMER_ID: CUSTOMER ID is null or blank");
            }

            if (orderId.contains("retry-test")){
                int attempts = attemptTracker.compute(orderId, (key, value) -> value == null ? 1 : value + 1);
                if (attempts < 3){

                    throw new ProcessingException("Temporary failure simulated (attempt " + attempts + ")");
                }

                attemptTracker.remove(orderId);
                log.info("Succeeded on attempt 3");
            }


            /*
                SIMULATING ORDER PROCESS
             */
            log.info("Saving order {} for customer {} with an amount of {}", orderId, customerId, amount);
            Thread.sleep(200);
            log.info("Successfully processed event {}", orderId);

        } catch (ProcessingException ex) {
            log.error("Order processing failed {}", ex.getMessage());
            throw ex;
        } catch (Exception e) {
            log.error("Unexpected error processing order event {}", e.getMessage());
            throw new ProcessingException("Unexpected error while processing order event");
        }

    }

    @Override
    public String getProcessorName() {
        return "OrderEventProcessor";
    }
}