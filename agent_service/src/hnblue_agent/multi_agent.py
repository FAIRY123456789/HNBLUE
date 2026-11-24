from __future__ import annotations

import re
import uuid
from dataclasses import dataclass
from typing import Any, Callable

from .models import EvidenceStatus, TaskState, ToolResult


ToolCaller = Callable[[TaskState, str, dict[str, Any]], ToolResult]
EventWriter = Callable[..., list[dict[str, Any]]]


def send_message(state: TaskState, sender: str, recipient: str, kind: str,
                 content: dict[str, Any]) -> dict[str, Any]:
    """Append a small, structured inter-agent message to checkpointed state."""
    item = {
        "messageId": uuid.uuid4().hex,
        "from": sender,
        "to": recipient,
        "kind": kind,
        "round": int(state.get("revision_round", 0)),
        "content": content,
    }
    state.setdefault("agent_messages", []).append(item)
    return item


def inbox(state: TaskState, agent: str, *, kind: str | None = None) -> list[dict[str, Any]]:
    rows = [item for item in state.get("agent_messages", [])
            if item.get("to") in {agent, "broadcast"}]
    return rows if kind is None else [item for item in rows if item.get("kind") == kind]


def _tool_already_ran(state: TaskState, tool: str) -> bool:
    return any(item.tool == tool for item in state.get("tool_results", []))


@dataclass(frozen=True)
class ResearchAgent:
    name: str = "research_worker"

    def run(self, state: TaskState, call: ToolCaller, event: EventWriter) -> None:
        state["active_worker"] = self.name
        state["trace_events"] = event(state, "agent_loop_start", agent=self.name,
                                      round=state.get("revision_round", 0))
        feedback = str(state.get("review_feedback", "")).strip()
        query = state["message"] if not feedback else f"{state['message']}；复核意见：{feedback}"
        top_k = 4 + min(2, int(state.get("revision_round", 0)))
        tools: list[str] = []
        result = call(state, "bluecarbon.knowledge.search", {"query": query, "topK": top_k})
        tools.append(result.tool)
        if ("bluecarbon.methodology.lookup" in state["context"].allowed_tools
                and re.search(r"方法|核证|标准", state["message"])
                and state.get("step_count", 0) < state["context"].max_steps
                and not _tool_already_ran(state, "bluecarbon.methodology.lookup")):
            result = call(state, "bluecarbon.methodology.lookup", {"topic": state["message"]})
            tools.append(result.tool)
        source_ids = [item.get("sourceId", "") for item in state.get("sources", []) if item.get("sourceId")]
        send_message(state, self.name, "monitoring_worker", "RESEARCH_PACKAGE", {
            "tools": tools, "sourceIds": source_ids[-12:], "feedbackApplied": bool(feedback),
        })
        send_message(state, self.name, "consistency_agent", "RESEARCH_PACKAGE", {
            "tools": tools, "sourceIds": source_ids[-12:],
        })
        state.setdefault("worker_outputs", []).append({
            "worker": self.name, "tools": tools, "sourceCount": len(state.get("sources", [])),
            "round": int(state.get("revision_round", 0)),
        })
        state["trace_events"] = event(state, "agent_loop_complete", agent=self.name, tools=tools)


@dataclass(frozen=True)
class MonitoringAgent:
    regions: tuple[str, ...]
    name: str = "monitoring_worker"

    def run(self, state: TaskState, call: ToolCaller, event: EventWriter) -> None:
        state["active_worker"] = self.name
        state["trace_events"] = event(state, "agent_loop_start", agent=self.name,
                                      inboxCount=len(inbox(state, self.name)))
        tools: list[str] = []
        region = next((item for item in self.regions if item in state["message"]), "")
        existing = _tool_already_ran(state, "bluecarbon.region.metrics.read")
        if region and not existing:
            result = call(state, "bluecarbon.region.metrics.read", {"region": region})
            tools.append(result.tool)
            status = result.status.value
        elif region:
            status = "REUSED"
        else:
            result = ToolResult(
                tool="bluecarbon.region.metrics.read", status=EvidenceStatus.INSUFFICIENT,
                content={"reason": "a supported region is required"},
            )
            state.setdefault("tool_results", []).append(result)
            state["trace_events"] = event(
                state, "tool_skipped", tool=result.tool, reason="region_required", agent=self.name)
            status = result.status.value
        payload = {"region": region, "status": status, "tools": tools}
        send_message(state, self.name, "evidence_merge", "MONITORING_PACKAGE", payload)
        send_message(state, self.name, "consistency_agent", "MONITORING_PACKAGE", payload)
        state.setdefault("worker_outputs", []).append({
            "worker": self.name, "tools": tools, "sourceCount": len(state.get("sources", [])),
            "round": int(state.get("revision_round", 0)),
        })
        state["trace_events"] = event(state, "agent_loop_complete", agent=self.name, tools=tools)


