package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.WorkOrderActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderActionLogRepository extends JpaRepository<WorkOrderActionLog, Long> {
    List<WorkOrderActionLog> findByWorkOrderIdOrderByCreatedAtAsc(Long workOrderId);
}
