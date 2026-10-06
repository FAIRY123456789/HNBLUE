<template>
  <div class="carbon-ai" :class="{ open: isOpen, justClosed: justClosed }">
    <button
      v-if="!isOpen"
      class="ai-fab"
      type="button"
      aria-label="展开碳助手"
      title="展开碳助手"
      @click="openPanel"
    >
      <span class="ai-mark" aria-hidden="true">
        <svg viewBox="0 0 48 48" role="img">
          <circle cx="24" cy="24" r="19" />
          <path d="M16 27c4-9 12-13 20-12-1 10-6 16-16 17" />
          <path d="M18 29c5 0 11-2 17-10" />
          <path d="M15 34h18" />
          <path d="M20 38h8" />
        </svg>
      </span>
      <span class="fab-text">AI 碳助手</span>
      <span class="fab-arrow" aria-hidden="true">↑</span>
    </button>

    <section v-else class="ai-panel" aria-label="AI 碳助手聊天面板">
      <header class="ai-header">
        <div>
          <span>Carbon Assistant</span>
          <h2>AI 碳助手</h2>
        </div>
        <div class="panel-actions">
          <button type="button" aria-label="清空对话" title="清空对话" @click="clearMessages">清空</button>
          <button type="button" aria-label="收起碳助手" title="收起碳助手" @click="closePanel">收起</button>
        </div>
      </header>

      <div ref="messagePanel" class="message-panel" @scroll="handlePanelScroll">
        <article v-for="(item, index) in messages" :key="index" :class="['message', item.role, { streaming: item.streaming }]">
          <span class="message-avatar" aria-hidden="true">{{ item.role === 'user' ? '你' : 'AI' }}</span>
          <div class="message-content">
            <strong>{{ item.role === 'user' ? '你的提问' : '蓝碳 AI' }}</strong>
            <p v-if="item.role === 'user'">{{ item.content }}</p>
            <div v-else class="markdown-body">
              <MdPreview
                :editorId="`carbon-ai-preview-${index}`"
                previewTheme="vuepress"
                :codeFoldable="false"
                :modelValue="item.rawMarkdown || item.content || ' '"
              />
              <details v-if="item.meta" class="answer-meta">
                <summary>回答依据</summary>
                <div v-if="item.meta.sources?.length" class="meta-block">
                  <b>引用资料</b>
                  <ul>
                    <li v-for="(source, sourceIndex) in item.meta.sources.slice(0, 5)" :key="sourceIndex">
                      <a v-if="source.url && /^https:\/\//.test(source.url)" :href="source.url" target="_blank" rel="noopener noreferrer">{{ source.title || source.name || source.recordId || source.url }}</a>
                      <span v-else>{{ source.title || source.name || source.recordId || '未命名来源' }}</span>
                      <small v-if="source.recordId">（{{ source.recordId }}）</small>
                    </li>
                  </ul>
                </div>
                <p v-else>当前回答未附可核验来源，请勿据此作碳核算或政策判断。</p>
                <p v-if="item.meta.model"><b>使用模型</b>：{{ item.meta.model }}</p>
                <p v-if="item.meta.duration || item.meta.totalMs"><b>响应耗时</b>：{{ item.meta.duration || `${item.meta.totalMs} ms` }}</p>
                <p v-if="item.meta.metrics"><b>简要依据</b>：{{ summarizeMetrics(item.meta.metrics) }}</p>
              </details>
            </div>
          </div>
        </article>
      </div>

      <div class="prompt-row">
        <button v-for="prompt in prompts" :key="prompt" type="button" @click="ask(prompt)">{{ prompt }}</button>
      </div>

      <form class="send-row" @submit.prevent="sendMessage">
        <textarea v-model="draft" rows="3" placeholder="询问蓝碳、碳储估算、数据来源或模型解释，Ctrl+Enter 发送" @keydown.ctrl.enter.prevent="sendMessage"></textarea>
        <button type="submit" :disabled="loading">{{ loading ? '生成中' : '发送' }}</button>
      </form>

      <p v-if="serviceState" class="service-state">{{ serviceState }}</p>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/style.css';
