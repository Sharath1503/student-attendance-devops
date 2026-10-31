// Small pure helpers (unit-tested in utils.test.js)

/** 66.666 -> "66.67%", null -> "N/A" (no classes recorded yet). */
export function formatPercent(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return 'N/A';
  return `${Number(value).toFixed(2)}%`;
}

/** Colour tone for a percentage: 'na' (no data), 'low' (below threshold) or 'ok'. */
export function percentTone(value, threshold = 75) {
  if (value === null || value === undefined) return 'na';
  return Number(value) < threshold ? 'low' : 'ok';
}

/** Today as YYYY-MM-DD in the browser's local time zone. */
export function todayISO(offsetDays = 0) {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** Builds "?a=1&b=2" from an object, skipping empty values. */
export function toQuery(params = {}) {
  const parts = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`);
  return parts.length ? `?${parts.join('&')}` : '';
}

/** Where each role lands after login. */
export function homePathFor(role) {
  switch (role) {
    case 'ADMIN':
      return '/admin';
    case 'FACULTY':
      return '/faculty';
    case 'STUDENT':
      return '/student';
    default:
      return '/login';
  }
}

/** Basic e-mail shape check used by form validation (the server validates again). */
export function isEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value || '').trim());
}
