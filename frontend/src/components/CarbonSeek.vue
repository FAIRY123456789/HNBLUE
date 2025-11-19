<!-- 
碳溯主页面组件 - 智能碳汇决策系统入口

功能概述：
• 展示8大核心功能模块入口卡片
• 集成AI助手悬浮按钮和聊天对话框
• 动态导航栏根据用户类型显示不同入口

模块分类：
• 核心功能：结构参数预测器、虚拟样地实验设计器、碳储-价值转换器
• 展示功能：SHAP可视化、响应曲线图、文献比对
• 探索功能：AI碳助手、参数扰动敏感性分析

设计特点：
• 科技感渐变背景和网格卡片布局
• 动画效果增强用户体验
• 响应式设计适配不同屏幕

数据流管理：
• 通过localStorage获取用户类型
• 控制AI聊天窗口的显示状态和动画
-->
<template>
  <!-- 导航栏 -->
  <header>
    <nav>
      <router-link class="nav-items" to="/">首页</router-link>
      <router-link class="nav-items" to="/about">关于</router-link>
      <router-link class="nav-items" to="/visual">地图</router-link>
      <router-link class="nav-items active" to="/carbonseek">碳溯</router-link>
      <!-- 动态用户中心入口 -->
      <router-link class="nav-items" :to="userCenterPath">{{ userCenterLabel }}</router-link>
    </nav>
  </header>

  <div class="carbon-trace-container">

    <!-- 标题区域 -->
    <div class="intro-section animate__animated animate__fadeInDown">
      <h1 class="main-title">碳溯 · 智能碳汇决策中枢</h1>
      <p class="sub-title">Trace Carbon · Predict Structure · Empower Decisions</p>
    </div>

    <!-- 功能卡片网格布局 -->
    <div class="card-grid">

      <!-- 核心功能模块 -->
      <router-link to="/structure-predictor">
        <FeatureCard icon="🧮" title="结构参数预测器" desc="输入结构特征，预测生物量" tag="核心功能" />
      </router-link>

      <router-link to="/virtual-plot-designer">
        <FeatureCard icon="🧪" title="虚拟样地实验设计器" desc="自定义结构变量组，模拟干预后生物量变化" tag="核心功能" />
      </router-link>

      <router-link to="/carbon-value-converter">
        <FeatureCard icon="💰" title="碳储-价值转换器" desc="从 m.so → 碳储 → 金额，支持碳价灵活调整" tag="核心功能" />
      </router-link>

      <!-- 展示功能模块 -->
      <router-link to="/shap-visualizer">
        <FeatureCard icon="📊" title="SHAP 可视化解释" desc="展示预测结构变量的贡献度，强化解释性" tag="展示功能" />
      </router-link>

      <router-link to="/response-curve">
        <FeatureCard icon="📈" title="响应曲线图" desc="变量变化 vs 生物量趋势曲线，洞察生态机制" tag="展示功能" />
      </router-link>

      <router-link to="/literature-compare">
        <FeatureCard icon="📚" title="文献比对模块" desc="将结果与文献区间对照，验证合理性" tag="展示功能" />
      </router-link>

      <!-- 探索功能模块 -->
      <router-link to="/ai-assistant">
        <FeatureCard icon="🤖" title="AI 碳助手" desc="基于私域知识模型，智能解释预测结果" tag="探索功能" />
      </router-link>

      <router-link to="/param-sensitivity">
        <FeatureCard icon="🌀" title="参数扰动敏感性分析" desc="±20% 参数扰动模拟，自动展示预测波动区间" tag="探索功能" />
      </router-link>
    </div>

    <!-- AI助手功能区域 -->
    <div class="ai-button" @click="toggleChat">
      🤖
    </div>

    <!-- ✅ 遮罩层 -->
    <div v-if="isChatVisible" ref="backdrop" class="backdrop" @click="closeChat"></div>

    <!-- ✅ Chat对话框 -->
    <div v-show="isChatVisible" ref="chatModal" class="chat-modal">
      <CarbonChat :apiUrl="'http://localhost:8088/api/chat'" :visible="isChatVisible" @close="closeChat" />
    </div>

  </div>
</template>

<script setup>
import 'animate.css';
import FeatureCard from '@/components/FeatureCard.vue'; // 路径根据实际项目结构调整
import { ref, onMounted, nextTick, computed } from 'vue';
import CarbonChat from '@/components/CarbonChat.vue';

// 响应式状态
const isChatVisible = ref(false);
const chatModal = ref(null);
const backdrop = ref(null);
const userType = ref('User');

