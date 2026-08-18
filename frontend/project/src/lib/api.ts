import axios, { type AxiosResponse, type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { emitAuthEvent } from './auth-events';
import type { ApiResponse, FieldError, ApiErrorResponse, PageResponse, Pageable } from '../types/api';

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

/* ---------- Refresh token lock ---------- */

let refreshPromise: Promise<{ accessToken: string; refreshToken: string } | null> | null = null;

async function doRefresh(): Promise<{ accessToken: string; refreshToken: string } | null> {
  const stored = localStorage.getItem('collabix_auth');
  const refreshToken = stored ? JSON.parse(stored).refreshToken : null;
  if (!refreshToken) return null;
  const { data } = await axios.post(
    `${api.defaults.baseURL}/auth/refresh`,
    { refreshToken },
  );
  const storedState = localStorage.getItem('collabix_auth');
  if (storedState) {
    const authState = JSON.parse(storedState);
    authState.accessToken = data.accessToken;
    authState.refreshToken = data.refreshToken;
    localStorage.setItem('collabix_auth', JSON.stringify(authState));
    emitAuthEvent({ type: 'token-refreshed' });
  }
  return { accessToken: data.accessToken, refreshToken: data.refreshToken };
}

async function refreshOrWait(): Promise<{ accessToken: string; refreshToken: string } | null> {
  if (refreshPromise) {
    return refreshPromise;
  }
  refreshPromise = doRefresh().finally(() => { refreshPromise = null; });
  return refreshPromise;
}

/* ---------- Axios instance ---------- */

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: Number(import.meta.env.VITE_API_TIMEOUT ?? 15_000),
});

/* ---------- Request interceptor ---------- */

api.interceptors.request.use((config) => {
  const stored = localStorage.getItem('collabix_auth');
  const token = stored ? JSON.parse(stored).accessToken : null;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  // Let the browser set multipart boundary automatically for file uploads.
  if (config.data instanceof FormData && config.headers) {
    delete config.headers['Content-Type'];
  }
  return config;
});

/* ---------- Response interceptor ---------- */

api.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    if (response.data && 'success' in response.data) {
      response.data = response.data.data as ApiResponse<unknown> & { data: unknown };
    }
    return response;
  },
  async (error: AxiosError<ApiErrorResponse>) => {
    const originalRequest = error.config;

    if (error.response?.status === 403) {
      emitAuthEvent({ type: 'forbidden', status: 403 });
      return Promise.reject(normalizeError(error));
    }

    const retryReq = originalRequest as RetryableRequestConfig | undefined;
    if (error.response?.status === 401 && !retryReq?._retry) {
      if (retryReq) retryReq._retry = true;
      try {
        const tokens = await refreshOrWait();
        if (tokens && originalRequest) {
          originalRequest.headers.Authorization = `Bearer ${tokens.accessToken}`;
          return api(originalRequest);
        }
      } catch {
        localStorage.removeItem('collabix_auth');
        emitAuthEvent({ type: 'session-expired', status: 401 });
      }
    }

    return Promise.reject(normalizeError(error));
  },
);

/* ---------- Error normalizer ---------- */

export interface NormalizedApiError {
  message: string;
  status: number | null;
  fieldErrors: FieldError[];
  raw: unknown;
}

function normalizeError(error: AxiosError<ApiErrorResponse>): NormalizedApiError {
  const response = error.response;
  return {
    message: response?.data?.message ?? error.message ?? 'An unexpected error occurred',
    status: response?.status ?? null,
    fieldErrors: response?.data?.errors ?? [],
    raw: error,
  };
}

/* ---------- Typed API helpers ---------- */

async function getApiResponse<T>(response: AxiosResponse<ApiResponse<T>>): Promise<T> {
  if (response.data === null || response.data === undefined) {
    return undefined as unknown as T;
  }
  return response.data as unknown as T;
}

const apiClient = {
  get: async <T>(url: string, config?: Record<string, unknown>): Promise<T> => {
    const response = await api.get<ApiResponse<T>>(url, config);
    return getApiResponse(response);
  },

  post: async <T>(url: string, data?: unknown, config?: Record<string, unknown>): Promise<T> => {
    const response = await api.post<ApiResponse<T>>(url, data, config);
    return getApiResponse(response);
  },

  put: async <T>(url: string, data?: unknown, config?: Record<string, unknown>): Promise<T> => {
    const response = await api.put<ApiResponse<T>>(url, data, config);
    return getApiResponse(response);
  },

  patch: async <T>(url: string, data?: unknown, config?: Record<string, unknown>): Promise<T> => {
    const response = await api.patch<ApiResponse<T>>(url, data, config);
    return getApiResponse(response);
  },

  delete: async <T>(url: string, config?: Record<string, unknown>): Promise<T> => {
    const response = await api.delete<ApiResponse<T>>(url, config);
    return getApiResponse(response);
  },

  getPage: async <T>(url: string, pageable?: Pageable, config?: Record<string, unknown>): Promise<PageResponse<T>> => {
    const params = new URLSearchParams();
    if (pageable) {
      params.set('page', String(pageable.page));
      params.set('size', String(pageable.size));
      if (pageable.sort && pageable.sort.length > 0) {
        params.set('sort', pageable.sort.join(','));
      }
    }
    const query = params.toString();
    const fullUrl = query ? `${url}?${query}` : url;
    const response = await api.get<ApiResponse<PageResponse<T>>>(fullUrl, config);
    return getApiResponse(response);
  },
};

export { api, apiClient };
