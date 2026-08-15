import { apiClient } from '../lib/api';
import type { PageResponse } from '../types/api';

export type EmployeeDocumentType =
  | 'CONTRACT' | 'NDA' | 'IDENTITY' | 'PASSPORT' | 'WORK_PERMIT' | 'DIPLOMA' | 'CERTIFICATE'
  | 'RESUME' | 'PERFORMANCE_REVIEW' | 'PROMOTION' | 'SALARY' | 'MEDICAL' | 'INSURANCE'
  | 'TAX' | 'RESIGNATION' | 'EXIT_DOCUMENT' | 'OTHER';

export type DocumentStatus = 'ACTIVE' | 'DELETED';

export interface EmployeeDocumentResponse {
  id: string;
  employeeId: string;
  documentType: EmployeeDocumentType;
  title: string;
  originalFileName: string;
  storedFileName: string;
  mimeType: string;
  fileExtension?: string;
  fileSize: number;
  storagePath: string;
  checksum?: string;
  uploadedBy: string;
  fileVersion: number;
  expirationDate?: string;
  verified: boolean;
  verifiedBy?: string;
  verifiedAt?: string;
  status: DocumentStatus;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeDocumentSearchCriteria {
  employeeId?: string;
  documentType?: EmployeeDocumentType;
  status?: DocumentStatus;
  verified?: boolean;
  expirationFrom?: string;
  expirationTo?: string;
  dateFrom?: string;
  dateTo?: string;
  uploadedBy?: string;
  keyword?: string;
}

export interface EmployeeDocumentStatistics {
  totalDocuments: number;
  totalStorageBytes: number;
  verifiedCount: number;
  unverifiedCount: number;
  expiringCount: number;
  expiredCount: number;
  documentsByType: Record<string, number>;
}

function employeeBase(wsId: string, deptId: string, employeeId: string) {
  return `/workspaces/${wsId}/departments/${deptId}/employees/${employeeId}/documents`;
}

function deptBase(wsId: string, deptId: string) {
  return `/workspaces/${wsId}/departments/${deptId}/documents`;
}

function toQuery(params: object): string {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') qs.set(k, String(v));
  });
  const s = qs.toString();
  return s ? `?${s}` : '';
}

export const employeeDocumentService = {
  upload: (wsId: string, deptId: string, employeeId: string, data: {
    file: File;
    documentType: EmployeeDocumentType;
    title?: string;
    description?: string;
    expirationDate?: string;
  }) => {
    const formData = new FormData();
    formData.append('file', data.file);
    formData.append('documentType', data.documentType);
    if (data.title) formData.append('title', data.title);
    if (data.description) formData.append('description', data.description);
    if (data.expirationDate) formData.append('expirationDate', data.expirationDate);
    return apiClient.post<EmployeeDocumentResponse>(
      `${employeeBase(wsId, deptId, employeeId)}`,
      formData,
    );
  },

  replace: (wsId: string, deptId: string, employeeId: string, documentId: string, data: {
    file: File;
    title?: string;
    description?: string;
    expirationDate?: string;
  }) => {
    const formData = new FormData();
    formData.append('file', data.file);
    if (data.title) formData.append('title', data.title);
    if (data.description) formData.append('description', data.description);
    if (data.expirationDate) formData.append('expirationDate', data.expirationDate);
    return apiClient.put<EmployeeDocumentResponse>(
      `${employeeBase(wsId, deptId, employeeId)}/${documentId}`,
      formData,
    );
  },

  getById: (wsId: string, deptId: string, employeeId: string, documentId: string) =>
    apiClient.get<EmployeeDocumentResponse>(`${employeeBase(wsId, deptId, employeeId)}/${documentId}`),

  list: (wsId: string, deptId: string, employeeId: string) =>
    apiClient.get<PageResponse<EmployeeDocumentResponse>>(`${employeeBase(wsId, deptId, employeeId)}`),

  search: (wsId: string, deptId: string, employeeId: string, criteria?: EmployeeDocumentSearchCriteria) =>
    apiClient.get<PageResponse<EmployeeDocumentResponse>>(`${employeeBase(wsId, deptId, employeeId)}/search${toQuery(criteria ?? {})}`),

  downloadUrl: (wsId: string, deptId: string, employeeId: string, documentId: string) =>
    `${employeeBase(wsId, deptId, employeeId)}/${documentId}/download`,

  verify: (wsId: string, deptId: string, employeeId: string, documentId: string) =>
    apiClient.put<EmployeeDocumentResponse>(`${employeeBase(wsId, deptId, employeeId)}/${documentId}/verify`),

  unverify: (wsId: string, deptId: string, employeeId: string, documentId: string) =>
    apiClient.delete<EmployeeDocumentResponse>(`${employeeBase(wsId, deptId, employeeId)}/${documentId}/verify`),

  delete: (wsId: string, deptId: string, employeeId: string, documentId: string) =>
    apiClient.delete<void>(`${employeeBase(wsId, deptId, employeeId)}/${documentId}`),

  stats: (wsId: string, deptId: string, employeeId: string) =>
    apiClient.get<EmployeeDocumentStatistics>(`${employeeBase(wsId, deptId, employeeId)}/stats`),

  expiring: (wsId: string, deptId: string, withinDays = 30) =>
    apiClient.get<EmployeeDocumentResponse[]>(`${deptBase(wsId, deptId)}/expiring?withinDays=${withinDays}`),
};
