import { create } from 'zustand';
import { User, UserProfile } from '../types';
import * as api from '../services/api';

interface AuthState {
  user: User | null;
  token: string | null;
  profile: UserProfile | null;
  loading: boolean;
  error: string | null;

  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  loadProfile: () => Promise<void>;
  restoreSession: () => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  token: null,
  profile: null,
  loading: false,
  error: null,

  login: async (username, password) => {
    set({ loading: true, error: null });
    try {
      const res = await api.login(username, password);
      sessionStorage.setItem('token', res.token);
      sessionStorage.setItem('user', JSON.stringify(res.user));
      set({ user: res.user, token: res.token, loading: false });
    } catch (e: any) {
      set({ loading: false, error: e.message || 'Login failed' });
      throw e;
    }
  },

  register: async (username, email, password) => {
    set({ loading: true, error: null });
    try {
      const res = await api.register(username, email, password);
      sessionStorage.setItem('token', res.token);
      sessionStorage.setItem('user', JSON.stringify(res.user));
      set({ user: res.user, token: res.token, loading: false });
    } catch (e: any) {
      set({ loading: false, error: e.message || 'Registration failed' });
      throw e;
    }
  },

  logout: () => {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    set({ user: null, token: null, profile: null });
  },

  loadProfile: async () => {
    try {
      const profile = await api.fetchUserProfile();
      set({ profile });
    } catch {
      // ignore
    }
  },

  restoreSession: () => {
    const token = sessionStorage.getItem('token');
    const userStr = sessionStorage.getItem('user');
    if (token && userStr) {
      try {
        const user = JSON.parse(userStr);
        set({ user, token });
      } catch {
        sessionStorage.clear();
      }
    }
  },
}));
