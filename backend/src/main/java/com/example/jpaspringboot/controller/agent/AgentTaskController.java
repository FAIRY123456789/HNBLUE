package com.example.jpaspringboot.controller.agent;

import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.agent.AgentReviewActionType;
import com.example.jpaspringboot.entity.AgentTask;
import com.example.jpaspringboot.entity.AgentTaskReviewAction;
import com.example.jpaspringboot.security.HnbluePrincipal;
import com.example.jpaspringboot.service.agent.AgentTaskOrchestrator;
import com.example.jpaspringboot.service.agent.AgentTaskService;
import com.example.jpaspringboot.service.agent.QuotaExceededException;
import com.example.jpaspringboot.service.agent.IdempotencyConflictException;
import com.example.jpaspringboot.service.agent.AgentReviewException;
import com.example.jpaspringboot.service.agent.AgentReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/agent/tasks")
public class AgentTaskController {
    private final AgentTaskOrchestrator orchestrator;
    private final AgentTaskService tasks;
    private final AgentReviewService reviews;

    public AgentTaskController(AgentTaskOrchestrator orchestrator, AgentTaskService tasks,
                               AgentReviewService reviews) {
        this.orchestrator = orchestrator;
        this.tasks = tasks;
        this.reviews = reviews;
    }

    @PostMapping
    public ResponseEntity<?> submit(@AuthenticationPrincipal HnbluePrincipal principal,
                                    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                    @Valid @RequestBody SubmitRequest request) {
        AgentTask task = orchestrator.submit(principal, request.capability(), request.tokenBudget(),
                request.timeoutSeconds(), request.message(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(view(task));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<?> status(@PathVariable String taskId,
                                    @AuthenticationPrincipal HnbluePrincipal principal) {
        return ResponseEntity.ok(view(tasks.findAuthorized(taskId, principal)));
    }

    @GetMapping("/review-queue")
    public ResponseEntity<?> reviewQueue(@AuthenticationPrincipal HnbluePrincipal principal) {
        return ResponseEntity.ok(reviews.reviewQueue(principal).stream().map(this::view).toList());
    }

    @PostMapping("/{taskId}/commands")
    public ResponseEntity<?> reviewCommand(
            @PathVariable String taskId,
            @AuthenticationPrincipal HnbluePrincipal principal,
            @RequestHeader("Idempotency-Key") String requestId,
            @Valid @RequestBody ReviewCommandRequest request) {
        if (!requestId.matches("^[a-zA-Z0-9._:-]{8,64}$")) {
            throw new AgentReviewException("invalid_review_request_id", "invalid review idempotency key");
        }
        if ((request.action() == AgentReviewActionType.RETURN
                || request.action() == AgentReviewActionType.REJECT)
                && (request.comment() == null || request.comment().isBlank())) {
            throw new AgentReviewException("review_comment_required", "return and reject require a comment");
        }
        return ResponseEntity.ok(view(reviews.review(
                taskId, requestId, request.action(), request.comment(), request.reviewCycle(),
                request.expectedVersion(), principal)));
    }

    @GetMapping("/{taskId}/reviews")
    public ResponseEntity<?> reviewHistory(@PathVariable String taskId,
                                           @AuthenticationPrincipal HnbluePrincipal principal) {
        return ResponseEntity.ok(reviews.history(taskId, principal).stream()
                .map(this::reviewView).toList());
    }

    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String taskId,
                                    @AuthenticationPrincipal HnbluePrincipal principal) {
        return ResponseEntity.ok(view(orchestrator.cancel(taskId, principal)));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<?> quota(QuotaExceededException ex) {
        HttpStatus status = "capability_forbidden".equals(ex.getCode()) || "password_change_required".equals(ex.getCode())
                ? HttpStatus.FORBIDDEN : HttpStatus.TOO_MANY_REQUESTS;
        return ResponseEntity.status(status).body(Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> forbidden(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("code", "task_forbidden"));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<?> idempotencyConflict(IdempotencyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "idempotency_conflict", "message", ex.getMessage()));
    }

    @ExceptionHandler(AgentReviewException.class)
    public ResponseEntity<?> reviewFailure(AgentReviewException ex) {
        HttpStatus status = ex.getCode().contains("forbidden") || ex.getCode().contains("role")
                ? HttpStatus.FORBIDDEN
                : ex.getCode().contains("gateway") ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(Map.of("code", ex.getCode(), "message", ex.getMessage()));
    }

    private TaskView view(AgentTask task) {
        return new TaskView(task.getTaskId(), task.getTraceId(), task.getCapability(), task.getStatus(),
                task.getRequestedTokens(), task.getActualTokens(), task.getPriority(), task.getAttempts(),
                task.getDeadline(), task.getResultJson(), task.getErrorCode(), task.getErrorMessage(),
                task.getReviewLevel(), task.getReviewCycle(), task.getVersion(),
                task.getCreatedAt(), task.getUpdatedAt());
    }

    private ReviewActionView reviewView(AgentTaskReviewAction action) {
        return new ReviewActionView(action.getRequestId(), action.getTaskId(), action.getAction(),
                action.getReviewerKey(), action.getReviewerRole(), action.getReviewCycle(),
                action.getFromStatus(), action.getToStatus(), action.getComment(), action.getCreatedAt());
    }

    public record SubmitRequest(@NotNull AgentCapability capability,
                                @Min(1) @Max(200000) int tokenBudget,
                                @Min(5) @Max(600) int timeoutSeconds,
                                @NotBlank @Size(max = 8000) String message) {}

    public record ReviewCommandRequest(@NotNull AgentReviewActionType action,
                                       @Size(max = 2000) String comment,
                                       @Min(0) @Max(8) int reviewCycle,
                                       @Min(0) long expectedVersion) {}

    public record TaskView(String taskId, String traceId, AgentCapability capability,
                           com.example.jpaspringboot.agent.AgentTaskStatus status,
                           int requestedTokens, Integer actualTokens, int priority, int attempts,
                           java.time.LocalDateTime deadline, String resultJson,
                           String errorCode, String errorMessage, String reviewLevel,
                           int reviewCycle, long version,
                           java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {}

    public record ReviewActionView(String requestId, String taskId, AgentReviewActionType action,
                                   String reviewerKey, String reviewerRole, int reviewCycle,
                                   com.example.jpaspringboot.agent.AgentTaskStatus fromStatus,
                                   com.example.jpaspringboot.agent.AgentTaskStatus toStatus,
                                   String comment, java.time.LocalDateTime createdAt) {}
}
