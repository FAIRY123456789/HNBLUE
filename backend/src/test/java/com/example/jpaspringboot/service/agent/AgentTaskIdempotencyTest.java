package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.repository.AgentTaskRepository;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.security.HnbluePrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentTaskIdempotencyTest {
    private final AgentTaskRepository repository = mock(AgentTaskRepository.class);
    private final AgentTaskService service = new AgentTaskService(repository, new ObjectMapper());
    private final HnbluePrincipal owner = new HnbluePrincipal(
            42L, "professional-42", AccountRole.PROFESSIONAL, 3, false);

    @Test
    void sameKeyAndPayloadReturnsExistingTask() {
        AgentTask existing = existingTask();
        when(repository.findById("stable-key-0001")).thenReturn(Optional.of(existing));

        AgentTask resolved = service.findIdempotentSubmission("stable-key-0001", owner,
                AgentCapability.PROFESSIONAL_ANALYSIS, 1200, "analyse plot A").orElseThrow();

        assertSame(existing, resolved);
    }

    @Test
    void sameKeyWithDifferentPayloadIsAConflict() {
        when(repository.findById("stable-key-0001")).thenReturn(Optional.of(existingTask()));

        assertThrows(IdempotencyConflictException.class,
                () -> service.findIdempotentSubmission("stable-key-0001", owner,
                        AgentCapability.PROFESSIONAL_ANALYSIS, 1200, "different input"));
    }

    @Test
    void sameKeyCannotCrossAccountBoundary() {
        when(repository.findById("stable-key-0001")).thenReturn(Optional.of(existingTask()));
        HnbluePrincipal other = new HnbluePrincipal(
                43L, "professional-43", AccountRole.PROFESSIONAL, 0, false);

        assertThrows(IdempotencyConflictException.class,
                () -> service.findIdempotentSubmission("stable-key-0001", other,
                        AgentCapability.PROFESSIONAL_ANALYSIS, 1200, "analyse plot A"));
    }

    private AgentTask existingTask() {
        AgentTask task = new AgentTask();
        task.setTaskId("stable-key-0001");
        task.setOwnerKey(owner.subjectKey());
        task.setCapability(AgentCapability.PROFESSIONAL_ANALYSIS);
        task.setRequestedTokens(1200);
        task.setInputMessage("analyse plot A");
        return task;
    }
}
