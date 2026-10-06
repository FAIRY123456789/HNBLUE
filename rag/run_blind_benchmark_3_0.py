from __future__ import annotations

import argparse
import json
import math
from pathlib import Path

from hnblue_rag import HybridRagIndex, assess_evidence, detect_prompt_injection, load_domain_terms


def dcg(values: list[int]) -> float:
    return sum(value / math.log2(index + 2) for index, value in enumerate(values))


def main() -> None:
    parser = argparse.ArgumentParser(description="Run frozen HNBLUE RAG 3.0 blind benchmark.")
    parser.add_argument("--chunks", type=Path, required=True)
    parser.add_argument("--cases", type=Path, required=True)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    cases = json.loads(args.cases.read_text(encoding="utf-8"))
    domain_terms = load_domain_terms(args.source)
    index = HybridRagIndex.from_jsonl(args.chunks, domain_terms)
    rows: list[dict] = []

    for case in cases:
        category = case["category"]
        query = case["query"]
        injection = detect_prompt_injection(query)
        results = [] if injection["blocked"] else index.search(query, top_k=5, mode="hybrid", diversify=True)
        retrieved = [item["source_path"] for item in results]
        status = "blocked_prompt_injection" if injection["blocked"] else assess_evidence(query, results, domain_terms)
        expected = set(case.get("relevant_sources", []))
        ranks = [i + 1 for i, source in enumerate(retrieved) if source in expected]
        seen_relevant: set[str] = set()
        relevance = []
        for source in retrieved:
            is_new_relevant = source in expected and source not in seen_relevant
            relevance.append(1 if is_new_relevant else 0)
            if is_new_relevant:
                seen_relevant.add(source)
        ideal = [1] * min(len(expected), 5) + [0] * max(0, 5 - len(expected))
        joined = "\n".join(str(item.get("content", "")) for item in results).lower()
        required = [term.lower() for term in case.get("required_terms", [])]
        if category in {"semantic", "ui"}:
            passed = bool(ranks)
        elif category == "conflict":
            passed = bool(ranks) and all(term in joined for term in required)
        elif category == "unanswerable":
            passed = status in {"none", "insufficient", "external_verification_required"}
        elif category == "injection":
            passed = status == "blocked_prompt_injection"
        else:
            passed = False
        rows.append({
            "id": case["id"], "category": category, "query": query, "passed": passed,
            "evidence_status": status, "expected": sorted(expected), "retrieved": retrieved,
            "hit_at_1": bool(ranks and ranks[0] == 1),
            "recall_at_3": 0.0 if not expected else len(set(retrieved[:3]) & expected) / len(expected),
            "recall_at_5": 0.0 if not expected else len(set(retrieved) & expected) / len(expected),
            "reciprocal_rank": 0.0 if not ranks else 1.0 / min(ranks),
            "ndcg_at_5": 0.0 if not expected or dcg(ideal) == 0 else dcg(relevance) / dcg(ideal),
            "required_terms_found": {term: term in joined for term in required},
        })

    retrieval_rows = [row for row in rows if row["category"] in {"semantic", "conflict", "ui"}]
    category_summary = {}
    for category in sorted({row["category"] for row in rows}):
        subset = [row for row in rows if row["category"] == category]
        category_summary[category] = {"cases": len(subset), "passed": sum(row["passed"] for row in subset),
                                      "pass_rate": sum(row["passed"] for row in subset) / len(subset)}
    metrics = {
        "frozen_cases": len(rows),
        "overall_pass_rate": sum(row["passed"] for row in rows) / len(rows),
        "retrieval_hit_at_1": sum(row["hit_at_1"] for row in retrieval_rows) / len(retrieval_rows),
        "retrieval_recall_at_3": sum(row["recall_at_3"] for row in retrieval_rows) / len(retrieval_rows),
        "retrieval_recall_at_5": sum(row["recall_at_5"] for row in retrieval_rows) / len(retrieval_rows),
        "retrieval_mrr_at_5": sum(row["reciprocal_rank"] for row in retrieval_rows) / len(retrieval_rows),
        "retrieval_ndcg_at_5": sum(row["ndcg_at_5"] for row in retrieval_rows) / len(retrieval_rows),
        "categories": category_summary,
    }
    report = {"method": "frozen holdout; not used for tuning; not third-party authored", "metrics": metrics,
              "failures": [row for row in rows if not row["passed"]], "rows": rows}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metrics, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
