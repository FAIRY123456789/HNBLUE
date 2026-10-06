<template>
  <header :class="['unified-nav-wrap', variant]">
    <div class="nav-inner">
      <router-link class="nav-brand" to="/">HNBLUE</router-link>
      <nav class="nav-pill nav-links" aria-label="主导航">
        <router-link v-for="item in items" :key="item.to" :to="item.to">{{ item.label }}</router-link>
        <router-link v-if="isAuthenticated" class="account-inline" :to="accountPath">{{ accountLabel }}</router-link>
      </nav>
      <div class="nav-tools nav-actions">
        <div v-if="showTheme" ref="themePicker" class="theme-picker">
          <button
            class="nav-theme"
            type="button"
            aria-haspopup="menu"
            :aria-expanded="themeMenuOpen"
            @click.stop="themeMenuOpen = !themeMenuOpen"
          >
            主题 <span class="theme-caret" :class="{ open: themeMenuOpen }" aria-hidden="true">▾</span>
          </button>
          <div v-if="themeMenuOpen" class="theme-menu" role="menu" aria-label="选择页面主题">
            <button type="button" role="menuitemradio" :aria-checked="theme === 'light'" @click="setTheme('light')">
              <span>浅色主题</span><b v-if="theme === 'light'">✓</b>
            </button>
            <button type="button" role="menuitemradio" :aria-checked="theme === 'dark'" @click="setTheme('dark')">
              <span>深色主题</span><b v-if="theme === 'dark'">✓</b>
            </button>
          </div>
        </div>
        <router-link v-if="!isAuthenticated" class="login-link" to="/login">登录</router-link>
        <template v-else>
          <router-link class="login-link account-link" :to="accountPath">{{ accountLabel }}</router-link>
          <button class="nav-theme logout-button" type="button" @click="handleLogout">退出</button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useAuth } from '@/composables/useAuth';

const props = defineProps({
  variant: { type: String, default: 'light' },
  showTheme: { type: Boolean, default: true },
});

const router = useRouter();
const auth = useAuth();
const THEME_KEY = 'hnblue_site_theme';
const theme = ref(localStorage.getItem(THEME_KEY) || 'light');
const themeMenuOpen = ref(false);
const themePicker = ref(null);
const items = [
  { label: '首页', to: '/' },
  { label: '碳溯', to: '/carbonseek' },
  { label: '数据资产', to: '/v2-public-data' },
  { label: '治理工作台', to: '/v2-government' },
  { label: '关于项目', to: '/about' },
];

const isAuthenticated = computed(() => auth.isAuthenticated.value);
const accountPath = computed(() => (auth.isAdmin.value ? '/usermanage' : '/userinfo'));
const accountLabel = computed(() => (auth.isAdmin.value ? '用户管理' : '个人中心'));

function applyTheme(value) {
  const normalized = value === 'dark' ? 'dark' : 'light';
  document.documentElement.dataset.hnTheme = normalized;
  document.documentElement.classList.toggle('theme-dark', normalized === 'dark');
  document.documentElement.classList.toggle('theme-light', normalized !== 'dark');
  localStorage.setItem(THEME_KEY, normalized);
}

function setTheme(value) {
  theme.value = value === 'dark' ? 'dark' : 'light';
  themeMenuOpen.value = false;
}

function closeThemeMenu(event) {
  if (!themePicker.value?.contains(event.target)) themeMenuOpen.value = false;
}

function handleLogout() {
  auth.logout();
  router.push('/login');
}

watch(theme, applyTheme);
onMounted(() => {
  auth.restoreSession();
  applyTheme(theme.value);
  document.addEventListener('click', closeThemeMenu);
});
onBeforeUnmount(() => document.removeEventListener('click', closeThemeMenu));
</script>

<style scoped>
.unified-nav-wrap {
  position: relative;
  z-index: 30;
  width: 100%;
  min-height: 76px;
  padding: 16px clamp(16px, 4vw, 64px);
}

.nav-inner {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto minmax(220px, 1fr);
  align-items: center;
  gap: 16px;
}

.nav-brand {
  justify-self: start;
  color: var(--hn-accent);
  font-size: 21px;
  font-weight: 900;
  letter-spacing: 0;
}

.nav-pill {
  justify-self: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: fit-content;
  max-width: 100%;
  padding: 6px;
  border: 1px solid var(--hn-border);
  border-radius: 999px;
  background: var(--hn-nav-bg);
  box-shadow: var(--hn-shadow-soft);
  backdrop-filter: blur(14px);
}

.nav-actions {
  justify-self: end;
}

.nav-pill a,
.login-link,
.nav-theme {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-radius: 999px;
  color: var(--hn-text);
  font-size: 15px;
  font-weight: 800;
  white-space: nowrap;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.nav-pill a:hover,
.nav-pill a.router-link-active,
.login-link:hover,
.login-link.router-link-active,
.nav-theme:hover {
  background: var(--hn-accent);
  color: #fff;
}

.nav-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.theme-picker {
  position: relative;
}

.theme-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  z-index: 60;
  width: 148px;
  display: grid;
  gap: 4px;
  padding: 6px;
  border: 1px solid var(--hn-border);
  border-radius: 10px;
  background: var(--hn-panel-solid);
  box-shadow: var(--hn-shadow);
}

.theme-menu button {
  min-height: 38px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 11px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--hn-text);
  cursor: pointer;
  font-family: inherit;
  font-weight: 800;
}

.theme-menu button:hover,
.theme-menu button[aria-checked="true"] {
  background: var(--hn-soft);
  color: var(--hn-accent);
}

.nav-theme,
.login-link {
  border: 1px solid var(--hn-border);
  background: var(--hn-control-bg);
  cursor: pointer;
  font-family: inherit;
}

.nav-theme { gap: 4px; }

.theme-caret {
  display: inline-grid;
  place-items: center;
  width: 16px;
  height: 16px;
  font-size: 16px;
  font-weight: 900;
  line-height: 1;
  transform-origin: center;
  transition: transform 0.18s ease;
}

.theme-caret.open { transform: rotate(180deg); }

.logout-button {
  color: var(--hn-danger);
}

.dark .nav-brand {
  color: #fff;
}

.dark .nav-pill,
.dark .login-link,
.dark .nav-theme {
  border-color: rgba(255, 255, 255, 0.18);
  background: rgba(5, 37, 33, 0.38);
  box-shadow: 0 18px 38px rgba(0, 0, 0, 0.16);
}

.dark .nav-pill a,
.dark .login-link,
.dark .nav-theme {
  color: rgba(255, 255, 255, 0.88);
}

.dark .nav-pill a:hover,
.dark .nav-pill a.router-link-active,
.dark .login-link:hover,
.dark .login-link.router-link-active,
.dark .nav-theme:hover {
  background: rgba(255, 255, 255, 0.92);
  color: #0b5448;
}

.account-inline {
  display: none !important;
}

@media (max-width: 900px) {
  .unified-nav-wrap {
    padding: 14px 12px;
  }

  .nav-inner {
    grid-template-columns: 1fr;
    justify-items: center;
  }

  .nav-brand,
  .nav-actions {
    justify-self: center;
  }

  .nav-pill {
    overflow-x: auto;
    justify-content: flex-start;
    width: min(100%, 680px);
  }

  .nav-pill a,
  .login-link,
  .nav-theme {
    padding: 0 12px;
  }

  .account-link {
    display: none;
  }

  .account-inline {
    display: inline-flex !important;
  }
}
</style>
