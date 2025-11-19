<!-- 
通用AI助手组件 - HNBLUEChat海南蓝碳数字化系统AI助手

功能概述：
• 通用智能对话：支持用户与AI进行自然语言交互
• 快捷提问：提供7个预设蓝碳相关问题快速提问
• 流式响应：使用EventSource实现实时流式回答
• Markdown渲染：AI回答支持Markdown格式渲染

与碳助手的区别：
• 通用性：不局限于碳汇专业问题，支持更广泛的系统相关问答
• 界面风格：采用蓝色主题，与系统整体风格一致
• 预设问题：侧重系统功能和使用相关的快速提问

数据流管理：
• 通过EventSource连接后端API获取流式响应
• 管理消息列表、加载状态和连接生命周期
• 处理快捷提问的预设问题映射
-->
<template>
    <div class="chat-panel">
        <!-- 标题栏 -->
        <div class="chat-title">
            HNBLUEChat-海南蓝碳数字化系统AI助手
            <button class="close-btn" @click="closeChat">×</button>
        </div>

        <!-- 消息显示区域 -->
        <div class="message-panel" id="message-panel">
            <div class="message-list">
                <!-- 消息列表渲染 - 区分用户和AI消息 -->
                <div :class="['message-item', item.type === 0 ? 'user-item' : 'ai-item']"
                    v-for="(item, index) in messageList" :key="index">
                    <!-- 用户消息布局 -->
                    <template v-if="item.type === 0">
                        <div class="message-content">
                            <div class="content-inner">{{ item.content }}</div>
                        </div>
                        <img class="user-icon" src="@/assets/user.png" alt="User" />
                    </template>
                    <!-- AI消息布局 - 包含Markdown渲染和加载状态 -->
                    <template v-else>
                        <img class="ai-icon" src="@/assets/deepseek.png" alt="AI" />
                        <div class="message-content ai-item">
                            <MdPreview previewTheme="vuepress" :codeFoldable="false" editorId="preview"
                                :modelValue="Array.isArray(item.content) ? item.content.join('') : item.content" />
                            <div class="loading" v-if="item.loading">
                                <img src="../assets/loading.gif" />
                            </div>
                        </div>
                    </template>
                </div>
            </div>
        </div>

        <!-- 快捷提问区域 - 7个预设系统相关问题 -->
        <div class="prompt-panel">
            <span class="prompt-label">🤔 快速提问：</span>
            <span class="prompt-link" @click="setPrompt('海南红树林碳储量是多少？')">海南红树林碳储量</span>
            <span class="prompt-link" @click="setPrompt('什么是蓝碳？')">什么是蓝碳</span>
            <span class="prompt-link" @click="setPrompt('碳通量和碳储量的区别？')">通量 vs 储量</span>
            <span class="prompt-link" @click="setPrompt('海南哪些市县碳储最多？')">高碳储区域</span>
            <span class="prompt-link" @click="setPrompt('如何评估碳汇的经济价值？')">碳汇价值评估</span>
            <span class="prompt-link" @click="setPrompt('碳储预测模型的核心变量有哪些？')">预测模型变量</span>
            <span class="prompt-link" @click="setPrompt('碳通量的监测方式有哪些？')">碳通量监测方法</span>
        </div>

        <!-- 消息发送区域 -->
        <div class="send-panel">
            <el-form :model="formData" ref="formDataRef" @submit.prevent class="form-container">
                <el-form-item label="" prop="content" class="input-wrapper">
                    <el-input type="textarea" :rows="4" clearable placeholder="请输入你想问的问题" v-model="formData.content"
                        @keyup="keySend" />
                </el-form-item>
                <el-form-item label="" prop="" class="send-btn-wrapper">
                    <el-button type="primary" @click="sendMessage" :disabled="loading">
                        发送(ctrl+enter)
                    </el-button>
                </el-form-item>
            </el-form>
        </div>

    </div>
</template>

<script setup>
import { ElMessage } from 'element-plus';
import { ref, nextTick, defineEmits, onUnmounted, watch } from 'vue';
import { MdPreview } from 'md-editor-v3';
import 'md-editor-v3/lib/style.css';

// 组件事件和属性定义
const emit = defineEmits(['close']);
const props = defineProps({
    apiUrl: {
        type: String,
        required: true
    },
    visible: {
        type: Boolean,
        required: true
    }
});

// 响应式数据
const formData = ref({ content: '' });
const messageList = ref([
    {
        type: 1,
        content: ['欢迎使用 HNBLUE AI 智能助手，有任何关于蓝碳系统的问题都可以问我哦！']
    }
]);
const loading = ref(false);

// 事件源和快捷键处理
let eventSource = null;
let isClosing = false; // ✅ 互斥标志
//将事件监听器存储在变量中，方便在模态框关闭时移除。
let keydownHandler = null;

