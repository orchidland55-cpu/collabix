import type { NormalizedApiError } from './api';

const DEFAULT_MESSAGE = 'Unable to upload the document. Please try again.';

export function getUploadErrorMessage(error: unknown, fallback = DEFAULT_MESSAGE): string {
  if (!error || typeof error !== 'object') return fallback;

  const err = error as Partial<NormalizedApiError> & { normalized?: NormalizedApiError };
  const status = err.normalized?.status ?? err.status;
  if (status === 403) return 'You do not have permission to upload documents.';
  if (status === 401) return 'Your session has expired. Please sign in again.';
  if (status === 404) return 'The selected project could not be found. Please choose a valid project.';
  if (status === 413) return 'This file exceeds the maximum allowed size.';
  if (status === 415) return 'This file type is not supported.';
  if (status === 400) {
    const message = err.message?.trim();
    if (message) return message;
    return 'Unable to upload this document. Please check the file and try again.';
  }
  if (status && status >= 500) return 'Something went wrong while uploading the document.';

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
