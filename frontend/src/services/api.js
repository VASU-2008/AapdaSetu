import axios from 'axios';

// Use relative API path when running with Vite proxy or fall back to full localhost:3000
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
  timeout: 10000,
});

// Interceptor to attach Authorization Bearer token from localStorage if present
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('aapdasetu_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const authService = {
  login: async (email, password) => {
    try {
      const response = await apiClient.post('/auth/login', { email, password });
      return response.data;
    } catch (error) {
      throw error.response?.data?.message || error.message || 'Login failed. Please check your credentials.';
    }
  },

  register: async (fullName, email, password) => {
    try {
      const response = await apiClient.post('/auth/register', { 
        fullName, 
        email, 
        password 
      });
      return response.data;
    } catch (error) {
      throw error.response?.data?.message || error.message || 'Registration failed.';
    }
  },

  getMe: async () => {
    try {
      const response = await apiClient.get('/auth/me');
      return response.data;
    } catch (error) {
      return null;
    }
  },

  logout: async () => {
    try {
      const response = await apiClient.get('/auth/logout');
      return response.data;
    } catch (error) {
      return { message: 'Logged out' };
    }
  },

  checkHealth: async () => {
    try {
      const response = await axios.get('/health', { timeout: 3000 });
      return response.data;
    } catch (error) {
      // Fallback direct check if proxy is not active
      try {
        const directResp = await axios.get('http://localhost:3000/health', { timeout: 3000 });
        return directResp.data;
      } catch (err) {
        return null;
      }
    }
  }
};

export const alertsService = {
  getAlerts: async () => {
    try {
      const response = await apiClient.get('/alerts');
      return response.data;
    } catch (error) {
      throw error.response?.data?.message || error.message || 'Failed to fetch alerts';
    }
  },

  createAlert: async (alertData) => {
    try {
      const response = await apiClient.post('/alerts', alertData);
      return response.data;
    } catch (error) {
      throw error.response?.data?.message || error.message || 'Failed to broadcast alert';
    }
  }
};
