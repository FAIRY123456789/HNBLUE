/**
 * Vue应用入口文件 - 应用程序初始化和全局配置
 *
 * 功能概述：
 * • 创建Vue应用实例并配置全局依赖
 * • 注册路由、状态管理、UI组件库等核心模块
 * • 设置全局错误处理机制
 * • 配置第三方库和组件注册
 *
 * 技术栈集成：
 * • Vue 3 - 核心框架
 * • Vue Router - 路由管理
 * • Vuex - 状态管理
 * • Element Plus - UI组件库
 * • md-editor-v3 - Markdown编辑器
 *
 * 模块说明：
 * • createApp - Vue应用工厂函数
 * • App.vue - 根组件
 * • router - 路由配置
 * • store - 状态管理配置
 * • ElementPlus - UI组件库
 * • MdPreview/MdCatalog - Markdown组件
 */

// 导入Vue核心方法和组件
import { createApp } from "vue"; // Vue 3应用工厂函数
import App from "./App.vue";     // 应用根组件
import router from "./router";   // 路由配置
import store from "./stores/counter.js"; // Vuex状态管理

// 导入Element Plus UI组件库
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";

// 引入Markdown编辑器组件：editor-v3
import { MdPreview, MdCatalog } from "md-editor-v3";
import "md-editor-v3/lib/style.css";

/**
 * 创建并配置Vue应用实例
 * 
 * 配置流程：
 * 1. 使用createApp创建应用实例
 * 2. 使用use()方法安装插件
 * 3. 使用component()方法注册全局组件
 * 4. 使用mount()方法挂载到DOM
 */
createApp(App)
  .use(router)           // 安装路由插件
  .use(store)            // 安装状态管理插件
  .use(ElementPlus)      // 安装Element Plus UI库
  .component("MdPreview", MdPreview)   // 全局注册Markdown预览组件
  .component("MdCatalog", MdCatalog)   // 全局注册Markdown目录组件
  .mount("#app");        // 挂载到index.html中的#app元素

/**
 * 全局错误处理 - 解决ResizeObserver循环限制错误
 * 
 * 问题背景：
 * ResizeObserver在某些情况下可能会触发循环更新，导致控制台警告
 * 这个错误不影响功能，但会影响开发体验
 * 
 * 解决方案：
 * 捕获特定错误并阻止其传播，避免控制台污染
 */
window.addEventListener("error", (e) => {
  // 检查是否为ResizeObserver循环错误
  if (e.message?.includes("ResizeObserver loop")) {
    // 阻止错误继续传播，避免控制台警告
    e.stopImmediatePropagation();
  }
});

/**
 * 应用架构说明：
 * 
 * 模块化开发：
 * - 通过import语句导入各个功能模块
 * - 每个模块职责单一，便于维护和测试
 * - 支持按需加载，优化打包体积
 * 
 * 组件化开发：
 * - App.vue作为根组件协调整个应用
 * - 通过全局注册使组件在所有地方可用
 * - 支持组件复用和组合
 * 
 * 插件系统：
 * - 使用Vue的use()方法安装插件
 * - 插件可以扩展应用功能
 * - 提供一致的API使用方式
 */

/**
 * 开发注意事项：
 * 
 * 1. 依赖管理：
 *    - 所有第三方库都在此文件集中导入
 *    - 便于依赖版本管理和升级
 * 
 * 2. 全局配置：
 *    - 全局组件在此统一注册
 *    - 避免在多个地方重复注册
 * 
 * 3. 错误处理：
 *    - 在此处设置全局错误捕获
 *    - 提供统一的错误处理机制
 * 
 * 4. 性能优化：
 *    - 考虑按需导入大型库的组件
 *    - 监控打包体积和加载性能
 */
