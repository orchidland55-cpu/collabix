import { apiClient } from '../lib/api';

export interface TeamResponse {
  id: string;
  departmentId: string;
  departmentName?: string;
  name: string;
  description?: string;
  status: 'ACTIVE' | 'ARCHIVED';
  createdAt?: string;
  updatedAt?: string;
  memberCount?: number;
  managerId?: string | null;
  managerName?: string | null;
}

export interface TeamSummary {
  id: string;
  departmentId: string;
  departmentName?: string;
  name: string;
  description?: string;
  status: 'ACTIVE' | 'ARCHIVED';
  memberCount?: number;
  managerId?: string | null;
  managerName?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateTeamRequest {
  name: string;
  description?: string;
  managerId?: string | null;
}

export interface UpdateTeamRequest {
  name?: string;
  description?: string;
  managerId?: string | null;
  clearManager?: boolean;
}

export const listWorkspaceTeams = (workspaceId: string) =>
  apiClient.get<TeamSummary[]>(`/workspaces/${workspaceId}/teams`);

export const listTeamsByDepartment = (workspaceId: string, departmentId: string) =>
  apiClient.get<TeamSummary[]>(`/workspaces/${workspaceId}/departments/${departmentId}/teams`);

export const getTeamById = (workspaceId: string, departmentId: string, teamId: string) =>
  apiClient.get<TeamResponse>(`/workspaces/${workspaceId}/departments/${departmentId}/teams/${teamId}`);

export const createTeam = (workspaceId: string, departmentId: string, data: CreateTeamRequest) =>
  apiClient.post<TeamResponse>(`/workspaces/${workspaceId}/departments/${departmentId}/teams`, data);

export const updateTeam = (workspaceId: string, departmentId: string, teamId: string, data: UpdateTeamRequest) =>
  apiClient.put<TeamResponse>(`/workspaces/${workspaceId}/departments/${departmentId}/teams/${teamId}`, data);

export const archiveTeam = (workspaceId: string, departmentId: string, teamId: string) =>
  apiClient.delete<void>(`/workspaces/${workspaceId}/departments/${departmentId}/teams/${teamId}`);

export const deleteTeamPermanently = (workspaceId: string, departmentId: string, teamId: string) =>
  apiClient.delete<void>(`/workspaces/${workspaceId}/departments/${departmentId}/teams/${teamId}/permanent`);

export const restoreTeam = (workspaceId: string, departmentId: string, teamId: string) =>
  apiClient.put<TeamResponse>(`/workspaces/${workspaceId}/departments/${departmentId}/teams/${teamId}/restore`);

export const teamService = (wsId: string) => ({
  listByDepartment: (deptId: string) => listTeamsByDepartment(wsId, deptId),
});