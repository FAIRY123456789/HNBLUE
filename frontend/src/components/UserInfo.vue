<template>
  <PageShell>
    <UnifiedNav />
    <PageHero title="个人中心" eyebrow="Account" subtitle="查看并维护个人资料、头像和登录账号信息" :tags="['资料查看', '资料修改', '头像上传']" />
    <section class="account-page">
      <ActionToast v-model="toast.visible" :type="toast.type" :message="toast.message" />

      <section class="account-card">
        <div class="avatar" :style="avatarStyle">{{ avatarText }}</div>
        <div class="account-main">
          <p class="eyebrow">当前账号</p>
          <h2>{{ form.name || '未加载用户' }}</h2>
          <p>{{ form.email || '暂无邮箱' }}</p>
        </div>
        <dl class="account-meta">
          <div><dt>账号</dt><dd>{{ form.username || form.name || '-' }}</dd></div>
          <div><dt>角色</dt><dd>{{ roleDisplay }}</dd></div>
          <div><dt>最近登录</dt><dd>{{ form.lastLoginAt || '暂无记录' }}</dd></div>
        </dl>
      </section>

      <section class="profile-workspace">
        <form class="edit-card" @submit.prevent="saveProfile">
          <SectionHeader title="个人资料" eyebrow="Profile" subtitle="密码为空时不会修改密码" />
          <div class="field-grid">
            <label><span>姓名</span><input v-model="form.name" autocomplete="name" /></label>
            <label><span>邮箱</span><input v-model="form.email" type="email" autocomplete="email" /></label>
            <label><span>生日</span><input v-model="form.birthdate" type="date" /></label>
            <label>
              <span>新密码</span>
              <input v-model="form.password" type="password" autocomplete="new-password" placeholder="不修改请留空" />
              <p class="field-hint">新密码需不少于 8 位，并同时包含英文字母和数字。</p>
            </label>
          </div>
          <div class="action-row">
            <ActionButton label="保存资料" type="submit" :disabled="isSaving" />
            <ActionButton label="重新加载" type="button" variant="secondary" @click="loadProfile" />
          </div>
        </form>

        <aside class="edit-card avatar-card">
          <SectionHeader title="头像上传" eyebrow="Avatar" subtitle="支持 PNG、JPG、WebP，文件大小不超过 2MB" />
          <AvatarUploader :avatar="form.avatar" :busy="isUploading" @select="onAvatarSelect" @upload="submitAvatar" @error="show($event, 'error')" />
        </aside>
      </section>

      <section class="tips-card">
        <SectionHeader title="账号与使用提示" eyebrow="Tips" subtitle="保持资料准确有助于管理员核验和后续联系" />
        <ul>
          <li>邮箱请保持准确，便于接收账号相关通知。</li>
          <li>密码留空表示不修改当前密码。</li>
          <li>头像支持 PNG、JPG、WebP，大小不超过 2MB。</li>
          <li>在公共设备使用后请及时退出登录。</li>
          <li>普通用户不能访问管理员用户管理。</li>
          <li>数据页面请结合来源、更新时间和统计口径理解结果。</li>
          <li>账号或数据异常时请联系管理员核查。</li>
        </ul>
      </section>
    </section>
  </PageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import PageShell from '@/components/common/PageShell.vue';
import UnifiedNav from '@/components/common/UnifiedNav.vue';
import PageHero from '@/components/common/PageHero.vue';
import SectionHeader from '@/components/common/SectionHeader.vue';
import ActionButton from '@/components/common/ActionButton.vue';
import ActionToast from '@/components/common/ActionToast.vue';
import AvatarUploader from '@/components/common/AvatarUploader.vue';
import { useAuth } from '@/composables/useAuth';
import { getUserInfo, updateUserInfo, uploadAvatar } from '@/services/userService';

const router = useRouter();
const auth = useAuth();
const form = reactive({ id: null, name: '', username: '', email: '', birthdate: '', password: '', avatar: '', role: '', lastLoginAt: '' });
const avatarFile = ref(null);
const isSaving = ref(false);
const isUploading = ref(false);
const toast = reactive({ visible: false, type: 'info', message: '' });
const PASSWORD_RULE_MESSAGE = '请确保新密码不少于 8 位，并同时包含英文字母和数字。';

function isStrongPassword(value) {
  return typeof value === 'string' && value.length >= 8 && /[A-Za-z]/.test(value) && /\d/.test(value) && !/\s/.test(value);
}

const avatarText = computed(() => (form.name || 'H').slice(0, 1).toUpperCase());
const avatarStyle = computed(() => form.avatar ? { backgroundImage: `url(${normalizeAvatar(form.avatar)})`, color: 'transparent' } : {});
const roleDisplay = computed(() => (String(form.role || '').toLowerCase().includes('admin') ? '管理员' : '普通用户'));

