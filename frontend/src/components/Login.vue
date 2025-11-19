<!-- 
登录注册页面组件 - 用户认证系统入口

功能概述：
• 用户登录、注册、密码重置三合一页面
• 响应式设计适配不同设备
• 动画过渡和状态管理

页面模式：
• 登录模式：用户名密码登录 + 第三方登录
• 注册模式：用户名、密码、邮箱、生日信息注册
• 密码重置模式：用户名验证 + 新密码设置

安全特性：
• 登录防抖和频率限制处理
• 密码重置轮询状态检查
• Token自动拦截和存储
-->
<template>
  <div class="shell">
    <!-- 顶部导航栏 -->
    <header>
      <nav>
        <router-link class="nav-items" to="/">首页</router-link>
        <router-link class="nav-items" to="/about">关于</router-link>
        <router-link class="nav-items" to="/visual">地图</router-link>
        <router-link class="nav-items" to="/carbonseek">碳溯</router-link>
        <router-link class="nav-items" to="/userinfo">个人中心</router-link>
      </nav>
    </header>

    <!-- 侧边图片区域 -->
    <div id="img-box">
      <img src="../assets/login_bac2.png" alt="">
    </div>

    <!-- 表单区域 -->
    <form @submit.prevent>
      <div id="form-body">
        <!-- 欢迎语区域 -->
        <div id="welcome-lines">
          <div id="w-line-1">{{ welcomeText }}</div>
          <div id="w-line-2">欢迎您访问海南蓝碳数字化平台</div>
        </div>
        <!-- 输入区域 -->
        <div id="input-area">
          <!-- 登录输入框 -->
          <div class="f-inp" v-show="!isRegister && !isForgotPassword">
            <input type="text" placeholder="Your name" v-model="credentials.username" required>
          </div>
          <div class="f-inp" v-show="!isRegister && !isForgotPassword">
            <input type="password" placeholder="Password" v-model="credentials.password" required>
          </div>
          <!-- 注册输入框 -->
          <div class="columns" v-show="isRegister">
            <div class="column">
              <div class="f-inp">
                <input type="text" placeholder="用户名" v-model="credentials.username" required>
              </div>
              <div class="f-inp">
                <input type="password" placeholder="密码" v-model="credentials.password" required>
              </div>
            </div>
            <div class="column">
              <div class="f-inp">
                <input type="email" placeholder="邮箱" v-model="credentials.email" required>
              </div>
              <div class="f-inp">
                <input type="date" placeholder="生日" v-model="credentials.birthdate" required>
              </div>
            </div>
          </div>
          <!-- 密码重置输入框 - 双列布局 -->
          <div class="columns" v-show="isForgotPassword">
            <div class="column">
              <div class="f-inp">
                <input type="text" placeholder="用户名" v-model="credentials.username" required>
              </div>
              <div class="f-inp">
                <input type="text" placeholder="认证邮箱" v-model="credentials.email" required>
              </div>
            </div>
            <div class="column">
              <div class="f-inp">
                <input type="password" placeholder="新密码" v-model="newPassword" required>
              </div>
              <div class="f-inp">
                <input type="password" placeholder="新密码确认" v-model="confirmPassword" required>
              </div>
            </div>
          </div>
        </div>
        <!-- 第三方登录区域 -->
        <div id="social-login-area" v-show="!isRegister && !isForgotPassword">
          <button class="social-button wechat"></button><span
            style="font-size: small;margin-right: 20px;color: #7f7f7f;">微信</span>
          <button class="social-button alipay"></button><span style="font-size: small;color: #7f7f7f;">支付宝</span>
        </div>
        <!-- 提交按钮区域 -->
        <div id="submit-button-cvr">
          <button type="submit" id="submit-button" @click="login" :disabled="isLoginDisabled">登录</button>
          <button type="button" id="goto-register-button" @click="moveToRegister">去注册</button>
          <button type="submit" id="register-button" @click="register" style="display: none;">注册</button>
          <button type="button" id="back-button" style="display: none;" @click="moveBack">返回</button>
          <button type="submit" id="reset-pwd-button" @click="resetPassword" v-show="isForgotPassword">重置密码</button>
          <button type="button" id="back-pwd-button" @click="moveBackFromForgot" v-show="isForgotPassword">返回</button>
          <div id="status"></div>
        </div>
        <!-- 忘记密码链接 -->
        <div id="forgot-pass" v-show="!isRegister && !isForgotPassword">
          <a @click.prevent="moveToForgotPassword">忘记密码?</a>
        </div>
      </div>
      <!-- 消息显示区域 -->
      <transition name="fade">
        <div v-if="message && isMessageVisible" :class="['message',
          messageType === 'success' ? 'success-message' : 'error-message']">
          {{ message }}
        </div>
      </transition>
    </form>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

