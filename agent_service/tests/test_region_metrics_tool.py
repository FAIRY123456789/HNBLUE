from __future__ import annotations

import httpx

from hnblue_agent.models import Capability, EvidenceStatus, Role, TaskContext
from hnblue_agent.tools import knowledge_search, region_metrics_read, source_verify


def _context() -> TaskContext:
    return TaskContext(
        task_id="region-tool-test",
        trace_id="region-tool-trace",
        subject_key="pro:test",
        role=Role.PROFESSIONAL,
        capability=Capability.PROFESSIONAL_ANALYSIS,
        allowed_tools=frozenset({
            "bluecarbon.region.metrics.read",
            "bluecarbon.source.verify",
        }),
        token_budget=2_000,
        max_steps=6,
    )


class _FakeClient:
    def __init__(self, response: httpx.Response | None = None, error: Exception | None = None, **_: object):
        self.response = response
        self.error = error
        self.request: tuple[str, dict[str, object]] | None = None

    def __enter__(self) -> "_FakeClient":
        return self

    def __exit__(self, *_: object) -> None:
        return None

    def get(self, url: str, params: dict[str, object]) -> httpx.Response:
        self.request = (url, params)
        if self.error:
            raise self.error
        assert self.response is not None
        return self.response


def test_region_metrics_reads_java_v2_rows(monkeypatch) -> None:
    request = httpx.Request("GET", "http://127.0.0.1:8089/api/v2/ai/context")
    response = httpx.Response(
        200,
        request=request,
        json={
            "success": True,
            "data": {
                "mangrove_cover": [{"region_name": "文昌市", "area_value": 428.3}],
                "region_metrics": [],
                "literature_carbon": [{"region_name": "文昌市", "carbon_pool": "SOC"}],
                "boundary_notes": [],
            },
        },
    )
    fake = _FakeClient(response=response)
    monkeypatch.setenv("HNBLUE_JAVA_BASE_URL", "http://127.0.0.1:8089/")
    monkeypatch.setattr(httpx, "Client", lambda **kwargs: fake)

    result = region_metrics_read(_context(), {"region": "文昌"})

    assert result.status == EvidenceStatus.RETRIEVED
    assert result.content["mode"] == "java-v2-api"
    assert result.content["metrics"]["mangroveCover"][0]["region_name"] == "文昌市"
    assert result.content["metrics"]["literatureCarbon"][0]["carbon_pool"] == "SOC"
    assert result.content["evidenceRows"] == 2
    assert result.sources[0].datasetIdentity == "HNBLUE_JAVA_V2_CURRENT_DATABASE"
    assert result.sources[0].synthetic is False
    assert fake.request == (
        "http://127.0.0.1:8089/api/v2/ai/context",
        {"region": "文昌"},
    )


def test_region_metrics_does_not_fall_back_when_java_is_unavailable(monkeypatch) -> None:
    request = httpx.Request("GET", "http://127.0.0.1:8089/api/v2/ai/context")
    fake = _FakeClient(error=httpx.ConnectError("refused", request=request))
    monkeypatch.setenv("HNBLUE_JAVA_BASE_URL", "http://127.0.0.1:8089")
    monkeypatch.setattr(httpx, "Client", lambda **kwargs: fake)

    result = region_metrics_read(_context(), {"region": "文昌"})

    assert result.status == EvidenceStatus.INSUFFICIENT
    assert result.content["mode"] == "java-v2-api"
    assert result.sources == []


def test_region_metrics_preserves_java_v2_synthetic_provenance(monkeypatch) -> None:
    request = httpx.Request("GET", "http://127.0.0.1:8089/api/v2/ai/context")
    response = httpx.Response(
        200,
        request=request,
        json={
            "success": True,
            "data": {
                "mangrove_cover": [{
                    "region_name": "文昌", "area_value": 12.3, "is_simulated": 1,
                }],
                "region_metrics": [],
                "literature_carbon": [],
                "boundary_notes": [],
            },
        },
    )
    fake = _FakeClient(response=response)
    monkeypatch.setenv("HNBLUE_JAVA_BASE_URL", "http://127.0.0.1:8089")
    monkeypatch.setattr(httpx, "Client", lambda **kwargs: fake)

    result = region_metrics_read(_context(), {"region": "文昌"})

    assert result.status == EvidenceStatus.RETRIEVED
    assert result.sources[0].synthetic is True
    assert "not real government observations" in result.content["provenanceNotice"]
    assert result.content["metrics"]["mangroveCover"][0]["is_simulated"] == 1


