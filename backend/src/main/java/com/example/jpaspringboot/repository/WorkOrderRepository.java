package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    Optional<WorkOrder> findByCode(String code);
    List<WorkOrder> findBySubmitterNameOrderByUpdatedAtDesc(String submitterName);
    List<WorkOrder> findByAssignedAdminNameOrderByUpdatedAtDesc(String assignedAdminName);
    List<WorkOrder> findAllByOrderByUpdatedAtDesc();
}