import { parseAiStreamPayload } from '@/utils/aiStreamParser';
import { answerPublicFact, isPublicFactQuery } from '@/utils/publicFactAnswer.mjs';
import { apiUrl, publicUrl } from '@/utils/urls';

const STREAM_URL = apiUrl('/api/chat/stream-carbon');
const STATUS_URL = apiUrl('/api/chat/knowledge-status');
const HISTORY_URL = apiUrl('/api/chat/history');
const FLUSH_INTERVAL = 40;
const isOpen = ref(false);
const justClosed = ref(false);
const loading = ref(false);
const draft = ref('');
const serviceState = ref('');
const messagePanel = ref(null);
let eventSource = null;
let pendingDelta = '';
let flushTimer = null;
let scrollFrame = null;
let activeAssistantIndex = -1;
let shouldAutoScroll = true;
let historyLoaded = false;

const prompts = ['什么是蓝碳', '解释碳储估算', '海南红树林证据', '模型输入变量'];
const welcomeMessage = '你好，我是 蓝碳 AI 碳助手。可以询问蓝碳概念、碳储估算、数据来源、模型解释和平台使用问题。';
const messages = ref([{ role: 'assistant', content: welcomeMessage, rawMarkdown: welcomeMessage, streaming: false, meta: null }]);

function openPanel() {
  isOpen.value = true;
  justClosed.value = false;
  loadKnowledgeStatus();
  loadHistory();
  nextTick(scheduleScroll);
}

function authHeaders(extra = {}) {
  const token = localStorage.getItem('token');
  return token ? { ...extra, Authorization: `Bearer ${token}` } : { ...extra };
}

async function loadHistory() {
  if (historyLoaded || loading.value) return;
  historyLoaded = true;
  try {
    const response = await fetch(`${HISTORY_URL}?sessionId=${encodeURIComponent(getSessionId())}`, {
      headers: authHeaders({ Accept: 'application/json' }),
    });
    if (!response.ok) return;
    const payload = await response.json();
    if (Array.isArray(payload.messages) && payload.messages.length) {
      messages.value = payload.messages.map((item) => ({
        role: item.role === 'user' ? 'user' : 'assistant',
        content: item.content || '',
        rawMarkdown: item.content || '',
        streaming: false,
        meta: item.role === 'assistant' ? { evidenceStatus: item.evidenceStatus, sources: [] } : null,
      }));
    }
  } catch {
    // History is a convenience; the assistant remains available when it cannot be restored.
  }
}

