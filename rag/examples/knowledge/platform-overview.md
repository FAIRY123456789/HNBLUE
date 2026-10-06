# HNBLUE demonstration platform

## Purpose

HNBLUE is a research prototype for evidence-aware blue-carbon data exploration. The public demo shows how a web client, an application service, a model service, structured storage, and an evidence assistant can be composed. It is not an official monitoring, accounting, verification, or trading system.

## Agent workflow

The assistant first checks the request for instruction attacks, retrieves candidate evidence, evaluates evidence sufficiency, and only then calls an optional generation model. Every factual paragraph must cite one of the evidence blocks supplied for that request. If evidence is missing, conflicting, or time-sensitive, the assistant refuses to infer a definitive answer.

## Product principle

The interface should expose source, unit, time, spatial scope, processing status, and uncertainty before presenting a conclusion. A useful decision-support product makes uncertainty visible instead of hiding it behind a polished answer.
