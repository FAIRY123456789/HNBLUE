package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.AgentTaskReviewAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentTaskReviewActionRepository extends JpaRepository<AgentTaskReviewAction, Long> {
    Optional<AgentTaskReviewAction> findByRequestId(String requestId);
    List<AgentTaskReviewAction> findByTaskIdOrderByCreatedAtAsc(String taskId);
}