@dataclass(frozen=True)
class EvidenceAgent:
    name: str = "evidence_merge"

    def run(self, state: TaskState, call: ToolCaller, event: EventWriter) -> None:
        state["active_worker"] = self.name
        state["trace_events"] = event(state, "agent_loop_start", agent=self.name,
                                      inboxCount=len(inbox(state, self.name)))
        source_ids = list(dict.fromkeys(
            item.get("sourceId", "") for item in state.get("sources", []) if item.get("sourceId")))
        tools: list[str] = []
        if (source_ids and "bluecarbon.source.verify" in state["context"].allowed_tools
                and state.get("step_count", 0) < state["context"].max_steps):
            args = {"sourceIds": source_ids[:12]}
            # A revision may not have produced a new source set. In that case the
            # previous verification remains valid and duplicate-call policy stays intact.
            from .policy import tool_fingerprint
            if tool_fingerprint("bluecarbon.source.verify", args) not in state.get("seen_calls", set()):
                result = call(state, "bluecarbon.source.verify", args)
                tools.append(result.tool)
        verification = {
            "sourceIds": source_ids[:12], "sourceCount": len(source_ids),
            "verified": _tool_already_ran(state, "bluecarbon.source.verify"),
        }
        send_message(state, self.name, "consistency_agent", "VERIFICATION_REPORT", verification)
        state.setdefault("worker_outputs", []).append({
            "worker": self.name, "tools": tools, "sourceCount": len(source_ids),
            "round": int(state.get("revision_round", 0)),
        })
        state["trace_events"] = event(state, "agent_loop_complete", agent=self.name, tools=tools)


@dataclass(frozen=True)
class ConsistencyAgent:
    name: str = "consistency_agent"

    def run(self, state: TaskState, event: EventWriter) -> None:
        state["active_worker"] = self.name
        state["trace_events"] = event(state, "agent_loop_start", agent=self.name,
                                      inboxCount=len(inbox(state, self.name)))
        reasons: list[str] = []
        if not inbox(state, self.name, kind="RESEARCH_PACKAGE"):
            reasons.append("research_package_missing")
        monitoring = inbox(state, self.name, kind="MONITORING_PACKAGE")
        if not monitoring:
            reasons.append("monitoring_package_missing")
        elif any(item.get("content", {}).get("status") == EvidenceStatus.INSUFFICIENT.value
                 for item in monitoring):
            reasons.append("monitoring_evidence_insufficient")
        if not inbox(state, self.name, kind="VERIFICATION_REPORT"):
            reasons.append("verification_report_missing")
        if not state.get("sources"):
            reasons.append("no_sources")
        if any(item.status == EvidenceStatus.CONFLICT for item in state.get("tool_results", [])):
            reasons.append("evidence_conflict")
        consistent = not reasons
        can_revise = (not consistent
                      and int(state.get("revision_round", 0)) < int(state.get("max_revision_rounds", 1))
                      and state.get("step_count", 0) < state["context"].max_steps)
        outcome = "REVISE" if can_revise else "REVIEW"
        state["self_consistency"] = {
            "consistent": consistent,
            "reasons": reasons,
            "outcome": outcome,
            "round": int(state.get("revision_round", 0)),
        }
        if can_revise:
            state["revision_round"] = int(state.get("revision_round", 0)) + 1
            send_message(state, self.name, "research_worker", "REVISION_REQUEST", {"reasons": reasons})
        else:
            send_message(state, self.name, "human_reviewer", "REVIEW_PACKET", {
                "consistent": consistent, "reasons": reasons,
                "sourceCount": len(state.get("sources", [])),
            })
        state.setdefault("worker_outputs", []).append({
            "worker": self.name, "tools": [], "sourceCount": len(state.get("sources", [])),
            "round": int(state.get("revision_round", 0)), "outcome": outcome,
        })
        state["trace_events"] = event(
            state, "agent_loop_complete", agent=self.name, outcome=outcome, reasons=reasons)
