const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const TOKEN_KEY = 'visuart_token';

// "Ricordami": token in localStorage (resta), altrimenti in sessionStorage (sparisce chiudendo il browser)
function getToken() {
  return localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY);
}

export function setToken(token, remember = true) {
  localStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(TOKEN_KEY);
  if (token) {
    (remember ? localStorage : sessionStorage).setItem(TOKEN_KEY, token);
  }
}

export function isRemembered() {
  return localStorage.getItem(TOKEN_KEY) !== null;
}

export function imgUrl(path) {
  if (!path) {
    return null;
  }
  return path.startsWith('http') ? path : `${BASE_URL}${path}`;
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (!res.ok) {
    let message = `Errore ${res.status}`;
    try {
      const body = await res.json();
      message = body.error || message;
    } catch {
      // risposta senza corpo JSON
    }
    const error = new Error(message);
    error.status = res.status;
    throw error;
  }

  if (res.status === 204) {
    return null;
  }
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  get: (path) => request(path),
  post: (path, body) =>
    request(path, { method: 'POST', body: body !== undefined ? JSON.stringify(body) : undefined }),
  put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: (path, body) => request(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path, body) =>
    request(path, { method: 'DELETE', body: body !== undefined ? JSON.stringify(body) : undefined }),
};

export const auth = {
  login: (email, password) => api.post('/api/auth/login', { email, password }),
  logout: () => api.post('/api/auth/logout'),
  refresh: () => api.post('/api/auth/refresh'),
  forgotPassword: (email) => api.post('/api/auth/forgot-password', { email }),
  resetPassword: (token, password) => api.post('/api/auth/reset-password', { token, password }),
  register: (email, password, displayName, username) =>
    api.post('/api/auth/register', { email, password, displayName, username: username || undefined }),
};

export const works = {
  list: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    return api.get(`/api/works${qs ? `?${qs}` : ''}`);
  },
  get: (id) => api.get(`/api/works/${id}`),
  create: (data) => api.post('/api/works', data),
};

export const connections = {
  list: (categorySlug, sort) => {
    const qs = new URLSearchParams();
    if (categorySlug) qs.set('category', categorySlug);
    if (sort) qs.set('sort', sort);
    const s = qs.toString();
    return api.get(`/api/connections${s ? `?${s}` : ''}`);
  },
  get: (id) => api.get(`/api/connections/${id}`),
  create: (data) => api.post('/api/connections', data),
  registerView: (id) => api.post(`/api/connections/${id}/view`),
  search: (params = {}) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') qs.set(k, v);
    });
    return api.get(`/api/connections/search?${qs.toString()}`);
  },
};

export const account = {
  me: () => api.get('/api/users/me'),
  update: (displayName, bio) => api.put('/api/users/me', { displayName, bio }),
  changePassword: (currentPassword, newPassword) =>
    api.put('/api/users/me/password', { currentPassword, newPassword }),
  stats: () => api.get('/api/users/me/stats'),
  emailStats: () => api.post('/api/users/me/stats/email'),
  remove: (password) => api.delete('/api/users/me', { password }),
};

export const users = {
  search: (q) => api.get(`/api/users/search?q=${encodeURIComponent(q)}`),
  profile: (username) => api.get(`/api/users/${encodeURIComponent(username)}/profile`),
};

export const notifications = {
  list: () => api.get('/api/notifications'),
  unreadCount: () => api.get('/api/notifications/unread-count'),
  readAll: () => api.post('/api/notifications/read-all'),
};

export const admin = {
  users: (q = '', page = 0) =>
    api.get(`/api/admin/users?q=${encodeURIComponent(q)}&page=${page}&size=20`),
  changeRole: (id, role) => api.patch(`/api/admin/users/${id}/role`, { role }),
  remove: (id) => api.delete(`/api/admin/users/${id}`),
};

export const likes = {
  get: (connectionId) => api.get(`/api/connections/${connectionId}/likes`),
  add: (connectionId) => api.post(`/api/connections/${connectionId}/likes`),
  remove: (connectionId) => api.delete(`/api/connections/${connectionId}/likes`),
};

export const chat = {
  conversations: () => api.get('/api/chat/conversations'),
  create: (recipientUsername) => api.post('/api/chat/conversations', { recipientUsername }),
  history: (withUsername, before) =>
    api.get(`/api/chat/history?with=${encodeURIComponent(withUsername)}${before ? `&before=${before}` : ''}`),
  unreadCount: () => api.get('/api/chat/unread-count'),
  send: (recipient, text, connectionId) => api.post('/api/chat/messages', { recipient, text, connectionId }),
  suggest: (conversationId) => api.post('/api/chat/suggest', { conversationId }),
};

export function wsUrl() {
  return BASE_URL.replace(/^http/, 'ws') + '/ws';
}

export function currentToken() {
  return getToken();
}

export const graph = {
  get: ({ workId, depth = 2, category } = {}) => {
    const params = new URLSearchParams();
    if (workId) params.set('workId', workId);
    if (depth) params.set('depth', depth);
    if (category) params.set('category', category);
    return api.get(`/api/graph?${params.toString()}`);
  },
};

export const categories = {
  list: () => api.get('/api/categories'),
};

export const favorites = {
  list: () => api.get('/api/favorites'),
  add: (workId) => api.post(`/api/favorites/${workId}`),
  remove: (workId) => api.delete(`/api/favorites/${workId}`),
};

export const affinities = {
  get: (workId, limit = 3) => api.get(`/api/works/${workId}/affinities?limit=${limit}`),
};

export const comments = {
  list: (connectionId) => api.get(`/api/connections/${connectionId}/comments`),
  add: (connectionId, text) => api.post(`/api/connections/${connectionId}/comments`, { text }),
  edit: (connectionId, id, text) => api.put(`/api/connections/${connectionId}/comments/${id}`, { text }),
  remove: (connectionId, id) => api.delete(`/api/connections/${connectionId}/comments/${id}`),
};

export const ai = {
  analyze: (connectionId) => api.get(`/api/connections/${connectionId}/ai-analysis`),
  discover: (q, type = 'FILM') =>
    api.get(`/api/ai/discover?q=${encodeURIComponent(q)}&type=${type}`),
};

export const externalSearch = {
  tmdb: (q) => api.get(`/api/search/tmdb?q=${encodeURIComponent(q)}`),
  met: (q) => api.get(`/api/search/met?q=${encodeURIComponent(q)}`),
  artworks: (q) => api.get(`/api/search/artworks?q=${encodeURIComponent(q)}`),
  importExternal: (data) => api.post('/api/import/external', data),
};
