package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.agent.AgentTaskStatus;
import com.example.jpaspringboot.entity.AgentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AgentTaskRepository extends JpaRepository<AgentTask, String> {
    List<AgentTask> findTop100ByStatusInAndDeadlineBeforeOrderByDeadlineAsc(List<AgentTaskStatus> statuses,
                                                                           LocalDateTime deadline);
    List<AgentTask> findTop100ByStatusInOrderByUpdatedAtAsc(List<AgentTaskStatus> statuses);
}