export default {
  setup() {
    // 响应式数据定义
    const credentials = reactive({
      username: '',
      password: '',
      email: '',
      birthdate: ''
    });
    // 页面状态控制
    const isRegister = ref(false);
    const isForgotPassword = ref(false);
    const newPassword = ref('');
    const confirmPassword = ref('');
    const welcomeText = ref('登录');
    // 添加一个状态变量来控制登录按钮的可用性
    const isLoginDisabled = ref(false);

    // 定义消息和消息类型
    const message = ref('');
    const messageType = ref('');
    const isMessageVisible = ref(false); // 新增

    /**
     * 清除消息提示
     */
    const clearMessage = () => {
      message.value = '';
      messageType.value = '';
      isMessageVisible.value = false; // 清除消息时隐藏
    };

    /**
     * 用户登录功能
     * 处理登录请求，存储token和用户类型，跳转首页
     */
    async function login() {
      console.log("发送前的凭据对象：", credentials);
      try {
        console.log("登陆中");
        isLoginDisabled.value = true; // 禁用登录按钮
        const response = await axios.post('http://localhost:8088/api/login', credentials);

        // 存储 token 和 userType
        localStorage.setItem('token', response.data.token);
        localStorage.setItem('userType', response.data.userType); // 👈 存储用户类型

        // 清空表单和消息提示
        credentials.value = { username: '', password: '' };
        message.value = `Succeed to login! ✔`;
        messageType.value = 'success';
        isMessageVisible.value = true;

        setTimeout(clearMessage, 3000);
        isLoginDisabled.value = false;

        // 登录成功统一跳转首页
        router.push({ name: 'HomePage' });

      } catch (error) {
        isMessageVisible.value = true;
        if (error.response?.status === 429) {
          message.value = '请求过于频繁，请稍后再试！';
          messageType.value = 'error';
          setTimeout(clearMessage, 30000);
          setTimeout(() => { isLoginDisabled.value = false; }, 30000);
        } else {
          console.error(error.response ? error.response.data : error);
          message.value = `Fail to login! ❌`;
          messageType.value = 'error';
          setTimeout(clearMessage, 3000);
          isLoginDisabled.value = false;
        }
      }
    }

    /**
     * 用户注册功能
     * 处理用户注册请求，注册成功后跳转到登录页
     */
    async function register() {
      console.log("发送前的凭据对象：", credentials);
      try {
        console.log("注册中");
        // 发送请求到您的后端API
        const response = await axios.post('http://localhost:8088/api/register', credentials);
        // 处理注册成功的情况，可能是跳转到登录页面或直接登录
        console.log(response.data); // 这里可以根据后端返回的内容进行处理
        // 显示成功消息，response.status 应该是200
        message.value = `Succeed to register! ✔`;
        messageType.value = 'success';
        isMessageVisible.value = true; // 显示消息
        setTimeout(clearMessage, 3000);
        router.push({ name: 'Login' }); // 假设注册后跳转到登录界面
      } catch (error) {
        // 处理错误，例如显示错误消息
        console.error("出错啦！");
        message.value = ` Fail to register! ❌`;
        /**
        HTTP 状态码 409 表示“冲突”，这通常意味着请求与服务器当前状态存在冲突。
        在注册场景中，这可能是由于尝试注册一个已经存在的用户名或电子邮件地址。
        为了处理这种情况，你应该在前端显示一个适当的错误消息，通知用户已存在的账号，或是需要采取其他的行动。 */
        if (error.response && error.response.status === 409) {
          message.value = message.value + 'Username or email already exists, please try different options.';
        } else {
          message.value = message.value + 'Registration failed, please try again later.';
        }
        messageType.value = 'error';
        isMessageVisible.value = true; // 显示消息
        setTimeout(clearMessage, 3000);
        setTimeout(() => {
          message.value = '';
        }, 3000);
      }
    }

    /**
     * 切换到注册页面
     * 执行表单滑动动画，显示注册表单
     */
    function moveToRegister() {

      welcomeText.value = '注册'; // 修改标题

      const formElem = document.querySelector('form');
      const imgBoxElem = document.getElementById('img-box');
      const forgotPassElem = document.getElementById('forgot-pass');

      const loginButton = document.getElementById('submit-button');
      const gotoRegisterButton = document.getElementById('goto-register-button');
      const registerButton = document.getElementById('register-button');
      const backButton = document.getElementById('back-button');

      // 隐藏忘记密码链接
      forgotPassElem.style.opacity = '0';
      forgotPassElem.style.pointerEvents = 'none';

      // 执行滑动动画
      formElem.style.transform = 'translateX(-210px)';
      imgBoxElem.style.transform = 'translateX(548px)';

      // 淡出登录相关按钮
      loginButton.style.opacity = '0';
      gotoRegisterButton.style.opacity = '0';

      // 延迟显示注册相关按钮
      setTimeout(() => {
        isRegister.value = true;

        loginButton.style.display = 'none';
        gotoRegisterButton.style.display = 'none';

        backButton.style.display = 'inline-block';
        registerButton.style.display = 'inline-block';

        backButton.style.opacity = '1';
        registerButton.style.opacity = '1';
      }, 500);
    }

    /**
     * 切换回登录页面
     * 执行表单滑动动画，显示登录表单
     */
    function moveBack() {
      welcomeText.value = '登录'; // 恢复标题

      const formElem = document.querySelector('form');
      const imgBoxElem = document.getElementById('img-box');
      const forgotPassElem = document.getElementById('forgot-pass');

      const loginButton = document.getElementById('submit-button');
      const gotoRegisterButton = document.getElementById('goto-register-button');
      const registerButton = document.getElementById('register-button');
      const backButton = document.getElementById('back-button');

      // 执行反向滑动动画
      formElem.style.transform = 'translateX(0)';
      imgBoxElem.style.transform = 'translateX(0)';

      // 显示忘记密码链接
      forgotPassElem.style.opacity = '1';
      forgotPassElem.style.pointerEvents = 'auto';

      // 淡出注册相关按钮
      backButton.style.opacity = '0';
      registerButton.style.opacity = '0';

      // 延迟显示登录相关按钮
      setTimeout(() => {
        backButton.style.display = 'none';
        registerButton.style.display = 'none';

        loginButton.style.display = 'inline-block';
        gotoRegisterButton.style.display = 'inline-block';

        setTimeout(() => {
          loginButton.style.opacity = '1';
          gotoRegisterButton.style.opacity = '1';
          isRegister.value = false;
        }, 50);
      }, 500);
    }

    /**
     * 切换到忘记密码页面
     * 隐藏登录相关元素，显示密码重置表单
     */
    const moveToForgotPassword = () => {

      welcomeText.value = '重置密码'; // 修改标题

      isForgotPassword.value = true;
      isRegister.value = false;  // 确保注册表单被隐藏
      const formElem = document.querySelector('form');
      const imgBoxElem = document.getElementById('img-box');
      const forgotPassElem = document.getElementById('forgot-pass');

      const loginButton = document.getElementById('submit-button');
      const gotoRegisterButton = document.getElementById('goto-register-button');
      const registerButton = document.getElementById('register-button');
      const backButton = document.getElementById('back-button');

      forgotPassElem.style.opacity = '0';
      forgotPassElem.style.pointerEvents = 'none';

      loginButton.style.display = 'none';
      gotoRegisterButton.style.display = 'none';

    };

    /**
     * 从忘记密码页面返回
     * 恢复登录页面状态
     */
    const moveBackFromForgot = () => {

      welcomeText.value = '登录'; // 修改标题

      const formElem = document.querySelector('form');
      const imgBoxElem = document.getElementById('img-box');
      const forgotPassElem = document.getElementById('forgot-pass');

      const loginButton = document.getElementById('submit-button');
      const gotoRegisterButton = document.getElementById('goto-register-button');
      const registerButton = document.getElementById('register-button');
      const backButton = document.getElementById('back-button');

      // 恢复相关元素显示
      forgotPassElem.style.opacity = '1';
      forgotPassElem.style.pointerEvents = 'auto';
      loginButton.style.display = 'inline-block';
      gotoRegisterButton.style.display = 'inline-block';
      loginButton.style.opacity = '1';
      gotoRegisterButton.style.opacity = '1';

      isRegister.value = false;
      isForgotPassword.value = false;
    };

    /**
     * 密码重置功能
     * 验证用户信息并重置密码，使用轮询检查重置状态
     */
    const resetPassword = async () => {
      // 密码确认验证
      if (newPassword.value !== confirmPassword.value) {
        message.value = "Passwords do not match.";
        messageType.value = 'error';
        isMessageVisible.value = true;
        setTimeout(clearMessage, 3000);
        return;
      }
      try {
        // 发起密码重置请求
        const response = await axios.post('http://localhost:8088/api/initiateReset', {
          username: credentials.username,
          email: credentials.email,
          newPassword: newPassword.value // 传递新密码
        });

        // 启动轮询检查重置状态
        if (response.status === 200) {
          // 现在开始轮询后端以检查密码重置过程的状态
          pollResetStatus(credentials.username);
        }
      } catch (error) {
        message.value = error.response.data || "Failed to reset password. Please try again. ❌";
        messageType.value = 'error';
        isMessageVisible.value = true;
        setTimeout(clearMessage, 3000);
      }
    };

    // 轮询服务器以检查重置状态的函数，每5秒检查一次重置状态，直到完成或失败
    // 如果启动成功，前端开始轮询另一个端点 (checkResetStatus) 来获取过程的更新。轮询将持续进行，直到后端确认过程已经完成或失败。
    const pollResetStatus = (username) => {
      const interval = setInterval(async () => {
        try {
          const statusResponse = await axios.get(`http://localhost:8088/api/checkResetStatus?username=${username}`);
          if (statusResponse.data.status === "completed" || statusResponse.data.status === "failed") {
            clearInterval(interval);
            message.value = statusResponse.data.message;
            messageType.value = (statusResponse.data.status === "completed") ? 'success' : 'error';
            isMessageVisible.value = true;
            setTimeout(() => {
              clearMessage();
              if (statusResponse.data.status === "completed") {
                moveBackFromForgot(); // 导航回去或清除表单
              }
            }, 3000);
          }
        } catch (error) {
          clearInterval(interval);
          message.value = "检查密码重置状态时发生错误。❌";
          messageType.value = 'error';
          isMessageVisible.value = true;
          setTimeout(clearMessage, 3000);
        }
      }, 5000); // 每5秒轮询一次
    };
    const router = useRouter();

    axios.interceptors.request.use(config => {
      const token = localStorage.getItem('token');
      if (token) {
        config.headers['Authorization'] = 'Bearer ' + token;
      }
      return config;
    }, error => {
      return Promise.reject(error);
    });

    // 确保返回所有需要在模板中使用的响应式数据和方法
    return {
      credentials,
      login,
      register,
      moveToRegister,
      moveBack,
      isRegister,
      message,
      messageType,
      clearMessage,
      isMessageVisible,
      isForgotPassword,
      newPassword,
      confirmPassword,
      moveToForgotPassword,
      moveBackFromForgot,
      resetPassword,
      pollResetStatus,
      isLoginDisabled,
      welcomeText, // 绑定标题
    };
  },
  mounted() {
    // 设置第一个组件的背景样式
    Object.assign(document.body.style, {
      background: `url(${require('@/assets/login_bac.png')}) no-repeat center center fixed`,
      backgroundSize: "cover",
      display: "flex",
      justifyContent: "center",
      alignItems: "center",
      height: "97vh"
    });
  },
  beforeUnmount() {
    // 在组件卸载之前清除样式
    Object.assign(document.body.style, {
      background: "",
      backgroundSize: "",
      display: "",
      justifyContent: "",
      alignItems: "",
      height: ""
    });
  }
}
</script>


