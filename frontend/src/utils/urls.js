function normalizeBase(value) {
  const trimmed = String(value || '').trim();
  if (!trimmed || trimmed === '/') return '';
  return `/${trimmed.replace(/^\/+|\/+$/g, '')}`;
}

const apiBase = normalizeBase(process.env.VUE_APP_API_BASE);
const publicBase = normalizeBase(process.env.BASE_URL);

export function apiUrl(path = '') {
  const value = String(path || '');
  if (!apiBase || /^(?:https?:)?\/\//i.test(value)) return value;
  if (value === apiBase || value.startsWith(`${apiBase}/`)) return value;
  return `${apiBase}/${value.replace(/^\/+/, '')}`;
}

export function publicUrl(path = '') {
  const value = String(path || '');
  if (/^(?:https?:)?\/\//i.test(value)) return value;
  return `${publicBase}/${value.replace(/^\/+/, '')}` || '/';
}
