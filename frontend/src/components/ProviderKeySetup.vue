<template>
  <main class="setup-page">
    <section class="setup-card" aria-labelledby="provider-setup-title">
      <p class="eyebrow">HNBLUE INTERNAL SETUP</p>
      <h1 id="provider-setup-title">AI 服务密钥一次性设置</h1>

      <p v-if="loading" class="notice neutral">正在确认设置状态…</p>

      <template v-else-if="locked">
        <p class="notice success">已完成并锁定</p>
        <p class="description">该密钥不能查看、修改或删除。本页面不显示任何密钥信息。</p>
      </template>

      <template v-else>
        <p class="description">
          此入口无需登录，仅供项目维护人员首次初始化。提交成功后输入框将永久消失，密钥不能再次查看、修改或删除。
        </p>

        <p v-if="!encryptionReady" class="notice error">
          服务器加密服务尚未就绪，当前不能保存。
        </p>
        <p v-else-if="!transportSafe" class="notice warning">
          当前连接不是 HTTPS 或本机安全入口。为避免密钥在公网明文传输，输入已被禁用；请使用 HTTPS 或 SSH 加密隧道打开本页。
        </p>

        <form class="setup-form" @submit.prevent="save">
          <label for="provider-api-key">API Key</label>
          <input
            id="provider-api-key"
            v-model="apiKey"
            type="password"
            autocomplete="new-password"
            spellcheck="false"
            minlength="16"
            maxlength="256"
            :disabled="!canSubmit || saving"
            placeholder="仅此一次，请确认无误后提交"
          />
          <p class="field-hint">不会写入浏览器存储、URL、前端文件或日志。</p>
          <button type="submit" :disabled="!canSubmit || saving || apiKey.length < 16">
            {{ saving ? '正在加密并锁定…' : '确认保存并永久锁定' }}
          </button>
        </form>
      </template>

      <p v-if="message" class="notice" :class="messageType" role="status">{{ message }}</p>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { getProviderSetupStatus, initializeProviderKey } from '@/services/providerSetupService';

const apiKey = ref('');
const loading = ref(true);
const saving = ref(false);
const locked = ref(false);
const encryptionReady = ref(false);
const message = ref('');
const messageType = ref('neutral');

const transportSafe = computed(() => {
  if (typeof window === 'undefined') return false;
  const host = window.location.hostname;
  return window.isSecureContext || host === 'localhost' || host === '127.0.0.1' || host === '::1';
});
const canSubmit = computed(() => !locked.value && encryptionReady.value && transportSafe.value);

function applyStatus(payload = {}) {
  locked.value = Boolean(payload.locked);
  encryptionReady.value = Boolean(payload.encryptionReady);
}

async function loadStatus() {
  try {
    applyStatus(await getProviderSetupStatus());
  } catch (error) {
    messageType.value = 'error';
    message.value = error.message || '无法读取设置状态';
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (!canSubmit.value || saving.value || apiKey.value.length < 16) return;
  saving.value = true;
  message.value = '';
  try {
    applyStatus(await initializeProviderKey(apiKey.value));
    apiKey.value = '';
    messageType.value = 'success';
    message.value = '保存成功。密钥已加密并永久锁定，后续不能查看或修改。';
  } catch (error) {
    apiKey.value = '';
    if (error.status === 409) locked.value = true;
    messageType.value = 'error';
    message.value = error.message || '保存失败';
  } finally {
    saving.value = false;
  }
}

onMounted(loadStatus);
</script>

<style scoped>
.setup-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px 16px;
  background:
    radial-gradient(circle at 18% 12%, rgba(49, 145, 116, .13), transparent 34%),
    linear-gradient(160deg, #f5fbf8 0%, #edf6f2 100%);
}
.setup-card {
  width: min(560px, 100%);
  display: grid;
  gap: 16px;
  padding: clamp(24px, 5vw, 42px);
  border: 1px solid rgba(31, 104, 83, .18);
  border-radius: 18px;
  background: rgba(255, 255, 255, .94);
  box-shadow: 0 24px 70px rgba(18, 75, 59, .12);
}
.eyebrow { margin: 0; color: #357b66; font-size: 12px; font-weight: 700; letter-spacing: .16em; }
h1 { margin: 0; color: #163c31; font-size: clamp(26px, 5vw, 36px); line-height: 1.2; }
.description, .field-hint { margin: 0; color: #527067; line-height: 1.7; }
.field-hint { font-size: 13px; }
.setup-form { display: grid; gap: 9px; }
label { color: #234d41; font-size: 14px; font-weight: 700; }
input {
  height: 48px;
  padding: 0 14px;
  border: 1px solid #8cb3a7;
  border-radius: 9px;
  background: #fff;
  color: #173e33;
  font: inherit;
}
input:focus { outline: 3px solid rgba(44, 130, 105, .16); border-color: #2c8269; }
input:disabled { cursor: not-allowed; background: #eef3f1; }
button {
  min-height: 46px;
  margin-top: 7px;
  border: 0;
  border-radius: 9px;
  color: #fff;
  background: #1f765d;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}
button:disabled { cursor: not-allowed; opacity: .5; }
.notice { margin: 0; padding: 12px 14px; border-radius: 9px; line-height: 1.55; }
.notice.neutral { color: #45635b; background: #eef4f2; }
.notice.success { color: #126046; background: #e4f6ef; }
.notice.warning { color: #805b12; background: #fff5d9; }
.notice.error { color: #9a3636; background: #fff0f0; }
</style>