function show(message, type = 'info') { toast.message = message; toast.type = type; toast.visible = true; }
function normalizeAvatar(value) { return !value ? '' : (String(value).startsWith('data:') ? value : `data:image/png;base64,${value}`); }
function mapProfile(data = {}) {
  return {
    id: data.id ?? null,
    name: data.name || data.username || '',
    username: data.username || data.name || '',
    email: data.email || '',
    birthdate: data.birthdate || data.birth || data.birthday || '',
    password: '',
    avatar: data.avatarBase64 || data.avatar || '',
    role: data.role || data.userType || 'User',
    lastLoginAt: data.lastLoginAt || data.last_login_at || '',
  };
}
async function loadProfile() {
  if (!auth.isAuthenticated.value) { router.push('/login'); return; }
  try {
    const data = await getUserInfo();
    Object.assign(form, mapProfile(data));
    avatarFile.value = null;
    show('个人资料已加载', 'success');
  } catch (error) {
    show(error.message || '个人资料加载失败', 'error');
    if (error.status === 401) router.push('/login');
  }
}
async function saveProfile() {
  if (form.password && !isStrongPassword(form.password)) {
    show(PASSWORD_RULE_MESSAGE, 'error');
    return;
  }
  isSaving.value = true;
  try {
    const payload = { name: form.name, email: form.email, birthdate: form.birthdate };
    if (form.password) payload.password = form.password;
    await updateUserInfo(payload);
    form.password = '';
    await auth.refreshUser().catch(() => null);
    show('个人资料已保存', 'success');
  } catch (error) {
    show(error.message || '资料保存失败', 'error');
    if (error.status === 401) router.push('/login');
  } finally { isSaving.value = false; }
}
function onAvatarSelect(file) { avatarFile.value = file; show(`已选择头像文件：${file.name}`, 'info'); }
async function submitAvatar() {
  if (!avatarFile.value) { show('请先选择头像文件', 'warning'); return; }
  isUploading.value = true;
  try {
    await uploadAvatar(avatarFile.value);
    avatarFile.value = null;
    show('头像已上传', 'success');
    await loadProfile();
  } catch (error) {
    show(error.message || '头像上传失败', 'error');
  } finally { isUploading.value = false; }
}
onMounted(loadProfile);
</script>

<style scoped>
.account-page { width: var(--hn-page); margin: 0 auto 48px; display: grid; gap: 16px; }
.account-card, .edit-card, .tips-card { padding: 18px; border: 1px solid var(--hn-border); border-radius: var(--hn-radius); background: var(--hn-card); box-shadow: var(--hn-shadow-soft); }
.account-card { display: grid; grid-template-columns: auto minmax(0, 1fr) minmax(280px, 0.8fr); gap: 18px; align-items: center; }
.avatar { width: 86px; height: 86px; border-radius: 50%; display: grid; place-items: center; background: var(--hn-accent); color: #fff; font-size: 34px; font-weight: 900; background-size: cover; background-position: center; }
.eyebrow { margin: 0 0 6px; color: var(--hn-accent); font-size: 12px; font-weight: 900; text-transform: uppercase; }
h2, p { margin: 0; color: var(--hn-text); }
.account-main p { color: var(--hn-muted); margin-top: 5px; }
.account-meta { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin: 0; }
.account-meta div { padding: 10px; border: 1px solid var(--hn-border); border-radius: 8px; background: var(--hn-soft); }
dt { color: var(--hn-muted); font-size: 12px; font-weight: 800; } dd { margin: 4px 0 0; color: var(--hn-text); font-weight: 800; word-break: break-word; }
.profile-workspace { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(300px, 0.65fr); gap: 16px; align-items: stretch; }
.field-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
label { display: grid; gap: 7px; }
label span { color: var(--hn-muted); font-size: 12px; font-weight: 800; }
.field-hint { margin: 2px 0 0; color: var(--hn-muted); font-size: 11px; font-weight: 700; line-height: 1.5; }
input { height: 42px; border: 1px solid var(--hn-border-strong); border-radius: 7px; background: var(--hn-control-bg); color: var(--hn-text); padding: 0 12px; }
.action-row { display: flex; gap: 10px; margin-top: 14px; flex-wrap: wrap; }
.avatar-card { align-content: start; }
.tips-card ul { margin: 0; padding-left: 20px; display: grid; gap: 8px; color: var(--hn-muted); line-height: 1.55; }
@media (max-width: 980px) { .account-card, .profile-workspace { grid-template-columns: 1fr; } .account-meta { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .field-grid { grid-template-columns: 1fr; } .account-card { align-items: flex-start; } }
</style>
