from hnblue_agent.models import AgentRequest, AgentResult


def test_java_gateway_request_and_result_contract_round_trip() -> None:
    java_payload = {
        "taskId": "contract-task-0001",
        "traceId": "contract-trace-0001",
        "subjectKey": "PROFESSIONAL:42",
        "role": "PROFESSIONAL",
        "capability": "PROFESSIONAL_ANALYSIS",
        "allowedTools": [
            "bluecarbon.knowledge.search",
            "bluecarbon.region.metrics.read",
            "bluecarbon.source.verify",
        ],
        "tokenBudget": 4000,
        "maxSteps": 4,
        "message": "查询文昌蓝碳监测指标趋势",
    }
    request = AgentRequest.model_validate(java_payload)
    assert request.model_dump(mode="json") == java_payload

    result = AgentResult(
        success=True,
        output="evidence-backed result",
        sources=[{"sourceId": "rag:chunk-1", "visibility": "PUBLIC"}],
        tokenUsage=12,
    )
    response = result.model_dump(mode="json")
    assert set(response) == {
        "success", "partial", "output", "sources", "tokenUsage", "errorCode", "errorMessage",
        "routingAudit", "executionStatus", "reviewRequest", "collaboration",
    }
    assert response["routingAudit"] is None
    assert response["tokenUsage"] == 12
