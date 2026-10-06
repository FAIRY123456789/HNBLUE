<template>
  <PageShell>
    <UnifiedNav />
    <section class="login-page">
      <div class="login-copy">
        <p class="eyebrow">平台账号</p>
        <h1>登录</h1>
        <p class="lead">使用统一账号入口进入平台，保持导航、主题和生态蓝绿色视觉一致。</p>
        <div class="auth-facts">
          <span>统一认证入口</span>
          <span>会话自动管理</span>
          <span>按角色进入账户中心</span>
        </div>
      </div>

      <form class="auth-card" @submit.prevent="submitPrimary">
        <div class="mode-tabs" role="tablist" aria-label="登录模式">
          <button type="button" :class="{ active: mode === 'login' }" @click="setMode('login')">登录</button>
          <button type="button" :class="{ active: mode === 'register' }" @click="setMode('register')">注册</button>
          <button type="button" :class="{ active: mode === 'reset' }" @click="setMode('reset')">忘记密码</button>
        </div>

        <header class="card-head">
          <span>{{ modeMeta.eyebrow }}</span>
          <h2>{{ modeMeta.title }}</h2>
          <p>{{ modeMeta.desc }}</p>
        </header>

        <div class="field-grid">
          <label>
            <span>用户名</span>
            <input v-model.trim="credentials.username" type="text" autocomplete="username" placeholder="请输入用户名" required />
          </label>
          <label v-if="mode !== 'reset'">
            <span>密码</span>
            <input v-model="credentials.password" type="password" autocomplete="current-password" placeholder="请输入密码" required />
          </label>
          <label v-if="mode === 'register' || mode === 'reset'">
            <span>邮箱</span>
            <input v-model.trim="credentials.email" type="email" autocomplete="email" placeholder="用于注册或重置验证" required />
          </label>
          <label v-if="mode === 'register'">
            <span>生日</span>
            <input v-model="credentials.birthdate" type="date" required />
          </label>
          <label v-if="mode === 'reset'">
            <span>新密码</span>
            <input v-model="newPassword" type="password" autocomplete="new-password" placeholder="请输入新密码" required />
          </label>
          <label v-if="mode === 'reset'">
            <span>确认新密码</span>
            <input v-model="confirmPassword" type="password" autocomplete="new-password" placeholder="请再次输入新密码" required />
          </label>
        </div>


        <div v-if="message" :class="['auth-message', messageType]">{{ message }}</div>

        <div class="action-row">
          <ActionButton :label="modeMeta.primary" :disabled="isSubmitting" type="submit" />
          <ActionButton v-if="mode !== 'login'" label="返回登录" variant="secondary" type="button" @click="setMode('login')" />
        </div>
      </form>
    </section>
  </PageShell>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { useAuth } from '@/composables/useAuth';
import PageShell from '@/components/common/PageShell.vue';
import UnifiedNav from '@/components/common/UnifiedNav.vue';
import ActionButton from '@/components/common/ActionButton.vue';
import { apiUrl } from '@/utils/urls';

const API_BASE = apiUrl('/api');
const router = useRouter();
const auth = useAuth();
const mode = ref('login');
const isSubmitting = ref(false);
const message = ref('');
const messageType = ref('info');
const newPassword = ref('');
const confirmPassword = ref('');
const credentials = reactive({ username: '', password: '', email: '', birthdate: '' });
const PASSWORD_RULE_MESSAGE = '密码至少 8 位，且需同时包含英文字母和数字。';

function isStrongPassword(value) {
  return typeof value === 'string' && value.length >= 8 && /[A-Za-z]/.test(value) && /\d/.test(value) && !/\s/.test(value);
}


const modeMeta = computed(() => {
  if (mode.value === 'register') return { eyebrow: 'Register', title: '创建账号', desc: '创建账号后返回登录入口', primary: '注册' };
  if (mode.value === 'reset') return { eyebrow: 'Reset', title: '重置密码', desc: '完成密码重置后返回登录入口', primary: '重置密码' };
  return { eyebrow: 'Login', title: '欢迎回来', desc: '登录成功后进入平台工作区', primary: '登录' };
});

