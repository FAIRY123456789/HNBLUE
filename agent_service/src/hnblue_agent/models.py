from __future__ import annotations

from enum import StrEnum
from typing import Any, TypedDict

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


class Role(StrEnum):
    ADMIN = "ADMIN"
    PROFESSIONAL = "PROFESSIONAL"
    PUBLIC = "PUBLIC"


class Capability(StrEnum):
    BASIC_QA = "BASIC_QA"
    PROFESSIONAL_ANALYSIS = "PROFESSIONAL_ANALYSIS"
    MULTI_AGENT = "MULTI_AGENT"


class EvidenceStatus(StrEnum):
    RETRIEVED = "RETRIEVED"
    INSUFFICIENT = "INSUFFICIENT"
    CONFLICT = "CONFLICT"
    BLOCKED = "BLOCKED"


class AgentExecutionStatus(StrEnum):
    COMPLETED = "COMPLETED"
    AWAITING_REVIEW = "AWAITING_REVIEW"
    AWAITING_ARCHIVE = "AWAITING_ARCHIVE"
    REJECTED = "REJECTED"
    ARCHIVED = "ARCHIVED"


class AgentCommandAction(StrEnum):
    EXECUTE = "EXECUTE"
    APPROVE = "APPROVE"
    RETURN = "RETURN"
    REJECT = "REJECT"
    ARCHIVE = "ARCHIVE"


class ReviewLevel(StrEnum):
    DOMAIN_REVIEWER = "DOMAIN_REVIEWER"
    SENIOR_REVIEWER = "SENIOR_REVIEWER"
    ARCHIVE_OFFICER = "ARCHIVE_OFFICER"


class AgentRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    taskId: str = Field(min_length=8, max_length=64, pattern=r"^[a-zA-Z0-9._:-]+$")
    traceId: str = Field(min_length=8, max_length=64)
    subjectKey: str = Field(min_length=3, max_length=96)
    role: Role
    capability: Capability
    allowedTools: list[str] = Field(default_factory=list, max_length=16)
    tokenBudget: int = Field(ge=1, le=200_000)
    maxSteps: int = Field(ge=1, le=16)
    message: str = Field(min_length=1, max_length=8_000)

    @field_validator("allowedTools")
    @classmethod
    def tools_are_unique_and_named(cls, value: list[str]) -> list[str]:
        if len(value) != len(set(value)):
            raise ValueError("allowedTools must not contain duplicates")
        for name in value:
            if not name or len(name) > 96 or not all(ch.isalnum() or ch in "._-" for ch in name):
                raise ValueError("invalid tool name")
        return value


class ReviewDecision(BaseModel):
    model_config = ConfigDict(extra="forbid")

    taskId: str = Field(min_length=8, max_length=64, pattern=r"^[a-zA-Z0-9._:-]+$")
    traceId: str = Field(min_length=8, max_length=64)
    requestId: str = Field(min_length=8, max_length=64, pattern=r"^[a-zA-Z0-9._:-]+$")
    action: AgentCommandAction
    reviewerSubjectKey: str = Field(min_length=3, max_length=96)
    reviewerRole: Role
    comment: str = Field(default="", max_length=2000)
    reviewCycle: int = Field(ge=0, le=8)

    @model_validator(mode="after")
    def action_is_a_review_action(self) -> "ReviewDecision":
        if self.action == AgentCommandAction.EXECUTE:
            raise ValueError("review decision cannot use EXECUTE")
        if self.action in {AgentCommandAction.RETURN, AgentCommandAction.REJECT} and not self.comment.strip():
            raise ValueError("RETURN and REJECT require a review comment")
        return self


class AgentCommandRequest(BaseModel):
    """One internal API contract for initial execution and every review transition."""

    model_config = ConfigDict(extra="forbid")

    action: AgentCommandAction
    request: AgentRequest | None = None
    review: ReviewDecision | None = None

    @model_validator(mode="after")
    def exactly_one_payload_matches_action(self) -> "AgentCommandRequest":
        if self.action == AgentCommandAction.EXECUTE:
            if self.request is None or self.review is not None:
                raise ValueError("EXECUTE requires request and forbids review")
        elif self.review is None or self.request is not None:
            raise ValueError("review action requires review and forbids request")
        elif self.review.action != self.action:
            raise ValueError("command action and review action must match")
        return self


