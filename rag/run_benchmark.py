from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

from hnblue_rag import HybridRagIndex, load_domain_terms


MODES = ("bm25", "char_tfidf", "semantic_lsa", "hybrid")


def dcg(relevance: list[int]) -> float:
    return sum(value / math.log2(index + 2) for index, value in enumerate(relevance))


def evaluate(index: HybridRagIndex, cases: list[dict], mode: str) -> tuple[dict, list[dict]]:
    rows = []
    for case in cases:
        expected = set(case["relevant_sources"])
        results = index.search(case["query"], top_k=5, mode=mode, diversify=True)
        retrieved = [item["source_path"] for item in results]
        ranks = [position + 1 for position, source in enumerate(retrieved) if source in expected]
        relevance = [1 if source in expected else 0 for source in retrieved]
        ideal = [1] * min(len(expected), 5) + [0] * max(0, 5 - len(expected))
        rows.append({
            "id": case["id"],
            "query": case["query"],
            "expected": sorted(expected),
            "retrieved": retrieved,
            "hit_at_1": bool(ranks and ranks[0] == 1),
            "recall_at_3": len({source for source in retrieved[:3] if source in expected}) / len(expected),
            "recall_at_5": len({source for source in retrieved if source in expected}) / len(expected),
            "reciprocal_rank": 0.0 if not ranks else 1.0 / min(ranks),
            "ndcg_at_5": 0.0 if dcg(ideal) == 0 else dcg(relevance) / dcg(ideal),
        })
    total = len(rows)
    metrics = {
        "mode": mode,
        "cases": total,
        "hit_at_1": sum(row["hit_at_1"] for row in rows) / total,
        "recall_at_3": sum(row["recall_at_3"] for row in rows) / total,
        "recall_at_5": sum(row["recall_at_5"] for row in rows) / total,
        "mrr_at_5": sum(row["reciprocal_rank"] for row in rows) / total,
        "ndcg_at_5": sum(row["ndcg_at_5"] for row in rows) / total,
    }
    return metrics, rows


def main() -> None:
    parser = argparse.ArgumentParser(description="Evaluate HNBLUE hybrid retrieval.")
    parser.add_argument("--chunks", type=Path, required=True)
    parser.add_argument("--cases", type=Path, required=True)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    cases = json.loads(args.cases.read_text(encoding="utf-8"))
    index = HybridRagIndex.from_jsonl(args.chunks, load_domain_terms(args.source))
    report = {"modes": {}, "failures": {}}
    for mode in MODES:
        metrics, rows = evaluate(index, cases, mode)
        report["modes"][mode] = metrics
        report["failures"][mode] = [row for row in rows if row["recall_at_5"] < 1.0]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report["modes"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