<style scoped>
/* 重置样式 */
* {
  padding: 0;
  margin: 0;
  outline: none;
}

/* 主容器样式 */
.shell {
  height: 97vh;
  /* 确保.shell的高度占据整个视口高度 */
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.5s;
  flex-direction: row;
}

/* 表单容器样式 */
form {
  width: 562px;
  height: 520px;
  background-color: #fff;
  box-shadow: 0px 15px 40px #0e3e35;
  border-radius: 15px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.shell {
  transition: all 0.5s;
  /* 添加0.5秒的过渡效果 */
  flex-direction: row;
  /* 确保子元素是水平排列的 */
  margin-right: 8px;
}

form,
#img-box {
  transition: transform 0.5s;
  /* 平滑的移动效果 */

}

/* 图片区域样式 */
#img-box {
  width: 230px;
  height: 520px;
  display: flex;
  align-items: center;
  /* 这会确保图片垂直居中 */
  justify-content: center;
  /* 这会确保图片水平居中 */
}

#img-box img {
  margin-left: 30px;
  width: 100%;
  /* 保证图片宽度充满容器 */
  height: 100%;
  object-fit: cover;
  /* 使图片保持宽高比且充满容器 */
}

#img-box img {
  height: 100%;
}

/* 表单容器样式 */
#form-body {
  width: 320px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
}

