from __future__ import annotations

import hashlib
import json
import re
from dataclasses import dataclass
from typing import Any

from .models import Capability, EvidenceStatus, Role, TaskContext, TaskState


CAPABILITY_TOOLS: dict[Capability, frozenset[str]] = {
    Capability.BASIC_QA: frozenset({"bluecarbon.knowledge.search"}),
    Capability.PROFESSIONAL_ANALYSIS: frozenset({
        "bluecarbon.knowledge.search",
        "bluecarbon.region.metrics.read",
        "bluecarbon.source.verify",
    }),
    Capability.MULTI_AGENT: frozenset({
        "bluecarbon.knowledge.search",
        "bluecarbon.region.metrics.read",
        "bluecarbon.methodology.lookup",
        "bluecarbon.source.verify",
    }),
}

ROLE_CAPABILITIES: dict[Role, frozenset[Capability]] = {
    Role.PUBLIC: frozenset({Capability.BASIC_QA}),
    Role.PROFESSIONAL: frozenset({
        Capability.BASIC_QA,
        Capability.PROFESSIONAL_ANALYSIS,
        Capability.MULTI_AGENT,
    }),
    Role.ADMIN: frozenset(Capability),
}

INJECTION_PATTERNS = (
    r"ignore (?:all|previous) instructions",
    r"reveal (?:the )?(?:system prompt|secret|token)",
    r"绕过.{0,12}(?:权限|来源|安全|配额)",
    r"忽略.{0,12}(?:规则|指令|权限)",
    r"输出.{0,12}(?:密钥|密码|token)",
)


class PolicyViolation(RuntimeError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


def tool_fingerprint(tool_name: str, arguments: dict[str, Any]) -> str:
    return hashlib.sha256(
        (tool_name + "\n" + json.dumps(arguments, ensure_ascii=False, sort_keys=True)).encode("utf-8")
    ).hexdigest()


@dataclass(frozen=True)
class ExecutionPolicy:
    max_handoffs: int = 2

    def validate_context(self, context: TaskContext) -> None:
        if context.capability not in ROLE_CAPABILITIES[context.role]:
            raise PolicyViolation("role_capability_forbidden", "role cannot use requested capability")
        ceiling = CAPABILITY_TOOLS[context.capability]
        if not context.allowed_tools.issubset(ceiling):
            raise PolicyViolation("tool_scope_escalation", "request contains tools outside the capability scope")

    def guard_message(self, message: str) -> None:
        normalized = " ".join(message.lower().split())
        if any(re.search(pattern, normalized, re.IGNORECASE) for pattern in INJECTION_PATTERNS):
            raise PolicyViolation("prompt_injection", "request attempted to bypass HNBLUE safety boundaries")

    def before_tool(self, state: TaskState, tool_name: str, arguments: dict[str, Any]) -> str:
        context = state["context"]
        if tool_name not in context.allowed_tools:
            raise PolicyViolation("tool_forbidden", f"tool is not allowed for this task: {tool_name}")
        if state.get("step_count", 0) >= context.max_steps:
            raise PolicyViolation("step_budget_exhausted", "maximum Agent steps reached")
        fingerprint = tool_fingerprint(tool_name, arguments)
        if fingerprint in state.get("seen_calls", set()):
            raise PolicyViolation("duplicate_tool_call", "duplicate tool call was suppressed")
        return fingerprint

    def before_handoff(self, state: TaskState, target_worker: str) -> int:
        context = state["context"]
        if context.capability != Capability.MULTI_AGENT:
            raise PolicyViolation("handoff_forbidden", "task capability does not allow worker handoffs")
        current = state.get("handoff_count", 0)
        if current >= self.max_handoffs:
            raise PolicyViolation("handoff_budget_exhausted", "maximum worker handoffs reached")
        if target_worker not in {"monitoring_worker", "evidence_merge"}:
            raise PolicyViolation("handoff_target_forbidden", "worker handoff target is not registered")
        return current + 1

    def validate_output(self, state: TaskState) -> None:
        context = state["context"]
        for source in state.get("sources", []):
            visibility = str(source.get("visibility", "PUBLIC")).upper()
            if context.role == Role.PUBLIC and visibility != "PUBLIC":
                raise PolicyViolation("source_visibility_violation", "output contains a non-public source")
        if state.get("evidence_status") in {EvidenceStatus.INSUFFICIENT, EvidenceStatus.CONFLICT}:
            state["partial"] = True
