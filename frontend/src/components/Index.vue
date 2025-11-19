<!-- 
通用聊天面板组件 - 支持实时对话和Markdown渲染

功能概述：
• 实时对话交互：支持用户与AI进行问答交流
• Markdown渲染：AI回答内容支持Markdown格式显示
• 流式响应：使用EventSource实现实时流式回答
• 自动滚动：新消息自动滚动到底部

技术特点：
• 使用EventSource处理服务器推送
• 集成md-editor-v3渲染Markdown内容
• 响应式设计和消息布局优化
• Vuex状态管理集成（已注释）
-->
<template>
  <div class="chat-panel">
    <!-- 聊天标题 -->
    <div class="chat-title">HNBLUEChat</div>

    <!-- 消息显示区域 -->
    <div class="message-panel" id="message-panel">
      <div class="message-list">
        <!-- 消息列表渲染 - 区分用户和AI消息 -->
        <div :class="['message-item', item.type == 1 ? 'ai-item' : '']" v-for="(item, index) in messageList"
          :id="'item' + index">

          <!-- 用户消息布局 -->
          <template v-if="item.type == 0">
            <div class="message-content">
              <div class="content-inner">{{ item.content }}</div>
            </div>
            <div class="user-icon">我</div>
          </template>

          <!-- AI消息布局 - 包含Markdown渲染和加载状态 -->
          <template v-else>
            <div class="user-icon">AI</div>
            <div class="message-content ai-item">
              <!-- Markdown内容渲染 -->
              <MdPreview previewTheme="vuepress" :codeFoldable="false" editorId="preview"
                :modelValue="item.content.join('')" />
              <!-- 加载指示器 -->
              <div class="loading" v-if="item.loading">
                <img src="../assets/loading.gif" />
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- 消息发送区域 -->
    <div class="send-panel">
      <el-form :model="formData" ref="formDataRef" @submit.prevent>
        <!-- 消息输入框 -->
        <el-form-item label="" prop="content">
          <el-input type="textarea" :rows="3" clearable placeholder="请输入你想问的问题" v-model="formData.content"
            @keyup="keySend"></el-input>
        </el-form-item>
        <!-- 发送按钮 -->
        <el-form-item label="" prop="" class="send-btn">
          <el-button type="primary" @click="sendMessage" :disabled="loading">
            发送
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus';
import { ref, reactive, getCurrentInstance, nextTick, computed } from 'vue';
import { mapState, mapGetters, mapActions } from 'vuex';
import { MdPreview, MdCatalog } from 'md-editor-v3';
import 'md-editor-v3/lib/style.css';

export default {
  computed: {
    // 从 Vuex 绑定状态
    ...mapState(['count']),
    ...mapGetters(['doubleCount'])
  },
  methods: {
    // 绑定 Vuex 的 action
    ...mapActions(['increment']),
    /**
     * 键盘快捷键发送消息
     * 支持Ctrl+Enter快速发送
     */
    keySend(event) {
      if (!(event.ctrlKey && event.key === 'Enter')) {
        return;
      }
      this.sendMessage();
    },
    /**
     * 发送消息方法
     * 1. 验证输入内容
     * 2. 添加用户消息到消息列表
     * 3. 使用EventSource与后端建立连接，接收AI流式回答
     * 4. 实时更新AI消息内容
     * 5. 处理连接关闭和错误情况
     */
    sendMessage() {
      const message = this.formData.content;
      // 输入验证
      if (!message) {
        ElMessage({
          type: 'warning',
          message: '请输入内容',
          duration: 2000,
        });
        return;
      }
      // 添加用户消息到列表
      this.messageList.push({
        type: 0,
        content: message,
      });
      // 添加AI回复占位符
      this.messageList.push({
        type: 1,
        content: [], // 直接定义为空字符串
        loading: true,
      });
      this.loading = true;
      // 建立EventSource连接
      const eventSource = new EventSource(`http://localhost:8088/api/chat/stream?message=${message}`);
      this.formData.content = '';
      /**
       * 处理服务器推送的消息
       */
      eventSource.onmessage = (event) => {
        let response = event.data;
        // 流式响应结束标记
        if (response === 'end') {
          this.close(eventSource);
          return;
        }
        try {
          response = JSON.parse(response).content;
          if (response) {
            // 需要保证 content 是数组格式
            if (!Array.isArray(this.messageList[this.messageList.length - 1].content)) {
              this.messageList[this.messageList.length - 1].content = [];
            }
            this.messageList[this.messageList.length - 1].content.push(response);
          }
        } catch (error) {
          console.error("JSON 解析失败：", error);
        }
        // 滚动到底部
        nextTick(() => {
          const content = document.getElementById('message-panel');
          content.scrollTop = content.scrollHeight;
        });
      };
      /**
       * 处理连接错误
       */
      eventSource.onerror = (error) => {
        this.close(eventSource);
      };
    },
    /**
     * 关闭EventSource连接并清理状态
     */
    close(eventSource) {
      eventSource.close();
      this.messageList[this.messageList.length - 1].loading = false;
      this.loading = false;
    },
  },
  setup() {
    // 响应式数据定义
    const formData = ref({});
    const messageList = ref([]);
    const loading = ref(false);
    // 获取当前组件实例
    const { proxy } = getCurrentInstance() || {};
    /**
     * 格式化消息内容
     * 将数组内容合并为字符串用于显示
     */
    const formattedContent = (item) => {
      if (!item || !Array.isArray(item.content)) {
        return '';
      }
      return item.content.join('') || '';
    };
    return {
      formData,
      messageList,
      loading,
      formattedContent
    };
  },
};
</script>