async function loadKnowledgeStatus() {
  if (loading.value) return;
  try {
    const response = await fetch(STATUS_URL, { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error('status unavailable');
    const status = await response.json();
    serviceState.value = status.configured
      ? 'HNBLUE 本地知识库已连接'
      : '本地知识库暂时不可用；公开记录问答可用';
  } catch {
    serviceState.value = '知识库状态暂时无法读取；公开记录问答可用';
  }
}

function closePanel() {
  closeEventSource();
  clearFlushTimers(true);
  isOpen.value = false;
  justClosed.value = true;
  window.setTimeout(() => { justClosed.value = false; }, 520);
}

async function clearMessages() {
  closeEventSource();
  clearFlushTimers(true);
  messages.value = [{ role: 'assistant', content: welcomeMessage, rawMarkdown: welcomeMessage, streaming: false, meta: null }];
  serviceState.value = '';
  loading.value = false;
  shouldAutoScroll = true;
  try {
    await fetch(`${HISTORY_URL}?sessionId=${encodeURIComponent(getSessionId())}`, {
      method: 'DELETE',
      headers: authHeaders({ Accept: 'application/json' }),
    });
  } catch {
    serviceState.value = '本地界面已清空；服务器历史暂时未能清除';
  }
}

function ask(prompt) {
  draft.value = prompt;
  sendMessage();
}

function getSessionId() {
  const key = 'hnblue_ai_session_id';
  let value = sessionStorage.getItem(key);
  if (!value) {
    value = crypto?.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    sessionStorage.setItem(key, value);
  }
  return value;
}

async function sendMessage() {
  const text = draft.value.trim();
  if (!text || loading.value) return;

  closeEventSource();
  clearFlushTimers(true);
  serviceState.value = '正在连接 AI 碳助手';
  messages.value.push({ role: 'user', content: text, streaming: false });
  activeAssistantIndex = messages.value.push({ role: 'assistant', content: '', rawMarkdown: '', streaming: true, meta: null }) - 1;
  loading.value = true;
  draft.value = '';
  shouldAutoScroll = true;

  if (isPublicFactQuery(text)) {
    const requestIndex = activeAssistantIndex;
    try {
      serviceState.value = '正在核对公开记录';
      const response = await fetch(publicUrl('/data/demo/frontend_literature_carbon_cards.json'));
      if (!response.ok) throw new Error('公开记录暂时不可读取');
      const data = await response.json();
      const fact = answerPublicFact(text, data.cards || []);
      const current = messages.value[requestIndex];
      if (!current?.streaming) return;
      current.rawMarkdown = fact.answer;
      current.content = fact.answer;
      current.meta = { sources: fact.sources, route: fact.route, evidenceStatus: fact.evidenceStatus };
      finishResponse();
    } catch (error) {
      if (messages.value[requestIndex]?.streaming) showError(error.message || '公开记录暂时不可读取');
    }
    return;
  }

  const controller = new AbortController();
  eventSource = { close: () => controller.abort() };
  try {
    const response = await fetch(STREAM_URL, {
      method: 'POST',
      headers: authHeaders({ 'Content-Type': 'application/json', Accept: 'text/event-stream' }),
      body: JSON.stringify({ message: text, sessionId: getSessionId() }),
      signal: controller.signal,
    });
    if (!response.ok || !response.body) throw new Error('AI 服务连接失败，请稍后重试。');
    serviceState.value = '正在分析问题并检索知识资料';
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const events = buffer.split(/\r?\n\r?\n/);
      buffer = events.pop() || '';
      for (const block of events) {
        const data = block.split(/\r?\n/).filter((line) => line.startsWith('data:'))
          .map((line) => line.slice(5).trim()).join('\n');
        if (!data) continue;
        const payload = parseAiStreamPayload(data);
        if (!payload) continue;
        if (payload.error) throw new Error(payload.error);
        if (payload.status) serviceState.value = statusLabel(payload.status);
        if (payload.meta) setMeta(payload.meta);
        if (payload.delta) { appendDelta(payload.delta); serviceState.value = '正在生成回答'; }
        if (payload.done) finishResponse(payload.meta || {});
      }
    }
    if (loading.value) finishResponse();
  } catch (error) {
    if (error?.name !== 'AbortError' && loading.value) showError(error.message || 'AI 服务连接失败，请稍后重试。');
  }
  scheduleScroll();
}

function appendDelta(content) {
  if (!content) return;
  pendingDelta += content;
  if (flushTimer) return;
  flushTimer = window.setTimeout(flushPendingDelta, FLUSH_INTERVAL);
}

function flushPendingDelta() {
  if (flushTimer) {
    clearTimeout(flushTimer);
    flushTimer = null;
  }
  if (!pendingDelta) return;
  const current = messages.value[activeAssistantIndex];
  if (current?.role !== 'assistant') {
    pendingDelta = '';
    return;
  }
  current.rawMarkdown += pendingDelta;
  current.content = current.rawMarkdown;
  pendingDelta = '';
  scheduleScroll();
}

function setMeta(meta) {
  const current = messages.value[activeAssistantIndex];
  if (current?.role === 'assistant') current.meta = meta;
}

function finishResponse(payload = {}) {
  flushPendingDelta();
  const current = messages.value[activeAssistantIndex];
  if (current?.role === 'assistant' && !current.meta) current.meta = { sources: [] };
  closeEventSource();
  markAssistantDone();
  loading.value = false;
  serviceState.value = payload.totalMs ? `回答完成 · ${payload.totalMs} ms` : '回答完成';
  scheduleScroll();
}

function showError(message) {
  flushPendingDelta();
  const current = messages.value[activeAssistantIndex];
  if (current?.role === 'assistant' && !current.rawMarkdown.trim()) {
    current.rawMarkdown = message;
    current.content = message;
  }
  serviceState.value = message;
  closeEventSource();
  markAssistantDone();
  loading.value = false;
  scheduleScroll();
}

function markAssistantDone() {
  const current = messages.value[activeAssistantIndex];
  if (current?.role === 'assistant') current.streaming = false;
}

function clearFlushTimers(clearPending = false) {
  if (flushTimer) {
    clearTimeout(flushTimer);
    flushTimer = null;
  }
  if (scrollFrame) {
    cancelAnimationFrame(scrollFrame);
    scrollFrame = null;
  }
  if (clearPending) pendingDelta = '';
}

function closeEventSource() {
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
}

function handlePanelScroll() {
  const panel = messagePanel.value;
  if (!panel) return;
  shouldAutoScroll = panel.scrollHeight - panel.scrollTop - panel.clientHeight < 72;
}

function scheduleScroll() {
  if (!shouldAutoScroll || scrollFrame) return;
  scrollFrame = requestAnimationFrame(() => {
    scrollFrame = null;
    nextTick(() => {
      if (messagePanel.value) messagePanel.value.scrollTop = messagePanel.value.scrollHeight;
    });
  });
}

function statusLabel(status) {
  if (status === 'thinking') return '正在分析问题并检索知识资料';
  if (status === 'generating') return '正在生成回答';
  if (status === 'unavailable') return '项目知识库暂时不可用';
  return '正在连接 AI 碳助手';
}

function summarizeMetrics(metrics) {
  if (!metrics || typeof metrics !== 'object') return '';
  return Object.entries(metrics)
    .slice(0, 4)
    .map(([key, value]) => `${key}: ${typeof value === 'object' ? JSON.stringify(value) : value}`)
    .join('；');
}

function handleGlobalOpen() {
  openPanel();
}

onMounted(() => {
  window.addEventListener('hnblue-open-ai', handleGlobalOpen);
});

onBeforeUnmount(() => {
  window.removeEventListener('hnblue-open-ai', handleGlobalOpen);
  closeEventSource();
  clearFlushTimers(true);
});
</script>

<style scoped>
.carbon-ai {
  position: fixed;
  right: clamp(14px, 2vw, 24px);
  bottom: clamp(14px, 2vw, 24px);
  z-index: 900;
  font-family: Arial, "Microsoft YaHei", sans-serif;
}

.ai-fab {
  width: 56px;
  min-height: 52px;
  display: inline-flex;
  align-items: center;
  gap: 9px;
  overflow: hidden;
  padding: 0 12px 0 9px;
  border: 1px solid color-mix(in srgb, var(--hn-accent) 42%, var(--hn-border));
  border-radius: 999px;
  background: var(--hn-control-bg);
  color: var(--hn-text);
  box-shadow: var(--hn-shadow);
  cursor: pointer;
  font-weight: 900;
  transition: width 240ms ease, transform 240ms ease, box-shadow 240ms ease, background 240ms ease, color 240ms ease;
}

.ai-fab:hover,
.ai-fab:focus-visible,
.carbon-ai.justClosed .ai-fab {
  width: 154px;
  transform: translateY(-2px);
  box-shadow: 0 18px 42px rgba(10, 68, 58, 0.18);
}

.ai-mark {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: color-mix(in srgb, var(--hn-accent) 12%, var(--hn-card));
}

.ai-mark svg {
  width: 26px;
  height: 26px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.fab-text,
.fab-arrow {
  opacity: 0;
  transform: translateX(-8px);
  white-space: nowrap;
  transition: opacity 220ms ease, transform 220ms ease;
}

.ai-fab:hover .fab-text,
.ai-fab:focus-visible .fab-text,
.ai-fab:hover .fab-arrow,
.ai-fab:focus-visible .fab-arrow,
.carbon-ai.justClosed .fab-text,
.carbon-ai.justClosed .fab-arrow {
  opacity: 1;
  transform: translateX(0);
}

.ai-panel {
  width: min(520px, calc(100vw - 28px));
  overflow: hidden;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-card);
  color: var(--hn-text);
  box-shadow: var(--hn-shadow);
  animation: panel-in 240ms ease both;
}

.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--hn-border);
  background: var(--hn-surface);
}

