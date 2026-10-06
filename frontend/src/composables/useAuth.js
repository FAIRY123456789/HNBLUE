import { reactive, computed } from 'vue';

import { apiUrl } from '@/utils/urls';

const TOKEN_KEY = 'token';
const ROLE_KEY = 'userType';
const USER_KEY = 'hnblue_user_info';

const state = reactive({
  token: '',
  role: '',
  userInfo: null,
  username: '',
  restored: false,
});

function normalizeRole(role) {
  const value = String(role || '').toLowerCase();
  if (value === 'admin') return 'Admin';
  if (value === 'user') return 'User';
  return '';
}

function persistSession(token, role, userInfo = undefined) {
  state.token = token || '';
  state.role = normalizeRole(role);
  if (!state.token) state.userInfo = null;
  else if (userInfo !== undefined) state.userInfo = userInfo;
  state.username = state.userInfo?.name || state.userInfo?.username || '';
  if (state.token) localStorage.setItem(TOKEN_KEY, state.token);
  else localStorage.removeItem(TOKEN_KEY);
  if (state.role) localStorage.setItem(ROLE_KEY, state.role);
  else localStorage.removeItem(ROLE_KEY);
  if (state.userInfo) localStorage.setItem(USER_KEY, JSON.stringify(state.userInfo));
  else localStorage.removeItem(USER_KEY);
  window.dispatchEvent(new CustomEvent('hnblue-auth-changed'));
}

export function useAuth() {
  function restoreSession() {
    state.token = localStorage.getItem(TOKEN_KEY) || '';
    state.role = normalizeRole(localStorage.getItem(ROLE_KEY));
    try {
      state.userInfo = JSON.parse(localStorage.getItem(USER_KEY) || 'null');
    } catch {
      state.userInfo = null;
    }
    state.username = state.userInfo?.name || state.userInfo?.username || '';
    state.restored = true;
    return state;
  }

  async function login(credentials) {
    const response = await fetch(apiUrl('/api/login'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials),
    });
    const contentType = response.headers.get('content-type') || '';
    const payload = contentType.includes('application/json') ? await response.json() : await response.text();
    if (!response.ok) {
      const message = typeof payload === 'string' ? payload : payload?.message;
      const error = new Error(message || '登录失败');
      error.status = response.status;
      error.payload = payload;
      throw error;
    }
    persistSession(payload.token, payload.userType, { name: credentials.username, username: credentials.username });
    return payload;
  }

  function logout() {
    persistSession('', '', null);
  }

  async function refreshUser() {
    if (!state.token) return null;
    const response = await fetch(apiUrl('/user/info'), { headers: authHeaders() });
    if (response.status === 401) {
      logout();
      throw new Error('登录状态已失效');
    }
    if (!response.ok) throw new Error('无法获取用户信息');
    const data = await response.json();
    persistSession(state.token, state.role, data);
    return data;
  }

  function hasRole(roles = []) {
    const normalized = roles.map(normalizeRole);
    if (!normalized.length) return true;
    if (normalized.includes(state.role)) return true;
    return state.role === 'Admin' && normalized.includes('User');
  }

  function authHeaders(extra = {}) {
    return state.token ? { ...extra, Authorization: `Bearer ${state.token}` } : { ...extra };
  }

  if (!state.restored) restoreSession();

  return {
    state,
    token: computed(() => state.token),
    role: computed(() => state.role),
    username: computed(() => state.username),
    userInfo: computed(() => state.userInfo),
    isAuthenticated: computed(() => Boolean(state.token)),
    isAdmin: computed(() => state.role === 'Admin'),
    restoreSession,
    login,
    logout,
    refreshUser,
    hasRole,
    authHeaders,
  };
}
