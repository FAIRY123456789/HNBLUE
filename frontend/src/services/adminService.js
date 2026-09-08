import { apiFetch } from '@/services/httpClient';

export function listUsers({ page = 0, size = 10, keyword = '' } = {}) {
  const path = keyword ? `/admin/search?page=${page}&size=${size}&keyword=${encodeURIComponent(keyword)}` : `/admin/users?page=${page}&size=${size}`;
  return apiFetch(path);
}

export function addUser(payload) {
  return apiFetch('/admin/add', { method: 'POST', body: JSON.stringify(payload) });
}

export function updateUser(payload) {
  return apiFetch('/admin/update', { method: 'PUT', body: JSON.stringify(payload) });
}

export function deleteUser(id) {
  return apiFetch(`/admin/delete/${id}`, { method: 'DELETE' });
}

export function deleteUsers(ids) {
  return apiFetch('/admin/delete', { method: 'DELETE', body: JSON.stringify(ids) });
}

export function getUserDetail(id) {
  return apiFetch(`/admin/users/${id}`);
}