#welcome-lines {
  width: 100%;
  text-align: center;
  line-height: 1;
}

#w-line-1 {
  color: #7f7f7f;
  font-size: 50px;
}

#w-line-2 {
  color: #9c9c9c;
  font-size: 20px;
  margin-top: 17px;
}

#input-area {
  width: 100%;
  margin-top: 40px;
}

.f-inp {
  padding: 13px 25px;
  border: 2px solid #6e6d6d;
  line-height: 1;
  border-radius: 20px;
  margin-bottom: 15px;
  transition: box-shadow 0.3s ease;
  /* 添加过渡效果，使阴影效果平滑 */

}

.f-inp:focus-within {
  box-shadow: 0 0 10px #b6b64d;
  /* 当内部的input获得焦点时，整个.f-inp容器增加阴影 */
}

.f-inp input {
  width: 100%;
  font-size: 14px;
  padding: 0;
  margin: 0;
  border: 0;
  font-family: 'Comic Sans MS', sans-serif;
  /* 使用Comic Sans字体为其增添可爱的效果 */
}

.f-inp input::placeholder {
  color: #b9b9b9;
}

#submit-button-cvr {
  display: flex;
  justify-content: space-around;
  width: 100%;
  margin-top: 20px;
}

/* 按钮通用样式 */
#submit-button,
#register-button,
#goto-register-button,
#reset-pwd-button,
#back-pwd-button {
  display: inline-block;
  width: 45%;
  color: #fff;
  font-size: 16px;
  margin: 0;
  padding: 14px 40px;
  border: 0;
  background-color: #10b78d;
  border-radius: 25px;
  line-height: 1;
  cursor: pointer;
  transition: background-color 0.3s;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.3);
  /* 轻微的阴影效果 */
  font-weight: 550;
  /* 字体加粗 */
  font-family: 'Comic Sans MS', sans-serif;
  /* 使用Comic Sans字体为其增添可爱的效果 */
  transition: background-color 0.3s, transform 0.4s, box-shadow 0.3s, opacity 0.5s;
  height: 60px;
}

