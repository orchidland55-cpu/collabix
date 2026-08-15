import { apiClient } from '../lib/api';
import type { PageResponse } from '../types/api';

export type CandidateAttachmentType =
  | 'CV' | 'COVER_LETTER' | 'DIPLOMA' | 'CERTIFICATE' | 'PORTFOLIO' | 'IDENTITY'
  | 'RECOMMENDATION' | 'OFFER_LETTER' | 'CONTRACT' | 'OTHER';

export interface CandidateAttachmentResponse {
  id: string;
  candidateId: string;
  attachmentType: CandidateAttachmentType;
  originalFileName: string;
  storedFileName: string;
  fileExtension?: string;
  mimeType: string;
  fileSize: number;
  storagePath: string;
  description?: string;
  uploadedBy: string;
  fileVersion: number;
  createdAt: string;
  updatedAt: string;
}

export interface AttachmentSearchCriteria {
  candidateId?: string;
  attachmentType?: CandidateAttachmentType;
  uploadedBy?: string;
  fileExtension?: string;
  dateFrom?: string;
  dateTo?: string;
  keyword?: string;
}

export interface AttachmentStatistics {
  totalAttachments: number;
  totalStorageBytes: number;
  hasCv: boolean;
  certificatesCount: number;
  attachmentsByType: Record<string, number>;
}

function base(wsId: string, deptId: string, candidateId: string) {
  return `/workspaces/${wsId}/departments/${deptId}/candidates/${candidateId}/attachments`;
}

function deptBase(wsId: string, deptId: string) {
  return `/workspaces/${wsId}/departments/${deptId}/attachments`;
}

function toQuery(params: object): string {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') qs.set(k, String(v));
  });
  const s = qs.toString();
  return s ? `?${s}` : '';
}

export const candidateAttachmentService = {
  upload: (wsId: string, deptId: string, candidateId: string, data: {
    file: File;
    attachmentType: CandidateAttachmentType;
    description?: string;
  }) => {
    const formData = new FormData();
    formData.append('file', data.file);
    formData.append('attachmentType', data.attachmentType);
    if (data.description) formData.append('description', data.description);
    return apiClient.post<CandidateAttachmentResponse>(
      `${base(wsId, deptId, candidateId)}`,
      formData,
    );
  },

  replace: (wsId: string, deptId: string, candidateId: string, attachmentId: string, data: {
    file: File;
    description?: string;
  }) => {
    const formData = new FormData();
    formData.append('file', data.file);
    if (data.description) formData.append('description', data.description);
    return apiClient.put<CandidateAttachmentResponse>(
      `${base(wsId, deptId, candidateId)}/${attachmentId}`,
      formData,
    );
  },

  getById: (wsId: string, deptId: string, candidateId: string, attachmentId: string) =>
    apiClient.get<CandidateAttachmentResponse>(`${base(wsId, deptId, candidateId)}/${attachmentId}`),

  list: (wsId: string, deptId: string, candidateId: string) =>
    apiClient.get<PageResponse<CandidateAttachmentResponse>>(`${base(wsId, deptId, candidateId)}`),

  search: (wsId: string, deptId: string, candidateId: string, criteria?: AttachmentSearchCriteria) =>
    apiClient.get<PageResponse<CandidateAttachmentResponse>>(`${base(wsId, deptId, candidateId)}/search${toQuery(criteria ?? {})}`),

  downloadUrl: (wsId: string, deptId: string, candidateId: string, attachmentId: string) =>
    `${base(wsId, deptId, candidateId)}/${attachmentId}/download`,

  delete: (wsId: string, deptId: string, candidateId: string, attachmentId: string) =>
    apiClient.delete<void>(`${base(wsId, deptId, candidateId)}/${attachmentId}`),

  stats: (wsId: string, deptId: string) =>
    apiClient.get<AttachmentStatistics>(`${deptBase(wsId, deptId)}/stats`),
};
