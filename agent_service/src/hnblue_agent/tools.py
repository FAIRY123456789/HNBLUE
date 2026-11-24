from __future__ import annotations

import json
import os
import sys
import threading
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable

import httpx
from jsonschema import Draft202012Validator

from .models import EvidenceStatus, Role, SourceRecord, TaskContext, ToolResult


PROJECT_ROOT = Path(__file__).resolve().parents[3]
RESOURCE_ROOT = PROJECT_ROOT / "agent_service" / "resources"
_RAG_LOCK = threading.Lock()
_RAG_INDEX: Any | None = None


def _env_bool(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


def _rag_chunks_path() -> Path:
    configured = os.getenv("HNBLUE_RAG_CHUNKS")
    return Path(configured).expanduser().resolve() if configured else PROJECT_ROOT / "rag" / "artifacts" / "chunks.jsonl"


def _rag_source_root() -> Path:
    configured = os.getenv("HNBLUE_RAG_SOURCE")
    return Path(configured).expanduser().resolve() if configured else PROJECT_ROOT / "rag" / "examples" / "knowledge"


def _rag_dataset_identity() -> str:
    return os.getenv("HNBLUE_RAG_DATASET_IDENTITY", "HNBLUE_RAG_3_0_LOCAL_INDEX").strip()


def _rag_is_synthetic() -> bool:
    return _env_bool("HNBLUE_RAG_SYNTHETIC", True)


@dataclass(frozen=True)
class ToolSpec:
    name: str
    description: str
    schema: dict[str, Any]
    handler: Callable[[TaskContext, dict[str, Any]], ToolResult]
    read_only: bool = True


class ToolRegistry:
    def __init__(self) -> None:
        self._tools: dict[str, ToolSpec] = {}

    def register(self, spec: ToolSpec) -> None:
        if spec.name in self._tools:
            raise ValueError(f"duplicate tool: {spec.name}")
        if not spec.read_only:
            raise ValueError("HNBLUE phase-two registry accepts read-only tools only")
        self._tools[spec.name] = spec

    def execute(self, context: TaskContext, name: str, arguments: dict[str, Any]) -> ToolResult:
        try:
            spec = self._tools[name]
        except KeyError as exc:
            raise ValueError(f"unknown tool: {name}") from exc
        errors = sorted(Draft202012Validator(spec.schema).iter_errors(arguments), key=lambda item: list(item.path))
        if errors:
            raise ValueError(f"invalid arguments for {name}: {errors[0].message}")
        return spec.handler(context, arguments)

    def describe(self, allowed_tools: frozenset[str] | None = None) -> list[dict[str, Any]]:
        return [
            {"name": item.name, "description": item.description, "readOnly": item.read_only, "schema": item.schema}
            for item in self._tools.values() if allowed_tools is None or item.name in allowed_tools
        ]


def _rag_index() -> Any:
    global _RAG_INDEX
    if _RAG_INDEX is not None:
        return _RAG_INDEX
    with _RAG_LOCK:
        if _RAG_INDEX is None:
            # The RAG package is a sibling in this monorepo, not part of the
            # service wheel. Make its absolute project root available so the
            # service behaves the same regardless of its launch directory.
            project = str(PROJECT_ROOT)
            if project not in sys.path:
                sys.path.insert(0, project)
            from rag.hnblue_rag import HybridRagIndex, load_domain_terms

            _RAG_INDEX = HybridRagIndex.from_jsonl(
                _rag_chunks_path(),
                load_domain_terms(_rag_source_root()),
            )
    return _RAG_INDEX


def knowledge_search(context: TaskContext, arguments: dict[str, Any]) -> ToolResult:
    query = str(arguments.get("query", "")).strip()
    top_k = max(1, min(int(arguments.get("topK", 4)), 6))
    payload = _rag_index().context(query, top_k=top_k)
    raw_status = str(payload.get("evidenceStatus", "insufficient")).lower()
    status = {
        "retrieved": EvidenceStatus.RETRIEVED,
        "blocked_prompt_injection": EvidenceStatus.BLOCKED,
    }.get(raw_status, EvidenceStatus.INSUFFICIENT)
    sources = [
        SourceRecord(
            sourceId=f"rag:{item.get('recordId', '')}",
            title=str(item.get("title", "HNBLUE knowledge source")),
            datasetIdentity=_rag_dataset_identity(),
            visibility=os.getenv("HNBLUE_RAG_VISIBILITY", "PUBLIC").strip(),
            recordId=str(item.get("recordId", "")),
            excerpt=str(item.get("excerpt", ""))[:420],
            synthetic=_rag_is_synthetic(),
            governmentReviewed=_env_bool("HNBLUE_RAG_GOVERNMENT_REVIEWED", False),
            reviewStatus=os.getenv("HNBLUE_RAG_REVIEW_STATUS", "not-reviewed").strip(),
            accessStatus=os.getenv("HNBLUE_RAG_ACCESS_STATUS", "test-only").strip(),
        )
        for item in payload.get("sources", [])
    ]
    return ToolResult(
        tool="bluecarbon.knowledge.search",
        status=status,
        content={"query": query, "evidenceStatus": raw_status, "matches": len(sources)},
        sources=sources,
    )


def methodology_lookup(context: TaskContext, arguments: dict[str, Any]) -> ToolResult:
    topic = str(arguments.get("topic", "")).strip()
    result = knowledge_search(context, {"query": f"蓝碳 方法学 核证 来源 {topic}", "topK": 5})
    return result.model_copy(update={"tool": "bluecarbon.methodology.lookup"})


def _compact_rows(data: dict[str, Any], name: str, fields: tuple[str, ...], limit: int) -> list[dict[str, Any]]:
    rows = data.get(name, [])
    if not isinstance(rows, list):
        return []
    compact: list[dict[str, Any]] = []
    for row in rows[:limit]:
        if not isinstance(row, dict):
            continue
        selected: dict[str, Any] = {}
        for field in fields:
            value = row.get(field)
            if value is not None:
                selected[field] = value[:240] if isinstance(value, str) else value
        compact.append(selected)
    return compact


def _compact_region_context(data: dict[str, Any]) -> dict[str, Any]:
    return {
        "mangroveCover": _compact_rows(data, "mangrove_cover", (
            "region_name", "year", "area_value", "unit", "cover_ratio", "method",
            "quality_flag", "source_code", "is_simulated",
        ), 4),
        "regionMetrics": _compact_rows(data, "region_metrics", (
            "region_name", "indicator_code", "indicator_name", "metric_category", "year",
            "value", "unit", "stat_method", "quality_flag", "is_proxy", "is_simulated",
            "source_code",
        ), 8),
        "literatureCarbon": _compact_rows(data, "literature_carbon", (
            "region_name", "site_name", "ecosystem_type", "carbon_pool", "value_mg_ha",
            "unit", "method_type", "data_type", "source_code", "citation",
        ), 5),
        "boundaryNotes": [str(item)[:300] for item in data.get("boundary_notes", [])[:5]],
    }


def _contains_synthetic_v2_rows(data: dict[str, Any]) -> bool:
    for name in ("mangrove_cover", "region_metrics", "literature_carbon"):
        rows = data.get(name, [])
        if not isinstance(rows, list):
            continue
        for row in rows:
            if isinstance(row, dict) and bool(row.get("is_simulated")):
                return True
    return False


def region_metrics_read(context: TaskContext, arguments: dict[str, Any]) -> ToolResult:
    region = str(arguments.get("region", "")).strip()
    if not region:
        return ToolResult(tool="bluecarbon.region.metrics.read", status=EvidenceStatus.INSUFFICIENT,
                          content={"reason": "region is required"})
    base_url = os.getenv("HNBLUE_JAVA_BASE_URL", "").rstrip("/")
    if base_url:
        try:
            with httpx.Client(timeout=5.0, trust_env=False) as client:
                response = client.get(
                    f"{base_url}/api/v2/ai/context",
                    params={"region": region},
                )
                response.raise_for_status()
                payload = response.json()
        except (httpx.HTTPError, ValueError) as exc:
            return ToolResult(
                tool="bluecarbon.region.metrics.read",
                status=EvidenceStatus.INSUFFICIENT,
                content={
                    "region": region,
                    "reason": "java V2 region context API unavailable",
                    "errorType": type(exc).__name__,
                    "mode": "java-v2-api",
                },
            )

        data = payload.get("data") if isinstance(payload, dict) else None
        meta = payload.get("meta") if isinstance(payload, dict) and isinstance(payload.get("meta"), dict) else {}
        evidence_rows = sum(
            len(data.get(name, []))
            for name in ("mangrove_cover", "region_metrics", "literature_carbon")
            if isinstance(data, dict) and isinstance(data.get(name), list)
        )
        if payload.get("success") is not True or evidence_rows == 0:
            return ToolResult(
                tool="bluecarbon.region.metrics.read",
                status=EvidenceStatus.INSUFFICIENT,
                content={
                    "region": region,
                    "reason": "java V2 region context API returned no usable rows",
                    "metrics": data if isinstance(data, dict) else {},
                    "mode": "java-v2-api",
                },
            )
        contains_synthetic = _contains_synthetic_v2_rows(data)
        title_prefix = "HNBLUE V2 synthetic test context" if contains_synthetic else "HNBLUE V2 region context"
        declared_mode = meta.get("data_mode")
        source = SourceRecord(
            sourceId=f"java:v2:region-context:{region}", title=f"{title_prefix}: {region}",
            datasetIdentity=str(meta.get("dataset_identity", "HNBLUE_JAVA_V2_CURRENT_DATABASE")),
            visibility=str(meta.get("access_status", "PUBLIC")),
            recordId=region,
            synthetic=contains_synthetic or (
                declared_mode is not None and str(declared_mode).upper() != "AUTHORIZED_OFFICIAL"
            ),
            governmentReviewed=bool(meta.get("government_reviewed", False)),
            reviewStatus=str(meta.get("review_status", "unknown")),
            accessStatus=str(meta.get("access_status", "unknown")),
        )
        compact = _compact_region_context(data)
        return ToolResult(tool="bluecarbon.region.metrics.read", status=EvidenceStatus.RETRIEVED,
                          content={"region": region, "metrics": compact, "evidenceRows": evidence_rows,
                                   "mode": "java-v2-api",
                                   "provenanceNotice": (
                                       "Java V2 rows include explicitly simulated local test data; "
                                       "they are not real government observations."
                                       if contains_synthetic else
                                       "Java V2 rows did not carry a simulated-data flag."
                                   )}, sources=[source])

    fixture = json.loads((RESOURCE_ROOT / "monitoring_snapshot.json").read_text(encoding="utf-8"))
    row = fixture.get("regions", {}).get(region)
    if row is None:
        return ToolResult(tool="bluecarbon.region.metrics.read", status=EvidenceStatus.INSUFFICIENT,
                          content={"region": region, "reason": "no isolated-stage fixture for region"})
    source = SourceRecord(
        sourceId=f"fixture:monitoring:{region}", title=f"Synthetic monitoring fixture: {region}",
        datasetIdentity=fixture["datasetIdentity"], visibility="PUBLIC", recordId=region,
        excerpt=fixture["notice"], synthetic=True,
    )
    return ToolResult(tool="bluecarbon.region.metrics.read", status=EvidenceStatus.RETRIEVED,
                      content={"region": region, "metrics": row, "mode": "synthetic-fixture",
                               "notice": fixture["notice"]}, sources=[source], units=fixture.get("units", {}))


def source_verify(context: TaskContext, arguments: dict[str, Any]) -> ToolResult:
    source_ids = [str(item) for item in arguments.get("sourceIds", [])][:12]
    known = {f"rag:{chunk.chunk_id}" for chunk in _rag_index().chunks}
    verified = [item for item in source_ids if item in known or item.startswith("java:v2:region-context:")]
    synthetic = [item for item in source_ids if item.startswith("fixture:") or (item in known and _rag_is_synthetic())]
    authorized = [item for item in source_ids if item in known and not _rag_is_synthetic()]
    unknown = [item for item in source_ids if item not in verified and item not in synthetic]
    status = EvidenceStatus.RETRIEVED if verified and not unknown else EvidenceStatus.INSUFFICIENT
    return ToolResult(
        tool="bluecarbon.source.verify", status=status,
        content={"verified": verified, "authorizedOfficial": authorized,
                 "syntheticTestOnly": synthetic, "unknown": unknown},
        sources=[],
    )


def default_registry() -> ToolRegistry:
    registry = ToolRegistry()
    registry.register(ToolSpec(
        "bluecarbon.knowledge.search", "Search the existing local HNBLUE RAG index with evidence gating.",
        {"type": "object", "required": ["query"], "properties": {
            "query": {"type": "string", "minLength": 1, "maxLength": 4000},
            "topK": {"type": "integer", "minimum": 1, "maximum": 6},
        }, "additionalProperties": False}, knowledge_search))
    registry.register(ToolSpec(
        "bluecarbon.region.metrics.read", "Read a whitelisted Hainan region through the Java V2 current-database API, or an explicit synthetic fixture in isolated tests.",
        {"type": "object", "required": ["region"], "properties": {
            "region": {"type": "string", "enum": [
                "琼海", "文昌", "万宁", "海口", "三亚", "儋州", "五指山", "东方",
                "定安县", "屯昌县", "澄迈县", "临高县", "白沙", "昌江", "乐东",
                "陵水", "保亭", "琼中",
            ]},
        }, "additionalProperties": False}, region_metrics_read))
    registry.register(ToolSpec(
        "bluecarbon.methodology.lookup", "Retrieve methodology and verification evidence from the local RAG index.",
        {"type": "object", "required": ["topic"], "properties": {
            "topic": {"type": "string", "minLength": 1, "maxLength": 1000},
        }, "additionalProperties": False}, methodology_lookup))
    registry.register(ToolSpec(
        "bluecarbon.source.verify", "Verify HNBLUE source identifiers without fetching arbitrary URLs.",
        {"type": "object", "required": ["sourceIds"], "properties": {
            "sourceIds": {"type": "array", "maxItems": 12, "items": {"type": "string"}},
        }, "additionalProperties": False}, source_verify))
    return registry
