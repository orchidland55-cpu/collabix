export const DEFAULT_API_BASE_URL = 'https://collabix-production-eead.up.railway.app/api';

export function getApiBaseUrl(): string {
  const configured = import.meta.env.VITE_API_BASE_URL;
  if (configured && configured.trim() !== '') {
    return configured.replace(/\/+$/, '');
  }
  if (import.meta.env.DEV) {
    return '/api';
  }
  console.warn(
    `[collabix] VITE_API_BASE_URL is not set. Falling back to ${DEFAULT_API_BASE_URL}.`,
  );
  return DEFAULT_API_BASE_URL;
}