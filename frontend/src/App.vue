<!-- 
App.vue - Vue应用根组件

功能概述：
• Vue应用的入口组件，负责全局路由管理和组件渲染
• 配置所有页面组件的路由映射和过渡动画
• 提供全局样式和动画效果定义

架构设计：
• 路由管理：通过Vue Router实现单页面应用导航
• 组件注册：集中管理所有页面组件的导入和注册
• 过渡动画：为路由切换提供统一的视觉过渡效果

技术特点：
• 组件化开发：模块化导入和组件复用
• 路由过渡：使用Vue Transition实现页面切换动画
• 全局样式：定义应用级别的样式规范
-->
<template>
  <div>
    <!-- 主页的全局入口 -->
    <router-link to="/homepage"></router-link>
    <!-- 路由视图容器，带有过渡动画 -->
    <transition name="zoom-fade" mode="out-in">
      <router-view />
    </transition>
  </div>
</template>

<script>

// 1.首先导入组件。模块化的开发思想。不同的文件可以看成是不同的模块进行导入。
// 核心页面组件
import HomePage from './components/HomePage.vue';
import Login from './components/Login.vue';
import VisualPage from './components/VisualPage.vue';
import DeVisualPage from './components/DeVisualPage.vue';
import UserInfo from './components/UserInfo.vue';
import UserManage from './components/UserManage.vue';

// 碳溯功能模块组件
import CarbonSeek from './components/CarbonSeek.vue';
import StructurePredictor from './components/StructurePredictor.vue';
import VirtualPlotDesigner from './components/VirtualPlotDesigner.vue';
import CarbonValueConverter from './components/CarbonValueConverter.vue';

// 数据分析与可视化组件
import ShapVisualizer from './components/ShapVisualizer.vue';
import ResponseCurve from './components/ResponseCurve.vue';
import LiteratureCompare from './components/LiteratureCompare.vue';
import ParamSensitivity from './components/ParamSensitivity.vue';

// AI助手与工具组件
import CarbonAIAssistant from './components/CarbonAIAssistant.vue';
import Chat from './components/Chat.vue';

// 2.完成组件注册
//使用 export default 来导出这个组件对象。这样，其他文件或组件可以通过 import 关键字来引入这个组件。
export default {
  name: 'App',
  components: {
    // 核心页面
    HomePage,
    Login,
    VisualPage,
    DeVisualPage,
    UserInfo,
    UserManage,

    // 碳溯功能
    CarbonSeek,
    StructurePredictor,
    VirtualPlotDesigner,
    CarbonValueConverter,

    // 数据分析
    ShapVisualizer,
    ResponseCurve,
    LiteratureCompare,
    ParamSensitivity,

    // AI助手与工具
    CarbonAIAssistant,
    Chat
  }
}
</script>

<style>
/**
 * 全局样式定义
 * 设置应用级别的样式规则和动画效果
 */

/* 应用根容器样式 */
#app {
  font-family: 'Comic Sans MS', sans-serif;
}

/* 全屏布局设置 */
html,
body,
#app {
  height: 100%;
  width: 100%;
}

/* 链接样式重置 */
a {
  text-decoration: none;
}

/* 定义缩放 + 渐隐动画 */
.zoom-fade-enter-active,
.zoom-fade-leave-active {
  transition: opacity 0.5s ease, transform 0.5s ease;
}

/* 进入动画：从小到大，透明度渐显 */
.zoom-fade-enter-from {
  opacity: 0.5;
  transform: scale(0.95);
}

/* 离开动画：从大到小，透明度渐隐 */
.zoom-fade-leave-to {
  opacity: 0.5;
  transform: scale(1.05);
}
</style>
