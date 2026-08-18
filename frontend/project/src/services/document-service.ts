import { apiClient } from '../lib/api';
import { getApiBaseUrl } from '../lib/api-base';
import type { PageResponse } from '../types/api';
import type { DocumentResponse, CreateDocumentRequest, UpdateDocumentRequest } from '../pages/knowledge/types/document-types';

function base(wsId: string, deptId: string, projId: string) {
  return `/workspaces/${wsId}/departments/${deptId}/projects/${projId}/documents`;
}

function baseWs(wsId: string) {
  return `/workspaces/${wsId}`;
}

export const documentService = {
  list: (wsId: string, deptId: string, projId: string, page?: number) =>
    apiClient.get<PageResponse<DocumentResponse>>(`${base(wsId, deptId, projId)}`, { params: { page } }),

  listByWorkspace: (wsId: string, page?: number) =>
    apiClient.get<PageResponse<DocumentResponse>>(`${baseWs(wsId)}/documents`, { params: { page } }),

  getById: (wsId: string, deptId: string, projId: string, docId: string) =>
    apiClient.get<DocumentResponse>(`${base(wsId, deptId, projId)}/${docId}`),

  create: (wsId: string, deptId: string, projId: string, data: CreateDocumentRequest) =>
    apiClient.post<DocumentResponse>(`${base(wsId, deptId, projId)}`, data),

  upload: (wsId: string, deptId: string, projId: string, file: File, taskId?: string, title?: string, description?: string, category?: string, tags?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (taskId) formData.append('taskId', taskId);
    if (title) formData.append('title', title);
    if (description) formData.append('description', description);
    if (category) formData.append('category', category);
    if (tags) formData.append('tags', tags);
    return apiClient.post<DocumentResponse>(`${base(wsId, deptId, projId)}/upload`, formData);
  },

  update: (wsId: string, deptId: string, projId: string, docId: string, data: UpdateDocumentRequest) =>
    apiClient.put<DocumentResponse>(`${base(wsId, deptId, projId)}/${docId}`, data),

  delete: (wsId: string, deptId: string, projId: string, docId: string) =>
    apiClient.delete<void>(`${base(wsId, deptId, projId)}/${docId}`),

  archive: (wsId: string, deptId: string, projId: string, docId: string) =>
    apiClient.post<DocumentResponse>(`${base(wsId, deptId, projId)}/${docId}/archive`),

  restore: (wsId: string, deptId: string, projId: string, docId: string) =>
    apiClient.post<DocumentResponse>(`${base(wsId, deptId, projId)}/${docId}/restore`),

  search: (wsId: string, deptId: string, projId: string, query: string) =>
    apiClient.get<PageResponse<DocumentResponse>>(`${base(wsId, deptId, projId)}/search`, { params: { query } }),

  download: (wsId: string, deptId: string, projId: string, docId: string): string =>
    `${getApiBaseUrl()}${base(wsId, deptId, projId)}/${docId}/download`,

  getVersions: (wsId: string, deptId: string, projId: string, docId: string) =>
    apiClient.get<DocumentResponse[]>(`${base(wsId, deptId, projId)}/${docId}/versions`),

  submitForApproval: (wsId: string, deptId: string, projId: string, docId: string) =>
    apiClient.post<DocumentResponse>(`${base(wsId, deptId, projId)}/${docId}/submit-for-approval`),

  approve: (wsId: string, deptId: string, projId: string, docId: string) =>
    apiClient.post<DocumentResponse>(`${base(wsId, deptId, projId)}/${docId}/approve`),

  reject: (wsId: string, deptId: string, projId: string, docId: string) =>
    apiClient.post<DocumentResponse>(`${base(wsId, deptId, projId)}/${docId}/reject`),
};
