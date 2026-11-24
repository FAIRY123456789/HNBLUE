from __future__ import annotations

import json

import httpx

from hnblue_agent.graph import AgentHarness
from hnblue_agent.models import AgentRequest
from hnblue_agent.providers import OllamaModelProvider


def _request(task_id: str, *, role: str = "PUBLIC", capability: str = "BASIC_QA",
             tools: list[str] | None = None) -> AgentRequest:
    return AgentRequest(
        taskId=task_id,
        traceId="trace-" + task_id,
        subjectKey=f"{role}:model-test",
        role=role,
        capability=capability,
        allowedTools=tools or ["bluecarbon.knowledge.search"],
        tokenBudget=2000,
        maxSteps=4,
        message="蓝碳生态系统是什么？",
    )


def test_ollama_selected_tool_runs_through_langgraph_and_registry(tmp_path) -> None:
    captured: dict = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured.update(json.loads(request.content))
        return httpx.Response(200, json={
            "message": {"role": "assistant", "content": "", "tool_calls": [{
                "function": {
                    "name": "bluecarbon__knowledge__search",
                    "arguments": {"query": "蓝碳生态系统是什么？", "topK": 3},
                },
            }]},
        })

    provider = OllamaModelProvider(transport=httpx.MockTransport(handler))
    runtime = AgentHarness(provider=provider, checkpoint_path=tmp_path / "checkpoints.sqlite")
    result = runtime.execute(_request("task-ollama-0001"))
    metadata = runtime.execution_metadata("task-ollama-0001")

    assert result.success
    assert metadata["route"] == "education"
    assert metadata["tools"] == ["bluecarbon.knowledge.search"]
    assert [item["function"]["name"] for item in captured["tools"]] == ["bluecarbon__knowledge__search"]
    assert captured["think"] is False


def test_ollama_only_receives_request_authorized_tools(tmp_path) -> None:
    exposed: list[str] = []

    def handler(request: httpx.Request) -> httpx.Response:
        body = json.loads(request.content)
        exposed.extend(item["function"]["name"] for item in body["tools"])
        return httpx.Response(200, json={"message": {"role": "assistant", "content": "无工具"}})

    provider = OllamaModelProvider(transport=httpx.MockTransport(handler))
    result = AgentHarness(provider=provider, checkpoint_path=tmp_path / "only.sqlite").execute(
        _request("task-ollama-0002")
    )

    assert exposed == ["bluecarbon__knowledge__search"]
    assert not result.success
    assert result.errorCode == "clarification_required"


def test_policy_blocks_capability_escalation_before_ollama_call(tmp_path) -> None:
    called = False

    def handler(request: httpx.Request) -> httpx.Response:
        nonlocal called
        called = True
        return httpx.Response(500)

    provider = OllamaModelProvider(transport=httpx.MockTransport(handler))
    result = AgentHarness(provider=provider, checkpoint_path=tmp_path / "blocked.sqlite").execute(
        _request(
            "task-ollama-0003",
            role="PUBLIC",
            capability="MULTI_AGENT",
            tools=["bluecarbon.region.metrics.read"],
        )
    )

    assert not called
    assert not result.success
    assert result.errorCode == "role_capability_forbidden"


def test_model_arguments_are_json_schema_validated(tmp_path) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={
            "message": {"tool_calls": [{"function": {
                "name": "bluecarbon__knowledge__search",
                "arguments": {"query": "蓝碳", "topK": 99},
            }}]},
        })

    provider = OllamaModelProvider(transport=httpx.MockTransport(handler))
    result = AgentHarness(provider=provider, checkpoint_path=tmp_path / "schema.sqlite").execute(
        _request("task-ollama-0004")
    )

    assert not result.success
    assert result.errorCode == "agent_execution_failed"
    assert result.errorMessage == "ValueError"


def test_model_cannot_invent_a_region_not_present_in_user_message(tmp_path) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={
            "message": {"tool_calls": [{"function": {
                "name": "bluecarbon__region__metrics__read",
                "arguments": {"region": "琼海"},
            }}]},
        })

    provider = OllamaModelProvider(transport=httpx.MockTransport(handler))
    request = _request(
        "task-ollama-0005",
        role="PROFESSIONAL",
        capability="PROFESSIONAL_ANALYSIS",
        tools=["bluecarbon.region.metrics.read"],
    ).model_copy(update={"message": "查询某区域的蓝碳监测指标趋势"})
    runtime = AgentHarness(provider=provider, checkpoint_path=tmp_path / "grounding.sqlite")
    result = runtime.execute(request)

    assert not result.success
    assert result.errorCode == "insufficient_evidence"
    assert runtime.execution_metadata(request.taskId)["tools"] == []


def test_source_verification_is_orchestrated_after_retrieval(tmp_path) -> None:
    exposed: list[str] = []

    def handler(request: httpx.Request) -> httpx.Response:
        body = json.loads(request.content)
        exposed.extend(item["function"]["name"] for item in body["tools"])
        return httpx.Response(200, json={
            "message": {"tool_calls": [{"function": {
                "name": "bluecarbon__knowledge__search",
                "arguments": {"query": "红树林蓝碳来源", "topK": 2},
            }}]},
        })

    provider = OllamaModelProvider(transport=httpx.MockTransport(handler))
    request = _request(
        "task-ollama-0006",
        role="PROFESSIONAL",
        capability="PROFESSIONAL_ANALYSIS",
        tools=["bluecarbon.knowledge.search", "bluecarbon.source.verify"],
    ).model_copy(update={"message": "红树林蓝碳来源是什么？"})
    runtime = AgentHarness(provider=provider, checkpoint_path=tmp_path / "verify.sqlite")
    result = runtime.execute(request)

    assert result.success
    assert "bluecarbon__source__verify" not in exposed
    assert runtime.execution_metadata(request.taskId)["tools"] == [
        "bluecarbon.knowledge.search", "bluecarbon.source.verify"]


def test_explicit_region_completes_missing_region_tool_call(tmp_path, monkeypatch) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={
            "message": {"tool_calls": [{"function": {
                "name": "bluecarbon__knowledge__search",
                "arguments": {"query": "文昌红树林蓝碳趋势和来源", "topK": 2},
            }}]},
        })

    monkeypatch.delenv("HNBLUE_JAVA_BASE_URL", raising=False)
    provider = OllamaModelProvider(transport=httpx.MockTransport(handler))
    request = _request(
        "task-ollama-region-completion",
        role="PROFESSIONAL",
        capability="MULTI_AGENT",
        tools=[
            "bluecarbon.knowledge.search",
            "bluecarbon.region.metrics.read",
            "bluecarbon.source.verify",
        ],
    ).model_copy(update={"message": "结合文昌红树林蓝碳指标趋势和来源形成报告"})
    runtime = AgentHarness(provider=provider, checkpoint_path=tmp_path / "region-completion.sqlite")

    result = runtime.execute(request)

    assert result.executionStatus.value == "AWAITING_REVIEW"
    assert result.reviewRequest is not None
    assert runtime.execution_metadata(request.taskId)["route"] == "supervisor"
    assert runtime.execution_metadata(request.taskId)["tools"] == [
        "bluecarbon.knowledge.search",
        "bluecarbon.region.metrics.read",
        "bluecarbon.source.verify",
    ]
    assert any(source.get("synthetic") is True for source in result.sources)