function setMode(nextMode) {
  mode.value = nextMode;
  message.value = '';
  messageType.value = 'info';
}

function showMessage(text, type = 'info') {
  message.value = text;
  messageType.value = type;
}

function extractErrorMessage(error, fallback) {
  const payload = error?.response?.data || error?.payload;
  if (typeof payload === 'string' && payload.trim()) return payload.trim();
  if (payload?.message) return payload.message;
  if (payload?.error) return payload.error;
  return error?.message || fallback;
}

function normalizeBirthdate(value) {
  return String(value || '').trim().replaceAll('/', '-');
}

function validateRegisterInput(payload) {
  if (!/^[a-zA-Z0-9_-]{3,20}$/.test(payload.username || '')) return '用户名需为 3-20 位字母、数字、下划线或短横线。';
  if (!isStrongPassword(payload.password)) return PASSWORD_RULE_MESSAGE;
  if (!/^[\w.%+-]+@[\w.-]+\.[a-zA-Z]{2,6}$/.test(payload.email || '')) return '邮箱格式不正确。';
  if (!/^\d{4}-\d{2}-\d{2}$/.test(payload.birthdate || '')) return '生日格式应为 YYYY-MM-DD。';
  return '';
}

function resetForm() {
  credentials.username = '';
  credentials.password = '';
  credentials.email = '';
  credentials.birthdate = '';
  newPassword.value = '';
  confirmPassword.value = '';
}

function submitPrimary() {
  if (mode.value === 'register') return register();
  if (mode.value === 'reset') return resetPassword();
  return login();
}

async function login() {
  isSubmitting.value = true;
  showMessage('正在登录...', 'info');
  try {
    const username = credentials.username;
    const payload = await auth.login({
      username,
      password: credentials.password,
    });
    if (payload.userType !== 'Admin') await auth.refreshUser().catch(() => null);
    showMessage('登录成功，正在进入账户页面。', 'success');
    resetForm();
    const redirect = router.currentRoute.value.query.redirect;
    const fallback = payload.userType === 'Admin' ? '/usermanage' : '/userinfo';
    setTimeout(() => router.push(typeof redirect === 'string' ? redirect : fallback), 300);
  } catch (error) {
    if ((error.response?.status || error.status) === 429) {
      showMessage('请求过于频繁，请稍后再试。', 'error');
    } else if ((error.response?.status || error.status) === 401) {
      showMessage('登录失败，请检查用户名或密码。', 'error');
    } else if ((error.response?.status || error.status) === 400) {
      showMessage(error.response?.data?.message || error.response?.data || '请填写用户名和密码。', 'error');
    } else if ((error.response?.status || error.status) >= 500) {
      showMessage(error.response?.data?.message || '登录接口异常。请检查后端日志、MySQL 登录表和 Redis 限流服务。', 'error');
    } else {
      showMessage('登录失败。请确认后端服务已启动，或检查用户名和密码。', 'error');
    }
  } finally {
    isSubmitting.value = false;
  }
}

async function register() {
  isSubmitting.value = true;
  showMessage('正在提交注册信息...', 'info');
  const payload = {
    username: credentials.username,
    password: credentials.password,
    email: credentials.email,
    birthdate: normalizeBirthdate(credentials.birthdate),
  };
  const validationMessage = validateRegisterInput(payload);
  if (validationMessage) {
    showMessage(validationMessage, 'error');
    isSubmitting.value = false;
    return;
  }
  try {
    await axios.post(`${API_BASE}/register`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });
    showMessage('注册成功，请登录。', 'success');
    resetForm();
    mode.value = 'login';
  } catch (error) {
    showMessage(extractErrorMessage(error, '注册失败，请检查填写信息。'), 'error');
  } finally {
    isSubmitting.value = false;
  }
}

