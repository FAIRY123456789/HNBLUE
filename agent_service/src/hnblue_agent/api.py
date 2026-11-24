from __future__ import annotations

import hmac
import os
from pathlib import Path

from fastapi import FastAPI, Header, HTTPException, Request

from . import __version__
from .graph import AgentHarness
from .models import (AgentCommandAction, AgentCommandRequest, AgentRequest,
                     AgentResult)
from .policy import PolicyViolation
from .providers import (LocalRouterShadowClient, OllamaModelProvider,
                        RuleModelProvider, ShadowModelProvider)


def create_app(harness: AgentHarness | None = None) -> FastAPI:
    app = FastAPI(title="HNBLUE Agent Service", version=__version__)
    internal_token = os.getenv("HNBLUE_AGENT_INTERNAL_TOKEN", "")
    require_internal_token = os.getenv("HNBLUE_AGENT_REQUIRE_INTERNAL_TOKEN", "false").strip().lower() in {
        "1", "true", "yes", "on",
    }
    if require_internal_token and not internal_token:
        raise RuntimeError("HNBLUE_AGENT_INTERNAL_TOKEN is required by this runtime profile")
    checkpoint = os.getenv("HNBLUE_AGENT_CHECKPOINT_DB", "").strip()
    provider_name = os.getenv("HNBLUE_AGENT_PROVIDER", "rule-r0").strip().lower()
    if provider_name == "ollama":
        provider = OllamaModelProvider(
            model=os.getenv("HNBLUE_OLLAMA_MODEL", "qwen3:1.7b"),
            base_url=os.getenv("HNBLUE_OLLAMA_BASE_URL", "http://127.0.0.1:11434"),
        )
    elif provider_name == "shadow":
        provider = ShadowModelProvider(
            active=RuleModelProvider(),
            candidate=LocalRouterShadowClient(
                base_url=os.getenv("HNBLUE_ROUTE_SHADOW_URL", "http://127.0.0.1:18090"),
                timeout_seconds=float(os.getenv("HNBLUE_ROUTE_SHADOW_TIMEOUT", "5")),
            ),
        )
    elif provider_name == "rule-r0":
        provider = RuleModelProvider()
    else:
        raise RuntimeError(f"unsupported HNBLUE_AGENT_PROVIDER: {provider_name}")
    runtime = harness or AgentHarness(provider=provider, checkpoint_path=Path(checkpoint) if checkpoint else None)

    def authenticate_internal(request: Request, supplied: str | None, supplied_scope: str | None,
                              expected_scope: str) -> None:
        configured = internal_token
        if configured:
            if supplied is None or not hmac.compare_digest(configured, supplied):
                raise HTTPException(status_code=401, detail="invalid internal service credential")
            if supplied_scope != expected_scope:
                raise HTTPException(status_code=403, detail="invalid internal service scope")
            return
        host = request.client.host if request.client else ""
        if host not in {"127.0.0.1", "::1", "localhost", "testclient"}:
            raise HTTPException(status_code=503, detail="internal token is required outside loopback")

    @app.get("/health")
    def health() -> dict[str, object]:
        return {"ok": True, "service": "hnblue-agent", "version": __version__,
                "provider": runtime.provider.name, "checkpoint": runtime.checkpoint_kind}

    @app.get("/v1/capabilities")
    def capabilities() -> dict[str, object]:
        return {"tools": runtime.registry.describe(), "provider": runtime.provider.name,
                "predictionAgentEnabled": False,
                "routingMode": "SHADOW" if runtime.provider.name.startswith("shadow:") else "ACTIVE"}

    @app.post("/v1/agent/tasks/execute", response_model=AgentResult)
    def execute(payload: AgentRequest, request: Request,
                x_hnblue_internal_token: str | None = Header(default=None),
                x_hnblue_service_scope: str | None = Header(default=None),
                x_trace_id: str | None = Header(default=None)) -> AgentResult:
        authenticate_internal(request, x_hnblue_internal_token, x_hnblue_service_scope, "agent.execute")
        if x_trace_id is not None and x_trace_id != payload.traceId:
            raise HTTPException(status_code=400, detail="trace id mismatch")
        return runtime.execute(payload)

    @app.post("/v1/agent/tasks/command", response_model=AgentResult)
    def command(payload: AgentCommandRequest, request: Request,
                x_hnblue_internal_token: str | None = Header(default=None),
                x_hnblue_service_scope: str | None = Header(default=None),
                x_trace_id: str | None = Header(default=None)) -> AgentResult:
        """Unified internal API for execute, approve, return, reject and archive."""
        authenticate_internal(request, x_hnblue_internal_token, x_hnblue_service_scope, "agent.command")
        trace_id = payload.request.traceId if payload.request else payload.review.traceId  # type: ignore[union-attr]
        if x_trace_id is not None and x_trace_id != trace_id:
            raise HTTPException(status_code=400, detail="trace id mismatch")
        try:
            if payload.action == AgentCommandAction.EXECUTE:
                return runtime.execute(payload.request)  # type: ignore[arg-type]
            return runtime.resume(payload.review)  # type: ignore[arg-type]
        except PolicyViolation as exc:
            status = 403 if exc.code in {
                "self_review_forbidden", "review_action_forbidden", "archive_role_forbidden",
                "senior_review_forbidden", "domain_review_forbidden",
            } else 409 if exc.code in {"review_not_waiting", "stale_review_decision"} else 400
            raise HTTPException(status_code=status, detail={"code": exc.code, "message": str(exc)}) from exc

    return app


app = create_app()
