package com.example.event_replay_dlq_system.specification;

import com.example.event_replay_dlq_system.entity.Event;
import com.example.event_replay_dlq_system.entity.EventFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EventSpecification {
    public static Specification<Event> byFilter(EventFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();


            if (filter.getEventIds() != null && !filter.getEventIds().isEmpty()) {
                predicates.add(root.get("eventId").in(filter.getEventIds()));
            }

            if (filter.getEventType() != null && !filter.getEventType().isEmpty()) {
                predicates.add(root.get("eventType").in(filter.getEventType()));
            }

            if (filter.getFromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate()));
            }


            if (filter.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.getToDate()));
            }

            if (filter.getSourceSystem() != null && !filter.getSourceSystem().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("sourceSystem"), filter.getSourceSystem()));
            }

            if (filter.getCorrelationId() != null && !filter.getCorrelationId().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("correlationId"), filter.getCorrelationId()));
            }


            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

    }
}
