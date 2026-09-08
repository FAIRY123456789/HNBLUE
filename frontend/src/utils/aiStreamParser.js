function parseMaybeJson(value) {
  if (typeof value !== "string") return value;
  const trimmed = value.trim();
  if (!trimmed) return "";
  try {
    return JSON.parse(trimmed);
  } catch {
    return value;
  }
}

function firstText(...values) {
  for (const value of values) {
    if (value !== undefined && value !== null && String(value) !== "") return String(value);
  }
  return "";
}

export function stripThinkBlocks(text = "") {
  return String(text).replace(/<think>[\s\S]*?<\/think>/gi, "").replace(/<think>[\s\S]*$/gi, "");
}

export function parseAiStreamPayload(raw) {
  if (!raw || raw === "[DONE]" || raw === "end" || raw === "done") return { done: true };
  let payload = parseMaybeJson(raw);
  if (typeof payload === "string") return { delta: stripThinkBlocks(payload) };
  if (payload?.content && typeof payload.content === "string") {
    const nested = parseMaybeJson(payload.content);
    if (nested && typeof nested === "object") payload = { ...payload, ...nested };
  }
  const type = payload?.type || payload?.status;
  const delta = firstText(payload?.delta, payload?.textResponse, payload?.text, payload?.content, payload?.message);
  return {
    status: type === "status" ? payload?.status || payload?.message : payload?.status,
    delta: type === "done" || type === "error" ? "" : stripThinkBlocks(delta),
    meta: payload?.meta || payload?.sources || payload?.metrics || payload?.model ? payload : null,
    done: type === "done" || payload?.done === true,
    error: type === "error" ? payload?.message || "AI 服务连接失败，请稍后重试。" : "",
  };
}