// 快捷提问预设问题映射
const promptMap = {
    '海南红树林碳储量是多少？': '请介绍海南省红树林的碳储量现状，并提供简要分析。',
    '什么是蓝碳？': '请用简明语言解释“蓝碳”的概念，并结合红树林生态系统举例。',
    '碳通量和碳储量的区别？': '请解释碳通量与碳储量的区别及其生态意义。',
    '海南哪些市县碳储最多？': '海南省哪些市县具有较高的蓝碳储量？请说明可能原因。',
    '如何评估碳汇的经济价值？': '如何从经济角度评估碳汇价值？请给出红树林相关案例。',
    '碳储预测模型的核心变量有哪些？': '碳储预测模型中，哪些变量是影响结果的关键因子？',
    '碳通量的监测方式有哪些？': '目前常用的碳通量监测方法有哪些？适用于红树林吗？'
};

// 监听visible属性变化控制快捷键
watch(() => props.visible, (newValue) => {
    if (newValue) {
        openChat(); // ✅ 打开时触发监听
    } else {
        closeChat(); // ✅ 关闭时移除监听
    }
});

/**
 * 打开聊天：绑定Ctrl+Enter快捷键
 */
const openChat = () => {
    if (!keydownHandler) {
        keydownHandler = (event) => {
            if (event.ctrlKey && event.key === 'Enter') {
                sendMessage();
            }
        };
        window.addEventListener('keydown', keydownHandler);
    }
};

/**
 * 关闭聊天：清理事件源和快捷键
 */
const closeChat = () => {
    closeEventSource();
    emit('close');

    if (keydownHandler) {
        window.removeEventListener('keydown', keydownHandler);
        keydownHandler = null;
    }
};

/**
 * 发送消息核心逻辑
 */
const sendMessage = (input) => {
    const isString = typeof input === 'string';
    const message = isString ? input : formData.value.content;

    // 输入验证
    if (!message) {
        ElMessage({
            type: 'warning',
            message: '请输入内容',
            duration: 2000
        });
        return;
    }

    // 处理显示消息（去除系统提示）
    const displayMessage = isString && message.includes('\n\n')
        ? message.split('\n\n').pop()
        : message;

    // 添加用户消息和AI加载状态
    messageList.value.push({
        type: 0,
        content: displayMessage
    });

    messageList.value.push({
        type: 1,
        content: [],
        loading: true
    });

    loading.value = true;
    isClosing = false;

    // 建立EventSource连接
    eventSource = new EventSource(`${props.apiUrl}/chat/stream?message=${encodeURIComponent(message)}`);
    formData.value.content = '';

    // 处理流式响应
    eventSource.onmessage = (event) => {
        if (isClosing) return;

        let response = event.data;

        if (response === 'end') {
            isClosing = true;
            closeEventSource();
            return;
        }

        try {
            response = JSON.parse(response).content;
            messageList.value[messageList.value.length - 1].content.push(response);
        } catch (error) {
            console.error("JSON 解析失败：", error);
        }

        // 滚动到底部
        nextTick(() => {
            const content = document.getElementById('message-panel');
            content.scrollTop = content.scrollHeight;
        });
    };

    eventSource.onerror = () => {
        if (isClosing) return;
        closeEventSource();
    };
};

/**
 * 设置快捷提问
 */
const setPrompt = (key) => {
    const userPrompt = promptMap[key] || key;
    formData.value.content = userPrompt; // 只展示用户问题
    sendMessage(userPrompt);             // 将纯用户问题作为参数传入
};

/**
 * 关闭EventSource连接
 */
const closeEventSource = () => {
    if (eventSource) {
        console.log('Closing EventSource');
        eventSource.close();
        eventSource = null;
    }

    // 清理加载状态
    if (messageList.value.length > 0) {
        messageList.value[messageList.value.length - 1].loading = false;
    }

    loading.value = false;
};

// 组件卸载时清理资源
onUnmounted(() => {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }
});
</script>


<style scoped>
/* 主容器样式 */
.chat-panel {
    background: #f5f5f5;
    height: 100%;
    display: flex;
    flex-direction: column;
}

/* 标题栏样式 */
.chat-title {
    font-size: 24px;
    font-weight: bold;
    padding: 12px;
    background-color: #2d65f7;
    color: #fff;
    display: flex;
    justify-content: space-between;
}

.close-btn {
    background: none;
    border: none;
    color: #fff;
    font-size: 20px;
    cursor: pointer;
}

/* 消息面板样式 */
.message-panel {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
}

.send-panel {
    padding: 12px;
    background-color: #fff;
}

/* 将消息框设置为中央 70% 宽度 */
.message-list {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    /* 改为顶部对齐，防止居中 */
    width: 70%;
    margin: 0 auto;
    padding: 12px;
    overflow-x: hidden;
}

/* 确保 Markdown 渲染区域继承背景 */
.ai-item .md-editor {
    background-color: #e0e0e0 !important;
}

.ai-item .md-editor .md-editor-preview {
    background-color: #e0e0e0 !important;
}

