import { useCallback, useEffect, useState } from 'react';
import { api, describeError, onUnauthorized, readSession, writeSession } from '../api/client';

/**
 * Sign-in state for the single demo account. The token lives in localStorage, so a page reload
 * keeps the session until it expires; the response interceptor logs the app out on a 401.
 */
export function useAuth() {
  const [session, setSession] = useState(() => readSession());
  const [error, setError] = useState('');
  const [pending, setPending] = useState(false);

  useEffect(() => onUnauthorized(() => setSession(null)), []);

  const login = useCallback(async (username, password) => {
    setPending(true);
    setError('');
    try {
      const { data } = await api.post('/auth/login', { username, password });
      const next = {
        token: data.token,
        role: data.role,
        username,
        expiresAt: data.expiresAt,
      };
      writeSession(next);
      setSession(next);
      return true;
    } catch (requestError) {
      setError(describeError(requestError, 'Sign in failed'));
      return false;
    } finally {
      setPending(false);
    }
  }, []);

  const logout = useCallback(() => {
    writeSession(null);
    setSession(null);
  }, []);

  return {
    isAuthenticated: Boolean(session?.token),
    session,
    error,
    pending,
    login,
    logout,
  };
}
