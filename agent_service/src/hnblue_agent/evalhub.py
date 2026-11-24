from __future__ import annotations

import json
import hashlib
import platform
import sqlite3
import statistics
import sys
import time
import uuid
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable

from .graph import AgentHarness
from .models import AgentRequest, AgentResult, Capability, Role, TaskContext
from .policy import PolicyViolation


@dataclass(frozen=True)
class EvalCase:
    case_id: str
    category: str
    role: str
    capability: str
    allowed_tools: list[str]
    message: str
    expected_success: bool
    expected_error: str | None = None
    expected_source_identity: str | None = None
    expect_synthetic: bool | None = None
    scenario: str = ""
    expected_route: str | None = None
    expected_workers: list[str] | None = None
    min_handoffs: int | None = None
    forbidden_tools: list[str] | None = None
    required_source_ids: list[str] | None = None
    success_criteria: str = ""
    failure_severity: str = "MEDIUM"
    annotator: str = "codex-initial-draft"
    review_status: str = "NEEDS_DOMAIN_REVIEW"
    privacy_level: str = "PUBLIC_OR_SYNTHETIC"

    @classmethod
    def from_dict(cls, item: dict[str, Any]) -> "EvalCase":
        return cls(
            case_id=item["id"], category=item["category"], role=item["role"],
            capability=item["capability"], allowed_tools=item["allowedTools"], message=item["message"],
            expected_success=item["expectedSuccess"], expected_error=item.get("expectedError"),
            expected_source_identity=item.get("expectedSourceIdentity"),
            expect_synthetic=item.get("expectSynthetic"),
            scenario=item.get("scenario", item["category"]),
            expected_route=item.get("expectedRoute"),
            expected_workers=item.get("expectedWorkers"),
            min_handoffs=item.get("minHandoffs"),
            forbidden_tools=item.get("forbiddenTools", []),
            required_source_ids=item.get("requiredSourceIds", []),
            success_criteria=item.get("successCriteria", "matches executable assertions"),
            failure_severity=item.get("failureSeverity", "MEDIUM"),
            annotator=item.get("annotator", "codex-initial-draft"),
            review_status=item.get("reviewStatus", "NEEDS_DOMAIN_REVIEW"),
            privacy_level=item.get("privacyLevel", "PUBLIC_OR_SYNTHETIC"),
        )


def load_cases(path: Path) -> list[EvalCase]:
    with path.open("r", encoding="utf-8") as handle:
        return [EvalCase.from_dict(json.loads(line)) for line in handle if line.strip()]


