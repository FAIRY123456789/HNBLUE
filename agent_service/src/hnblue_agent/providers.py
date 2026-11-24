from __future__ import annotations

import re
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from typing import Any, Protocol

import httpx

from .models import Capability, RoutingAudit, TaskContext


@dataclass(frozen=True)
class RouteDecision:
    route: str
    confidence: float
    reason: str
    clarification: str | None = None
    tool_calls: tuple[dict[str, Any], ...] = ()
    model: str | None = None
    latency_ms: float | None = None
    routing_audit: RoutingAudit | None = None


class ModelProvider(Protocol):
    name: str

    def route(self, context: TaskContext, message: str,
              tools: list[dict[str, Any]] | None = None) -> RouteDecision: ...


@dataclass(frozen=True)
class CandidateRoute:
    route: str
    outcome: str
    intent: str | None
    suggested_capability: str | None
    policy_decision: str | None
    clarification_needed: bool
    required_fields: tuple[str, ...]
    confidence: float | None
    model_id: str | None
    latency_ms: float


class LocalRouterShadowClient:
    """Adapter for the local Qwen Route V2 HTTP contract.

    The adapter returns comparison metadata only. It never emits a tool call and
    therefore cannot expand the Java-authorized tool set.
    """

    name = "local-route-v2"
    _intent_routes = {
        "PUBLIC_FACT": "education",
        "EDUCATION": "education",
        "KNOWLEDGE_QA": "knowledge",
        "MONITORING_STATS": "monitoring",
        "SPATIAL_QUERY": "monitoring",
        "PREDICTION": "monitoring",
        "WORK_ORDER": "clarify",
        "COMPLEX_ANALYSIS": "supervisor",
        "CLARIFY": "clarify",
        "OUT_OF_SCOPE": "clarify",
    }

    def __init__(self, base_url: str = "http://127.0.0.1:18090",
                 timeout_seconds: float = 5.0,
                 transport: httpx.BaseTransport | None = None) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout_seconds = timeout_seconds
        self.transport = transport

    def route_candidate(self, context: TaskContext, message: str) -> CandidateRoute:
        payload = {
            "requestId": context.trace_id[:128],
            "text": message,
            "userRole": context.role.value,
            "context": {},
        }
        started = time.perf_counter()
        with httpx.Client(timeout=self.timeout_seconds, trust_env=False,
                          transport=self.transport) as client:
            response = client.post(f"{self.base_url}/v1/route", json=payload)
            response.raise_for_status()
            body = response.json()
        elapsed_ms = (time.perf_counter() - started) * 1000
        outcome = str(body.get("outcome", ""))
        intent = body.get("intent")
        clarification = bool(body.get("clarificationNeeded", False))
        if outcome == "POLICY_DENIED" or clarification:
            route = "clarify"
        else:
            route = self._intent_routes.get(str(intent), "clarify")
        if route == "supervisor" and context.capability != Capability.MULTI_AGENT:
            route = "knowledge"
        policy = body.get("policy") if isinstance(body.get("policy"), dict) else {}
        raw_confidence = body.get("confidence")
        return CandidateRoute(
            route=route,
            outcome=outcome,
            intent=str(intent) if intent is not None else None,
            suggested_capability=(str(body.get("suggestedCapability"))
                                  if body.get("suggestedCapability") is not None else None),
            policy_decision=(str(policy.get("decision"))
                             if policy.get("decision") is not None else None),
            clarification_needed=clarification,
            required_fields=tuple(str(item) for item in body.get("requiredFields", []) or []),
            confidence=float(raw_confidence) if raw_confidence is not None else None,
            model_id=str(body.get("modelId")) if body.get("modelId") is not None else None,
            latency_ms=float(body.get("latencyMs", elapsed_ms)),
        )


