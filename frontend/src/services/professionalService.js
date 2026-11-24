import { apiFetch } from '@/services/httpClient';

export function listProfessionals({ keyword = '', page = 0, size = 20 } = {}) {
  const query = new URLSearchParams({ keyword, page: String(page), size: String(size) });
  return apiFetch(`/admin/professionals?${query}`);
}

export function createProfessional(username) {
  return apiFetch('/admin/professionals', { method: 'POST', body: JSON.stringify({ username }) });
}

export function createProfessionalBatch(prefix, count) {
  return apiFetch('/admin/professionals/batch', { method: 'POST', body: JSON.stringify({ prefix, count }) });
}

export function setProfessionalEnabled(id, enabled) {
  return apiFetch(`/admin/professionals/${id}/status`, { method: 'PUT', body: JSON.stringify({ enabled }) });
}

export function resetProfessionalPassword(id) {
  return apiFetch(`/admin/professionals/${id}/reset-password`, { method: 'POST' });
}

export function updateProfessionalEntitlement(id, entitlement) {
  return apiFetch(`/admin/professionals/${id}/entitlement`, { method: 'PUT', body: JSON.stringify(entitlement) });
}

export function changeProfessionalPassword(currentPassword, newPassword) {
  return apiFetch('/api/professional/change-password', {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}
