package com.example.jpaspringboot.integration;

import com.example.jpaspringboot.JpAspringbootApplication;
import com.example.jpaspringboot.agent.AgentCapability;
import com.example.jpaspringboot.agent.AgentGatewayContract;
import com.example.jpaspringboot.service.agent.AgentGatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(classes = JpAspringbootApplication.class)
@TestPropertySource(properties = {
        "hnblue.agent.gateway.enabled=true",
        "hnblue.agent.gateway.url=http://127.0.0.1:65534",
        "hnblue.agent.gateway.timeout=250ms",
        "resilience4j.retry.instances.agentService.max-attempts=1"
})
@EnabledIfEnvironmentVariable(named = "HNBLUE_RUN_AGENT_FAILURE_IT", matches = "(?i)true")
class AgentGatewayFailureIntegrationTest {
    @Autowired private AgentGatewayClient client;

    @Test
    void unavailablePythonServiceReturnsBoundedMachineReadableFailure() throws Exception {
        AgentGatewayContract.Request request = new AgentGatewayContract.Request(
                "fault-task-0001", "fault-trace-0001", "PUBLIC:fault-user", "PUBLIC",
                AgentCapability.BASIC_QA, List.of("bluecarbon.knowledge.search"),
                1000, 4, "蓝碳是什么？");
        AgentGatewayContract.Result result = client.execute(request);
        assertFalse(result.success());
        assertEquals("gateway_unavailable", result.errorCode());
    }
}
