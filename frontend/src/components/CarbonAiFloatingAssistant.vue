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
          <strong>{{ item.role === 'user' ? '你' : '蓝碳 AI' }}</strong>
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
                    {{ source.title || source.name || source.source || source.url || '未命名来源' }}
                  </li>
                </ul>
              </div>
              <p v-if="item.meta.model"><b>使用模型</b>：{{ item.meta.model }}</p>
              <p v-if="item.meta.duration || item.meta.totalMs"><b>响应耗时</b>：{{ item.meta.duration || `${item.meta.totalMs} ms` }}</p>
              <p v-if="item.meta.metrics"><b>简要依据</b>：{{ summarizeMetrics(item.meta.metrics) }}</p>
            </details>
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

const STREAM_URL = '/api/chat/stream-carbon';
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

const prompts = ['什么是蓝碳', '解释碳储估算', '海南红树林证据', '模型输入变量'];
const welcomeMessage = '你好，我是 蓝碳 AI 碳助手。可以询问蓝碳概念、碳储估算、数据来源、模型解释和平台使用问题。';
const messages = ref([{ role: 'assistant', content: welcomeMessage, rawMarkdown: welcomeMessage, streaming: false, meta: null }]);

function openPanel() {
  isOpen.value = true;
  justClosed.value = false;
  nextTick(scheduleScroll);
}

function closePanel() {
  closeEventSource();
  clearFlushTimers(true);
  isOpen.value = false;
  justClosed.value = true;
  window.setTimeout(() => { justClosed.value = false; }, 520);
}

function clearMessages() {
  closeEventSource();
  clearFlushTimers(true);
  messages.value = [{ role: 'assistant', content: welcomeMessage, rawMarkdown: welcomeMessage, streaming: false, meta: null }];
  serviceState.value = '';
  loading.value = false;
  shouldAutoScroll = true;
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

function sendMessage() {
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

  const url = `${STREAM_URL}?message=${encodeURIComponent(text)}&sessionId=${encodeURIComponent(getSessionId())}`;
  eventSource = new EventSource(url);

  eventSource.onopen = () => {
    serviceState.value = '正在分析问题并检索知识资料';
  };

  eventSource.onmessage = (event) => {
    const payload = parseAiStreamPayload(event.data);
    if (!payload) return;
    if (payload.error) {
      showError(payload.error);
      return;
    }
    if (payload.status) serviceState.value = statusLabel(payload.status);
    if (payload.meta) setMeta(payload.meta);
    if (payload.delta) {
      appendDelta(payload.delta);
      serviceState.value = '正在生成回答';
    }
    if (payload.done) finishResponse(payload.meta || {});
  };

  eventSource.onerror = () => {
    if (!loading.value) return;
    showError('AI 服务连接失败，请稍后重试。');
  };

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
  padding: 14px;
  background: var(--hn-bg-soft);
}

.message {
  max-width: 92%;
  margin-bottom: 12px;
  padding: 11px 12px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-card);
}

.message.user {
  margin-left: auto;
  background: var(--hn-accent);
  color: #fff;
}

.message strong {
  display: block;
  margin-bottom: 5px;
  font-size: 12px;
}

.message p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.6;
}

.markdown-body :deep(.md-editor-preview-wrapper),
.markdown-body :deep(.md-editor-preview) {
  padding: 0;
  background: transparent;
  color: var(--hn-text);
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
  color: #08201c;
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
</style>
