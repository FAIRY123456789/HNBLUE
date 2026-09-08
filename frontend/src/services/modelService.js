import { apiFetch } from '@/services/httpClient';

export function checkModelHealth() {
  return apiFetch('/api/model/health');
}

export function predictCarbon(payload) {
  return apiFetch('/api/model/predict-carbon', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function predictCarbonBatch(payload) {
  return apiFetch('/api/model/predict-carbon/batch', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
