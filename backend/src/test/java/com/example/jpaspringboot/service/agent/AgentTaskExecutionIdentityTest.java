package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.agent.AgentTaskStatus;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.security.IdentityService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AgentTaskExecutionIdentityTest {
    @Test
    void disabledOwnerFailsBeforeGatewayExecutionAndReleasesQuota() {
        AgentTaskService tasks = mock(AgentTaskService.class);
        AgentGatewayClient gateway = mock(AgentGatewayClient.class);
        AgentQuotaService quotas = mock(AgentQuotaService.class);
        IdentityService identities = mock(IdentityService.class);
        AgentTaskExecutionService service = new AgentTaskExecutionService(tasks, gateway, quotas, identities);
        AgentTask queued = queuedTask();
        AgentTask failed = queuedTask();
        failed.setStatus(AgentTaskStatus.FAILED);
        failed.setErrorCode("identity_revoked");
        when(tasks.findForExecution("task-identity-01")).thenReturn(queued);
        when(identities.resolveExpected(AccountRole.PROFESSIONAL, "professional-9", 4L))
                .thenReturn(Optional.empty());
        when(tasks.fail("task-identity-01", "identity_revoked",
                "Task owner is disabled, changed, or no longer authorized")).thenReturn(failed);

        AgentTask result = service.run("task-identity-01");

        assertEquals(AgentTaskStatus.FAILED, result.getStatus());
        verify(quotas).release("quota:task-identity-01");
        verifyNoInteractions(gateway);
        verify(tasks, never()).markRunning(anyString());
    }

    private AgentTask queuedTask() {
        AgentTask task = new AgentTask();
        task.setTaskId("task-identity-01");
        task.setOwnerKey("PROFESSIONAL:9");
        task.setOwnerRole(AccountRole.PROFESSIONAL.name());
        task.setOwnerUsername("professional-9");
        task.setOwnerTokenVersion(4L);
        task.setPasswordChangeRequired(false);
        task.setInputMessage("analyse sample");
        task.setCapability(AgentCapability.PROFESSIONAL_ANALYSIS);
        task.setReservationId("quota:task-identity-01");
        task.setStatus(AgentTaskStatus.QUEUED);
        return task;
    }
}
