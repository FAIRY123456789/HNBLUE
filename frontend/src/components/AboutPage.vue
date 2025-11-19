<!-- 
关于页面组件 - 海南蓝碳项目信息展示页面

功能概述：
• 展示项目三大核心内容：蓝碳平台、红树林、生态环境厅
• 集成AI助手聊天功能
• 动态导航栏（根据用户类型显示不同入口）

设计特点：
• 视差滚动效果增强视觉体验
• 毛玻璃模糊背景提升文字可读性  
• 响应式动画交互（导航栏入场、模态框淡入淡出）

数据流管理：
• 通过localStorage获取用户类型控制导航显示
• 管理AI聊天窗口的显示/隐藏状态
• 计算属性动态生成用户中心路由

组件依赖：
• Chat.vue - AI聊天组件
• Vue Router - 路由导航
-->
<template>
  <div class="about-page">
    <!-- 导航栏区域 -->
    <header>
      <nav>
        <router-link class="nav-items" to="/">首页</router-link>
        <router-link class="nav-items active" to="/about">关于</router-link>
        <router-link class="nav-items" to="/visual">地图</router-link>
        <router-link class="nav-items" to="/carbonseek">碳溯</router-link>
        <router-link class="nav-items" :to="userCenterPath">{{ userCenterLabel }}</router-link>
      </nav>
    </header>

    <!-- 主要内容区域 -->
    <div class="about-3page">
      <!-- 第一屏：海南蓝碳数字化平台 -->
      <div class="parallax" id="blue-carbon" :style="{ backgroundImage: `url(${require('@/assets/hainan2.jpg')})` }">
        <div class="section-content">
          <div class="blurry-background"></div> <!-- 添加模糊背景层 -->
          <div class="content"> <!-- 文本内容 -->
            <h1>海南蓝碳数字化平台</h1>
            <p style="margin-left: 10%;">
              海南蓝碳数字化平台旨在通过科技手段监控和保护海南省的蓝碳生态系统。该平台集成了数据收集、分析和可视化技术，支持对海南省沿海区域的红树林、海草床和盐沼等蓝碳生态系统的实时监测。通过这些数据，平台帮助研究人员和政策制定者更好地理解和管理这些生态系统，以及它们在碳循环中的作用。
            </p>
          </div>
        </div>
      </div>
      <!-- 第二屏：海南红树林 -->
      <div class="parallax" id="mangrove" :style="{ backgroundImage: `url(${require('@/assets/hainan1.jpg')})` }">
        <div class="section-content">
          <div class="blurry-background"></div> <!-- 添加模糊背景层 -->
          <div class="content"> <!-- 文本内容 -->
            <h1>海南红树林</h1>
            <p>
              海南红树林是中国最重要的红树林之一，分布在海南省的沿海地区。这些红树林不仅提供了丰富的生物多样性，还在防止海岸侵蚀、维护海洋生物栖息地和固碳等方面发挥着重要作用。红树林是海南蓝碳生态系统的重要组成部分，对全球气候变化的缓解提供了不可忽视的贡献。
            </p>
          </div>
        </div>
      </div>
      <!-- 第三屏：海南省生态环境厅 -->
      <div class="parallax" id="environment" :style="{ backgroundImage: `url(${require('@/assets/hainan3.jpg')})` }">
        <div class="section-content">
          <div class="blurry-background"></div> <!-- 添加模糊背景层 -->
          <div class="content"> <!-- 文本内容 -->
            <h1 style="margin-right: 8.8%;">海南省生态环境厅</h1>
            <p style="margin-left: 11%;">
              海南省生态环境厅负责制定和执行海南省的环保政策和法规。该厅着力于推动海南省环境质量的改善，协调和解决生态环境保护中的关键问题。厅内设有多个部门，专注于水质、空气质量、土壤污染和生态修复等领域，致力于建设生态文明，保障海南省的可持续发展。
            </p>
          </div>
        </div>
      </div>

      <!-- AI助手按钮 -->
      <div class="ai-button" @click="toggleChat">
        🤖
      </div>

      <!-- 聊天模态框和遮罩层 -->
      <div v-if="isChatVisible" ref="backdrop" class="backdrop" @click="closeChat"></div>

      <div v-show="isChatVisible" ref="chatModal" class="chat-modal">
        <!-- 在 AboutPage.vue 里传递 props 来控制 Chat.vue 的显示状态 -->
        <Chat :apiUrl="'http://localhost:8088/api'" :visible="isChatVisible" @close="closeChat" />
      </div>
    </div>
    <!-- 这里可以继续添加其他的视差滚动区块 -->
  </div>
</template>

<script>
import { ref, onMounted, nextTick, computed } from 'vue';
import Chat from '../components/Chat.vue';

