package com.example.jpaspringboot.service.impl;

import com.example.jpaspringboot.entity.UserActivityLog;
import com.example.jpaspringboot.repository.UserActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserActivityLogServiceImpl {
    @Autowired
    private UserActivityLogRepository repository;

    public void record(Integer actorUserId, String actorName, String actorRole, Integer targetUserId, String actionType, String actionDescription, String requestPath, String resultStatus, String ipAddress) {
        UserActivityLog log = new UserActivityLog();
        log.setActorUserId(actorUserId);
        log.setActorName(actorName);
        log.setActorRole(actorRole);
        log.setTargetUserId(targetUserId);
        log.setActionType(actionType);
        log.setActionDescription(actionDescription);
        log.setRequestPath(requestPath);
        log.setResultStatus(resultStatus);
        log.setIpAddress(ipAddress);
        log.setOccurredAt(LocalDateTime.now());
        repository.save(log);
    }

    public List<UserActivityLog> recentForUser(Integer userId, int limit) {
        return repository.findByTargetUserIdOrderByOccurredAtDesc(userId, PageRequest.of(0, Math.max(1, limit)));
    }
}
