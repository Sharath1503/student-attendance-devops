import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, setUnauthorizedHandler, storage } from './api.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => (storage.getToken() ? storage.getUser() : null));
  const [notice, setNotice] = useState('');

  const expire = useCallback((message) => {
    storage.clear();
    setUser(null);
    setNotice(message || '');
  }, []);

  // Any 401 from a protected call (expired / invalid token) ends the session.
  useEffect(() => {
    setUnauthorizedHandler(() => expire('Your session has expired. Please sign in again.'));
  }, [expire]);

  const login = useCallback(async (username, password) => {
    const data = await api.login(username, password);
    storage.save(data.token, data.user);
    setNotice('');
    setUser(data.user);
    return data.user;
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } catch {
      /* the token is discarded either way */
    }
    storage.clear();
    setUser(null);
    setNotice('You have been signed out.');
  }, []);

  const value = useMemo(() => ({ user, notice, login, logout }), [user, notice, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