def test_region_metrics_preserves_authorized_official_identity_without_claiming_review(monkeypatch) -> None:
    request = httpx.Request("GET", "http://127.0.0.1:8089/api/v2/ai/context")
    response = httpx.Response(
        200,
        request=request,
        json={
            "success": True,
            "meta": {
                "data_mode": "AUTHORIZED_OFFICIAL",
                "dataset_identity": "HNBLUE_FORMAL_MYSQL_20261003",
                "review_status": "material-scope-unproven",
                "access_status": "school-approved",
                "government_reviewed": False,
            },
            "data": {
                "mangrove_cover": [{"region_name": "文昌", "area_value": 12.3, "is_simulated": 0}],
                "region_metrics": [],
                "literature_carbon": [],
                "boundary_notes": [],
            },
        },
    )
    fake = _FakeClient(response=response)
    monkeypatch.setenv("HNBLUE_JAVA_BASE_URL", "http://127.0.0.1:8089")
    monkeypatch.setattr(httpx, "Client", lambda **kwargs: fake)

    result = region_metrics_read(_context(), {"region": "文昌"})

    source = result.sources[0]
    assert source.datasetIdentity == "HNBLUE_FORMAL_MYSQL_20261003"
    assert source.synthetic is False
    assert source.governmentReviewed is False
    assert source.reviewStatus == "material-scope-unproven"
    assert source.accessStatus == "school-approved"


def test_source_verify_accepts_only_current_java_v2_source_prefix() -> None:
    result = source_verify(
        _context(),
        {"sourceIds": ["java:v2:region-context:文昌", "java:devisual:文昌"]},
    )

    assert result.status == EvidenceStatus.INSUFFICIENT
    assert result.content["verified"] == ["java:v2:region-context:文昌"]
    assert result.content["unknown"] == ["java:devisual:文昌"]


def test_packaged_rag_is_always_labeled_as_synthetic_test_evidence() -> None:
    result = knowledge_search(_context(), {"query": "蓝碳是什么", "topK": 2})

    assert result.status == EvidenceStatus.RETRIEVED
    assert result.sources
    assert all(source.datasetIdentity == "HNBLUE_RAG_3_0_LOCAL_INDEX" for source in result.sources)
    assert all(source.synthetic is True for source in result.sources)

    verified = source_verify(_context(), {"sourceIds": [result.sources[0].sourceId]})
    assert result.sources[0].sourceId in verified.content["verified"]
    assert result.sources[0].sourceId in verified.content["syntheticTestOnly"]


def test_authorized_rag_identity_is_configurable(monkeypatch) -> None:
    monkeypatch.setenv("HNBLUE_RAG_DATASET_IDENTITY", "HNBLUE_RAG_AUTHORIZED_20261031_V4")
    monkeypatch.setenv("HNBLUE_RAG_SYNTHETIC", "false")
    monkeypatch.setenv("HNBLUE_RAG_ACCESS_STATUS", "school-approved")
    monkeypatch.setenv("HNBLUE_RAG_REVIEW_STATUS", "material-scope-unproven")

    result = knowledge_search(_context(), {"query": "蓝碳是什么", "topK": 1})
    source = result.sources[0]
    assert source.datasetIdentity == "HNBLUE_RAG_AUTHORIZED_20261031_V4"
    assert source.synthetic is False
    assert source.accessStatus == "school-approved"

    verified = source_verify(_context(), {"sourceIds": [source.sourceId]})
    assert source.sourceId in verified.content["authorizedOfficial"]
    assert source.sourceId not in verified.content["syntheticTestOnly"]
