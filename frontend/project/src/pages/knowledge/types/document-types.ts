export type DocumentStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED';

export interface DocumentResponse {
  id: string;
  projectId: string;
  departmentId?: string;
  taskId?: string;
  title: string;
  description?: string;
  fileName: string;
  mimeType: string;
  fileSize: number;
  storagePath: string;
  category?: string;
  tags?: string;
  viewCount: number;
  status: DocumentStatus;
  version: number;
  aiProcessed: boolean;
  storageType: string;
  pdfExportAvailable: boolean;
  approvalStatus?: string;
  approvedBy?: string;
  approvedAt?: string;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}

export interface CreateDocumentRequest {
  title: string;
  description?: string;
  fileName: string;
  mimeType: string;
  fileSize: number;
  storagePath: string;
  taskId?: string;
  category?: string;
  tags?: string;
}

export interface UpdateDocumentRequest {
  title?: string;
  description?: string;
  category?: string;
  tags?: string;
  status?: DocumentStatus;
  aiProcessed?: boolean;
  pdfExportAvailable?: boolean;
}

export function getFileIcon(mimeType: string): string {
  if (mimeType.includes('pdf')) return 'pdf';
  if (mimeType.includes('word') || mimeType.includes('doc')) return 'docx';
  if (mimeType.includes('spreadsheet') || mimeType.includes('excel') || mimeType.includes('xls')) return 'xlsx';
  if (mimeType.includes('presentation') || mimeType.includes('powerpoint') || mimeType.includes('ppt')) return 'pptx';
  if (mimeType.includes('image')) return 'img';
  if (mimeType.includes('zip') || mimeType.includes('rar')) return 'zip';
  return 'other';
}

export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

export function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  try {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  } catch {
    return dateStr;
  }
}
