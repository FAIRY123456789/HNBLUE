package com.example.jpaspringboot.service.agent;

import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.agent.EntitlementPlan;
import com.example.jpaspringboot.entity.AgentEntitlement;
import com.example.jpaspringboot.security.AccountRole;
import com.example.jpaspringboot.security.HnbluePrincipal;
import com.example.jpaspringboot.service.professional.ProfessionalAccountService;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

@Service
public class AgentEntitlementService {
    private final ProfessionalAccountService professionals;

    public AgentEntitlementService(ProfessionalAccountService professionals) {
        this.professionals = professionals;
    }

    public EntitlementPlan planFor(HnbluePrincipal principal) {
        if (principal.role() == AccountRole.ADMIN) {
            return new EntitlementPlan(EnumSet.allOf(AgentCapability.class), 10_000, 20_000_000,
                    100_000, 16, 100);
        }
        if (principal.role() == AccountRole.PUBLIC) {
            return new EntitlementPlan(EnumSet.of(AgentCapability.BASIC_QA), 20, 40_000,
                    2_000, 1, 1);
        }
        AgentEntitlement item = professionals.entitlementFor(principal.accountId());
        EnumSet<AgentCapability> capabilities = EnumSet.noneOf(AgentCapability.class);
        if (item.isBasicQaEnabled()) capabilities.add(AgentCapability.BASIC_QA);
        if (item.isProfessionalAnalysisEnabled()) capabilities.add(AgentCapability.PROFESSIONAL_ANALYSIS);
        if (item.isMultiAgentEnabled()) capabilities.add(AgentCapability.MULTI_AGENT);
        return new EntitlementPlan(capabilities, item.getDailyRequestLimit(), item.getDailyTokenLimit(),
                item.getMaxTokensPerRequest(), item.getMaxConcurrentTasks(), item.getQueuePriority());
    }
}
