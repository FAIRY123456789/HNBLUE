import pytest
from fastapi.testclient import TestClient

from hnblue_agent.api import create_app
from hnblue_agent.graph import AgentHarness
from hnblue_agent.tracing import TraceStore


def payload(task_id: str = "task-api-0001") -> dict:
    return {
        "taskId": task_id,
        "traceId": "trace-api-0001",
        "subjectKey": "PUBLIC:api-user",
        "role": "PUBLIC",
        "capability": "BASIC_QA",
        "allowedTools": ["bluecarbon.knowledge.search"],
        "tokenBudget": 1200,
        "maxSteps": 4,
        "message": "请解释蓝碳生态系统是什么？",
    }


def test_health_and_java_contract(tmp_path) -> None:
    client = TestClient(create_app(AgentHarness(trace_store=TraceStore(tmp_path / "api-traces.jsonl"))))
    assert client.get("/health").json()["ok"] is True
    response = client.post("/v1/agent/tasks/execute", json=payload(), headers={"X-Trace-Id": "trace-api-0001"})
    assert response.status_code == 200
    body = response.json()
    assert set(body) == {"success", "partial", "output", "sources", "tokenUsage", "errorCode",
                         "errorMessage", "routingAudit", "executionStatus", "reviewRequest",
                         "collaboration"}
    assert body["routingAudit"] is None
    assert body["success"] is True


def test_trace_mismatch_and_internal_token(tmp_path, monkeypatch) -> None:
    monkeypatch.setenv("HNBLUE_AGENT_INTERNAL_TOKEN", "integration-secret")
    client = TestClient(create_app(AgentHarness(trace_store=TraceStore(tmp_path / "auth-traces.jsonl"))))
    assert client.post("/v1/agent/tasks/execute", json=payload()).status_code == 401
    assert client.post(
        "/v1/agent/tasks/execute", json=payload(),
        headers={"X-HNBLUE-Internal-Token": "integration-secret"},
    ).status_code == 403
    headers = {
        "X-HNBLUE-Internal-Token": "integration-secret",
        "X-HNBLUE-Service-Scope": "agent.execute",
        "X-Trace-Id": "wrong",
    }
    assert client.post("/v1/agent/tasks/execute", json=payload(), headers=headers).status_code == 400
    headers["X-Trace-Id"] = "trace-api-0001"
    assert client.post("/v1/agent/tasks/execute", json=payload("task-api-auth-0001"), headers=headers).status_code == 200


def test_unknown_fields_are_rejected(tmp_path) -> None:
    client = TestClient(create_app(AgentHarness(trace_store=TraceStore(tmp_path / "schema-traces.jsonl"))))
    body = payload("task-api-schema-0001")
    body["admin"] = True
    assert client.post("/v1/agent/tasks/execute", json=body).status_code == 422


def test_required_internal_token_fails_closed_at_startup(tmp_path, monkeypatch) -> None:
    monkeypatch.setenv("HNBLUE_AGENT_REQUIRE_INTERNAL_TOKEN", "true")
    monkeypatch.delenv("HNBLUE_AGENT_INTERNAL_TOKEN", raising=False)
    with pytest.raises(RuntimeError, match="HNBLUE_AGENT_INTERNAL_TOKEN"):
        create_app(AgentHarness(trace_store=TraceStore(tmp_path / "required-token-traces.jsonl")))