.ai-header span {
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.ai-header h2 {
  margin: 4px 0 0;
  font-size: 20px;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.panel-actions button,
.prompt-row button,
.send-row button {
  border: 1px solid var(--hn-border);
  border-radius: 7px;
  background: var(--hn-control-bg);
  color: var(--hn-accent);
  cursor: pointer;
  font-weight: 900;
}

.panel-actions button {
  height: 32px;
  padding: 0 9px;
}

.message-panel {
  max-height: min(54vh, 520px);
  overflow: auto;
  padding: 18px 16px;
  background: var(--hn-bg-soft);
}

.message {
  max-width: 94%;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin: 0 0 18px;
}

.message.user {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-avatar {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  display: grid;
  place-items: center;
  border: 1px solid var(--hn-border-strong);
  border-radius: 50%;
  background: var(--hn-surface);
  color: var(--hn-accent);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.02em;
}

.message.user .message-avatar {
  border-color: color-mix(in srgb, var(--hn-accent) 62%, transparent);
  background: var(--hn-accent);
  color: var(--hn-on-accent);
}

.message-content {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid var(--hn-border);
  border-radius: 5px 16px 16px 16px;
  background: var(--hn-card);
  box-shadow: 0 8px 24px rgba(6, 45, 38, 0.06);
}

.message.user .message-content {
  border-color: color-mix(in srgb, var(--hn-accent) 74%, var(--hn-border));
  border-radius: 16px 5px 16px 16px;
  background: var(--hn-accent);
  color: var(--hn-on-accent);
}

.message strong {
  display: block;
  margin-bottom: 6px;
  color: var(--hn-accent);
  font-size: 12px;
  letter-spacing: 0.02em;
}

.message.user strong {
  color: var(--hn-on-accent);
  opacity: 0.82;
}

.message p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.6;
}

.markdown-body :deep(.md-editor),
.markdown-body :deep(.md-editor-preview-wrapper),
.markdown-body :deep(.md-editor-preview) {
  padding: 0;
  background: transparent !important;
  color: var(--hn-text);
  --md-bk-color: transparent;
  --md-color: var(--hn-text);
}

.markdown-body :deep(.md-editor-preview-wrapper) {
  overflow: visible;
}

.markdown-body :deep(.md-editor-preview p),
.markdown-body :deep(.md-editor-preview li),
.markdown-body :deep(.md-editor-preview blockquote) {
  color: var(--hn-text);
  line-height: 1.65;
}

.markdown-body :deep(.md-editor-preview code) {
  border-radius: 5px;
  background: var(--hn-surface);
  color: var(--hn-accent);
}

.markdown-body :deep(.md-editor-preview pre code) {
  color: inherit;
}

.message.assistant.streaming .markdown-body::after {
  content: "";
  display: inline-block;
  width: 7px;
  height: 1.1em;
  margin-left: 3px;
  border-radius: 2px;
  background: var(--hn-accent);
  vertical-align: -0.18em;
  animation: caret-blink 0.85s ease-in-out infinite;
}

.answer-meta {
  margin-top: 10px;
  border-top: 1px dashed var(--hn-border);
  padding-top: 8px;
  color: var(--hn-muted);
  font-size: 12px;
}

.answer-meta summary {
  cursor: pointer;
  color: var(--hn-accent);
  font-weight: 900;
}

.answer-meta ul {
  margin: 6px 0 0 18px;
  padding: 0;
}

.prompt-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 14px 0;
}

.prompt-row button {
  min-height: 30px;
  padding: 0 9px;
  font-size: 12px;
}

.send-row {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.send-row textarea {
  width: 100%;
  resize: vertical;
  border: 1px solid var(--hn-border-strong);
  border-radius: 8px;
  background: var(--hn-control-bg);
  color: var(--hn-text);
  padding: 10px;
  font: inherit;
}

.send-row button {
  min-height: 40px;
  background: var(--hn-accent);
  color: var(--hn-on-accent);
}

.send-row button:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.service-state {
  margin: 0;
  padding: 0 14px 14px;
  color: var(--hn-muted);
  font-size: 13px;
  line-height: 1.5;
}

@keyframes caret-blink {
  0%, 100% { opacity: 0.2; }
  50% { opacity: 1; }
}

@keyframes panel-in {
  from { opacity: 0; transform: translateY(12px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@media (prefers-reduced-motion: reduce) {
  .ai-fab,
  .fab-text,
  .fab-arrow,
  .ai-panel,
  .message.assistant.streaming .markdown-body::after {
    animation: none;
    transition: none;
  }
}

@media (max-width: 560px) {
  .carbon-ai {
    right: 10px;
    bottom: 10px;
  }

  .ai-panel {
    width: calc(100vw - 20px);
  }

  .ai-fab:hover,
  .ai-fab:focus-visible,
  .carbon-ai.justClosed .ai-fab {
    width: 134px;
  }
}
/* 助手细节：使用与悬浮入口一致的叶片符号，并让对话区更有呼吸感。 */
.message-panel {
  min-height: 250px;
  max-height: min(42vh, 380px);
  padding: 18px 16px 24px;
}

.message.assistant .message-avatar {
  display: grid;
  place-items: center;
  color: transparent;
  font-size: 0;
}

.message.assistant .message-avatar::before {
  width: 24px;
  height: 24px;
  content: '';
  background: var(--hn-accent);
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M20.8 3.2C13.1 3.5 6.5 6.5 4.1 12.3c-1.8 4.3.3 7.5 3.8 8.3 4.4 1 8.8-2.2 10.8-6.3 1.8-3.8 1.6-7.6 2.1-11.1ZM5.2 20.8c3-4.6 6.6-7.7 11.7-10.2' fill='none' stroke='black' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") center / contain no-repeat;
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M20.8 3.2C13.1 3.5 6.5 6.5 4.1 12.3c-1.8 4.3.3 7.5 3.8 8.3 4.4 1 8.8-2.2 10.8-6.3 1.8-3.8 1.6-7.6 2.1-11.1ZM5.2 20.8c3-4.6 6.6-7.7 11.7-10.2' fill='none' stroke='black' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E") center / contain no-repeat;
}

.message.assistant .message-content > strong:first-child {
  display: none;
}

.send-row textarea {
  min-height: 76px;
  resize: none;
  border-radius: 12px;
  background: color-mix(in srgb, var(--hn-panel) 94%, transparent);
  box-shadow: inset 0 1px 0 color-mix(in srgb, white 64%, transparent);
}

.send-row textarea:focus,
.send-row textarea:focus-visible {
  outline: 1px solid color-mix(in srgb, var(--hn-accent) 46%, transparent);
  outline-offset: 1px;
  border-color: color-mix(in srgb, var(--hn-accent) 64%, var(--hn-border));
  box-shadow:
    0 0 0 1px color-mix(in srgb, var(--hn-accent) 10%, transparent),
    inset 0 1px 0 color-mix(in srgb, white 64%, transparent);
}

@media (max-width: 640px) {
  .message-panel {
    min-height: 220px;
    max-height: 38vh;
  }
}
</style>
