from __future__ import annotations

import json
import os
from pathlib import Path

from flask import Blueprint, jsonify, request

from rag.hnblue_rag import HybridRagIndex, load_domain_terms


def create_rag_blueprint(project_root: Path) -> Blueprint:
    blueprint = Blueprint("hnblue_rag", __name__)
    chunks_path = Path(
        os.getenv("HNBLUE_RAG_CHUNKS", str(project_root / "rag" / "artifacts" / "chunks.jsonl"))
    ).expanduser().resolve()
    manifest_path = chunks_path.parent / "index_manifest.json"
    source_root = Path(
        os.getenv("HNBLUE_RAG_SOURCE", str(project_root / "rag" / "examples" / "knowledge"))
    ).expanduser().resolve()

    index = None
    manifest = {}
    load_error = ""
    try:
        if manifest_path.exists():
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        index = HybridRagIndex.from_jsonl(chunks_path, load_domain_terms(source_root))
    except Exception as exc:
        load_error = f"{exc.__class__.__name__}: {exc}"

    def parse_request() -> tuple[str, int]:
        payload = request.get_json(silent=True) or {}
        query = str(payload.get("message") or payload.get("query") or "").strip()
        if not query:
            raise ValueError("请输入要检索的问题。")
        if len(query) > 4000:
            raise ValueError("问题内容过长，请控制在 4000 字以内。")
        try:
            top_k = int(payload.get("topK", 5))
        except (TypeError, ValueError) as exc:
            raise ValueError("topK 必须是整数。") from exc
        return query, max(1, min(top_k, 10))

    @blueprint.get("/api/rag/status")
    def status():
        ready = index is not None
        payload = {
            "ok": ready,
            "provider": "hnblue-local-hybrid-rag",
            "retrievalModes": ["bm25", "char_tfidf", "semantic_lsa", "hybrid"],
            "chunks": len(index.chunks) if ready else 0,
            "sourceFiles": int(manifest.get("source_files", 0)),
            "indexSha256": manifest.get("chunks_sha256", ""),
        }
        if not ready:
            payload["message"] = "RAG index is unavailable"
            payload["errorType"] = load_error.split(":", 1)[0] if load_error else "missing_index"
        return jsonify(payload), 200 if ready else 503

    @blueprint.post("/api/rag/search")
    def search():
        if index is None:
            return jsonify({"ok": False, "message": "本地知识库索引不可用"}), 503
        try:
            query, top_k = parse_request()
            results = index.search(query, top_k=top_k, mode="hybrid", diversify=True)
            return jsonify({"ok": True, "provider": "hnblue-local-hybrid-rag", "results": results})
        except ValueError as exc:
            return jsonify({"ok": False, "message": str(exc)}), 400

    @blueprint.post("/api/rag/answer")
    def answer():
        if index is None:
            return jsonify({"ok": False, "message": "本地知识库索引不可用"}), 503
        try:
            query, top_k = parse_request()
            result = index.answer(query, top_k=top_k)
            return jsonify({
                "ok": True,
                "provider": "hnblue-local-hybrid-rag",
                "answer": result["answer"],
                "sources": result["sources"],
                "evidenceStatus": result["evidenceStatus"],
                "metrics": {"retrieval": "hybrid-rrf", "returnedSources": len(result["sources"])},
            })
        except ValueError as exc:
            return jsonify({"ok": False, "message": str(exc)}), 400

    @blueprint.post("/api/rag/context")
    def context():
        if index is None:
            return jsonify({"ok": False, "message": "本地知识库索引不可用"}), 503
        try:
            query, top_k = parse_request()
            result = index.context(query, top_k=top_k)
            return jsonify({
                "ok": True,
                "provider": "hnblue-local-hybrid-rag-3.0",
                **result,
                "metrics": {"retrieval": "hybrid-rrf", "returnedSources": len(result["sources"])},
            })
        except ValueError as exc:
            return jsonify({"ok": False, "message": str(exc)}), 400

    return blueprint
