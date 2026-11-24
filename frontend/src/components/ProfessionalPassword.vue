<template>
  <PageShell>
    <UnifiedNav />
    <section class="password-page">
      <form class="panel" @submit.prevent="submit">
        <p class="eyebrow">Professional security</p>
        <h1>首次登录修改密码</h1>
        <p>临时密码只用于首次登录。修改成功后当前令牌立即失效，请使用新密码重新登录。</p>
        <label><span>当前密码</span><input v-model="currentPassword" type="password" autocomplete="current-password" required /></label>
        <label><span>新密码</span><input v-model="newPassword" type="password" autocomplete="new-password" required /></label>
        <label><span>确认新密码</span><input v-model="confirmPassword" type="password" autocomplete="new-password" required /></label>
        <p v-if="message" :class="['message', messageType]" role="status">{{ message }}</p>
        <ActionButton label="保存并重新登录" type="submit" :disabled="busy" />
      </form>
    </section>
  </PageShell>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import PageShell from '@/components/common/PageShell.vue';
import UnifiedNav from '@/components/common/UnifiedNav.vue';
import ActionButton from '@/components/common/ActionButton.vue';
import { changeProfessionalPassword } from '@/services/professionalService';
import { useAuth } from '@/composables/useAuth';

const router = useRouter();
const auth = useAuth();
const currentPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const message = ref('');
const messageType = ref('info');
const busy = ref(false);

async function submit() {
  if (newPassword.value !== confirmPassword.value) {
    message.value = '两次输入的新密码不一致。'; messageType.value = 'error'; return;
  }
  if (newPassword.value.length < 8 || !/[A-Za-z]/.test(newPassword.value) || !/\d/.test(newPassword.value)) {
    message.value = '新密码至少 8 位，并同时包含英文字母和数字。'; messageType.value = 'error'; return;
  }
  busy.value = true;
  try {
    await changeProfessionalPassword(currentPassword.value, newPassword.value);
    message.value = '密码已修改，正在退出当前会话。'; messageType.value = 'success';
    auth.logout();
    setTimeout(() => router.replace('/login'), 250);
  } catch (error) {
    message.value = error.message || '密码修改失败。'; messageType.value = 'error';
  } finally { busy.value = false; }
}
</script>

<style scoped>
.password-page { min-height: calc(100vh - 78px); display: grid; place-items: center; padding: 42px 20px; }
.panel { width: min(520px, 100%); display: grid; gap: 14px; padding: 28px; border: 1px solid var(--hn-border); border-radius: var(--hn-radius); background: var(--hn-card); box-shadow: var(--hn-shadow); }
.panel h1 { margin: 0; color: var(--hn-text); }.panel p { color: var(--hn-muted); line-height: 1.65; }.eyebrow { color: var(--hn-accent) !important; font-weight: 900; text-transform: uppercase; font-size: 12px; }
label { display: grid; gap: 6px; color: var(--hn-muted); font-weight: 800; } input { height: 44px; padding: 0 12px; border: 1px solid var(--hn-border-strong); border-radius: 7px; background: var(--hn-control-bg); color: var(--hn-text); }
.message { padding: 10px 12px; border-radius: 7px; background: var(--hn-soft); }.message.error { color: var(--hn-danger); }.message.success { color: var(--hn-accent); }
</style>
