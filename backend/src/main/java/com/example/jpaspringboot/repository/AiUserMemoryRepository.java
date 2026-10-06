package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.AiUserMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AiUserMemoryRepository extends JpaRepository<AiUserMemory, Long> {
    List<AiUserMemory> findTop20ByOwnerKeyOrderByUpdatedAtDesc(String ownerKey);
    Optional<AiUserMemory> findByOwnerKeyAndMemoryKey(String ownerKey, String memoryKey);
}
