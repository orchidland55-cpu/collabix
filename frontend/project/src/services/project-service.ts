import { apiClient } from '../lib/api';
import type { PageResponse } from '../types/api';
import type { ProjectResponse, CreateProjectRequest, UpdateProjectRequest } from '../pages/projects/projects-types';

export function listProjects(workspaceId: string, departmentId: string, search?: string, page = 0, size = 20) {
  const params = new URLSearchParams();
  if (search) params.set('search', search);
  params.set('page', String(page));
  params.set('size', String(size));
  const qs = params.toString();
  return apiClient.get<PageResponse<ProjectResponse>>(
    `/workspaces/${workspaceId}/departments/${departmentId}/projects${qs ? `?${qs}` : ''}`
  );
}

export function getProjectById(workspaceId: string, departmentId: string, projectId: string) {
  return apiClient.get<ProjectResponse>(
    `/workspaces/${workspaceId}/departments/${departmentId}/projects/${projectId}`
  );
}

export function createProject(workspaceId: string, departmentId: string, data: CreateProjectRequest) {
  return apiClient.post<ProjectResponse>(
    `/workspaces/${workspaceId}/departments/${departmentId}/projects`,
    data
  );
}

export function updateProject(workspaceId: string, departmentId: string, projectId: string, data: UpdateProjectRequest) {
  return apiClient.put<ProjectResponse>(
    `/workspaces/${workspaceId}/departments/${departmentId}/projects/${projectId}`,
    data
  );
}

export function deleteProject(workspaceId: string, departmentId: string, projectId: string) {
  return apiClient.delete<void>(
    `/workspaces/${workspaceId}/departments/${departmentId}/projects/${projectId}`
  );
}

export function restoreProject(workspaceId: string, departmentId: string, projectId: string) {
  return apiClient.put<ProjectResponse>(
    `/workspaces/${workspaceId}/departments/${departmentId}/projects/${projectId}/restore`
  );
}

export function hardDeleteProject(workspaceId: string, departmentId: string, projectId: string) {
  return apiClient.delete<void>(
    `/workspaces/${workspaceId}/departments/${departmentId}/projects/${projectId}/hard-delete`
  );
}

export function listArchivedProjects(workspaceId: string, departmentId: string) {
  return apiClient.get<ProjectResponse[]>(
    `/workspaces/${workspaceId}/departments/${departmentId}/projects/archived`
  );
}

export function listWorkspaceProjects(workspaceId: string, search?: string, page = 0, size = 100) {
  const params = new URLSearchParams();
  if (search) params.set('search', search);
  params.set('page', String(page));
  params.set('size', String(size));
  const qs = params.toString();
  return apiClient.get<PageResponse<ProjectResponse>>(
    `/workspaces/${workspaceId}/projects${qs ? `?${qs}` : ''}`,
  );
}
