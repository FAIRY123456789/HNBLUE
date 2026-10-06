package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.SystemSecret;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemSecretRepository extends JpaRepository<SystemSecret, Long> {
    Optional<SystemSecret> findBySecretName(String secretName);
    boolean existsBySecretName(String secretName);
}
