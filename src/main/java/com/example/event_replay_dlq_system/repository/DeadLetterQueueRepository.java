package com.example.event_replay_dlq_system.repository;

import com.example.event_replay_dlq_system.entity.DeadLetterQueue;
import com.example.event_replay_dlq_system.enums.DLQStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeadLetterQueueRepository extends JpaRepository<DeadLetterQueue, Long> {
    List<DeadLetterQueue> findByDlqStatus(DLQStatus dlqStatus);

    Optional<DeadLetterQueue> findByEventId(String eventId);

    void deleteByEventId(String eventId);

    @Modifying
    @Query("DELETE FROM DeadLetterQueue d WHERE d.approximateRetentionTime < :cutoff")
    void deleteByApproximateRetentionTimeBefore(@Param("cutoff") LocalDateTime cutoff);
}
