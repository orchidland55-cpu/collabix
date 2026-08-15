import type { NormalizedApiError } from './api';

const DEFAULT_MESSAGE = 'Unable to upload the document. Please try again.';

export function getUploadErrorMessage(error: unknown, fallback = DEFAULT_MESSAGE): string {
  if (!error || typeof error !== 'object') return fallback;

  const err = error as Partial<NormalizedApiError>;
  if (err.status === 403) return 'You do not have permission to upload documents.';
  if (err.status === 401) return 'Your session has expired. Please sign in again.';
  if (err.status === 404) return 'The selected project could not be found. Please choose a valid project.';

  const message = err.message?.trim();
  if (!message) return fallback;

  if (/permission|forbidden|unauthorized/i.test(message)) {
    return 'You do not have permission to upload documents.';
  }
  if (/extension|not allowed|invalid pdf|invalid png|invalid jpeg|file type|signature/i.test(message)) {
    return message;
  }
  if (/empty|required|must have a name|select a file/i.test(message)) {
    return message;
  }
  if (/size exceeds|too large|maximum allowed/i.test(message)) {
    return message;
  }

  return message;
}

export function logUploadError(context: string, error: unknown): void {
  if (import.meta.env.DEV) {
    console.error(`[upload:${context}]`, error);
  }
}
