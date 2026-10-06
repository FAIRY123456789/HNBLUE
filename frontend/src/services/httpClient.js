import { useAuth } from '@/composables/useAuth';

import { apiUrl } from '@/utils/urls';

function normalizeError(payload, fallback) {
  if (!payload) return fallback;
  if (typeof payload === 'string') return payload;
  return payload.message || payload.error || fallback;
}

export async function apiFetch(url, options = {}) {
  const auth = useAuth();
  const headers = { ...(options.headers || {}) };
  const hasBody = options.body !== undefined && !(options.body instanceof FormData);
  if (hasBody && !headers['Content-Type']) headers['Content-Type'] = 'application/json';
  const response = await fetch(apiUrl(url), {
    ...options,
    headers: auth.authHeaders(headers),
  });
  const contentType = response.headers.get('content-type') || '';
  let payload = null;
  if (contentType.includes('application/json')) payload = await response.json();
  else payload = await response.text();
  if (!response.ok) {
    if (response.status === 401) auth.logout();
    const error = new Error(normalizeError(payload, `请求失败：${response.status}`));
    error.status = response.status;
    error.payload = payload;
    throw error;
  }
  return payload;
}
