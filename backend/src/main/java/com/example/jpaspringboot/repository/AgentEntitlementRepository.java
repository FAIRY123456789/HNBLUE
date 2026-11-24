package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.AgentEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentEntitlementRepository extends JpaRepository<AgentEntitlement, Long> {
    Optional<AgentEntitlement> findByProfessionalAccountId(Long professionalAccountId);
}
