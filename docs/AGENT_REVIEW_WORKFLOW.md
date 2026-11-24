# HNBLUE Agent review workflow

The 2026-11-20 Agent and Java update adds a LangGraph analysis workflow with research, monitoring, evidence and consistency roles, bounded revisions and two-stage human review. The existing Vue workspace submits and polls jobs. Reviewers use the Java task API for the review queue, review commands and history.

The Java API checks task ownership, reviewer roles, the expected task version and review cycle. The FastAPI service applies the corresponding review policy to the persisted checkpoint. The consumer uses MySQL for task/outbox state and supports the optional Redis Streams dispatch mode.

## Local development

Keep MySQL, Redis, model and service credentials in the local environment. This public release supports synthetic sample data and an optional local SQLite checkpoint. Use the project README for the main Vue, Java and Flask setup.

Build a local RAG index from the checked-in sample knowledge:

```sh
python rag/build_index.py --source rag/examples/knowledge --output rag/artifacts
```

Run the FastAPI service after installing its dependencies from `agent_service/requirements.txt`. Launch from the repository root with `agent_service/src` on the Python path.

```sh
python -m uvicorn --app-dir agent_service/src hnblue_agent.api:app --host 127.0.0.1 --port 18091
```

Configure both services to use the same local `HNBLUE_AGENT_INTERNAL_TOKEN`. Enable `HNBLUE_AGENT_REQUIRE_INTERNAL_TOKEN` for Python and set Java's `HNBLUE_AGENT_GATEWAY_ENABLED` and `HNBLUE_AGENT_GATEWAY_URL` for the local service. The Python `HNBLUE_AGENT_CHECKPOINT_DB` setting specifies the local SQLite checkpoint file. For the Streams path, configure the Redis connection and set `HNBLUE_AGENT_DISPATCH_MODE=redis-stream`. The Java Flyway migrations include task, outbox and review state.

## Review API

- `POST /api/agent/tasks` submits work.
- `GET /api/agent/tasks/review-queue` lists reviews visible to the caller.
- `POST /api/agent/tasks/{taskId}/commands` accepts approve, return, reject or archive commands for the current review level.
- `GET /api/agent/tasks/{taskId}/reviews` reads review history.

Run the Python regression suite with `python -m pytest` from the repository root. The Java service uses its Maven wrapper; the default test profile exercises the local regression suite.
