<!--
AI聊天对话框组件 - 碳汇智能对话系统界面

功能概述：
• 实时对话交互：支持用户与AI进行碳汇相关问答
• 快捷提问：提供8个预设碳汇问题快速提问
• 流式响应：使用EventSource实现实时流式回答
• Markdown渲染：AI回答支持Markdown格式渲染

数据流管理：
• 通过EventSource连接后端API获取流式响应
• 管理消息列表、加载状态和连接生命周期
• 处理快捷提问的预设问题映射

组件依赖：
• Element Plus - 表单和UI组件
• md-editor-v3 - Markdown内容渲染
-->
<template>
  <div class="carbon-chat-panel">
    <!-- 标题栏 -->
    <div class="chat-title">
      Carbon AI · 碳汇智能对话系统
      <button class="close-btn" @click="closeChat">×</button>
    </div>

    <!-- 消息显示区域 -->
    <div class="message-panel" id="message-panel">
      <div class="message-list">
        <!-- 消息列表渲染 - 区分用户和AI消息 -->
        <div v-for="(item, index) in messageList" :key="index"
          :class="['message-item', item.type === 0 ? 'user-item' : 'ai-item']">
          <template v-if="item.type === 0">
            <!-- 用户消息布局 -->
            <div class="message-row user-item">
              <div class="message-content user-bubble">{{ item.content }}</div>
              <img class="user-icon" src="@/assets/user.png" alt="User" />
            </div>
          </template>
          <template v-else>
            <!-- AI消息布局 - 包含Markdown渲染和加载状态 -->
            <div class="message-row ai-item">
              <img class="ai-icon" src="@/assets/deepseek.png" alt="AI" />
              <div class="message-content ai-bubble">
                <MdPreview previewTheme="vuepress" :codeFoldable="false" editorId="preview"
                  :modelValue="item.content" />
                <div class="loading" v-if="item.loading">
                  <img src="../assets/loading.gif" />
                </div>
              </div>
            </div>
          </template>

        </div>
      </div>
    </div>

    <!-- 快捷提问区域 - 8个预设碳汇问题 -->
    <div class="prompt-panel">
      <span class="prompt-label">🤔 快速提问：</span>
      <span class="prompt-link" @click="setPrompt('海南红树林碳储量是多少？')">海南红树林碳储量</span>
      <span class="prompt-link" @click="setPrompt('什么是蓝碳？')">什么是蓝碳</span>
      <span class="prompt-link" @click="setPrompt('碳通量和碳储量的区别？')">通量 vs 储量</span>
      <span class="prompt-link" @click="setPrompt('海南哪些市县碳储最多？')">高碳储区域</span>
      <span class="prompt-link" @click="setPrompt('如何评估碳汇的经济价值？')">碳汇价值评估</span>
      <span class="prompt-link" @click="setPrompt('碳储预测模型的核心变量有哪些？')">预测模型变量</span>
      <span class="prompt-link" @click="setPrompt('碳通量的监测方式有哪些？')">碳通量监测方法</span>
      <span class="prompt-link" @click="setPrompt('碳溯用到了哪个AI模型？')">碳溯模型介绍</span>
    </div>

    <!-- 消息发送区域 -->
    <div class="send-panel">
      <el-form :model="formData" @submit.prevent class="form-container">
        <el-input type="textarea" :rows="4" v-model="formData.content" placeholder="输入你想探索的碳汇问题..." @keyup="keySend"
          class="carbon-textarea" />
        <el-button type="primary" @click="sendMessage" :disabled="loading" class="carbon-button">
          提问(ctrl+enter)
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, defineEmits, watch, nextTick, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/style.css';
import { parseAiStreamPayload } from '@/utils/aiStreamParser';

// 组件事件和属性定义
const emit = defineEmits(['close']);
const props = defineProps({
  apiUrl: String,
  visible: Boolean
});

// 响应式数据
const formData = ref({ content: '' });
const messageList = ref([]);
const loading = ref(false);
let eventSource = null;
let keydownHandler = null;
let isClosing = false;
// 快捷提问预设问题映射
const promptMap = {
  '海南红树林碳储量是多少？': '请介绍海南省红树林的碳储量现状，并提供简要分析。',
  '什么是蓝碳？': '请用简明语言解释“蓝碳”的概念，并结合红树林生态系统举例。',
  '碳通量和碳储量的区别？': '请解释碳通量与碳储量的区别及其生态意义。',
  '海南哪些市县碳储最多？': '海南省哪些市县具有较高的蓝碳储量？请说明可能原因。',
  '如何评估碳汇的经济价值？': '如何从经济角度评估碳汇价值？请给出红树林相关案例。',
  '碳储预测模型的核心变量有哪些？': '碳储预测模型中，哪些变量是影响结果的关键因子？',
  '碳通量的监测方式有哪些？': '目前常用的碳通量监测方法有哪些？适用于红树林吗？',
  '碳溯用到了哪个AI模型？': '请你详细介绍碳溯模块使用到的AI模型（CatBoost+BAAD数据集）。'
};

// 监听visible属性变化
watch(() => props.visible, (val) => {
  val ? openChat() : closeChat();
});

// 打开聊天：绑定快捷键
function openChat() {
  if (!keydownHandler) {
    keydownHandler = (e) => {
      if (e.ctrlKey && e.key === 'Enter') sendMessage();
    };
    window.addEventListener('keydown', keydownHandler);
  }
}

// 关闭聊天：清理资源
function closeChat() {
  closeEventSource();
  emit('close');
  if (keydownHandler) {
    window.removeEventListener('keydown', keydownHandler);
    keydownHandler = null;
  }
}

