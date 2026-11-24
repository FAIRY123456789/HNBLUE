package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.agent.AgentTaskStatus;
import com.example.jpaspringboot.entity.AgentDispatchOutbox;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.repository.AgentDispatchOutboxRepository;
import com.example.jpaspringboot.security.HnbluePrincipal;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.Range;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

@Service
@ConditionalOnProperty(name = "hnblue.agent.dispatch.mode", havingValue = "redis-stream")
public class RedisAgentDispatchService {
    private final AgentTaskService tasks;
    private final AgentTaskExecutionService execution;
    private final AgentQuotaService quotas;
    private final AgentDispatchOutboxRepository outboxes;
    private final StringRedisTemplate redis;
    private final String stream;
    private final String group;
    private final String consumer;
    private final Duration retryAfter;
    private final int maxAttempts;

    public RedisAgentDispatchService(AgentTaskService tasks, AgentTaskExecutionService execution,
                                     AgentQuotaService quotas, AgentDispatchOutboxRepository outboxes,
                                     StringRedisTemplate redis,
                                     @Value("${hnblue.agent.redis.stream:hnblue:agent:tasks}") String stream,
                                     @Value("${hnblue.agent.redis.group:hnblue-agent-workers}") String group,
                                     @Value("${hnblue.agent.redis.consumer:${HOSTNAME:local}-worker}") String consumer,
                                     @Value("${hnblue.agent.redis.retry-after:60s}") Duration retryAfter,
                                     @Value("${hnblue.agent.redis.max-attempts:3}") int maxAttempts) {
        this.tasks = tasks;
        this.execution = execution;
        this.quotas = quotas;
        this.outboxes = outboxes;
        this.redis = redis;
        this.stream = stream;
        this.group = group;
        this.consumer = consumer;
        this.retryAfter = retryAfter;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @PostConstruct
    void createConsumerGroup() {
        try {
            redis.opsForStream().createGroup(stream, ReadOffset.from("0-0"), group);
        } catch (RuntimeException ex) {
            // Spring Data wraps Redis' BUSYGROUP response in RedisSystemException,
            // whose top-level message is only "Error in execution".  Inspect the
            // complete cause chain so an existing group remains an idempotent
            // startup condition while every other Redis failure still aborts.
            if (!hasMessageInCauseChain(ex, "BUSYGROUP")) throw ex;
        }
    }

    private boolean hasMessageInCauseChain(Throwable failure, String marker) {
        Throwable current = failure;
        while (current != null) {
            if (String.valueOf(current.getMessage()).contains(marker)) return true;
            current = current.getCause();
        }
        return false;
    }

    @Transactional
    public AgentTask createQueued(String taskId, String traceId, HnbluePrincipal owner,
                                  AgentCapability capability, String reservationId, int tokens,
                                  int priority, LocalDateTime deadline, String message) {
        AgentTask task = tasks.create(taskId, traceId, owner, capability, reservationId,
                tokens, priority, deadline, message);
        AgentDispatchOutbox outbox = new AgentDispatchOutbox();
        outbox.setEventId(UUID.randomUUID().toString());
        outbox.setTaskId(taskId);
        // The outbox is transport metadata only. Identity and business input
        // remain in the access-controlled task row and are revalidated on consume.
        outbox.setPayloadJson("{\"schemaVersion\":1}");
        outbox.setStatus(AgentDispatchOutbox.Status.PENDING);
        outbox.setAvailableAt(LocalDateTime.now());
        outboxes.save(outbox);
        return task;
    }

    @Scheduled(fixedDelayString = "${hnblue.agent.redis.publish-ms:250}")
    @Transactional
    public void publishAvailable() {
        List<AgentDispatchOutbox> available = outboxes
                .findTop50ByStatusInAndAvailableAtBeforeOrderByCreatedAtAsc(
                        List.of(AgentDispatchOutbox.Status.PENDING, AgentDispatchOutbox.Status.PUBLISHED),
                        LocalDateTime.now());
        for (AgentDispatchOutbox outbox : available) {
            if (outbox.getStatus() == AgentDispatchOutbox.Status.PUBLISHED) {
                AgentTask recovered = tasks.prepareRetry(outbox.getTaskId(), maxAttempts);
                if (recovered.getStatus() == AgentTaskStatus.FAILED) {
                    quotas.release(recovered.getReservationId());
                    outbox.setStatus(AgentDispatchOutbox.Status.DEAD);
                    outbox.setLastError("dispatch retries exhausted");
                    outboxes.save(outbox);
                    continue;
                }
                if (recovered.getStatus() != AgentTaskStatus.QUEUED) {
                    outbox.setStatus(AgentDispatchOutbox.Status.CONSUMED);
                    outbox.setConsumedAt(LocalDateTime.now());
                    outboxes.save(outbox);
                    continue;
                }
            }
            MapRecord<String, String, String> record = StreamRecords.mapBacked(Map.of(
                    "eventId", outbox.getEventId(), "taskId", outbox.getTaskId()))
                    .withStreamKey(stream);
            redis.opsForStream().add(record);
            outbox.setAttempts(outbox.getAttempts() + 1);
            outbox.setStatus(AgentDispatchOutbox.Status.PUBLISHED);
            outbox.setPublishedAt(LocalDateTime.now());
            outbox.setAvailableAt(LocalDateTime.now().plus(retryAfter));
            outbox.setLastError(null);
            outboxes.save(outbox);
        }
    }

    @Scheduled(fixedDelayString = "${hnblue.agent.redis.consume-ms:250}")
    public void consumeAvailable() {
        processRecords(reclaimStalePending());
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                Consumer.from(group, consumer), StreamReadOptions.empty().count(10),
                StreamOffset.create(stream, ReadOffset.lastConsumed()));
        processRecords(records);
    }

