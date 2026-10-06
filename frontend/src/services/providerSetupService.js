import { apiFetch } from '@/services/httpClient';

const SETUP_ENDPOINT = '/api/internal/provider-setup';

export function getProviderSetupStatus() {
  return apiFetch(SETUP_ENDPOINT);
}

export function initializeProviderKey(apiKey) {
  return apiFetch(SETUP_ENDPOINT, {
    method: 'POST',
    body: JSON.stringify({ apiKey }),
  });
}
