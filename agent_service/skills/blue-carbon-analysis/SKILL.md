---
name: blue-carbon-analysis
description: Produce source-grounded HNBLUE blue-carbon explanations and scoped regional analyses using only approved read-only tools.
version: 0.1.0
---

# Blue Carbon Analysis

Use this skill only for blue-carbon, mangrove, seagrass, salt-marsh, carbon-stock, carbon-flux, methodology, or HNBLUE questions.

## Required workflow

1. Preserve the caller's Java-issued role, capability, tool allowlist, token budget, task ID, and trace ID. Never infer a higher role.
2. Use `bluecarbon.knowledge.search` for explanations and source-grounded claims.
3. Use `bluecarbon.region.metrics.read` only when the task allowlist contains it and the user names a supported region. Treat `synthetic-fixture` results as test-only and say so.
4. Use `bluecarbon.methodology.lookup` only for `MULTI_AGENT` tasks. Use `bluecarbon.source.verify` for compound reports when available.
5. If evidence is absent, conflicting, synthetic, or missing a unit, state that limitation. Never manufacture a measurement, citation, government status, or prediction.
6. Do not call write, approval, account, shell, arbitrary SQL, file, network-fetch, or administrator tools.

## Output contract

Return a concise result plus structured sources. Each source identifies dataset identity, visibility, record ID when available, unit when applicable, and whether it is synthetic. Use `INSUFFICIENT` or `CONFLICT` when a supported conclusion cannot be formed.
