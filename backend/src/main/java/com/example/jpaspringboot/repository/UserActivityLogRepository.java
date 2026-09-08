package com.example.jpaspringboot.repository;

import com.example.jpaspringboot.entity.UserActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {
    List<UserActivityLog> findByTargetUserIdOrderByOccurredAtDesc(Integer targetUserId, Pageable pageable);
}
