package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.agent.AgentGatewayContract;
import com.example.jpaspringboot.agent.AgentTaskStatus;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.security.HnbluePrincipal;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.security.IdentityService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentTaskExecutionService {
    private final AgentTaskService tasks;
    private final AgentGatewayClient gateway;
    private final AgentQuotaService quotas;
    private final IdentityService identities;

    public AgentTaskExecutionService(AgentTaskService tasks, AgentGatewayClient gateway,
                                     AgentQuotaService quotas, IdentityService identities) {
        this.tasks = tasks;
        this.gateway = gateway;
        this.quotas = quotas;
        this.identities = identities;
    }

    public AgentTask run(String taskId) {
        AgentTask submitted = tasks.findForExecution(taskId);
        HnbluePrincipal principal;
        try {
            AccountRole expectedRole = AccountRole.valueOf(submitted.getOwnerRole());
            principal = identities.resolveExpected(expectedRole, submitted.getOwnerUsername(),
                            submitted.getOwnerTokenVersion())
                    .filter(candidate -> candidate.subjectKey().equals(submitted.getOwnerKey()))
                    .filter(candidate -> !candidate.passwordChangeRequired())
                    .orElse(null);
        } catch (IllegalArgumentException ex) {
            principal = null;
        }
        if (principal == null) {
            AgentTask failed = tasks.fail(taskId, "identity_revoked",
                    "Task owner is disabled, changed, or no longer authorized");
            quotas.release(submitted.getReservationId());
            return failed;
        }
        if (submitted.getInputMessage() == null || submitted.getInputMessage().isBlank()) {
            AgentTask failed = tasks.fail(taskId, "durable_payload_missing",
                    "Durable task input is unavailable");
            quotas.release(submitted.getReservationId());
            return failed;
        }
        String message = submitted.getInputMessage();
        AgentTask running = tasks.markRunning(taskId);
        if (running.getStatus() != AgentTaskStatus.RUNNING) return running;
        try {
            AgentGatewayContract.Request request = new AgentGatewayContract.Request(
                    running.getTaskId(), running.getTraceId(), principal.subjectKey(), principal.role().name(),
                    running.getCapability(), allowedTools(running.getCapability()), running.getRequestedTokens(),
                    running.getCapability() == AgentCapability.MULTI_AGENT ? 8 : 4, message);
            AgentGatewayContract.Result result = gateway.execute(request);
            AgentTask completed = tasks.complete(running.getTaskId(), result);
            if (completed.getStatus() == AgentTaskStatus.SUCCEEDED
                    || completed.getStatus() == AgentTaskStatus.PARTIAL
                    || completed.getStatus() == AgentTaskStatus.ARCHIVED) {
                quotas.settle(running.getReservationId(), result.tokenUsage());
            } else if (completed.getStatus() != AgentTaskStatus.AWAITING_REVIEW
                    && completed.getStatus() != AgentTaskStatus.AWAITING_ARCHIVE) {
                quotas.release(running.getReservationId());
            }
            return completed;
        } catch (Exception ex) {
            AgentTask failed = tasks.fail(running.getTaskId(), "execution_failed", ex.getClass().getSimpleName());
            quotas.release(running.getReservationId());
            return failed;
        }
    }

    private List<String> allowedTools(AgentCapability capability) {
        return switch (capability) {
            case BASIC_QA -> List.of("bluecarbon.knowledge.search");
            case PROFESSIONAL_ANALYSIS -> List.of("bluecarbon.knowledge.search", "bluecarbon.region.metrics.read", "bluecarbon.source.verify");
            case MULTI_AGENT -> List.of("bluecarbon.knowledge.search", "bluecarbon.region.metrics.read", "bluecarbon.methodology.lookup", "bluecarbon.source.verify");
        };
    }
}
