# Public demonstration data policy

## 中文摘要

所有公开样例都必须标注为模拟、代理或示例数据。示例记录不能冒充现场实测，代理值不能描述为已核实事实；来源标识用于追溯数据从哪里来、经过什么处理以及能否用于当前结论。

## Synthetic records only

All records in this public knowledge pack and in `data/examples/` are fabricated interface fixtures. They may demonstrate schemas, filters, pagination, citations, and quality labels, but they must not be used for scientific conclusions, official reports, model training, carbon accounting, or project verification.

## Required provenance fields

Each publishable record should expose a stable source identifier, data class, unit, time scope, spatial scope, processing status, quality label, and simulation or proxy flag. A source label proves traceability only; it does not prove correctness or legal validity.

## Interpretation boundary

`is_simulated=false` means only that the record was not marked as simulated in its source workflow. It does not mean that the value was independently verified. Proxy data, literature values, model estimates, and scenario results must remain visibly distinct from observations.
