# Evidence agent policy

## 中文摘要

Agent 必须先检索、再判断证据是否充分，最后才生成回答。知识库证据不足时，应明确说明“现有证据不足”，并建议补充资料或人工核验，不能为了流畅而强行回答。后端没有返回的字段、数字或状态，生成模型不得凭经验补齐。

对话记忆按匿名会话、登录用户历史和最小生成上下文分层隔离。安全审计只保留必要的技术事件、风险类型和处理结果，不保存完整敏感问题，也不把密码、API Key、身份信息或联系方式写入长期记忆。

## Retrieval before generation

Generation is never the first step. The agent uses hybrid retrieval, selects a small diversified evidence set, checks domain relevance, and labels the evidence state. The generation model receives only the current question, minimal disambiguation context, and the selected evidence blocks.

## Citation and refusal

Only source identifiers provided in the current request may be cited. Unknown citations, unsupported numbers, or uncited factual claims cause a fallback to extractive evidence. When the knowledge pack has insufficient support, the agent says that the evidence is insufficient instead of filling gaps from model memory.

## Memory and privacy

Anonymous sessions are isolated by an unpredictable session identifier. Signed-in history may be stored in the application database, while only the minimum context needed to resolve the current follow-up is sent to the generation model. Passwords, API keys, identity documents, contact details, and raw sensitive questions are never promoted to long-term memory. Users can clear their own session history.

## Security boundary

Instructions found inside retrieved documents are treated as untrusted text. Requests to ignore evidence, reveal secrets, convert simulated data into verified data, or relabel a draft as an active rule are blocked before generation.