async function resetPassword() {
  if (newPassword.value !== confirmPassword.value) {
    showMessage('两次输入的新密码不一致。', 'error');
    return;
  }
  isSubmitting.value = true;
  showMessage('正在发起密码重置...', 'info');
  try {
    await axios.post(`${API_BASE}/initiateReset`, {
      username: credentials.username,
      email: credentials.email,
      newPassword: newPassword.value,
    });
    pollResetStatus(credentials.username);
  } catch (error) {
    showMessage(error.response?.data || '密码重置请求失败。请确认后端服务已启动。', 'error');
    isSubmitting.value = false;
  }
}

function pollResetStatus(username) {
  let count = 0;
  const timer = setInterval(async () => {
    count += 1;
    try {
      const response = await axios.get(`${API_BASE}/checkResetStatus?username=${encodeURIComponent(username)}`);
      if (response.data.status === 'completed' || response.data.status === 'failed') {
        clearInterval(timer);
        showMessage(response.data.message || (response.data.status === 'completed' ? '密码重置完成。' : '密码重置失败。'), response.data.status === 'completed' ? 'success' : 'error');
        isSubmitting.value = false;
        if (response.data.status === 'completed') {
          resetForm();
          mode.value = 'login';
        }
      }
    } catch (error) {
      clearInterval(timer);
      showMessage('检查密码重置状态失败。请确认后端服务已启动。', 'error');
      isSubmitting.value = false;
    }
    if (count >= 12) {
      clearInterval(timer);
      showMessage('重置状态暂未返回，请稍后重试。', 'error');
      isSubmitting.value = false;
    }
  }, 5000);
}
</script>

<style scoped>
.login-page {
  width: var(--hn-page);
  min-height: calc(100vh - 78px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(360px, 520px);
  gap: 42px;
  align-items: center;
  padding: 42px 0 70px;
}

.login-copy h1 {
  margin: 0;
  color: var(--hn-text);
  font-size: clamp(42px, 6vw, 68px);
  line-height: 1.06;
}

.eyebrow {
  margin: 0 0 12px;
  color: var(--hn-accent);
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.lead {
  max-width: 620px;
  margin: 18px 0 0;
  color: var(--hn-muted);
  font-size: 18px;
  line-height: 1.75;
}

.auth-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.auth-facts span {
  min-height: 32px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border: 1px solid var(--hn-border);
  border-radius: 999px;
  background: var(--hn-soft);
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
}

.auth-card {
  padding: 24px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-card);
  box-shadow: var(--hn-shadow);
}

.mode-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  padding: 6px;
  border: 1px solid var(--hn-border);
  border-radius: 999px;
  background: var(--hn-surface);
}

.mode-tabs button {
  min-height: 36px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--hn-muted);
  cursor: pointer;
  font-weight: 900;
}

.mode-tabs button.active {
  background: var(--hn-accent);
  color: #fff;
}

.card-head {
  margin: 24px 0 18px;
}

.card-head span {
  color: var(--hn-accent);
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.card-head h2 {
  margin: 8px 0;
  color: var(--hn-text);
  font-size: 30px;
}

.card-head p {
  margin: 0;
  color: var(--hn-muted);
  line-height: 1.6;
}

.field-grid {
  display: grid;
  gap: 12px;
}

label {
  display: grid;
  gap: 6px;
}

label span {
  color: var(--hn-muted);
  font-size: 12px;
  font-weight: 900;
}

.field-hint {
  margin: -1px 0 0;
  color: var(--hn-muted);
  font-size: 12px;
  line-height: 1.45;
}

input {
  width: 100%;
  height: 44px;
  border: 1px solid var(--hn-border-strong);
  border-radius: 7px;
  background: var(--hn-control-bg);
  color: var(--hn-text);
  padding: 0 12px;
  font: inherit;
}


.auth-message {
  margin-top: 16px;
  padding: 12px 14px;
  border: 1px solid var(--hn-border);
  border-radius: 8px;
  background: var(--hn-surface);
  color: var(--hn-text);
  line-height: 1.5;
}

.auth-message.success { border-color: rgba(31, 138, 122, 0.34); color: var(--hn-accent); }
.auth-message.error { border-color: rgba(161, 58, 58, 0.28); color: var(--hn-danger); }

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

@media (max-width: 860px) {
  .login-page {
    grid-template-columns: 1fr;
    min-height: auto;
  }
}
</style>
