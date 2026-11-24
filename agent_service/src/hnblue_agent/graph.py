from __future__ import annotations

import json
import re
import sqlite3
import threading
from pathlib import Path
from typing import Any

from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import END, START, StateGraph
from langgraph.types import Command, interrupt

from .models import (AgentCommandAction, AgentExecutionStatus, AgentRequest, AgentResult,
                     EvidenceStatus, ReviewDecision, ReviewLevel, ReviewRequest,
                     TaskContext, TaskState, ToolResult)
from .multi_agent import (ConsistencyAgent, EvidenceAgent, MonitoringAgent,
                          ResearchAgent, send_message)
from .policy import ExecutionPolicy, PolicyViolation
from .providers import ModelProvider, RuleModelProvider
from .tools import ToolRegistry, default_registry
from .tracing import TraceStore


REGIONS = ("琼海", "文昌", "万宁", "海口", "三亚", "儋州", "五指山", "东方", "定安县", "屯昌县",
           "澄迈县", "临高县", "白沙", "昌江", "乐东", "陵水", "保亭", "琼中")


class AgentHarness:
    def __init__(self, registry: ToolRegistry | None = None, provider: ModelProvider | None = None,
                 trace_store: TraceStore | None = None, checkpoint_path: Path | None = None) -> None:
        self.registry = registry or default_registry()
        self.provider = provider or RuleModelProvider()
        self.policy = ExecutionPolicy()
        self.trace_store = trace_store or TraceStore()
        self.research_agent = ResearchAgent()
        self.monitoring_agent = MonitoringAgent(REGIONS)
        self.evidence_agent = EvidenceAgent()
        self.consistency_agent = ConsistencyAgent()
        self._checkpoint_connection: sqlite3.Connection | None = None
        from langgraph.checkpoint.serde.jsonplus import JsonPlusSerializer

        serde = JsonPlusSerializer(allowed_msgpack_modules=[
            ("hnblue_agent.models", "Role"),
            ("hnblue_agent.models", "Capability"),
            ("hnblue_agent.models", "EvidenceStatus"),
            ("hnblue_agent.models", "TaskContext"),
            ("hnblue_agent.models", "SourceRecord"),
            ("hnblue_agent.models", "ToolResult"),
            ("hnblue_agent.models", "RoutingAudit"),
        ])
        if checkpoint_path is None:
            self.checkpointer = MemorySaver(serde=serde)
            self.checkpoint_kind = "memory"
        else:
            from langgraph.checkpoint.sqlite import SqliteSaver

            checkpoint_path.parent.mkdir(parents=True, exist_ok=True)
            self._checkpoint_connection = sqlite3.connect(checkpoint_path, check_same_thread=False)
            self.checkpointer = SqliteSaver(self._checkpoint_connection, serde=serde)
            self.checkpoint_kind = "sqlite"
        self.graph = self._build_graph().compile(checkpointer=self.checkpointer)
        self._cache: dict[str, AgentResult] = {}
        self._cache_lock = threading.Lock()
        # SqliteSaver shares one connection in the local runtime. LangGraph
        # checkpoint reads/writes are transactional but that connection cannot
        # safely execute overlapping graphs from multiple HTTP worker threads.
        # Serialize the local graph boundary; production-scale parallelism must
        # use a concurrent checkpoint backend instead of silently losing tasks.
        self._graph_lock = threading.RLock()

    def execute(self, request: AgentRequest) -> AgentResult:
        with self._cache_lock:
            cached = self._cache.get(request.taskId)
        if cached is not None:
            return cached
        with self._graph_lock:
            persisted = self._current_result(request.taskId)
        if persisted is not None:
            self._cache_if_terminal(request.taskId, persisted)
            return persisted
        context = TaskContext.from_request(request)
        initial: TaskState = {
            "context": context,
            "message": request.message.strip(),
            "step_count": 0,
            "token_usage": 0,
            "tool_results": [],
            "seen_calls": set(),
            "sources": [],
            "trace_events": [],
            "active_worker": None,
            "handoff_count": 0,
            "worker_outputs": [],
            "agent_messages": [],
            "revision_round": 0,
            "max_revision_rounds": 1,
            "self_consistency": {},
            "review_cycle": 0,
            "max_review_cycles": 2,
            "review_history": [],
            "execution_status": AgentExecutionStatus.COMPLETED.value,
            "success": False,
            "partial": False,
            "evidence_status": EvidenceStatus.INSUFFICIENT.value,
        }
        try:
            with self._graph_lock:
                # A same-task request may have completed while this caller was
                # waiting behind another graph execution.
                persisted_after_wait = self._current_result(request.taskId)
                if persisted_after_wait is not None:
                    self._cache_if_terminal(request.taskId, persisted_after_wait)
                    return persisted_after_wait
                state = self.graph.invoke(initial, config={"configurable": {"thread_id": request.taskId}})
            result = self._result_from_state(state)
        except PolicyViolation as exc:
            result = AgentResult(success=False, errorCode=exc.code, errorMessage=str(exc))
        except Exception as exc:  # boundary: do not disclose internal stack or prompt content
            result = AgentResult(success=False, errorCode="agent_execution_failed",
                                 errorMessage=type(exc).__name__)
        self._record_boundary_result(request.taskId, request.traceId, result)
        self._cache_if_terminal(request.taskId, result)
        return result

    def resume(self, decision: ReviewDecision) -> AgentResult:
        """Resume exactly one checkpointed human-review interrupt."""
        with self._graph_lock:
            snapshot = self.graph.get_state({"configurable": {"thread_id": decision.taskId}})
            values = snapshot.values if snapshot is not None else None
            if values and any(item.get("requestId") == decision.requestId
                              for item in values.get("review_history", [])):
                return self._result_from_state(values)
            if not values or not snapshot.next:
                raise PolicyViolation("review_not_waiting", "task is not waiting for a review decision")
            self._validate_review_decision(values, decision)
            state = self.graph.invoke(
                Command(resume=decision.model_dump(mode="json")),
                config={"configurable": {"thread_id": decision.taskId}},
            )
        result = self._result_from_state(state)
        with self._cache_lock:
            self._cache.pop(decision.taskId, None)
        self._record_boundary_result(decision.taskId, decision.traceId, result)
        self._cache_if_terminal(decision.taskId, result)
        return result

    def status(self, task_id: str) -> AgentResult | None:
        with self._graph_lock:
            return self._current_result(task_id)

    def close(self) -> None:
        with self._graph_lock:
            if self._checkpoint_connection is not None:
                self._checkpoint_connection.close()
                self._checkpoint_connection = None

    def _current_result(self, task_id: str) -> AgentResult | None:
        snapshot = self.graph.get_state({"configurable": {"thread_id": task_id}})
        values = snapshot.values if snapshot is not None else None
        if not values:
            return None
        if snapshot.next and not values.get("review_request"):
            return None
        if not snapshot.next and "output" not in values and "error_code" not in values:
            return None
        return self._result_from_state(values)

    def _result_from_state(self, values: dict[str, Any]) -> AgentResult:
        raw_status = values.get("execution_status", AgentExecutionStatus.COMPLETED.value)
        status = AgentExecutionStatus(raw_status)
        review_request = values.get("review_request")
        waiting = status in {AgentExecutionStatus.AWAITING_REVIEW,
                             AgentExecutionStatus.AWAITING_ARCHIVE}
        return AgentResult(
            success=bool(values.get("success")) and not waiting,
            partial=bool(values.get("partial")) or waiting,
            output=values.get("output") or values.get("draft_output"),
            sources=values.get("sources", []),
            tokenUsage=int(values.get("token_usage", 0)),
            errorCode=values.get("error_code"),
            errorMessage=values.get("error_message"),
            routingAudit=values.get("routing_audit"),
            executionStatus=status,
            reviewRequest=(ReviewRequest.model_validate(review_request) if review_request else None),
            collaboration={
                "messages": len(values.get("agent_messages", [])),
                "workers": [item.get("worker") for item in values.get("worker_outputs", [])],
                "handoffs": int(values.get("handoff_count", 0)),
                "revisionRound": int(values.get("revision_round", 0)),
                "selfConsistency": values.get("self_consistency", {}),
                "reviewHistory": values.get("review_history", []),
            },
        )

    def _validate_review_decision(self, values: dict[str, Any], decision: ReviewDecision) -> None:
        context = values["context"]
        request = values.get("review_request") or {}
        if decision.traceId != context.trace_id:
            raise PolicyViolation("review_trace_mismatch", "review trace does not match task")
        if decision.reviewCycle != int(request.get("cycle", -1)):
            raise PolicyViolation("stale_review_decision", "review cycle is stale")
        if decision.reviewerSubjectKey == context.subject_key:
            raise PolicyViolation("self_review_forbidden", "task owner cannot approve their own result")
        allowed = {str(item) for item in request.get("allowedActions", [])}
        if decision.action.value not in allowed:
            raise PolicyViolation("review_action_forbidden", "action is not allowed at this review level")
        level = ReviewLevel(str(request.get("level")))
        if level == ReviewLevel.ARCHIVE_OFFICER and decision.reviewerRole.value != "ADMIN":
            raise PolicyViolation("archive_role_forbidden", "archive action requires an administrator")
        if level == ReviewLevel.SENIOR_REVIEWER and decision.reviewerRole.value != "ADMIN":
            raise PolicyViolation("senior_review_forbidden", "senior review requires an administrator")
        if level == ReviewLevel.DOMAIN_REVIEWER and decision.reviewerRole.value not in {"PROFESSIONAL", "ADMIN"}:
            raise PolicyViolation("domain_review_forbidden", "domain review requires a professional or administrator")

    def _cache_if_terminal(self, task_id: str, result: AgentResult) -> None:
        if result.executionStatus not in {
                AgentExecutionStatus.COMPLETED, AgentExecutionStatus.REJECTED,
                AgentExecutionStatus.ARCHIVED}:
            return
        with self._cache_lock:
            self._cache[task_id] = result

    def _record_boundary_result(self, task_id: str, trace_id: str, result: AgentResult) -> None:
        self.trace_store.append(task_id, trace_id, {
            "type": "task_paused" if result.reviewRequest else "task_complete",
            "executionStatus": result.executionStatus.value,
            "success": result.success, "partial": result.partial,
            "errorCode": result.errorCode, "sourceCount": len(result.sources),
            "provider": self.provider.name,
            "routingAudit": result.routingAudit.model_dump() if result.routingAudit else None,
        })

    def _build_graph(self) -> StateGraph:
        graph = StateGraph(TaskState)
        graph.add_node("guard", self._guard)
        graph.add_node("route", self._route)
        graph.add_node("education", self._education)
        graph.add_node("knowledge", self._knowledge)
        graph.add_node("monitoring", self._monitoring)
        graph.add_node("supervisor", self._supervisor)
        graph.add_node("research_worker", self._research_worker)
        graph.add_node("monitoring_worker", self._monitoring_worker)
        graph.add_node("evidence_merge", self._evidence_merge)
        graph.add_node("consistency_agent", self._consistency_agent)
        graph.add_node("prepare_review", self._prepare_review)
        graph.add_node("human_review", self._human_review)
        graph.add_node("prepare_archive", self._prepare_archive)
        graph.add_node("archive_review", self._archive_review)
        graph.add_node("planned", self._planned)
        graph.add_node("finalize", self._finalize)
        graph.add_edge(START, "guard")
        graph.add_conditional_edges("guard", lambda state: "finalize" if state.get("error_code") else "route")
        graph.add_conditional_edges("route", self._route_target, {
            "education": "education", "knowledge": "knowledge", "monitoring": "monitoring",
            "supervisor": "supervisor", "planned": "planned", "clarify": "finalize", "finalize": "finalize",
        })
        graph.add_edge("supervisor", "research_worker")
        graph.add_edge("research_worker", "monitoring_worker")
        graph.add_edge("monitoring_worker", "evidence_merge")
        graph.add_edge("evidence_merge", "consistency_agent")
        graph.add_conditional_edges("consistency_agent", self._consistency_target, {
            "revise": "supervisor", "review": "prepare_review",
        })
        graph.add_edge("prepare_review", "human_review")
        graph.add_conditional_edges("human_review", self._review_target, {
            "revise": "supervisor", "archive": "prepare_archive", "finalize": "finalize",
        })
        graph.add_edge("prepare_archive", "archive_review")
        graph.add_conditional_edges("archive_review", self._archive_target, {
            "revise": "supervisor", "finalize": "finalize",
        })
        for node in ("education", "knowledge", "monitoring", "planned"):
            graph.add_edge(node, "finalize")
        graph.add_edge("finalize", END)
        return graph

    def _event(self, state: TaskState, event_type: str, **details: Any) -> list[dict[str, Any]]:
        event = {"type": event_type, **details}
        context = state["context"]
        self.trace_store.append(context.task_id, context.trace_id, event)
        return [*state.get("trace_events", []), event]

    def _guard(self, state: TaskState) -> dict[str, Any]:
        try:
            self.policy.validate_context(state["context"])
            self.policy.guard_message(state["message"])
            return {"trace_events": self._event(state, "guard_pass")}
        except PolicyViolation as exc:
            return {"error_code": exc.code, "error_message": str(exc), "evidence_status": EvidenceStatus.BLOCKED.value,
                    "trace_events": self._event(state, "guard_block", code=exc.code)}

    def _route(self, state: TaskState) -> dict[str, Any]:
        context = state["context"]
        # source.verify consumes identifiers produced by earlier tools, so it is
        # orchestrated after retrieval and is never offered as a first action.
        planning_tools = frozenset(item for item in context.allowed_tools
                                   if item != "bluecarbon.source.verify")
        decision = self.provider.route(context, state["message"], self.registry.describe(planning_tools))
        route = decision.route
        planned_names = {str(item.get("name", "")) for item in decision.tool_calls}
        has_region = any(item in state["message"] for item in REGIONS)
        if (context.capability.value == "MULTI_AGENT" and has_region
                and "bluecarbon.region.metrics.read" in context.allowed_tools
                and (route == "supervisor" or "bluecarbon.knowledge.search" in planned_names)):
            route = "supervisor"
        return {"route": route, "confidence": decision.confidence,
                "clarification": decision.clarification,
                "planned_calls": list(decision.tool_calls),
                "routing_audit": decision.routing_audit,
                "trace_events": self._event(state, "route", route=route,
                                              confidence=decision.confidence, provider=self.provider.name,
                                              model=decision.model, modelLatencyMs=decision.latency_ms,
                                              plannedTools=[item["name"] for item in decision.tool_calls],
                                              shadow=(decision.routing_audit.model_dump()
                                                      if decision.routing_audit else None))}

    @staticmethod
    def _route_target(state: TaskState) -> str:
        route = state.get("route", "finalize")
        if route == "supervisor":
            return "supervisor"
        return "planned" if state.get("planned_calls") else route

    def _call(self, state: TaskState, name: str, arguments: dict[str, Any]) -> ToolResult:
        fingerprint = self.policy.before_tool(state, name, arguments)
        result = self.registry.execute(state["context"], name, arguments)
        state.setdefault("seen_calls", set()).add(fingerprint)
        state["step_count"] = state.get("step_count", 0) + 1
        state.setdefault("tool_results", []).append(result)
        state.setdefault("sources", []).extend(item.model_dump() for item in result.sources)
        state["trace_events"] = self._event(state, "tool", tool=name, status=result.status.value,
                                             sources=len(result.sources))
        return result

    def _education(self, state: TaskState) -> dict[str, Any]:
        self._call(state, "bluecarbon.knowledge.search", {"query": state["message"], "topK": 3})
        return self._mutable_result(state)

    def _knowledge(self, state: TaskState) -> dict[str, Any]:
        self._call(state, "bluecarbon.knowledge.search", {"query": state["message"], "topK": 5})
        if "bluecarbon.methodology.lookup" in state["context"].allowed_tools and re.search(r"方法|核证|标准", state["message"]):
            self._call(state, "bluecarbon.methodology.lookup", {"topic": state["message"]})
        return self._mutable_result(state)

    def _monitoring(self, state: TaskState) -> dict[str, Any]:
        region = next((item for item in REGIONS if item in state["message"]), "")
        if not region:
            result = ToolResult(
                tool="bluecarbon.region.metrics.read",
                status=EvidenceStatus.INSUFFICIENT,
                content={"reason": "a supported Hainan region is required"},
            )
            state.setdefault("tool_results", []).append(result)
            state["trace_events"] = self._event(state, "tool_skipped", tool=result.tool,
                                                  reason="region_required")
            return self._mutable_result(state)
        self._call(state, "bluecarbon.region.metrics.read", {"region": region})
        return self._mutable_result(state)

    def _supervisor(self, state: TaskState) -> dict[str, Any]:
        state["active_worker"] = "supervisor"
        # Handoff budget is per collaboration round. Review history and trace
        # events preserve the cumulative audit trail across revision rounds.
        state["handoff_count"] = 0
        send_message(state, "supervisor", "research_worker", "TASK_ASSIGNMENT", {
            "objective": state["message"], "required": ["knowledge", "methodology"],
        })
        send_message(state, "supervisor", "monitoring_worker", "TASK_ASSIGNMENT", {
            "objective": state["message"], "required": ["region_metrics"],
        })
        send_message(state, "supervisor", "evidence_merge", "TASK_ASSIGNMENT", {
            "objective": state["message"], "required": ["source_verification"],
        })
        state["trace_events"] = self._event(
            state, "coordinator_start",
            workers=["research_worker", "monitoring_worker", "evidence_merge", "consistency_agent"],
            revisionRound=state.get("revision_round", 0))
        return self._mutable_result(state)

    def _record_worker(self, state: TaskState, worker: str, tools: list[str]) -> None:
        state.setdefault("worker_outputs", []).append({
            "worker": worker,
            "tools": tools,
            "sourceCount": len(state.get("sources", [])),
        })
        state["trace_events"] = self._event(state, "worker_complete", worker=worker, tools=tools)

    def _handoff(self, state: TaskState, source: str, target: str) -> None:
        state["handoff_count"] = self.policy.before_handoff(state, target)
        state["active_worker"] = target
        state["trace_events"] = self._event(
            state, "handoff", source=source, target=target, count=state["handoff_count"])

    def _research_worker(self, state: TaskState) -> dict[str, Any]:
        self.research_agent.run(state, self._call, self._event)
        self._handoff(state, "research_worker", "monitoring_worker")
        return self._mutable_result(state)

    def _monitoring_worker(self, state: TaskState) -> dict[str, Any]:
        self.monitoring_agent.run(state, self._call, self._event)
        self._handoff(state, "monitoring_worker", "evidence_merge")
        return self._mutable_result(state)

    def _evidence_merge(self, state: TaskState) -> dict[str, Any]:
        self.evidence_agent.run(state, self._call, self._event)
        return self._mutable_result(state)

    def _consistency_agent(self, state: TaskState) -> dict[str, Any]:
        self.consistency_agent.run(state, self._event)
        state["active_worker"] = None
        return self._mutable_result(state)

    @staticmethod
    def _consistency_target(state: TaskState) -> str:
        return "revise" if state.get("self_consistency", {}).get("outcome") == "REVISE" else "review"

    def _prepare_review(self, state: TaskState) -> dict[str, Any]:
        draft = self._compose_output(state)
        consistency = state.get("self_consistency", {})
        reasons = list(consistency.get("reasons", []))
        level = (ReviewLevel.DOMAIN_REVIEWER if consistency.get("consistent")
                 else ReviewLevel.SENIOR_REVIEWER)
        request = ReviewRequest(
            level=level,
            cycle=int(state.get("review_cycle", 0)),
            taskId=state["context"].task_id,
            traceId=state["context"].trace_id,
            draft=draft["output"],
            evidenceStatus=draft["evidence_status"],
            sourceCount=len(state.get("sources", [])),
            reasons=reasons,
            allowedActions=[AgentCommandAction.APPROVE, AgentCommandAction.RETURN,
                            AgentCommandAction.REJECT],
        )
        state["draft_output"] = draft["output"]
        state["token_usage"] = draft["token_usage"]
        state["partial"] = draft["partial"]
        state["evidence_status"] = draft["evidence_status"]
        state["execution_status"] = AgentExecutionStatus.AWAITING_REVIEW.value
        state["review_request"] = request.model_dump(mode="json")
        state["trace_events"] = self._event(
            state, "human_review_requested", level=level.value, cycle=request.cycle,
            reasons=reasons, sourceCount=request.sourceCount)
        return self._mutable_result(state)

    def _human_review(self, state: TaskState) -> dict[str, Any]:
        decision = interrupt(state["review_request"])
        action = AgentCommandAction(str(decision["action"]))
        history = list(state.get("review_history", []))
        history.append({
            "stage": "DOMAIN_REVIEW", "cycle": int(state.get("review_cycle", 0)),
            "requestId": decision["requestId"],
            "action": action.value, "reviewerSubjectKey": decision["reviewerSubjectKey"],
            "reviewerRole": decision["reviewerRole"], "comment": decision.get("comment", ""),
        })
        state["review_history"] = history
        state["review_outcome"] = action.value
        state["review_feedback"] = str(decision.get("comment", "")).strip()
        state["review_request"] = {}
        state["review_cycle"] = int(state.get("review_cycle", 0)) + 1
        state["trace_events"] = self._event(
            state, "human_review_decision", stage="DOMAIN_REVIEW", action=action.value,
            cycle=state["review_cycle"] - 1, reviewerRole=decision["reviewerRole"])
        if action == AgentCommandAction.RETURN:
            if state["review_cycle"] > int(state.get("max_review_cycles", 2)):
                state["review_outcome"] = AgentCommandAction.REJECT.value
                state["execution_status"] = AgentExecutionStatus.REJECTED.value
                state["error_code"] = "review_cycles_exhausted"
                state["error_message"] = "maximum human review cycles exceeded"
            else:
                state["revision_round"] = int(state.get("revision_round", 0)) + 1
                send_message(state, "human_reviewer", "research_worker", "HUMAN_RETURN", {
                    "comment": state["review_feedback"], "cycle": state["review_cycle"],
                })
        elif action == AgentCommandAction.REJECT:
            state["execution_status"] = AgentExecutionStatus.REJECTED.value
            state["error_code"] = "human_review_rejected"
            state["error_message"] = state["review_feedback"] or "result rejected by human reviewer"
        return self._mutable_result(state)

    @staticmethod
    def _review_target(state: TaskState) -> str:
        outcome = state.get("review_outcome")
        if outcome == AgentCommandAction.APPROVE.value:
            return "archive"
        if outcome == AgentCommandAction.RETURN.value:
            return "revise"
        return "finalize"

    def _prepare_archive(self, state: TaskState) -> dict[str, Any]:
        request = ReviewRequest(
            level=ReviewLevel.ARCHIVE_OFFICER,
            cycle=int(state.get("review_cycle", 0)),
            taskId=state["context"].task_id,
            traceId=state["context"].trace_id,
            draft=state.get("draft_output", ""),
            evidenceStatus=state.get("evidence_status", EvidenceStatus.INSUFFICIENT.value),
            sourceCount=len(state.get("sources", [])),
            reasons=[],
            allowedActions=[AgentCommandAction.ARCHIVE, AgentCommandAction.RETURN,
                            AgentCommandAction.REJECT],
        )
        state["execution_status"] = AgentExecutionStatus.AWAITING_ARCHIVE.value
        state["review_request"] = request.model_dump(mode="json")
        state["trace_events"] = self._event(
            state, "archive_review_requested", level=request.level.value, cycle=request.cycle)
        return self._mutable_result(state)

    def _archive_review(self, state: TaskState) -> dict[str, Any]:
        decision = interrupt(state["review_request"])
        action = AgentCommandAction(str(decision["action"]))
        history = list(state.get("review_history", []))
        history.append({
            "stage": "ARCHIVE_REVIEW", "cycle": int(state.get("review_cycle", 0)),
            "requestId": decision["requestId"],
            "action": action.value, "reviewerSubjectKey": decision["reviewerSubjectKey"],
            "reviewerRole": decision["reviewerRole"], "comment": decision.get("comment", ""),
        })
        state["review_history"] = history
        state["review_outcome"] = action.value
        state["review_feedback"] = str(decision.get("comment", "")).strip()
        state["review_request"] = {}
        state["review_cycle"] = int(state.get("review_cycle", 0)) + 1
        state["trace_events"] = self._event(
            state, "human_review_decision", stage="ARCHIVE_REVIEW", action=action.value,
            cycle=state["review_cycle"] - 1, reviewerRole=decision["reviewerRole"])
        if action == AgentCommandAction.ARCHIVE:
            state["execution_status"] = AgentExecutionStatus.ARCHIVED.value
        elif action == AgentCommandAction.RETURN:
            state["revision_round"] = int(state.get("revision_round", 0)) + 1
            send_message(state, "archive_officer", "research_worker", "ARCHIVE_RETURN", {
                "comment": state["review_feedback"], "cycle": state["review_cycle"],
            })
        else:
            state["execution_status"] = AgentExecutionStatus.REJECTED.value
            state["error_code"] = "archive_review_rejected"
            state["error_message"] = state["review_feedback"] or "archive officer rejected the result"
        return self._mutable_result(state)

    @staticmethod
    def _archive_target(state: TaskState) -> str:
        return "revise" if state.get("review_outcome") == AgentCommandAction.RETURN.value else "finalize"

    def _planned(self, state: TaskState) -> dict[str, Any]:
        calls = list(state.get("planned_calls", []))
        context = state["context"]
        if "bluecarbon.region.metrics.read" in context.allowed_tools:
            region = next((item for item in REGIONS if item in state["message"]), "")
            if (region and not any(item["name"] == "bluecarbon.region.metrics.read" for item in calls)
                    and len(calls) < context.max_steps):
                calls.append({"name": "bluecarbon.region.metrics.read", "arguments": {"region": region}})
                state["trace_events"] = self._event(
                    state, "planner_completion", tool="bluecarbon.region.metrics.read",
                    reason="explicit_region_requires_region_evidence")
                if context.capability.value == "MULTI_AGENT" and len(calls) > 1:
                    state["route"] = "supervisor"
        for call in calls:
            if call["name"] == "bluecarbon.region.metrics.read":
                region = str(call.get("arguments", {}).get("region", "")).strip()
                if not region or region not in state["message"]:
                    result = ToolResult(
                        tool="bluecarbon.region.metrics.read",
                        status=EvidenceStatus.INSUFFICIENT,
                        content={"reason": "model region argument is not grounded in the user request"},
                    )
                    state.setdefault("tool_results", []).append(result)
                    state["trace_events"] = self._event(
                        state, "tool_rejected", tool=result.tool, reason="argument_not_grounded")
                    continue
            self._call(state, str(call["name"]), dict(call.get("arguments", {})))
        source_ids = [item.get("sourceId", "") for item in state.get("sources", []) if item.get("sourceId")]
        if (source_ids and "bluecarbon.source.verify" in context.allowed_tools
                and state.get("step_count", 0) < context.max_steps):
            self._call(state, "bluecarbon.source.verify", {"sourceIds": source_ids[:12]})
        return self._mutable_result(state)

    def _mutable_result(self, state: TaskState) -> dict[str, Any]:
        return {key: state[key] for key in (
            "route", "step_count", "tool_results", "seen_calls", "sources", "trace_events",
            "active_worker", "handoff_count", "worker_outputs", "agent_messages",
            "revision_round", "max_revision_rounds", "self_consistency", "review_cycle",
            "max_review_cycles", "review_history", "review_request", "review_outcome",
            "review_feedback", "execution_status", "draft_output", "token_usage",
            "partial", "evidence_status", "error_code", "error_message"
        ) if key in state}

    def _finalize(self, state: TaskState) -> dict[str, Any]:
        execution_status = AgentExecutionStatus(
            state.get("execution_status", AgentExecutionStatus.COMPLETED.value))
        if execution_status == AgentExecutionStatus.REJECTED:
            return {
                "success": False, "partial": False,
                "output": state.get("draft_output"),
                "sources": state.get("sources", []),
                "token_usage": int(state.get("token_usage", 0)),
                "execution_status": execution_status.value,
                "error_code": state.get("error_code", "human_review_rejected"),
                "error_message": state.get("error_message", "result rejected by human review"),
                "trace_events": self._event(state, "finalize", executionStatus=execution_status.value),
            }
        if execution_status == AgentExecutionStatus.ARCHIVED:
            output = state.get("draft_output") or "已完成人工复核与归档。"
            return {
                "success": True, "partial": bool(state.get("partial")), "output": output,
                "sources": state.get("sources", []),
                "token_usage": int(state.get("token_usage", 0)),
                "evidence_status": state.get("evidence_status", EvidenceStatus.INSUFFICIENT.value),
                "execution_status": execution_status.value,
                "trace_events": self._event(state, "finalize", executionStatus=execution_status.value,
                                              tokenUsage=int(state.get("token_usage", 0))),
            }
        if state.get("error_code"):
            return {"success": False, "partial": False, "output": None, "token_usage": 0}
        if state.get("route") == "clarify":
            return {"success": False, "partial": False, "output": state.get("clarification"),
                    "error_code": "clarification_required", "error_message": state.get("clarification"),
                    "token_usage": self._estimate_tokens(state.get("clarification") or "")}

        if not any(item.status == EvidenceStatus.RETRIEVED for item in state.get("tool_results", [])):
            return {"success": False, "partial": False, "output": "现有获授权来源不足，无法形成可核查结论。",
                    "error_code": "insufficient_evidence", "error_message": "no authorized evidence",
                    "evidence_status": EvidenceStatus.INSUFFICIENT.value, "token_usage": 18,
                    "execution_status": AgentExecutionStatus.COMPLETED.value}

        composed = self._compose_output(state)
        state["output"] = composed["output"]
        state["partial"] = composed["partial"]
        state["evidence_status"] = composed["evidence_status"]
        state["execution_status"] = AgentExecutionStatus.COMPLETED.value
        self.policy.validate_output(state)
        return {"success": True, "partial": state["partial"], "output": state["output"],
                "sources": state.get("sources", []), "token_usage": composed["token_usage"],
                "evidence_status": state["evidence_status"],
                "execution_status": AgentExecutionStatus.COMPLETED.value,
                "trace_events": self._event(state, "finalize", evidenceStatus=state["evidence_status"],
                                              tokenUsage=composed["token_usage"])}

    def _compose_output(self, state: TaskState) -> dict[str, Any]:
        results = state.get("tool_results", [])
        retrieved = [item for item in results if item.status == EvidenceStatus.RETRIEVED]
        if not retrieved:
            return {"partial": True, "output": "现有获授权来源不足，无法形成可核查结论。",
                    "evidence_status": EvidenceStatus.INSUFFICIENT.value, "token_usage": 18}

        statuses = {item.status for item in results}
        evidence_status = EvidenceStatus.INSUFFICIENT if EvidenceStatus.INSUFFICIENT in statuses else EvidenceStatus.RETRIEVED
        sections: list[str] = []
        for result in retrieved:
            if result.tool == "bluecarbon.region.metrics.read":
                mode = result.content.get("mode")
                notice = result.content.get("notice", "")
                sections.append("区域监测工具返回（%s）：%s%s" % (
                    mode, json.dumps(result.content.get("metrics", {}), ensure_ascii=False, sort_keys=True),
                    f"。{notice}" if notice else ""))
            else:
                excerpts = [item.excerpt for item in result.sources if item.excerpt]
                if excerpts:
                    sections.append("本地证据摘录：" + "；".join(excerpts[:3]))
        output = "\n".join(sections) or "已完成受控工具调用；请根据返回来源继续人工核查。"
        output += f"\n证据状态：{evidence_status.value}；执行路由：{state.get('route')}。"
        budget = state["context"].token_budget
        usage = self._estimate_tokens(state["message"] + output)
        partial = evidence_status != EvidenceStatus.RETRIEVED
        if usage > budget:
            output = output[: max(64, budget * 4)] + "\n[输出已按 Token 预算截断]"
            usage = budget
            partial = True
        return {"partial": partial, "output": output, "token_usage": usage,
                "evidence_status": evidence_status.value}

    @staticmethod
    def _estimate_tokens(text: str) -> int:
        return max(1, (len(text) + 3) // 4)

    def execution_metadata(self, task_id: str) -> dict[str, Any]:
        snapshot = self.graph.get_state({"configurable": {"thread_id": task_id}})
        values = snapshot.values if snapshot is not None else {}
        events = values.get("trace_events", []) if values else []
        return {
            "route": values.get("route") if values else None,
            "tools": [item.get("tool") for item in events if item.get("type") == "tool"],
            "workers": [item.get("agent") for item in events
                        if item.get("type") == "agent_loop_complete"],
            "handoffCount": int(values.get("handoff_count", 0)) if values else 0,
            "executionStatus": values.get("execution_status") if values else None,
            "reviewRequest": values.get("review_request") if values else None,
            "agentMessages": values.get("agent_messages", []) if values else [],
            "selfConsistency": values.get("self_consistency", {}) if values else {},
            "routingAudit": (values.get("routing_audit").model_dump()
                             if values and values.get("routing_audit") else None),
            "traceEvents": events,
        }
