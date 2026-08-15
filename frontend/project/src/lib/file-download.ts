import { api } from './api';

export function downloadAuthenticatedFile(url: string, fileName: string): Promise<void> {
  return api
    .get<Blob>(url, { responseType: 'blob' })
    .then((response) => {
      const blob = response.data as Blob;
      const objectUrl = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = fileName || 'document';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(objectUrl);
    });
}