/* Markdown 主题设置 */
.ai-item .md-editor .vuepress-theme {
    --md-theme-color: #333;
    --md-theme-code-inline-bg-color: #dcdcdc;
    --md-theme-code-block-bg-color: #dcdcdc;
    --md-theme-quote-bg-color: #e0e0e0;
}

/* 去掉顶部和底部间距 */
.ai-item :deep(.md-editor-preview p),
.ai-item :deep(.md-editor-preview div),
.ai-item :deep(.md-editor-preview pre),
.ai-item :deep(.md-editor-preview blockquote) {
    margin-top: 0 !important;
    margin-bottom: 0 !important;
}

/* AI 消息（左对齐） */
.ai-item {
    align-self: flex-start;
    display: flex;
    justify-content: flex-start;
}

/* 用户消息（右对齐） */
.user-item {
    display: flex;
    justify-content: flex-end;
    /* 保持右对齐 */
    align-items: flex-start;
    /* 保证头像和内容顶部对齐 */
    width: 100%;
    /* 防止内容超出 */
}

/* 消息项通用样式 */
.message-item {
    display: flex;
    align-items: flex-start;
    /* 头像和文本顶部对齐 */
    margin-bottom: 16px;
    max-width: 100%;
    box-sizing: border-box;
}

/* 将头像宽度固定，防止被文本推开 */
.user-icon,
.ai-icon {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
    margin-right: 12px;
    flex-shrink: 0;
    /* 防止被文本挤压 */
    align-self: flex-start;
    /* 保证头像靠顶部对齐 */
}

/* 内容框动态宽度，最小 50px，最大 75% 宽度 */
.message-content {
    background: #e0e0e0;
    padding: 12px;
    border-radius: 12px;
    font-size: 16px;
    color: #333;
    min-width: 15px;
    /* 保持一个最小宽度 */
    max-width: 75%;
    /* 限制最大宽度，防止过长 */
    width: auto;
    /* 自动根据内容调整宽度 */
    word-break: break-word;
    flex-grow: 0;
    /* 允许自然伸缩 */
    flex-shrink: 1;
    /* 防止扩展导致头像被挤开 */
}

/* 用户消息框颜色 */
.user-item .message-content {
    background: #2d65f7;
    color: #ffffff;
}


/* 让输入框和按钮放大并对齐 */
.form-container {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.input-wrapper {
    flex: 1;
}

.send-btn-wrapper {
    display: flex;
    margin-left: 1050px;
}

::v-deep(.el-textarea__inner) {
    font-size: 20px;
    /* 增大字体 */
    height: 140px;
    /* 增大高度 */
    padding: 20px;
    /* 增大内边距 */
    border-radius: 20px;
    background-color: #f9f9f9;
    /* 浅灰色背景，柔和感 */
    border: 2px solid transparent;
    /* 透明边框，防止布局抖动 */
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
    /* 添加阴影，提升层次感 */
    transition: all 0.3s ease;
    /* 平滑过渡效果 */
    resize: none;
    /* 关闭调整大小功能，去掉右下角的黑色斜线 */
}

/* 输入框聚焦状态 */
::v-deep(.el-textarea__inner:focus) {
    background-color: #ffffff;
    /* 聚焦时改为白色 */
    border: 2px solid #2d65f7;
    /* 聚焦时增加蓝色边框 */
    box-shadow: 0 4px 12px rgba(45, 101, 247, 0.3);
    /* 聚焦时增加更明显的阴影 */
}

/* 禁用状态 */
::v-deep(.el-textarea__inner:disabled) {
    background-color: #f0f0f0;
    border-color: #dcdcdc;
    color: #999;
    box-shadow: none;
    cursor: not-allowed;
}

.el-button {
    font-size: 20px;
    padding: 18px 24px;
    border-radius: 15px;
    background-color: #2d65f7;
    color: #ffffff;
    transition: all 0.3s ease;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.493);
    /* 添加按钮阴影 */
    border: 2px solid transparent;
    transform: translateX(-100px);
    /* 整体右移 100px */
    height: 60px;
}

/* 按钮悬停时增强阴影效果 */
.el-button:hover {
    background-color: #1b4dcf;
    box-shadow: 0 4px 12px rgba(45, 101, 247, 0.3);
    /* 增强悬停时阴影效果 */
}

/* 按钮点击时增加按压效果 */
.el-button:active {
    /* transform: translateY(4px); */
    /* 模拟按压感 */
    box-shadow: 0 2px 6px rgba(45, 101, 247, 0.2);
}

/* 快捷提问面板样式 */
.prompt-panel {
    padding: 12px 24px;
    background-color: #ffffff;
    border-bottom: 1px solid #e0e0e0;
    font-size: 15px;
}

.prompt-label {
    font-weight: bold;
    color: #2d65f7;
    margin-right: 8px;
}

.prompt-link {
    color: #1f2d3d;
    cursor: pointer;
    margin-right: 16px;
    transition: all 0.2s ease;
    font-weight: 500;
}

.prompt-link:hover {
    color: #2d65f7;
    text-decoration: underline;
}
</style>