class ShadowModelProvider:
    """Run the existing provider and LoRA candidate in parallel.

    Only the existing provider's RouteDecision is returned to LangGraph. The
    candidate is attached as audit evidence and cannot select tools or routes.
    """

    def __init__(self, active: ModelProvider | None = None,
                 candidate: LocalRouterShadowClient | None = None) -> None:
        self.active = active or RuleModelProvider()
        self.candidate = candidate or LocalRouterShadowClient()
        self.name = f"shadow:{self.active.name}+{self.candidate.name}"

    def route(self, context: TaskContext, message: str,
              tools: list[dict[str, Any]] | None = None) -> RouteDecision:
        with ThreadPoolExecutor(max_workers=2, thread_name_prefix="hnblue-route-shadow") as pool:
            active_future = pool.submit(self.active.route, context, message, tools)
            candidate_future = pool.submit(self.candidate.route_candidate, context, message)
            active = active_future.result()
            try:
                candidate = candidate_future.result()
                audit = RoutingAudit(
                    activeProvider=self.active.name,
                    activeRoute=active.route,
                    candidateProvider=self.candidate.name,
                    candidateAvailable=True,
                    candidateRoute=candidate.route,
                    matched=active.route == candidate.route,
                    outcome=candidate.outcome,
                    intent=candidate.intent,
                    suggestedCapability=candidate.suggested_capability,
                    policyDecision=candidate.policy_decision,
                    clarificationNeeded=candidate.clarification_needed,
                    requiredFields=list(candidate.required_fields),
                    confidence=candidate.confidence,
                    candidateModelId=candidate.model_id,
                    candidateLatencyMs=candidate.latency_ms,
                )
            except Exception as exc:
                audit = RoutingAudit(
                    activeProvider=self.active.name,
                    activeRoute=active.route,
                    candidateProvider=self.candidate.name,
                    candidateAvailable=False,
                    matched=None,
                    errorCode=type(exc).__name__,
                )
        return RouteDecision(
            route=active.route,
            confidence=active.confidence,
            reason=active.reason,
            clarification=active.clarification,
            tool_calls=active.tool_calls,
            model=active.model,
            latency_ms=active.latency_ms,
            routing_audit=audit,
        )


class RuleModelProvider:
    """Deterministic R0 provider used for offline, repeatable acceptance tests."""

    name = "rule-r0"
    domain_terms = ("蓝碳", "红树林", "海草床", "盐沼", "碳汇", "碳储量", "碳通量", "hnblue")
    monitoring_terms = ("区域", "琼海", "文昌", "万宁", "海口", "三亚", "趋势", "指标", "监测", "数值", "变化")
    knowledge_terms = ("方法", "政策", "依据", "来源", "引用", "核证", "标准", "怎么计算")
    education_terms = ("是什么", "解释", "科普", "为什么", "介绍", "区别")

    def route(self, context: TaskContext, message: str,
              tools: list[dict[str, Any]] | None = None) -> RouteDecision:
        text = " ".join(message.lower().split())
        if not any(term.lower() in text for term in self.domain_terms):
            return RouteDecision("clarify", 0.35, "no blue-carbon domain signal", "请补充蓝碳、红树林、海草床、盐沼或 HNBLUE 相关问题。")
        has_monitoring = any(term in text for term in self.monitoring_terms)
        has_knowledge = any(term in text for term in self.knowledge_terms)
        compound = has_monitoring and has_knowledge and bool(re.search(r"并|以及|同时|结合|报告", text))
        if compound and context.capability == Capability.MULTI_AGENT:
            return RouteDecision("supervisor", 0.94, "compound evidence and monitoring request")
        if has_monitoring and "bluecarbon.region.metrics.read" in context.allowed_tools:
            return RouteDecision("monitoring", 0.91, "region or metric request")
        if has_knowledge:
            return RouteDecision("knowledge", 0.88, "methodology or source request")
        if any(term in text for term in self.education_terms):
            return RouteDecision("education", 0.86, "public education request")
        return RouteDecision("knowledge", 0.72, "general blue-carbon knowledge request")