class ReviewRequest(BaseModel):
    level: ReviewLevel
    cycle: int
    taskId: str
    traceId: str
    draft: str
    evidenceStatus: str
    sourceCount: int
    reasons: list[str] = Field(default_factory=list)
    allowedActions: list[AgentCommandAction] = Field(default_factory=list)


class SourceRecord(BaseModel):
    sourceId: str
    title: str
    datasetIdentity: str
    visibility: str
    unit: str | None = None
    recordId: str | None = None
    excerpt: str | None = None
    synthetic: bool = False
    governmentReviewed: bool = False
    reviewStatus: str | None = None
    accessStatus: str | None = None


class RoutingAudit(BaseModel):
    """Non-authoritative routing comparison returned for Shadow Mode evidence."""

    mode: str = "SHADOW"
    authority: str = "JAVA_POLICY_AND_EXISTING_RULES"
    activeProvider: str
    activeRoute: str
    candidateProvider: str
    candidateAvailable: bool
    candidateRoute: str | None = None
    matched: bool | None = None
    outcome: str | None = None
    intent: str | None = None
    suggestedCapability: str | None = None
    policyDecision: str | None = None
    clarificationNeeded: bool | None = None
    requiredFields: list[str] = Field(default_factory=list)
    confidence: float | None = None
    candidateModelId: str | None = None
    candidateLatencyMs: float | None = None
    errorCode: str | None = None


class AgentResult(BaseModel):
    success: bool
    partial: bool = False
    output: str | None = None
    sources: list[dict[str, Any]] = Field(default_factory=list)
    tokenUsage: int = 0
    errorCode: str | None = None
    errorMessage: str | None = None
    routingAudit: RoutingAudit | None = None
    executionStatus: AgentExecutionStatus = AgentExecutionStatus.COMPLETED
    reviewRequest: ReviewRequest | None = None
    collaboration: dict[str, Any] = Field(default_factory=dict)


class ToolResult(BaseModel):
    tool: str
    status: EvidenceStatus
    content: dict[str, Any] = Field(default_factory=dict)
    sources: list[SourceRecord] = Field(default_factory=list)
    units: dict[str, str] = Field(default_factory=dict)


class TaskContext(BaseModel):
    task_id: str
    trace_id: str
    subject_key: str
    role: Role
    capability: Capability
    allowed_tools: frozenset[str]
    token_budget: int
    max_steps: int

    @classmethod
    def from_request(cls, request: AgentRequest) -> "TaskContext":
        return cls(
            task_id=request.taskId,
            trace_id=request.traceId,
            subject_key=request.subjectKey,
            role=request.role,
            capability=request.capability,
            allowed_tools=frozenset(request.allowedTools),
            token_budget=request.tokenBudget,
            max_steps=request.maxSteps,
        )


class TaskState(TypedDict, total=False):
    context: TaskContext
    message: str
    route: str
    confidence: float
    clarification: str | None
    planned_calls: list[dict[str, Any]]
    step_count: int
    token_usage: int
    tool_results: list[ToolResult]
    seen_calls: set[str]
    sources: list[dict[str, Any]]
    output: str | None
    success: bool
    partial: bool
    error_code: str | None
    error_message: str | None
    evidence_status: str
    trace_events: list[dict[str, Any]]
    active_worker: str | None
    handoff_count: int
    worker_outputs: list[dict[str, Any]]
    routing_audit: RoutingAudit | None
    agent_messages: list[dict[str, Any]]
    revision_round: int
    max_revision_rounds: int
    self_consistency: dict[str, Any]
    review_cycle: int
    max_review_cycles: int
    review_history: list[dict[str, Any]]
    review_request: dict[str, Any]
    review_outcome: str
    review_feedback: str
    execution_status: str
    draft_output: str
