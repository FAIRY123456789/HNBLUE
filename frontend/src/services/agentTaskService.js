import { apiFetch } from '@/services/httpClient';

export function submitAgentTask(payload, idempotencyKey) {
  return apiFetch('/api/agent/tasks', {
    method: 'POST',
    headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {},
    body: JSON.stringify(payload),
  });
}

export function getAgentTask(taskId) {
  return apiFetch(`/api/agent/tasks/${encodeURIComponent(taskId)}`);
}

export function cancelAgentTask(taskId) {
  return apiFetch(`/api/agent/tasks/${encodeURIComponent(taskId)}/cancel`, { method: 'POST' });
}
