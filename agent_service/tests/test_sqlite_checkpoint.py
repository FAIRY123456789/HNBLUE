from __future__ import annotations

from pathlib import Path
from concurrent.futures import ThreadPoolExecutor

from hnblue_agent.graph import AgentHarness
from hnblue_agent.models import AgentRequest


def _request(task_id: str) -> AgentRequest:
    return AgentRequest(
        taskId=task_id,
        traceId=f"trace-{task_id}",
        subjectKey="PUBLIC:checkpoint-test",
        role="PUBLIC",
        capability="BASIC_QA",
        allowedTools=["bluecarbon.knowledge.search"],
        tokenBudget=1000,
        maxSteps=4,
        message="蓝碳生态系统是什么？",
    )


def test_sqlite_checkpoint_recovers_terminal_result_without_tool_reexecution(tmp_path: Path) -> None:
    database = tmp_path / "checkpoints.sqlite"
    task_id = "checkpoint-restart-001"

    first = AgentHarness(checkpoint_path=database)
    first_result = first.execute(_request(task_id))
    first.close()

    second = AgentHarness(checkpoint_path=database)

    def fail_if_reexecuted(*_args, **_kwargs):
        raise AssertionError("persisted task was re-executed")

    second.registry.execute = fail_if_reexecuted  # type: ignore[method-assign]
    recovered = second.execute(_request(task_id))
    second.close()

    assert database.exists()
    assert first_result.success is True
    assert recovered == first_result


def test_sqlite_checkpoint_serializes_concurrent_graph_writes(tmp_path: Path) -> None:
    harness = AgentHarness(checkpoint_path=tmp_path / "concurrent-checkpoints.sqlite")
    try:
        requests = [_request(f"checkpoint-concurrent-{index:03d}") for index in range(8)]
        with ThreadPoolExecutor(max_workers=4) as pool:
            results = list(pool.map(harness.execute, requests))
    finally:
        harness.close()

    assert all(result.success for result in results)
    assert all(result.errorCode is None for result in results)
