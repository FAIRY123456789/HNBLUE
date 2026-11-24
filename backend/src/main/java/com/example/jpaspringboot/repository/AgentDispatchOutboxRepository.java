package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.AgentDispatchOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgentDispatchOutboxRepository extends JpaRepository<AgentDispatchOutbox, String> {
    Optional<AgentDispatchOutbox> findByTaskId(String taskId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AgentDispatchOutbox> findTop50ByStatusInAndAvailableAtBeforeOrderByCreatedAtAsc(
            List<AgentDispatchOutbox.Status> statuses, LocalDateTime availableBefore);
}
