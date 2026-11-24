from __future__ import annotations

import json

import httpx

from hnblue_agent.graph import AgentHarness
from hnblue_agent.models import Capability, Role, TaskContext
from hnblue_agent.providers import (LocalRouterShadowClient, RuleModelProvider,
                                    ShadowModelProvider)
from hnblue_agent.tracing import TraceStore


def context(capability: Capability = Capability.BASIC_QA) -> TaskContext:
    return TaskContext(
        task_id="shadow-task-0001",
        trace_id="shadow-trace-0001",
        subject_key="PUBLIC:shadow",
        role=Role.PUBLIC,
        capability=capability,
        allowed_tools=frozenset({"bluecarbon.knowledge.search"}),
        token_budget=2000,
        max_steps=4,
    )


def response(intent: str, *, clarification: bool = False) -> dict[str, object]:
    return {
        "apiVersion": "2026-10-20.v2",
        "requestId": "shadow-trace-0001",
        "outcome": "ROUTED",
        "intent": intent,
        "abstained": False,
        "clarificationNeeded": clarification,
        "requiredFields": ["region"] if clarification else [],
        "suggestedCapability": "PUBLIC_KNOWLEDGE_READ",
        "modelId": "hnblue-route-v2-lora-s3407-merged",
        "modelCalled": True,
        "latencyMs": 12.5,
        "confidence": 0.91,
        "policy": {"decision": "ALLOW", "ruleId": "LOCAL_ROUTER_ALLOW_V1", "reason": "test"},
        "warnings": [],
    }


def test_shadow_candidate_receives_same_role_and_text_but_cannot_change_active_route() -> None:
    captured: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured.update(json.loads(request.content))
        return httpx.Response(200, json=response("MONITORING_STATS"))

    provider = ShadowModelProvider(
        active=RuleModelProvider(),
        candidate=LocalRouterShadowClient(transport=httpx.MockTransport(handler)),
    )
    decision = provider.route(context(), "请解释蓝碳是什么", tools=[])

    assert decision.route == "education"
    assert decision.tool_calls == ()
    assert captured["text"] == "请解释蓝碳是什么"
    assert captured["userRole"] == "PUBLIC"
    assert decision.routing_audit is not None
    assert decision.routing_audit.activeRoute == "education"
    assert decision.routing_audit.candidateRoute == "monitoring"
    assert decision.routing_audit.matched is False
    assert decision.routing_audit.authority == "JAVA_POLICY_AND_EXISTING_RULES"


def test_shadow_candidate_failure_falls_back_to_existing_route() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("candidate unavailable", request=request)

    provider = ShadowModelProvider(
        active=RuleModelProvider(),
        candidate=LocalRouterShadowClient(transport=httpx.MockTransport(handler)),
    )
    decision = provider.route(context(), "请解释蓝碳是什么", tools=[])

    assert decision.route == "education"
    assert decision.routing_audit is not None
    assert decision.routing_audit.candidateAvailable is False
    assert decision.routing_audit.errorCode == "ConnectError"


def test_shadow_candidate_timeout_keeps_existing_route_authoritative() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ReadTimeout("candidate timed out", request=request)

    provider = ShadowModelProvider(
        active=RuleModelProvider(),
        candidate=LocalRouterShadowClient(transport=httpx.MockTransport(handler)),
    )
    decision = provider.route(context(), "请解释蓝碳是什么", tools=[])

    assert decision.route == "education"
    assert decision.routing_audit is not None
    assert decision.routing_audit.authority == "JAVA_POLICY_AND_EXISTING_RULES"
    assert decision.routing_audit.candidateAvailable is False
    assert decision.routing_audit.errorCode == "ReadTimeout"


def test_shadow_audit_flows_through_harness_without_changing_executed_tool(tmp_path) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json=response("EDUCATION"))

    provider = ShadowModelProvider(
        active=RuleModelProvider(),
        candidate=LocalRouterShadowClient(transport=httpx.MockTransport(handler)),
    )
    harness = AgentHarness(provider=provider, trace_store=TraceStore(tmp_path / "trace.jsonl"))
    from hnblue_agent.models import AgentRequest

    result = harness.execute(AgentRequest(
        taskId="shadow-task-0001",
        traceId="shadow-trace-0001",
        subjectKey="PUBLIC:shadow",
        role="PUBLIC",
        capability="BASIC_QA",
        allowedTools=["bluecarbon.knowledge.search"],
        tokenBudget=2000,
        maxSteps=4,
        message="请解释蓝碳是什么",
    ))

    assert result.success is True
    assert result.routingAudit is not None
    metadata = harness.execution_metadata("shadow-task-0001")
    assert metadata["route"] == "education"
    assert metadata["tools"] == ["bluecarbon.knowledge.search"]
    assert metadata["routingAudit"]["candidateModelId"] == "hnblue-route-v2-lora-s3407-merged"
