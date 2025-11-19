<!-- 
AI碳助手介绍页面组件 - 展示AI碳助手的功能和价值

功能概述：
• 展示AI碳助手的核心功能和应用场景
• 提供模块简介、功能列表和价值说明

设计特点：
• 科技感渐变背景和发光边框
• 卡片式布局突出内容层次
• 响应式文字颜色和间距

数据流管理：
• 页面加载时重置滚动条状态

组件依赖：
• 无外部组件依赖，纯展示页面
-->
<template>
  <div class="ai-assistant-container">
    <!-- 页面标题区域 -->
    <h1 class="title">🤖 AI 碳助手</h1>
    <p class="subtitle">智能分析 · 解释预测 · 服务决策</p>

    <!-- 主要内容卡片 -->
    <div class="description-card">
      <!-- 模块简介部分 -->
      <h2>🌿 模块简介</h2>
      <p>
        AI 碳助手是基于私域知识模型构建的智能解释引擎，专注于服务碳储预测模块与碳汇建模系统。
        它通过语义识别、知识关联和结果解释功能，为用户提供实时、可信赖的碳汇知识支持。
      </p>

      <!-- 核心功能列表 -->
      <h2>🔍 核心功能</h2>
      <ul>
        <li>🧠 智能解释预测结果</li>
        <li>📖 提供碳储科学背景知识</li>
        <li>📊 协助理解变量贡献度（SHAP）</li>
        <li>📚 支持与文献对照与验证</li>
      </ul>

      <!-- 应用价值说明 -->
      <h2>💡 应用价值</h2>
      <p>
        在复杂的碳储建模和预测系统中，模型结果往往难以理解。AI 碳助手通过自然语言对话与图文解释，
        降低了理解门槛，提升了模型透明度，有助于政策制定、科研交流与公众教育。
      </p>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';

/**
 * 组件挂载生命周期
 * 重置页面滚动条状态，确保页面可以正常滚动
 */
onMounted(() => {
  document.body.style.overflow = 'auto';
  document.documentElement.style.overflow = 'auto';
});
</script>

<style scoped>
/* 主容器样式 - 科技感渐变背景 */
.ai-assistant-container {
  background: linear-gradient(to right, #1a2a3a, #1d3a4a);
  color: #fff;
  min-height: 100vh;
  padding: 60px 40px;
}

/* 标题样式 */
.title {
  font-size: 36px;
  color: #00ffff;
  /* 科技蓝绿色 */
  text-align: center;
  margin-bottom: 10px;
}

/* 副标题样式 */
.subtitle {
  font-size: 18px;
  text-align: center;
  color: #ccc;
  margin-bottom: 40px;
}

/* 内容卡片样式 - 毛玻璃效果 */
.description-card {
  max-width: 800px;
  margin: 0 auto;
  background-color: rgba(255, 255, 255, 0.05);
  /* 半透明白色背景 */
  border: 1px solid #00ffff55;
  /* 半透明蓝绿色边框 */
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.2);
  /* 发光效果 */
}

/* 卡片内标题样式 */
.description-card h2 {
  color: #00ffff;
  margin-top: 20px;
  /* 每个部分的上边距 */
}

/* 段落文本样式 */
.description-card p {
  line-height: 1.8;
  /* 行高增强可读性 */
  font-size: 16px;
  color: #eee;
}

/* 列表容器样式 */
.description-card ul {
  padding-left: 20px;
  margin-top: 10px;
}

/* 列表项样式 */
.description-card li {
  line-height: 1.8;
  color: #ddd;
}
</style>