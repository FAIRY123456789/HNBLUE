<!-- 
首页组件 - 海南蓝碳数字化应用系统入口页面

功能概述：
• 系统导航和用户身份动态显示
• 退出登录功能（含确认模态框）
• 系统介绍和入口引导
• 响应式设计和动画效果

主要特性：
• 动态导航：根据用户类型显示不同用户中心入口
• 安全退出：带确认提示的退出登录流程
• 视觉设计：背景图片、毛玻璃导航、动画过渡
• 路由控制：登录状态管理和页面跳转
-->
<template>
  <div class="home-page">
    <!-- 顶部导航栏 -->
    <header>
      <nav>
        <router-link class="nav-items active" to="/">首页</router-link>
        <router-link class="nav-items" to="/about">关于</router-link>
        <router-link class="nav-items" to="/visual">地图</router-link>
        <router-link class="nav-items" to="/carbonseek">碳溯</router-link>
        <!-- 动态用户中心入口 -->
        <router-link class="nav-items" :to="userCenterPath">{{ userCenterLabel }}</router-link>
      </nav>
      <!-- 退出登录按钮 -->
      <div class="logout-btn" @click="showLogoutConfirm = true">
        退出
      </div>
    </header>

    <!-- 退出确认模态框 - 使用teleport挂载到body -->
    <teleport to="body">
      <div v-if="showLogoutConfirm" class="logout-modal">
        <div class="logout-modal-content">
          <h4>确认退出登录？</h4>
          <div class="logout-actions">
            <button @click="confirmLogout">确认</button>
            <button @click="cancelLogout">取消</button>
          </div>
        </div>
      </div>
    </teleport>

    <!-- 主内容区域 -->
    <div class="content">
      <!-- 标题区域 -->
      <div class="title">
        <h3>海南蓝碳数字化应用系统</h3>
        <h1>海南</h1>
      </div>
      <!-- 系统介绍文本 -->
      <div class="info-wrap">
        <p>
          位于中国南部的海南蓝碳生态系统，在气候调节和生物多样性保护方面发挥着至关重要的作用。它包含了红树林、海草床和盐沼，这些都有效地捕获二氧化碳，保护海岸区域，并支持海洋生命。
        </p>
      </div>
      <!-- 行动召唤按钮 -->
      <div class="cta">
        <router-link to="/login">
          <button>
            探索更多➡
          </button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HomePage',
  data() {
    return {
      showLogoutConfirm: false,  // 控制退出确认模态框显示
      userType: 'User' // 用户类型，默认普通用户
    };
  },
  computed: {
    /**
     * 动态用户中心标签文字
     * 管理员显示"用户管理"，普通用户显示"个人中心"
     */
    userCenterLabel() {
      return this.userType === 'Admin' ? '用户管理' : '个人中心';
    },
    /**
     * 动态用户中心路由路径
     * 管理员跳转到用户管理页面，普通用户跳转到个人信息页面
     */
    userCenterPath() {
      return this.userType === 'Admin' ? '/usermanage' : '/userinfo';
    }
  },
  methods: {
    /**
     * 确认退出登录
     * 清除本地存储的token和用户类型，跳转到登录页
     */
    openLogoutModal() {
      this.showLogoutConfirm = true;
    },
    confirmLogout() {
      localStorage.removeItem('token');
      localStorage.removeItem('userType');
      this.showLogoutConfirm = false;
      this.$router.push({ name: 'Login' }); // 跳转登录页
    },
    /**
     * 取消退出操作
     * 关闭确认模态框
     */
    cancelLogout() {
      this.showLogoutConfirm = false;
    }
  },
  mounted() {
    /**
     * 组件挂载时设置页面滚动状态
     * 从本地存储读取用户类型信息
     */
    document.documentElement.style.overflow = 'hidden';
    document.body.style.overflow = 'hidden';
    this.userType = localStorage.getItem('userType') || 'User'; // 从本地读取身份
  },
  beforeUnmount() {
    /**
     * 组件销毁前恢复页面滚动状态
     * 避免影响其他页面
     */
    document.documentElement.style.overflow = '';
    document.body.style.overflow = '';
  }
};
</script>