class OllamaModelProvider:
    """Local model planner using Ollama's native tool-calling API.

    Only the already-authorized tool schemas supplied by the Harness are sent to
    the model. Returned calls are mapped back to the canonical HNBLUE names and
    still pass through ExecutionPolicy and JSON Schema validation.
    """

    def __init__(self, model: str = "qwen3:1.7b", base_url: str = "http://127.0.0.1:11434",
                 timeout_seconds: float = 120.0,
                 transport: httpx.BaseTransport | None = None) -> None:
        self.model = model
        self.base_url = base_url.rstrip("/")
        self.timeout_seconds = timeout_seconds
        self.transport = transport
        self.name = f"ollama:{model}"

    def route(self, context: TaskContext, message: str,
              tools: list[dict[str, Any]] | None = None) -> RouteDecision:
        exposed = tools or []
        external_to_internal: dict[str, str] = {}
        ollama_tools: list[dict[str, Any]] = []
        for item in exposed:
            internal = str(item["name"])
            external = internal.replace(".", "__")
            external_to_internal[external] = internal
            ollama_tools.append({
                "type": "function",
                "function": {
                    "name": external,
                    "description": str(item["description"]),
                    "parameters": item["schema"],
                },
            })
        system = (
            "你是 HNBLUE 蓝碳任务规划器。只处理蓝碳、红树林、海草床、盐沼、碳汇、"
            "监测指标或核证方法问题。你的唯一任务是选择工具，不要直接回答用户。"
            "凡是蓝碳领域问题必须至少调用一个所提供的只读工具：概念、来源和一般问题调用"
            " knowledge search；明确地区监测问题调用 region metrics；方法或核证问题调用"
            " methodology lookup（若未提供则用 knowledge search）；同时要求地区指标和来源/"
            "报告时，在同一响应中并行调用 knowledge search 与 region metrics。"
            "工具参数只能逐字取自用户消息，不得猜测地区，不得调用未提供的工具。"
            "仅当问题不属于蓝碳领域时才不调用工具。"
            f"当前角色={context.role.value}，能力={context.capability.value}。"
        )
        payload: dict[str, Any] = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system},
                {"role": "user", "content": message},
            ],
            "tools": ollama_tools,
            "stream": False,
            "think": False,
            "options": {"temperature": 0, "seed": 7},
        }
        started = time.perf_counter()
        with httpx.Client(timeout=self.timeout_seconds, trust_env=False, transport=self.transport) as client:
            response = client.post(f"{self.base_url}/api/chat", json=payload)
            response.raise_for_status()
            body = response.json()
        latency_ms = (time.perf_counter() - started) * 1000
        raw_calls = body.get("message", {}).get("tool_calls", []) or []
        planned: list[dict[str, Any]] = []
        for raw_call in raw_calls[:context.max_steps]:
            function = raw_call.get("function", {})
            external_name = str(function.get("name", ""))
            if external_name not in external_to_internal:
                raise ValueError(f"model returned an unexposed tool: {external_name}")
            arguments = function.get("arguments", {})
            if isinstance(arguments, str):
                import json
                arguments = json.loads(arguments)
            if not isinstance(arguments, dict):
                raise ValueError("model tool arguments must be an object")
            planned.append({"name": external_to_internal[external_name], "arguments": arguments})
        if not planned:
            return RouteDecision(
                "clarify", 0.45, "local model did not select an authorized evidence tool",
                "本地模型未选择可核查的蓝碳证据工具，请补充研究对象、区域或方法学问题。",
                model=self.model, latency_ms=latency_ms,
            )
        names = {item["name"] for item in planned}
        if len(names) > 1 and context.capability == Capability.MULTI_AGENT:
            route = "supervisor"
        elif "bluecarbon.region.metrics.read" in names:
            route = "monitoring"
        elif context.capability == Capability.BASIC_QA and any(
                term in message for term in RuleModelProvider.education_terms):
            route = "education"
        else:
            route = "knowledge"
        return RouteDecision(
            route, 0.8, "local Ollama model selected authorized tools",
            tool_calls=tuple(planned), model=self.model, latency_ms=latency_ms,
        )
