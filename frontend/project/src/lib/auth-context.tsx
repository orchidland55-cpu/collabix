import { createContext, useContext, useState, useCallback, useMemo, useEffect, type ReactNode } from 'react';
import { onAuthEvent, type AuthEvent } from './auth-events';
import { authService } from '../services/auth-service';
import { queryClient } from './query-client';
import type { UserResponse } from '../types';

/* ---------- JWT Decode (minimal, no library needed) ---------- */

function decodeJwt(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

function extractPermissions(token: string): string[] {
  const claims = decodeJwt(token);
  if (!claims) return [];
  const perms = claims['permissions'];
  return Array.isArray(perms) ? perms.map(String) : [];
}

/* ---------- Types ---------- */

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  permissions: string[];
  memberType?: string;
  departmentId?: string;
  departmentName?: string;
  teamId?: string;
  teamName?: string;
}

export interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
}

export interface LoginPayload {
  email: string;
  password: string;
  remember?: boolean;
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface AuthContextValue extends AuthState {
  isAuthenticated: boolean;
  isAuthLoading: boolean;
  isRefreshing: boolean;
  sessionExpired: boolean;
  lastAuthEvent: AuthEvent | null;
  signIn: (payload: LoginPayload) => Promise<LoginResult>;
  signOut: () => void;
  clearSessionExpiry: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const AUTH_STORAGE_KEY = 'collabix_auth';

function mapUserResponse(r: UserResponse, token: string): User {
  return {
    id: r.id,
    email: r.email,
    firstName: r.firstName,
    lastName: r.lastName,
    roles: r.role ? [r.role] : [],
    permissions: extractPermissions(token),
    memberType: r.memberType,
    departmentId: r.departmentId,
    departmentName: r.departmentName,
    teamId: r.teamId,
    teamName: r.teamName,
  };
}

function loadAuth(): AuthState {
  try {
    const stored = localStorage.getItem(AUTH_STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored) as AuthState;
      if (parsed.accessToken && parsed.user) {
        return parsed;
      }
    }
  } catch {
    // corrupted data
  }
  return { user: null, accessToken: null, refreshToken: null };
}

function persistAuth(state: AuthState) {
  if (state.accessToken && state.user) {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(state));
  }
}

function clearAuth() {
  localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(loadAuth);
  const [sessionExpired, setSessionExpired] = useState(false);
  const [lastAuthEvent, setLastAuthEvent] = useState<AuthEvent | null>(null);
  const [isAuthLoading, setIsAuthLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Multi-tab synchronization
  useEffect(() => {
    function handleStorage(e: StorageEvent) {
      if (e.key === AUTH_STORAGE_KEY) {
        if (e.newValue) {
          try {
            const parsed = JSON.parse(e.newValue) as AuthState;
            if (parsed.accessToken && parsed.user) {
              setState(parsed);
              setSessionExpired(false);
            } else {
              setState({ user: null, accessToken: null, refreshToken: null });
            }
          } catch {
            setState({ user: null, accessToken: null, refreshToken: null });
          }
        } else {
          setState({ user: null, accessToken: null, refreshToken: null });
        }
      }
    }
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  // Global auth event subscription
  useEffect(() => {
    const unsubscribe = onAuthEvent((event) => {
      setLastAuthEvent(event);
      if (event.type === 'session-expired') {
        queryClient.clear();
        clearAuth();
        setState({ user: null, accessToken: null, refreshToken: null });
        setSessionExpired(true);
      }
      if (event.type === 'token-refreshed') {
        const fresh = loadAuth();
        if (fresh.accessToken && fresh.user) {
          setState(fresh);
        }
      }
    });
    return unsubscribe;
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => setIsAuthLoading(false), 300);
    return () => clearTimeout(timer);
  }, []);

  // Re-fetch the user profile on load so stored sessions stay in sync with the
  // backend (e.g. department assignment added after the session was persisted).
  useEffect(() => {
    if (!state.accessToken) return;
    let cancelled = false;
    authService
      .me()
      .then((profile) => {
        if (cancelled || !state.accessToken) return;
        setState((prev) => {
          if (!prev.accessToken) return prev;
          const user = mapUserResponse(profile, prev.accessToken);
          const next: AuthState = { ...prev, user };
          persistAuth(next);
          return next;
        });
      })
      .catch(() => {
        // Non-blocking: keep the stored profile on failure.
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const signIn = useCallback(
    async (payload: LoginPayload): Promise<LoginResult> => {
      setIsRefreshing(true);
      try {
        // A different user is signing in — drop any cached data belonging to
        // the previous session so nothing leaks across accounts.
        queryClient.clear();

        const loginRes = await authService.login({
          email: payload.email,
          password: payload.password,
        });
        const user = mapUserResponse(loginRes.user, loginRes.accessToken);
        const result: LoginResult = {
          accessToken: loginRes.accessToken,
          refreshToken: loginRes.refreshToken,
          user,
        };
        const newState: AuthState = {
          user,
          accessToken: loginRes.accessToken,
          refreshToken: loginRes.refreshToken,
        };
        persistAuth(newState);
        setState(newState);
        setSessionExpired(false);
        return result;
      } finally {
        setIsRefreshing(false);
      }
    },
    [],
  );

  const signOut = useCallback(async () => {
    const currentRefreshToken = state.refreshToken;
    // Flush all cached data for the current user.
    queryClient.clear();
    try {
      if (currentRefreshToken) {
        await authService.logout({ refreshToken: currentRefreshToken });
      }
    } catch {
      // Logout API failure is non-blocking
    }
    clearAuth();
    setState({ user: null, accessToken: null, refreshToken: null });
    setSessionExpired(false);
  }, [state.refreshToken]);

  const clearSessionExpiry = useCallback(() => {
    setSessionExpired(false);
    setLastAuthEvent(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      isAuthenticated: !!state.user && !!state.accessToken,
      isAuthLoading,
      isRefreshing,
      sessionExpired,
      lastAuthEvent,
      signIn,
      signOut,
      clearSessionExpiry,
    }),
    [state, isAuthLoading, isRefreshing, sessionExpired, lastAuthEvent, signIn, signOut, clearSessionExpiry],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
