# HNBLUE 自建混合 RAG

本目录实现一个适合小型服务器的透明检索层。检索、证据门控、引用校验和失败回退均由
HNBLUE 自己控制。未配置生成模型时返回可核验的摘录；配置 DeepSeek 后，模型只能读取
本轮选中的证据块，不能绕过来源约束。

## 管线

1. 只读取 Markdown、TXT、CSV 和 JSON；图片不进入文本索引，未知二进制不反序列化。
2. Markdown 先按标题树拆分，再用相邻段落的字符 TF-IDF 余弦相似度寻找语义断点。
3. CSV、JSON、TXT 作为原子文档，不进行行级切碎。
4. 每个块附加文档标题、章节路径、上文和下文摘要，形成 contextual chunk。
5. 排除重复的 `all_in_one` 合并文档，避免重复内容挤占召回结果。
6. 并行计算 BM25、中文字符 TF-IDF 和 LSA 潜在语义排序，再用加权 RRF 融合。
7. 前五条结果按来源去重，避免同一文档的多个相邻块挤占召回结果。
8. 回答前同时检查领域锚点与检索强度；域外或证据不足的问题明确拒答，不强行拼接资料。

## 服务接口

模型服务同时挂载三个本机接口：

- `GET /api/rag/status`：索引版本、块数、来源文件数和支持的检索模式。
- `POST /api/rag/search`：返回可审计的混合检索结果及各组件分数。
- `POST /api/rag/answer`：返回引用式摘录回答、来源列表和证据状态。

Spring Boot 的 `/api/chat/stream-carbon` 负责 Agent 编排、会话隔离、证据约束生成和 SSE 输出。

## 可复现命令

```powershell
python rag/build_index.py `
  --source rag/examples/knowledge `
  --output rag/artifacts

python rag/run_benchmark.py `
  --chunks rag/artifacts/chunks.jsonl `
  --cases rag/examples/benchmark_cases.json `
  --source rag/examples/knowledge `
  --output rag/artifacts/benchmark_report.json

python -m unittest discover -s rag/tests -v
```

## 指标

公开 Benchmark 使用合成知识与固定问题比较 BM25、字符 TF-IDF、LSA 和混合检索。主要报告
Hit@1、Recall@3、Recall@5、MRR@5 和 nDCG@5。该结果衡量“相关资料
是否被召回”，不等同于最终回答事实正确率；生成答案还需要单独做来源蕴含与拒答评测。

真实知识、真实评测题、索引产物和运行记录均通过 `.gitignore` 留在私有部署环境。
