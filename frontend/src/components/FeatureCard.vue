<!-- 
功能卡片组件 - 用于展示系统功能模块的卡片式UI

功能概述：
• 展示功能模块的图标、标题、描述和标签
• 支持悬停动画效果和视觉反馈
• 提供科技感的毛玻璃视觉效果

设计特点：
• 毛玻璃背景和发光边框
• 悬停时的3D浮动效果
• 渐变色和阴影增强视觉层次
• 响应式动画交互
-->
<template>
  <div class="feature-card animate__animated animate__zoomIn">
    <!-- 图标区域 -->
    <div class="icon">{{ icon }}</div>
    <!-- 信息内容区域 -->
    <div class="info">
      <h3>{{ title }}</h3>
      <p>{{ desc }}</p>
      <span class="tag">{{ tag }}</span>
    </div>
  </div>
</template>

<script setup>
// 组件属性定义
defineProps({
  icon: String,  // 功能图标（emoji或文字图标）
  title: String,  // 功能标题
  desc: String,  // 功能描述
  tag: String  // 功能分类标签
});
</script>

<style scoped>
/* 卡片容器样式 - 毛玻璃效果 */
.feature-card {
  background: rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 25px;
  box-shadow: 0 8px 20px rgba(0, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  transition: transform 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.12);
}

/* 悬停效果 - 3D浮动和阴影增强 */
.feature-card:hover {
  transform: translateY(-8px) scale(1.03);
  box-shadow: 0 12px 30px rgba(0, 255, 255, 0.3);
}

/* 图标样式 - 发光效果 */
.icon {
  font-size: 36px;
  margin-bottom: 15px;
  text-shadow: 0 0 5px #00ffff;
}

/* 标题样式 - 科技蓝主题色 */
.info h3 {
  font-size: 20px;
  margin-bottom: 10px;
  color: #00ffff;
}

/* 描述文字样式 */
.info p {
  font-size: 14px;
  color: #f0f0f0;
}

/* 标签样式 - 高亮标识 */
.tag {
  display: inline-block;
  margin-top: 10px;
  background: #00ffff;
  color: #000;
  padding: 5px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
  box-shadow: 0 0 6px #00ffff;
}
</style>