// 设置快捷提问
function setPrompt(key) {
  const userPrompt = promptMap[key] || key;
  const fullPrompt = userPrompt;
  formData.value.content = fullPrompt;
  sendMessage();
}

// 发送消息核心逻辑
function sendMessage() {
  const msg = formData.value.content;
  if (!msg) return ElMessage.warning('请输入内容');

  // 添加用户消息和AI加载状态
  const visibleUserMsg = msg.includes('\n') ? msg.split('\n').pop().trim() : msg;
  messageList.value.push({ type: 0, content: visibleUserMsg });
  messageList.value.push({ type: 1, content: ' ', loading: true });

  loading.value = true;
  isClosing = false;

  // 建立EventSource连接获取流式响应
  eventSource = new EventSource(`${props.apiUrl}/stream-carbon?message=${encodeURIComponent(msg)}`);
  formData.value.content = '';

  // 处理流式响应数据
  eventSource.onmessage = (event) => {
    if (isClosing) return;
    const raw = event.data;
    if (!raw || raw === 'end') {
      isClosing = true;
      closeEventSource();
      return;
    }
    try {
      // 解析嵌套的JSON响应
      const outer = JSON.parse(raw);
      if (outer.content) {
        const inner = JSON.parse(outer.content);
        if (inner.textResponse) {
          messageList.value[messageList.value.length - 1].content += inner.textResponse;
        }
      }
    } catch (err) {
      console.error('解析失败：', raw, err);
    }
    // 滚动到底部
    nextTick(() => {
      const el = document.getElementById('message-panel');
      if (el) el.scrollTop = el.scrollHeight;
    });
  };

  eventSource.onerror = () => {
    if (isClosing) return;
    closeEventSource();
  };
}

// 关闭EventSource连接
function closeEventSource() {
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
  loading.value = false;
  const last = messageList.value[messageList.value.length - 1];
  if (last) last.loading = false;
}

// 组件卸载时清理
onUnmounted(() => {
  closeEventSource();
});
</script>

<style scoped>
/* 主容器样式 */
.carbon-chat-panel {
  height: 100%;
  background: #1b2c26;
  display: flex;
  flex-direction: column;
  color: #fff;
}

/* 标题栏样式 */
.chat-title {
  background: #264d3b;
  padding: 12px 20px;
  font-size: 22px;
  font-weight: bold;
  display: flex;
  justify-content: space-between;
}

.close-btn {
  font-size: 20px;
  background: none;
  border: none;
  color: #fff;
  cursor: pointer;
}

/* 消息面板样式 */
.message-panel {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #0f1c18;
}

/* 消息项通用样式 */
.message-item {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
}

/* 用户和AI消息气泡样式 */
.user-bubble {
  align-self: flex-end;
  background: #2d7651;
  padding: 12px;
  border-radius: 10px;
  max-width: 70%;
}

.ai-bubble {
  align-self: flex-start;
  background: #2a2a2a;
  padding: 12px;
  border-radius: 10px;
  max-width: 70%;
  word-wrap: break-word;
}

/* 发送面板样式 */
.send-panel {
  padding: 16px;
  background: #1b2c26;
  display: flex;
  flex-direction: column;
}

.carbon-textarea {
  font-size: 18px;
  border-radius: 10px;
  background: #f1f3f1;
}

.carbon-button {
  margin-top: 10px;
  background-color: #2d7651;
  border: none;
  font-size: 18px;
  color: white;
  padding: 10px 20px;
  border-radius: 10px;
  cursor: pointer;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
}

.carbon-button:hover {
  background-color: #3e9d6c;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 16px;
  max-width: 100%;
}

.user-item {
  flex-direction: row-reverse;
  justify-content: flex-end;
}

.ai-item {
  flex-direction: row;
}

.user-icon,
.ai-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.user-bubble {
  background: #2d65f7;
  color: #fff;
  padding: 12px;
  border-radius: 12px;
  max-width: 70%;
  word-wrap: break-word;
}

.ai-bubble {
  background: #0a2518;
  color: #e7e7e7;
  padding: 12px;
  border-radius: 12px;
  max-width: 70%;
  word-wrap: break-word;
}

/* ✅ 确保 Markdown 渲染区域继承背景 */
.ai-item .md-editor {
  background-color: #0a2518 !important;
}

::v-deep(.ai-item .md-editor .vuepress-theme) {
  --md-theme-color: #ffffff !important;
  /* 全局字体白色 */
  --md-theme-bg-color: #1f2b25;
  --md-theme-code-inline-bg-color: #2c2c2c;
  --md-theme-code-block-bg-color: #2c2c2c;
  --md-theme-quote-bg-color: #2a2a2a;
  --md-border-color: #444;
}

::v-deep(.ai-item .md-editor .vuepress-theme h1),
::v-deep(.ai-item .md-editor .vuepress-theme h2),
::v-deep(.ai-item .md-editor .vuepress-theme h3),
::v-deep(.ai-item .md-editor .vuepress-theme h4),
::v-deep(.ai-item .md-editor .vuepress-theme h5),
::v-deep(.ai-item .md-editor .vuepress-theme h6) {
  color: #ffffff !important;
}

/* 快捷提问面板样式 */
.prompt-panel {
  padding: 12px 24px;
  background-color: #1b2c26;
  border-top: 1px solid #2a2a2a;
  font-size: 15px;
}

.prompt-label {
  font-weight: bold;
  color: #00ffa8;
  margin-right: 8px;
}

.prompt-link {
  color: #b3f0dd;
  cursor: pointer;
  margin-right: 16px;
  transition: all 0.2s ease;
  font-weight: 500;
}

.prompt-link:hover {
  color: #00ffa8;
  text-decoration: underline;
}
</style>