// 计算属性：动态用户信息
const userCenterLabel = computed(() =>
  userType.value === 'Admin' ? '用户管理' : '个人中心'
)
// 计算属性：动态用户路径
const userCenterPath = computed(() =>
  userType.value === 'Admin' ? '/usermanage' : '/userinfo'
)

// AI聊天窗口控制
const toggleChat = async () => {
  if (isChatVisible.value) {
    closeChat();
  } else {
    isChatVisible.value = true;
    await nextTick();
    // 应用动画类名
    if (chatModal.value) {
      chatModal.value.classList.add('fade-in');
      chatModal.value.classList.remove('fade-out');
    }
    if (backdrop.value) {
      backdrop.value.classList.add('backdrop-fade-in');
      backdrop.value.classList.remove('backdrop-fade-out');
    }
  }
};

const closeChat = () => {
  // 移除动画类名
  if (chatModal.value) {
    chatModal.value.classList.add('fade-out');
    chatModal.value.classList.remove('fade-in');
  }
  if (backdrop.value) {
    backdrop.value.classList.add('backdrop-fade-out');
    backdrop.value.classList.remove('backdrop-fade-in');
  }
  // 延迟隐藏确保动画完成
  setTimeout(() => {
    isChatVisible.value = false;
  }, 300);
};

// 组件挂载时初始化
onMounted(() => {
  // 进入 AboutPage 时，允许垂直滚动
  document.body.style.overflow = 'auto';
  document.documentElement.style.overflow = 'auto';
  userType.value = localStorage.getItem('userType') || 'User'
});

</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Orbitron:wght@600&display=swap');

/* 主容器样式 */
.carbon-trace-container {
  padding: 60px 40px;
  background: linear-gradient(to bottom right, #0f2027, #203a43, #2c5364);
  min-height: 100vh;
  color: white;
}

/* 标题区域样式 */
.intro-section {
  text-align: center;
  margin-bottom: 50px;
}

.main-title {
  font-size: 36px;
  color: #00ffff;
  margin-bottom: 10px;
  text-shadow: 0 0 10px #00ffff;
}

.sub-title {
  font-size: 18px;
  color: #ccc;
  letter-spacing: 1px;
}

/* 卡片网格布局 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 30px;
}

/* 导航栏样式 */
header {
  position: absolute;
  top: 0;
  /* 取消顶部间距 */
  z-index: 1;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

nav {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 5%;
  background-color: rgba(255, 255, 255, 0.2);
  height: 35px;
  border-bottom-left-radius: 20px;
  border-bottom-right-radius: 20px;
  backdrop-filter: blur(5px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
}

/* 导航项样式 */
nav a {
  height: 30px;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0);
  width: 110px;
  text-align: center;
  padding: 15px 0;
  border-bottom-left-radius: 20px;
  border-bottom-right-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.active {
  background-color: #e1cf459d;
  color: white;
}

nav a:hover {
  background-color: #bcc128de;
  color: white;
}

nav:hover>a:not(:hover) {
  background-color: transparent;
  color: rgb(185, 171, 201);
}

@keyframes topIn {
  from {
    transform: translateY(-100%);
    opacity: 0;
  }

  to {
    transform: translateY(0);
    opacity: 1;
  }
}

nav {
  animation: topIn 1.2s ease-out forwards;
}

/* AI助手按钮样式（碳配色） */
.ai-button {
  position: fixed;
  right: 20px;
  bottom: 20px;
  width: 60px;
  height: 60px;
  background-color: #1a3e2a;
  color: #ffffff;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  cursor: pointer;
  transition: background-color 0.3s ease;
  z-index: 9999;
}

.ai-button:hover {
  background-color: #2e7049;
}

/* 模态框和遮罩层动画样式 */
.backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 9997;
  opacity: 0;
  pointer-events: none;
}

.chat-modal {
  position: fixed;
  top: 40%;
  left: 40%;
  transform: translate(-50%, -50%);
  width: 1200px;
  height: 800px;
  background-color: #ffffff;
  border: 1px solid #ccc;
  border-radius: 12px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
  z-index: 9998;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  opacity: 0;
  scale: 0.8;
}

/* 动画定义 */
.backdrop-fade-in {
  animation: fadeInBackdrop 0.3s ease forwards;
  pointer-events: auto;
}

@keyframes fadeInBackdrop {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

.backdrop-fade-out {
  animation: fadeOutBackdrop 0.3s ease forwards;
}

@keyframes fadeOutBackdrop {
  from {
    opacity: 1;
  }

  to {
    opacity: 0;
  }
}

.fade-in {
  animation: fadeInScale 0.3s ease forwards;
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.8);
  }

  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

.fade-out {
  animation: fadeOutScale 0.3s ease forwards;
}

@keyframes fadeOutScale {
  from {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }

  to {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.8);
  }
}
</style>