/* 按钮悬停效果 */
#submit-button:hover,
#register-button:hover,
#goto-register-button:hover,
#reset-pwd-button:hover,
#back-pwd-button:hover {
  background-color: #08826c;
  transform: translateY(-4px);
  /* 鼠标悬停时上移 */
}

/* 返回按钮特殊样式 */
#back-button {
  display: none;
  width: 45%;
  color: #fff;
  font-size: 14px;
  margin: 0;
  padding: 14px 40px;
  border: 0;
  background-color: #10b78d;
  border-radius: 25px;
  line-height: 1;
  cursor: pointer;
  transition: background-color 0.3s, transform 0.4s, box-shadow 0.3s, opacity 0.5s;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.3);
  font-weight: 600;
  font-family: 'Comic Sans MS', sans-serif;
}

#back-button:hover {
  background-color: #08826c;
  transform: translateY(-4px);
}

/* 忘记密码链接样式 */
#forgot-pass {
  text-align: center;
  margin-top: 10px;
  font-family: 'Comic Sans MS', sans-serif;
  /* 使用Comic Sans字体为其增添可爱的效果 */
}

#forgot-pass a {
  color: #868686;
  font-size: 12px;
  text-decoration: none;
}

#forgot-pass a:hover {
  color: #25a6d1;
}

