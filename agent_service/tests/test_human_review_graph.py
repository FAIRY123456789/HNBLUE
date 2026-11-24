from __future__ import annotations

from pathlib import Path

import pytest

from hnblue_agent.graph import AgentHarness
from hnblue_agent.models import (AgentExecutionStatus, AgentRequest,
                                 ReviewDecision)
from hnblue_agent.policy import PolicyViolation


def _request(task_id: str) -> AgentRequest:
    return AgentRequest(
        taskId=task_id,
        traceId="trace-" + task_id,
        subjectKey="PROFESSIONAL:task-owner",
        role="PROFESSIONAL",
        capability="MULTI_AGENT",
        allowedTools=[
            "bluecarbon.knowledge.search",
            "bluecarbon.region.metrics.read",
            "bluecarbon.methodology.lookup",
            "bluecarbon.source.verify",
        ],
        tokenBudget=2000,
        maxSteps=8,
        message="结合文昌红树林蓝碳指标趋势以及核证方法来源形成报告",
    )


def _decision(request: AgentRequest, result, action: str, *, role: str = "PROFESSIONAL",
              subject: str = "PROFESSIONAL:domain-reviewer", comment: str = "复核完成") -> ReviewDecision:
    assert result.reviewRequest is not None
    return ReviewDecision(
        taskId=request.taskId,
        traceId=request.traceId,
        requestId=f"review-{action.lower()}-{result.reviewRequest.cycle:02d}",
        action=action,
        reviewerSubjectKey=subject,
        reviewerRole=role,
        comment=comment,
        reviewCycle=result.reviewRequest.cycle,
    )


def test_multi_agent_pauses_and_two_level_review_archives(tmp_path: Path) -> None:
    runtime = AgentHarness(checkpoint_path=tmp_path / "review.sqlite")
    request = _request("human-review-0001")
    paused = runtime.execute(request)

    assert paused.executionStatus == AgentExecutionStatus.AWAITING_REVIEW
    assert paused.reviewRequest is not None
    assert paused.collaboration["workers"] == [
        "research_worker", "monitoring_worker", "evidence_merge", "consistency_agent"]
    assert paused.collaboration["messages"] >= 8
    assert paused.collaboration["selfConsistency"]["consistent"] is True

    approved = runtime.resume(_decision(request, paused, "APPROVE"))
    assert approved.executionStatus == AgentExecutionStatus.AWAITING_ARCHIVE
    assert approved.reviewRequest is not None
    assert approved.reviewRequest.level.value == "ARCHIVE_OFFICER"

    archived = runtime.resume(_decision(
        request, approved, "ARCHIVE", role="ADMIN", subject="ADMIN:archive-officer"))
    assert archived.executionStatus == AgentExecutionStatus.ARCHIVED
    assert archived.success is True
    assert len(archived.collaboration["reviewHistory"]) == 2
    runtime.close()


def test_return_feedback_reenters_agent_loop_and_pauses_again(tmp_path: Path) -> None:
    runtime = AgentHarness(checkpoint_path=tmp_path / "return.sqlite")
    request = _request("human-return-0001")
    first = runtime.execute(request)
    returned = runtime.resume(_decision(
        request, first, "RETURN", comment="请补充证据说明并重新交叉核验"))

    assert returned.executionStatus == AgentExecutionStatus.AWAITING_REVIEW
    assert returned.reviewRequest is not None
    assert returned.reviewRequest.cycle == 1
    assert returned.collaboration["revisionRound"] == 1
    assert returned.collaboration["workers"].count("research_worker") == 2
    assert len(returned.collaboration["reviewHistory"]) == 1
    runtime.close()


def test_owner_cannot_review_and_professional_cannot_archive(tmp_path: Path) -> None:
    runtime = AgentHarness(checkpoint_path=tmp_path / "roles.sqlite")
    request = _request("human-roles-0001")
    paused = runtime.execute(request)

    with pytest.raises(PolicyViolation, match="own result"):
        runtime.resume(_decision(
            request, paused, "APPROVE", subject=request.subjectKey))

    approved = runtime.resume(_decision(request, paused, "APPROVE"))
    with pytest.raises(PolicyViolation, match="administrator"):
        runtime.resume(_decision(request, approved, "ARCHIVE"))
    runtime.close()


def test_inconsistent_evidence_requires_senior_admin_review(tmp_path: Path) -> None:
    runtime = AgentHarness(checkpoint_path=tmp_path / "senior.sqlite")
    request = _request("human-senior-0001").model_copy(update={
        "message": "结合红树林蓝碳指标趋势以及核证方法来源形成多步骤报告",
    })
    paused = runtime.execute(request)

    assert paused.executionStatus == AgentExecutionStatus.AWAITING_REVIEW
    assert paused.reviewRequest is not None
    assert paused.reviewRequest.level.value == "SENIOR_REVIEWER"
    assert paused.collaboration["selfConsistency"]["consistent"] is False
    with pytest.raises(PolicyViolation, match="administrator"):
        runtime.resume(_decision(request, paused, "APPROVE"))

    approved = runtime.resume(_decision(
        request, paused, "APPROVE", role="ADMIN", subject="ADMIN:senior-reviewer"))
    assert approved.executionStatus == AgentExecutionStatus.AWAITING_ARCHIVE
    runtime.close()


def test_interrupted_state_survives_runtime_restart(tmp_path: Path) -> None:
    checkpoint = tmp_path / "restart.sqlite"
    request = _request("human-restart-0001")
    first_runtime = AgentHarness(checkpoint_path=checkpoint)
    first = first_runtime.execute(request)
    first_runtime.close()

    second_runtime = AgentHarness(checkpoint_path=checkpoint)
    recovered = second_runtime.execute(request)
    assert recovered.executionStatus == AgentExecutionStatus.AWAITING_REVIEW
    assert recovered.reviewRequest == first.reviewRequest
    approved = second_runtime.resume(_decision(request, recovered, "APPROVE"))
    assert approved.executionStatus == AgentExecutionStatus.AWAITING_ARCHIVE
    second_runtime.close()