<style lang="scss" scoped>
@import 'element-plus/dist/index.css';

/* 主聊天面板样式 */
.chat-panel {
  background: #eff0f6;
  height: calc(100vh);

  .chat-title {
    text-align: center;
    font-size: 100px;
    font-weight: bold;
  }

  /* 消息面板样式 */
  .message-panel {
    position: relative;
    height: calc(100vh - 500px);
    overflow: auto;
    padding-bottom: 10px;

    .message-list {
      margin: 0px auto;
      width: 450px;

      /* 消息项通用样式 */
      .message-item {
        margin: 10px 0px;
        display: flex;
        width: 100%; // 让聊天气泡占满容器
        max-width: 100%; // 保持在容器内

        /* 用户头像样式 */
        .user-icon {
          width: 40px;
          height: 40px;
          line-height: 40px;
          border-radius: 20px;
          background: #535353;
          color: #fff;
          text-align: center;
          margin-left: 10px;
        }

        /* 消息内容容器 */
        .message-content {
          flex: 1;
          margin-left: 10px;
          align-items: center;
          display: flex;
          justify-content: flex-end;
          max-width: 75%; // 最大宽度，避免过长
          min-width: 30%; // 最小宽度，保持美观
        }

        /* 用户消息气泡样式 */
        .content-inner {
          background: #2d65f7;
          border-radius: 5px;
          padding: 10px;
          color: #fff;
        }
      }

      /* AI消息特殊样式 */
      .ai-item {
        line-height: 23px;

        .message-content {
          display: block;
          background: #fff;
          border-radius: 5px;
        }

        .user-icon {
          background: #64018f;
          margin-left: 0px;
        }

        /* Markdown预览样式覆盖 */
        :deep(.md-editor-preview :last-child) {
          margin-bottom: 0 !important; // 强制去掉底部间距
          margin: 0 !important; // 强制覆盖
        }

        :deep(.md-editor-previewOnly) {
          border-radius: 5px;
          background: #fff;
        }

        :deep(.md-editor-preview-wrapper) {
          padding: 10px;
        }

        /* 加载动画样式 */
        .loading {
          text-align: center;
        }
      }
    }
  }

  /* 发送面板样式 */
  .send-panel {
    position: relative;
    margin: 5px auto 0px;
    width: 800px;
    background: #fff;
    border-radius: 30px;
    padding: 20px;

    .send-btn {
      text-align: right;
      margin-bottom: 0px;
      padding: 5px;

      :deep(.el-form-item__content) {
        justify-content: flex-end;
        height: 50px;
      }
    }

    /* 发送按钮样式 */
    :deep(.el-button) {
      font-size: 20px; // 增大按钮字体
      padding: 12px 24px; // 增大按钮尺寸
      border-radius: 18px; // 圆角更明显
      height: 50px;
    }
  }

  /* 输入框样式优化 */
  :deep(.el-textarea__inner) {
    font-size: 20px; // 增大输入框字体
    height: 150px; // 增大输入框高度
    padding: 10px; // 增加内边距
    border: 1px solid #ccc;
    resize: none !important;
    box-shadow: none;
    border-radius: 20px;
  }
}

/* 空状态提示 */
.no-data {
  text-align: center;
  color: #5f5f5f;
}

/* Markdown编辑器样式引入 */
@import '@kangc/v-md-editor/lib/style/base-editor.css';
@import '@kangc/v-md-editor/lib/theme/style/vuepress.css';

/* Markdown预览样式定制 */
:deep(.v-md-editor-preview) {
  border-radius: 5px;
  background: #fff;
}

:deep(.v-md-editor-preview-wrapper) {
  padding: 10px;
  width: 400px;
}
</style>