export default {
  name: 'AboutPage',
  components: {
    Chat, // ✅ 注册组件
  },
  setup() {
    const isChatVisible = ref(false);
    const chatModal = ref(null);
    const backdrop = ref(null);

    // ✅ 用户类型判断
    const userType = ref('User');

    const userCenterLabel = computed(() =>
      userType.value === 'Admin' ? '用户管理' : '个人中心'
    );

    const userCenterPath = computed(() =>
      userType.value === 'Admin' ? '/usermanage' : '/userinfo'
    );

    const toggleChat = async () => {
      const token = localStorage.getItem('token');
      if (!token) {
        alert('请先登录后使用 AI 助手功能。');
        return;
      }
      if (isChatVisible.value) {
        closeChat();
      } else {
        isChatVisible.value = true;
        await nextTick();
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
      if (chatModal.value) {
        chatModal.value.classList.add('fade-out');
        chatModal.value.classList.remove('fade-in');
      }
      if (backdrop.value) {
        backdrop.value.classList.add('backdrop-fade-out');
        backdrop.value.classList.remove('backdrop-fade-in');
      }
      setTimeout(() => {
        isChatVisible.value = false;
      }, 300);
    };

    onMounted(() => {
      document.body.style.overflow = 'auto';
      document.documentElement.style.overflow = 'auto';
      userType.value = localStorage.getItem('userType') || 'User';
    });

    return {
      isChatVisible,
      toggleChat,
      closeChat,
      chatModal,
      backdrop,
      userCenterLabel,
      userCenterPath,
    };
  }
};
</script>


<style scoped>
.about-3page {
  background-color: rgba(11, 41, 24, 0.836);
  /* 自己的背景色 */
}

/* 设置整体样式，局限在 AboutPage */
.about-page {
  margin: 0;
  padding: 0;
  height: auto;
  overflow-y: scroll;
}

header {
  position: absolute;
  top: 0;
  /* 取消顶部间距 */
  z-index: 10000;
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

nav a {
  height: 30px;
  font-size: 20px;
  font-weight: 500;
  letter-spacing: 2px;
  color: rgb(53, 53, 53);
  width: 110px;
  text-align: center;
  padding: 15px 0;
  border-bottom-left-radius: 20px;
  border-bottom-right-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.active {
  background-color: #16423c;
  color: white;
}

nav a:hover {
  background-color: #16423c;
  color: white;
}

nav:hover>a:not(:hover) {
  background-color: transparent;
  color: rgb(53, 53, 53);
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

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-50px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

#blue-carbon .section-content {
  animation: fadeInDown 1s ease-out forwards;
}


.parallax {
  background-attachment: fixed;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  height: auto;
  width: 95%;
  /* 调整宽度为屏幕的80% */
  margin: auto;
  /* 水平居中显示 */
  position: relative;
  overflow: hidden;
  /* 防止背景图像溢出 */
  border-top: 5px solid rgba(11, 41, 24, 0.836);
  /* 添加顶部黑色边框 */
  border-bottom: 5px solid rgba(11, 41, 24, 0.836);
  /* 添加底部黑色边框 */
  box-shadow: 0px 0px 50px 20px rgba(11, 41, 24, 0.836);
  /* 在两侧添加阴影以增加黑色边框效果 */
}

.parallax::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  width: 55%;
  /* 覆盖左侧55% */
  height: auto;
  background-size: cover;
  background-repeat: no-repeat;
}

.section-content {
  position: relative;
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
  padding-right: 5%;
  padding-left: 60%;
}

.blurry-background {
  position: absolute;
  right: 0;
  top: 0;
  width: 43%;
  height: 100%;
  backdrop-filter: blur(5px);
  background-color: rgba(0, 0, 0, 0.3);
  z-index: 1;
}

.content {
  z-index: 2;
  color: white;
  text-align: right;
  text-shadow: 0 0 10px rgba(0, 0, 0, 0.5);
}

#mangrove .section-content {
  align-items: flex-start;
  /* 调整对齐到左侧 */
  padding-left: 6%;
  /* 左侧内边距调整 */
  padding-right: 62%;
  /* 右侧内边距，以留出空间 */
}

#mangrove .blurry-background {
  right: auto;
  /* 取消右侧定位 */
  left: 0;
  /* 定位到左侧 */
  width: 43%;
  /* 保持宽度不变 */
}

#mangrove .content {
  text-align: left;
  /* 文本对齐调整到左侧 */
}


h1 {
  font-size: 50px;
  /* 标题字体大小加倍 */
  margin-bottom: 50px;
  /* 添加标题和段落之间的间距 */
}

p {
  font-size: 20px;
  /* 内容字体大小加倍 */
  line-height: 1.6;
  /* 行高，增强可读性 */
}

/* AI 助手按钮 */
.ai-button {
  position: fixed;
  right: 20px;
  bottom: 20px;
  width: 60px;
  height: 60px;
  background-color: #2d65f7;
  color: white;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 30px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition: background-color 0.3s ease;
  z-index: 9999;
}

.ai-button:hover {
  background-color: #1b4dcf;
}

/* ✅ 遮罩层（半透明黑色背景） */
.backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  /* 半透明黑色 */
  z-index: 9997;
  opacity: 0;
  pointer-events: none;
}

/* ✅ 遮罩层进入动画 */
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

/* ✅ 遮罩层退出动画 */
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

/* ✅ 模态框设置为居中，并调整为横向长方形 */
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

/* ✅ 模态框标题栏 */
.chat-header {
  padding: 15px;
  background-color: #2d65f7;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
}

.chat-header .close-btn {
  background-color: transparent;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
}

.chat-header .close-btn:hover {
  color: #ff4d4f;
}

/* ✅ 模态框内容区域 */
.chat-content {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
  font-size: 16px;
  color: #333;
  line-height: 1.6;
}

/* ✅ 模态框进入动画 */
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

/* ✅ 模态框退出动画 */
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