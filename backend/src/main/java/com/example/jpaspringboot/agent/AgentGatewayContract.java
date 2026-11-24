package com.example.jpaspringboot.agent;

import java.util.List;
import java.util.Map;

public final class AgentGatewayContract {
    private AgentGatewayContract() {}

    public record Request(String taskId, String traceId, String subjectKey, String role,
                          AgentCapability capability, List<String> allowedTools,
                          int tokenBudget, int maxSteps, String message) {}

    public record ReviewCommand(String taskId, String traceId, String requestId,
                                AgentReviewActionType action, String reviewerSubjectKey,
                                String reviewerRole, String comment, int reviewCycle) {}

    public record Command(String action, Request request, ReviewCommand review) {
        public static Command execute(Request request) {
            return new Command("EXECUTE", request, null);
        }

        public static Command review(ReviewCommand review) {
            return new Command(review.action().name(), null, review);
        }

        public String traceId() {
            return request != null ? request.traceId() : review.traceId();
        }
    }

    public record ReviewRequest(String level, int cycle, String taskId, String traceId,
                                String draft, String evidenceStatus, int sourceCount,
                                List<String> reasons, List<String> allowedActions) {}

    public record RoutingAudit(String mode, String authority, String activeProvider,
                               String activeRoute, String candidateProvider,
                               boolean candidateAvailable, String candidateRoute,
                               Boolean matched, String outcome, String intent,
                               String suggestedCapability, String policyDecision,
                               Boolean clarificationNeeded, List<String> requiredFields,
                               Double confidence, String candidateModelId,
                               Double candidateLatencyMs, String errorCode) {}

    public record Result(boolean success, boolean partial, String output,
                         List<Map<String, Object>> sources, int tokenUsage,
                          String errorCode, String errorMessage, RoutingAudit routingAudit,
                          AgentExecutionStatus executionStatus, ReviewRequest reviewRequest,
                          Map<String, Object> collaboration) {
        public static Result failed(String code, String message) {
            return new Result(false, false, null, List.of(), 0, code, message, null,
                    AgentExecutionStatus.COMPLETED, null, Map.of());
        }
    }
}