/* 消息提示样式 */
.message {
  position: fixed;
  /* 绝对定位，相对于视口 */
  top: 30%;
  /* 顶部 */
  left: 50%;
  /* 水平居中 */
  transform: translateX(-50%);
  /* 通过转换确保水平居中 */
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 5px;
  z-index: 1000;
  /* 确保在顶层 */
  opacity: 1;
  transition: opacity 0.3s ease-in-out;
  /* 动画：淡入淡出效果 */
}

/* 成功消息的样式 */
.success-message {
  background-color: #d4edda;
  color: #155724;
  border-color: #c3e6cb;
}

/* 错误消息的样式 */
.error-message {
  background-color: #f8d7da;
  color: #721c24;
  border-color: #f5c6cb;
}

/* 第三方登录区域样式 */
#social-login-area {
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 社交按钮样式 */
.social-button {
  width: 50px;
  height: 50px;
  margin-right: 10px;
  border-radius: 25px;
  border: none;
  cursor: pointer;
  background-color: #f1f1f1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.3s;
}

.social-button:hover {
  background-color: #e2e2e2;
}

/* 微信按钮图标 */
.social-button.wechat {
  background-image: url('../assets/icons8-wechat-50.png');
  background-repeat: no-repeat;
  background-position: center;
}

/* 支付宝按钮图标 */
.social-button.alipay {
  background-image: url('../assets/icons8-alipay-a-third-party-mobile-and-online-payment-platform-28.png');
  background-repeat: no-repeat;
  background-position: center;
}

/* 双列布局样式 */
.columns {
  display: flex;
  width: 100%;
}

.column {
  flex: 1;
  padding: 0 2px;
}

/* 针对屏幕宽度小于850px的设定 */
@media (max-width: 850px) {
  .shell {
    transform: scale(0.8);
    /* 将shell缩小到85% */
    transform-origin: center;
    /* 保证缩放中心在中间 */
  }
}

/* 针对屏幕宽度小于650px的设定 */
@media (max-width: 650px) {
  .shell {
    transform: scale(0.6);
    /* 将shell缩小到70% */
    transform-origin: center;
    /* 保证缩放中心在中间 */
  }

  .social-button+span {
    display: none;
    /* 隐藏社交按钮旁边的文本标签 */
  }

  .columns {
    flex-direction: column;
  }
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
</style>