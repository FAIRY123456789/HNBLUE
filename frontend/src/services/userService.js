import { apiFetch } from '@/services/httpClient';

export function getUserInfo() {
  return apiFetch('/user/info');
}

export function updateUserInfo(payload) {
  return apiFetch('/user/updateInfo', {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function uploadAvatar(file) {
  const form = new FormData();
  form.append('file', file);
  return apiFetch('/user/avatar', { method: 'POST', body: form });
}
