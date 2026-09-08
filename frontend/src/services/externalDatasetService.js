import { apiFetch } from "@/services/httpClient";

const API_BASE = (process.env.VUE_APP_API_BASE || "").replace(/\/$/, "");
const ROOT = `${API_BASE}/api/v2/external-datasets`;

function query(params = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") search.set(key, String(value));
  });
  const suffix = search.toString();
  return suffix ? `?${suffix}` : "";
}

export function fetchExternalDatasets() {
  return apiFetch(ROOT);
}

export function fetchExternalDatasetTables(datasetId) {
  return apiFetch(`${ROOT}/${encodeURIComponent(datasetId)}/tables`);
}

export function fetchExternalDatasetSchema(datasetId, params) {
  return apiFetch(`${ROOT}/${encodeURIComponent(datasetId)}/schema${query(params)}`);
}

export function fetchExternalDatasetRecords(datasetId, params) {
  return apiFetch(`${ROOT}/${encodeURIComponent(datasetId)}/records${query(params)}`);
}
