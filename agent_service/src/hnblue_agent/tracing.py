from __future__ import annotations

import json
import os
import threading
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


class TraceStore:
    """Append-only, redacted local trace store; no prompts or credentials are persisted."""

    def __init__(self, path: Path | None = None) -> None:
        default = Path(__file__).resolve().parents[3] / "agent_service" / ".runtime" / "traces.jsonl"
        self.path = path or Path(os.getenv("HNBLUE_AGENT_TRACE_PATH", str(default)))
        self._lock = threading.Lock()

    def append(self, task_id: str, trace_id: str, event: dict[str, Any]) -> None:
        record = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "taskId": task_id,
            "traceId": trace_id,
            "event": self._redact(event),
        }
        with self._lock:
            self.path.parent.mkdir(parents=True, exist_ok=True)
            with self.path.open("a", encoding="utf-8", newline="\n") as handle:
                handle.write(json.dumps(record, ensure_ascii=False, separators=(",", ":")) + "\n")

    def _redact(self, value: Any) -> Any:
        if isinstance(value, dict):
            return {
                key: ("[REDACTED]" if any(term in key.lower() for term in ("token", "password", "secret"))
                      else self._redact(item))
                for key, item in value.items()
            }
        if isinstance(value, list):
            return [self._redact(item) for item in value]
        text = str(value)
        return text[:500]
