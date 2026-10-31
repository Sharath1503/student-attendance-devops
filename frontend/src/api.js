// Thin fetch wrapper around the Spring Boot REST API. Every call goes to /api/** on the same origin.

const TOKEN_KEY = 'attendance.token';
const USER_KEY = 'attendance.user';

// sessionStorage: the login is forgotten when the browser tab is closed.
export const storage = {
  getToken: () => sessionStorage.getItem(TOKEN_KEY),
  getUser: () => {
    try {
      return JSON.parse(sessionStorage.getItem(USER_KEY) || 'null');
    } catch {
      return null;
    }
  },
  save: (token, user) => {
    sessionStorage.setItem(TOKEN_KEY, token);
    sessionStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear: () => {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
  },
};

export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

let onUnauthorized = () => {};
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn;
}

async function request(method, path, body, { auth = true } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = storage.getToken();
  if (auth && token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`/api${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check your connection and try again.');
  }

  if (res.status === 204) return null;
  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }
  if (!res.ok) {
    if (res.status === 401 && auth) onUnauthorized();
    throw new ApiError(res.status, data?.message || `Request failed (${res.status})`, data?.fieldErrors || {});
  }
  return data;
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body ?? {}),
  put: (path, body) => request('PUT', path, body),
  del: (path) => request('DELETE', path),
  // login must not trigger the "session expired" handler on a wrong password
  login: (username, password) => request('POST', '/auth/login', { username, password }, { auth: false }),
};
