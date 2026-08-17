import React, { createContext, useContext, useState, useEffect } from 'react';
import { authService } from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('aapdasetu_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [loading, setLoading] = useState(false);
  const [initializing, setInitializing] = useState(true);
  const [toast, setToast] = useState(null);
  const [backendOnline, setBackendOnline] = useState(false);
  const [viewMode, setViewMode] = useState('both'); // 'both' | 'login' | 'register'

  const showToast = (message, type = 'info') => {
    setToast({ message, type });
    setTimeout(() => {
      setToast(null);
    }, 4500);
  };

  const checkBackend = async () => {
    const health = await authService.checkHealth();
    setBackendOnline(!!health);
  };

  // Verify active session on load
  useEffect(() => {
    const initAuth = async () => {
      await checkBackend();
      const token = localStorage.getItem('aapdasetu_token');
      if (token) {
        const profile = await authService.getMe();
        if (profile?.user) {
          setUser(profile.user);
          localStorage.setItem('aapdasetu_user', JSON.stringify(profile.user));
        }
      }
      setInitializing(false);
    };

    initAuth();
    const interval = setInterval(checkBackend, 10000);
    return () => clearInterval(interval);
  }, []);

  const login = async (email, password, rememberMe = true) => {
    setLoading(true);
    try {
      const data = await authService.login(email, password);
      
      if (data.token) {
        if (rememberMe) {
          localStorage.setItem('aapdasetu_token', data.token);
        } else {
          sessionStorage.setItem('aapdasetu_token', data.token);
        }
      }

      if (data.user) {
        setUser(data.user);
        localStorage.setItem('aapdasetu_user', JSON.stringify(data.user));
      }

      showToast(`Welcome back, ${data.user?.username || 'User'}! Login successful.`, 'success');
      return { success: true, data };
    } catch (err) {
      showToast(typeof err === 'string' ? err : 'Invalid login credentials', 'error');
      return { success: false, error: err };
    } finally {
      setLoading(false);
    }
  };

  const register = async (fullName, email, password) => {
    setLoading(true);
    try {
      const data = await authService.register(fullName, email, password);
      
      if (data.token) {
        localStorage.setItem('aapdasetu_token', data.token);
      }

      if (data.user) {
        setUser(data.user);
        localStorage.setItem('aapdasetu_user', JSON.stringify(data.user));
      }

      showToast(`Account created! Welcome to AapdaSetu, ${data.user?.username || fullName}.`, 'success');
      return { success: true, data };
    } catch (err) {
      showToast(typeof err === 'string' ? err : 'Registration failed. Try again.', 'error');
      return { success: false, error: err };
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    setLoading(true);
    try {
      await authService.logout();
    } catch (e) {
      console.warn('Logout notification', e);
    } finally {
      localStorage.removeItem('aapdasetu_token');
      localStorage.removeItem('aapdasetu_user');
      sessionStorage.removeItem('aapdasetu_token');
      setUser(null);
      setLoading(false);
      showToast('Logged out successfully', 'info');
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        initializing,
        toast,
        showToast,
        backendOnline,
        viewMode,
        setViewMode,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
