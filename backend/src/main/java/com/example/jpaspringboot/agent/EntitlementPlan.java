package com.example.jpaspringboot.agent;

import java.util.Set;

public record EntitlementPlan(Set<AgentCapability> capabilities,
                              int dailyRequestLimit,
                              long dailyTokenLimit,
                              int maxTokensPerRequest,
                              int maxConcurrentTasks,
                              int queuePriority) {
    public boolean permits(AgentCapability capability) {
        return capabilities.contains(capability);
    }
}