    private List<MapRecord<String, Object, Object>> reclaimStalePending() {
        PendingMessages pending = redis.opsForStream().pending(stream, group, Range.unbounded(), 10);
        if (pending == null || pending.isEmpty()) return List.of();
        List<RecordId> stale = new ArrayList<>();
        for (PendingMessage message : pending) {
            if (message.getElapsedTimeSinceLastDelivery().compareTo(retryAfter) >= 0) {
                stale.add(message.getId());
            }
        }
        if (stale.isEmpty()) return List.of();
        List<MapRecord<String, Object, Object>> claimed = redis.opsForStream().claim(
                stream, group, consumer, retryAfter, stale.toArray(RecordId[]::new));
        return claimed == null ? List.of() : claimed;
    }

    private void processRecords(List<MapRecord<String, Object, Object>> records) {
        if (records == null) return;
        for (MapRecord<String, Object, Object> record : records) {
            String eventId = String.valueOf(record.getValue().get("eventId"));
            AgentDispatchOutbox outbox = outboxes.findById(eventId).orElse(null);
            if (outbox == null) {
                redis.opsForStream().acknowledge(stream, group, record.getId());
                continue;
            }
            try {
                AgentTask result = execution.run(outbox.getTaskId());
                if (isTerminal(result.getStatus())) {
                    markConsumed(outbox.getEventId());
                    redis.opsForStream().acknowledge(stream, group, record.getId());
                }
            } catch (Exception ex) {
                markError(outbox.getEventId(), ex);
            }
        }
    }

    @Transactional
    void markConsumed(String eventId) {
        AgentDispatchOutbox outbox = outboxes.findById(eventId).orElseThrow();
        outbox.setStatus(AgentDispatchOutbox.Status.CONSUMED);
        outbox.setConsumedAt(LocalDateTime.now());
        outbox.setLastError(null);
        outboxes.save(outbox);
    }

    @Transactional
    void markError(String eventId, Exception ex) {
        outboxes.findById(eventId).ifPresent(outbox -> {
            String message = ex.getClass().getSimpleName() + ": " + String.valueOf(ex.getMessage());
            outbox.setLastError(message.length() <= 600 ? message : message.substring(0, 600));
            outboxes.save(outbox);
        });
    }

    private boolean isTerminal(AgentTaskStatus status) {
        return List.of(AgentTaskStatus.SUCCEEDED, AgentTaskStatus.PARTIAL,
                AgentTaskStatus.FAILED, AgentTaskStatus.CANCELLED).contains(status);
    }

}
