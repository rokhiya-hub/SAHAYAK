import { createContext, useContext, useMemo, useState } from 'react';
import apiClients, { setAuthToken } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setTokenState] = useState(null);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);

  const setAuth = (nextToken, nextUser) => {
    setTokenState(nextToken);
    setUser(nextUser);
    setAuthToken(nextToken);
  };

  const login = async (username, password) => {
    setLoading(true);
    try {
      const response = await apiClients.shelter.post('/api/auth/login', { username, password });
      const payload = response.data;
      setAuth(payload.token, {
        username: payload.username,
        role: payload.role,
      });
      return payload;
    } finally {
      setLoading(false);
    }
  };

  const register = async (username, password) => {
    setLoading(true);
    try {
      const response = await apiClients.shelter.post('/api/auth/register', { username, password });
      const payload = response.data;
      setAuth(payload.token, {
        username: payload.username,
        role: payload.role,
      });
      return payload;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    setAuth(null, null);
  };

  const value = useMemo(
    () => ({
      token,
      user,
      loading,
      isAuthenticated: Boolean(token),
      login,
      register,
      logout,
    }),
    [token, user, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}

// Note: token is intentionally kept in memory only to avoid storing a JWT in localStorage,
// which is vulnerable to XSS-based theft and increases the blast radius of a browser compromise.
