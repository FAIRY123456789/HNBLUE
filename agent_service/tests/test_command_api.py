from __future__ import annotations

from pathlib import Path

from fastapi.testclient import TestClient

from hnblue_agent.api import create_app
from hnblue_agent.graph import AgentHarness


def _request(task_id: str) -> dict:
    return {
        "taskId": task_id,
        "traceId": "trace-" + task_id,
        "subjectKey": "PROFESSIONAL:owner",
        "role": "PROFESSIONAL",
        "capability": "MULTI_AGENT",
        "allowedTools": [
            "bluecarbon.knowledge.search", "bluecarbon.region.metrics.read",
            "bluecarbon.methodology.lookup", "bluecarbon.source.verify",
        ],
        "tokenBudget": 2000,
        "maxSteps": 8,
        "message": "结合文昌红树林蓝碳指标趋势以及核证方法来源形成报告",
    }


def test_one_command_api_executes_reviews_and_archives(tmp_path: Path) -> None:
    runtime = AgentHarness(checkpoint_path=tmp_path / "api.sqlite")
    client = TestClient(create_app(runtime))
    request = _request("command-api-0001")
    headers = {"X-HNBLUE-Service-Scope": "agent.command", "X-Trace-Id": request["traceId"]}

    first = client.post("/v1/agent/tasks/command", headers=headers,
                        json={"action": "EXECUTE", "request": request}).json()
    assert first["executionStatus"] == "AWAITING_REVIEW"

    review = {
        "taskId": request["taskId"], "traceId": request["traceId"],
        "requestId": "review-approve-0001",
        "action": "APPROVE", "reviewerSubjectKey": "PROFESSIONAL:reviewer",
        "reviewerRole": "PROFESSIONAL", "comment": "同意", "reviewCycle": 0,
    }
    second = client.post("/v1/agent/tasks/command", headers=headers,
                         json={"action": "APPROVE", "review": review}).json()
    assert second["executionStatus"] == "AWAITING_ARCHIVE"

    review.update({"action": "ARCHIVE", "requestId": "review-archive-0001",
                   "reviewerSubjectKey": "ADMIN:archive",
                   "reviewerRole": "ADMIN", "comment": "归档", "reviewCycle": 1})
    third = client.post("/v1/agent/tasks/command", headers=headers,
                        json={"action": "ARCHIVE", "review": review}).json()
    assert third["executionStatus"] == "ARCHIVED"
    runtime.close()