class EvalHub:
    def __init__(self, harness: AgentHarness, database: Path) -> None:
        self.harness = harness
        self.database = database
        self.database.parent.mkdir(parents=True, exist_ok=True)
        self._init_schema()

    def run(self, cases: Iterable[EvalCase], mode: str) -> dict[str, Any]:
        if mode not in {"fixed-rag", "langgraph-r0", "langgraph-two-worker", "langgraph-ollama",
                        "langgraph-shadow"}:
            raise ValueError("unsupported evaluation mode")
        case_list = list(cases)
        uses_java_v2_current_data = any(
            case.expected_source_identity == "HNBLUE_JAVA_V2_CURRENT_DATABASE"
            or any(source_id.startswith("java:v2:region-context:") for source_id in case.required_source_ids)
            for case in case_list
        )
        dataset_sha = hashlib.sha256(json.dumps(
            [asdict(case) for case in case_list], ensure_ascii=False, sort_keys=True,
            separators=(",", ":"),
        ).encode("utf-8")).hexdigest()
        dependency_file = Path(__file__).resolve().parents[2] / "requirements.lock.txt"
        dependency_sha = hashlib.sha256(dependency_file.read_bytes()).hexdigest()
        run_id = str(uuid.uuid4())
        started = datetime.now(timezone.utc).isoformat()
        rows: list[dict[str, Any]] = []
        for case in case_list:
            task_id = f"eval-{mode.replace('-', '')}-{run_id[:8]}-{case.case_id}"[:64]
            request = AgentRequest(
                taskId=task_id, traceId=f"trace-{task_id}", subjectKey=f"{case.role}:eval",
                role=case.role, capability=case.capability, allowedTools=case.allowed_tools,
                tokenBudget=2000, maxSteps=4, message=case.message,
            )
            started_case = time.perf_counter()
            result = self.harness.execute(request) if mode.startswith("langgraph-") else self._fixed_rag(request)
            latency_ms = (time.perf_counter() - started_case) * 1000
            metadata = self.harness.execution_metadata(task_id) if mode.startswith("langgraph-") else {
                "route": None, "tools": [], "workers": [], "handoffCount": 0,
            }
            passed, reasons = self._score(case, result, metadata, mode)
            row = {
                "caseId": case.case_id, "category": case.category, "passed": passed,
                "reasons": reasons, "latencyMs": round(latency_ms, 3),
                "success": result.success, "partial": result.partial, "errorCode": result.errorCode,
                "tokenUsage": result.tokenUsage, "sources": result.sources,
                "route": metadata["route"], "tools": metadata["tools"],
                "workers": metadata.get("workers", []), "handoffCount": metadata.get("handoffCount", 0),
                "routingAudit": metadata.get("routingAudit"),
            }
            rows.append(row)
            self._insert_case(run_id, mode, row)
        latencies = [item["latencyMs"] for item in rows]
        summary = {
            "runId": run_id,
            "mode": mode,
            "datasetVersion": (
                "HNBLUE_EVAL_STAGE3_V5_REAL_JAVA_PROVENANCE"
                if uses_java_v2_current_data else
                "HNBLUE_EVAL_STAGE3_V5_SYNTHETIC_PROVENANCE"
                if any(case.expected_workers is not None for case in case_list) else
                "HNBLUE_EVAL_RC2_V2"
            ),
            "datasetSha256": dataset_sha,
            "modelId": self.harness.provider.name if mode.startswith("langgraph-") else "fixed-rag-v1",
            "promptVersion": "hnblue-tool-planner-v3-worker-handoff",
            "toolSchemaVersion": "bluecarbon-capability-kit-v1",
            "javaBuildVersion": "rc3-isolated-local",
            "pythonDependencyLock": "sha256:" + dependency_sha,
            "startedAt": started,
            "cases": len(rows),
            "passed": sum(1 for item in rows if item["passed"]),
            "passRate": round(sum(1 for item in rows if item["passed"]) / max(1, len(rows)), 4),
            "p50Ms": round(statistics.median(latencies), 3) if latencies else 0,
            "p95Ms": round(self._percentile(latencies, 0.95), 3) if latencies else 0,
            "environment": {
                "python": sys.version.split()[0], "platform": platform.platform(),
                "provider": self.harness.provider.name,
                "dependencyFile": "agent_service/requirements.lock.txt",
                "sourceRevision": "isolated-copy-from-5ff77724c440257854eab01c9ccd5ebe5cba082c",
            },
            "results": rows,
        }
        self._insert_run(summary)
        return summary

    def _fixed_rag(self, request: AgentRequest) -> AgentResult:
        try:
            context = TaskContext.from_request(request)
            self.harness.policy.validate_context(context)
            self.harness.policy.guard_message(request.message)
            if "bluecarbon.knowledge.search" not in context.allowed_tools:
                return AgentResult(success=False, errorCode="tool_forbidden", errorMessage="fixed RAG unavailable")
            tool_result = self.harness.registry.execute(context, "bluecarbon.knowledge.search",
                                                        {"query": request.message, "topK": 4})
            if not tool_result.sources:
                return AgentResult(success=False, errorCode="insufficient_evidence", errorMessage="no evidence")
            sources = [item.model_dump() for item in tool_result.sources]
            return AgentResult(success=True, output="Fixed RAG retrieved local evidence.", sources=sources,
                               tokenUsage=16)
        except PolicyViolation as exc:
            return AgentResult(success=False, errorCode=exc.code, errorMessage=str(exc))
        except Exception as exc:
            return AgentResult(success=False, errorCode="fixed_rag_failed", errorMessage=type(exc).__name__)

    def _score(self, case: EvalCase, result: AgentResult, metadata: dict[str, Any],
               mode: str) -> tuple[bool, list[str]]:
        failures: list[str] = []
        if result.success != case.expected_success:
            failures.append(f"success={result.success}, expected={case.expected_success}")
        if case.expected_error and result.errorCode != case.expected_error:
            failures.append(f"errorCode={result.errorCode}, expected={case.expected_error}")
        if case.expected_source_identity and not any(
                item.get("datasetIdentity") == case.expected_source_identity for item in result.sources):
            failures.append("expected source identity missing")
        if case.expect_synthetic is not None and bool(any(item.get("synthetic") for item in result.sources)) != case.expect_synthetic:
            failures.append("synthetic-source expectation mismatch")
        if mode.startswith("langgraph-") and case.expected_route and metadata.get("route") != case.expected_route:
            failures.append(f"route={metadata.get('route')}, expected={case.expected_route}")
        if mode.startswith("langgraph-") and case.expected_workers is not None:
            actual_workers = metadata.get("workers", [])
            if actual_workers != case.expected_workers:
                failures.append(f"workers={actual_workers}, expected={case.expected_workers}")
        if (mode.startswith("langgraph-") and case.min_handoffs is not None
                and int(metadata.get("handoffCount", 0)) < case.min_handoffs):
            failures.append(
                f"handoffCount={metadata.get('handoffCount', 0)}, minimum={case.min_handoffs}")
        called_tools = set(metadata.get("tools", []))
        forbidden = called_tools.intersection(case.forbidden_tools or [])
        if forbidden:
            failures.append("forbidden tools called: " + ",".join(sorted(forbidden)))
        source_ids = {str(item.get("sourceId", "")) for item in result.sources}
        missing_sources = [item for item in (case.required_source_ids or []) if item not in source_ids]
        if missing_sources:
            failures.append("required source ids missing: " + ",".join(missing_sources))
        return not failures, failures

    def _init_schema(self) -> None:
        with sqlite3.connect(self.database) as connection:
            connection.executescript("""
                CREATE TABLE IF NOT EXISTS eval_run (
                    run_id TEXT PRIMARY KEY, mode TEXT NOT NULL, dataset_version TEXT NOT NULL,
                    started_at TEXT NOT NULL, case_count INTEGER NOT NULL, passed_count INTEGER NOT NULL,
                    pass_rate REAL NOT NULL, p50_ms REAL NOT NULL, p95_ms REAL NOT NULL,
                    environment_json TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS eval_case_result (
                    run_id TEXT NOT NULL, case_id TEXT NOT NULL, category TEXT NOT NULL,
                    passed INTEGER NOT NULL, latency_ms REAL NOT NULL, success INTEGER NOT NULL,
                    partial INTEGER NOT NULL, error_code TEXT, token_usage INTEGER NOT NULL,
                    reasons_json TEXT NOT NULL, sources_json TEXT NOT NULL,
                    PRIMARY KEY (run_id, case_id)
                );
            """)

    def _insert_case(self, run_id: str, mode: str, row: dict[str, Any]) -> None:
        with sqlite3.connect(self.database) as connection:
            connection.execute(
                "INSERT INTO eval_case_result VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (run_id, row["caseId"], row["category"], int(row["passed"]), row["latencyMs"],
                 int(row["success"]), int(row["partial"]), row["errorCode"], row["tokenUsage"],
                 json.dumps(row["reasons"], ensure_ascii=False),
                 json.dumps(row["sources"], ensure_ascii=False)),
            )

    def _insert_run(self, summary: dict[str, Any]) -> None:
        with sqlite3.connect(self.database) as connection:
            connection.execute(
                "INSERT INTO eval_run VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (summary["runId"], summary["mode"], summary["datasetVersion"], summary["startedAt"],
                 summary["cases"], summary["passed"], summary["passRate"], summary["p50Ms"], summary["p95Ms"],
                 json.dumps(summary["environment"], ensure_ascii=False)),
            )

    @staticmethod
    def _percentile(values: list[float], quantile: float) -> float:
        ordered = sorted(values)
        index = min(len(ordered) - 1, max(0, int(round((len(ordered) - 1) * quantile))))
        return ordered[index]
