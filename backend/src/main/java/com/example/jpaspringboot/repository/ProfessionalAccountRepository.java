package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.ProfessionalAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessionalAccountRepository extends JpaRepository<ProfessionalAccount, Long> {
    Optional<ProfessionalAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    Page<ProfessionalAccount> findByUsernameContainingIgnoreCase(String keyword, Pageable pageable);
}
