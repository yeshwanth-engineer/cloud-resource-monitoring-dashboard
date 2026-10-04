import axios from 'axios';

/**
 * Single axios instance for the whole app.
 *
 * The base URL is same-origin by default (`/api`), which the Vite dev server and the container's
 * nginx both proxy to the backend. Set VITE_API_BASE to an absolute URL to call a remote API.
 */
const SESSION_STORAGE_KEY = 'cloud-monitor.session';
const UNAUTHORIZED_EVENT = 'cloud-monitor:unauthorized';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20000,
});

/** Reads the persisted session, tolerating a corrupted or hand-edited value. */
export function readSession() {
  try {
    const raw = window.localStorage.getItem(SESSION_STORAGE_KEY);
    const session = raw ? JSON.parse(raw) : null;
    return session?.token ? session : null;
  } catch {
    return null;
  }
}

export function writeSession(session) {
  if (session) {
    window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
  } else {
    window.localStorage.removeItem(SESSION_STORAGE_KEY);
  }
}

api.interceptors.request.use((config) => {
  const session = readSession();
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});

// A token can expire between two clicks. Broadcast it so the app can return to the login screen
// instead of showing an empty dashboard.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && readSession()) {
      writeSession(null);
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    return Promise.reject(error);
  },
);

export function onUnauthorized(listener) {
  window.addEventListener(UNAUTHORIZED_EVENT, listener);
  return () => window.removeEventListener(UNAUTHORIZED_EVENT, listener);
}

/** Prefer the message the API sends in its error body; fall back to something readable. */
export function describeError(error, fallback = 'Request failed') {
  const body = error?.response?.data;
  if (typeof body === 'string' && body.trim()) {
    return body;
  }
  return body?.message || error?.message || fallback;
}
