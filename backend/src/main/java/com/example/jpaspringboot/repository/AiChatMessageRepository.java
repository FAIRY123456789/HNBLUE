package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    List<AiChatMessage> findTop20ByOwnerKeyAndSessionIdOrderByCreatedAtDescIdDesc(String ownerKey, String sessionId);
    List<AiChatMessage> findTop200ByOwnerKeyAndSessionIdOrderByCreatedAtAscIdAsc(String ownerKey, String sessionId);
    long deleteByOwnerKeyAndSessionId(String ownerKey, String sessionId);
}