<style scoped>
/* 导入字体和图标库 */
@import "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css";
@import url('https://fonts.googleapis.com/css2?family=Inter:ital,opsz,wght@0,14..32,100..900;1,14..32,100..900&display=swap');
/* 全局样式重置 */
:global(html),
:global(body) {
  margin: 0;
  padding: 0;
  height: 100%;
  overflow: hidden;  /* 禁止页面滚动 */
}
/* 主容器样式 - 背景图片设置 */
.home-page {
  background-image: url('~@/assets/bac4.png') !important;
  background-size: cover !important;
  background-position: top !important;
  background-repeat: no-repeat !important;
  overflow: hidden;
  min-height: 100vh;
}
/* 背景图片 */
.content img {
  position: absolute;
  bottom: 0%;
}
/* 头部导航栏样式 */
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
/* 导航菜单样式 - 毛玻璃效果 */
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
/* 导航项基础样式 */
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
/* 激活状态样式 */
.active {
  background-color: #16423c;
  color: white;
}
/* 导航项悬停效果 */
nav a:hover {
  background-color: #16423c;
  color: white;
}
nav:hover>a:not(:hover) {
  background-color: transparent;
  color: rgb(53, 53, 53);
}
/* 标题区域样式 */
.title {
  position: absolute;
  top: 35%;
  right: 50%;
  transform: translate(50%, -50%);

}
.title h3 {
  font-size: 30px;
  font-weight: 100;
  letter-spacing: 10px;
  color: rgb(4, 35, 25);
  text-align: center;
  text-shadow: 0 1px 5px rgba(0, 0, 0, 0.429);
}
.title h1 {
  font-size: 240px;
  font-weight: 580;
  letter-spacing: 25px;
  text-transform: uppercase;
  color: white;
  text-align: center;
  margin: -5px 0;
  margin-left: 10px;
  text-shadow: 0 1px 5px rgba(0, 0, 0, 0.429);
}
/* 介绍文本样式 */
p {
  font-size: 21px;
  letter-spacing: 1px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.7);
  width: 70%;
  text-align: center;
  text-shadow: 0 1px 5px rgba(0, 0, 0, 0.429);
}
.info-wrap {
  width: 100%;
  display: flex;
  justify-content: center;
  position: absolute;
  bottom: 18%;
  z-index: 1;
}
/* 行动召唤按钮区域 */
.cta {
  position: absolute;
  bottom: 8%;
  display: flex;
  justify-content: center;
  width: 100%;
  z-index: 1;
}
.cta button {
  font-size: 20px;
  font-weight: 40;
  letter-spacing: 3px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.8);
  background-color: transparent;
  border: 2px solid rgba(255, 255, 255, 0.8);
  border-radius: 50px;
  height: 45px;
  width: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
  cursor: pointer;
  animation: bottomInText 1.2s ease-out forwards;
  animation-delay: 0.3s;
  opacity: 0;
}
.cta button:hover {
  background-color: rgba(255, 255, 255, 0.8);
  color: rgb(53, 53, 53);
}
.slider {
  position: absolute;
  top: 50%;
  width: 100%;
  display: flex;
  justify-content: space-between;
}
.slider i {
  padding: 0 5%;
  font-size: 36px;
  color: rgba(255, 255, 255, 0.4);
}

/* 海南项目 */
.hainan-projects {
  display: flex;
  justify-content: center;
  gap: 20px;
}
.project-card {
  background-color: rgba(255, 255, 255, 0.2);
  padding: 15px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.project-card:hover {
  background-color: rgba(255, 255, 255, 0.4);
}
/* 鼠标悬停展示的图片 */
.project-image {
  margin-top: 30px;
}
.project-image img {
  max-width: 100%;
  height: auto;
  display: none;
  /* 初始隐藏 */
}
/* 登录注册 */
.modal {
  display: none;
  /* 默认隐藏 */
  position: fixed;
  /* 固定定位 */
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  /* 半透明黑色背景 */
  align-items: center;
  justify-content: center;
  z-index: 1000;
  /* 高层级以覆盖其他内容 */
}
.modal-content {
  background-color: white;
  padding: 20px;
  border-radius: 10px;
  width: 90%;
  max-width: 400px;
  /* 限制最大宽度 */
  text-align: center;
}
.close-button {
  cursor: pointer;
  position: absolute;
  top: 10px;
  right: 10px;
}
.logout-btn {
  position: absolute;
  right: 60px;
  top: 10px;
  background: transparent;
  /* 去掉背景 */
  color: black;
  /* 保留字体颜色 */
  font-size: 18px;
  border: none;
  /* 去掉边框 */
  cursor: pointer;
  transition: color 0.3s ease;
  z-index: 10000;
}
.logout-btn:hover {
  color: #0a3f38;
  text-decoration: underline;
}
.logout-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.logout-modal-content {
  background: #fff;
  padding: 30px;
  border-radius: 10px;
  width: 300px;
  text-align: center;
}
.logout-actions {
  margin-top: 20px;
  display: flex;
  justify-content: space-around;
}
.logout-actions button {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  background-color: #244b7d;
  color: #fff;
  cursor: pointer;
}
.logout-actions button:hover {
  background-color: #3b5fa6;
}

/* 动画定义 */
@keyframes bottomIn {
  from {
    transform: translateY(200px);
  }

  to {
    transform: translateY(0);
  }
}
.back-1 {
  animation: bottomIn 1s ease-out forwards;
}
.back-2 {
  animation: bottomIn 1.3s ease-out forwards;
}
.back-3 {
  animation: bottomIn 1.5s ease-out forwards;
}
@keyframes bottomInText {
  from {
    transform: translateY(500px);
  }

  to {
    transform: translateY(0);
    opacity: 1;
  }
}
h3 {
  animation: bottomInText 1s ease-out forwards;
}
h1 {
  animation: bottomInText 1.2s ease-out forwards;
}
p {
  animation: bottomInText 1.2s ease-out forwards;
  animation-delay: 0.2s;
  opacity: 0;
}
@keyframes backgroundImage {
  from {
    background-position: top;
  }

  to {
    background-position: 50% 14%;
  }
}
body {
  animation: backgroundImage 1.6s ease-out forwards;
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
@keyframes zoomOut {
  from {
    transform: scale(1.5);
    opacity: 0;
  }

  to {
    transform: scale(1);
    opacity: 1;
  }
}
.slider {
  animation: zoomOut 1.2s ease-out forwards;
}

</style>