import { apiClient } from '../lib/api';

export interface WorkspaceSummaryResponse {
  id: string;
  name: string;
  description: string;
  status: string;
  memberCount: number;
  teamCount: number;
  projectCount: number;
  createdAt: string;
}

export interface WorkspaceResponse {
  id: string;
  name: string;
  description: string;
  status: string;
  owner: { id: string; firstName: string; lastName: string; email: string };
  myRole: string | null;
  memberCount: number;
  teamCount: number;
  projectCount: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

export interface CreateWorkspaceRequest {
  name: string;
  description?: string;
}

export interface UpdateWorkspaceRequest {
  name?: string;
  description?: string;
}

export interface WorkspaceDashboardResponse {
  workspaceSummary: {
    id: string;
    name: string;
    description: string;
    status: string;
    memberCount: number;
    teamCount: number;
    createdAt: string;
  };
  departmentSummary: { totalCount: number };
  teamSummary: { totalCount: number; activeCount: number };
  memberSummary: { totalCount: number; activeCount: number };
  projectSummary: { totalCount: number; activeCount: number; completedCount: number };
  taskSummary: { totalTasks: number; activeTasks: number; completedTasks: number; overdueTasks: number };
  notificationSummary: { total: number; unread: number };
  recentActivities: { id: string; description: string; actorName: string; projectName: string; createdAt: string }[];
}

export interface PersonalDashboardResponse {
  myTasks: { id: string; title: string; projectId: string; projectName: string; status: string; dueAt: string }[];
  overdueTasks: number;
  unreadNotifications: number;
  unreadMentions: { id: string; actorName: string; context: string; createdAt: string }[];
  recentComments: { id: string; taskId: string; content: string; status: string; createdAt: string; updatedAt: string }[];
  todaysHandovers: { id: string; projectName: string; senderName: string; receiverName: string; title: string; status: string; priority: string; dueDate: string; createdAt: string }[];
  recentActivities: { id: string; description: string; projectName: string; createdAt: string }[];
  recentWorkspaceProjects: { id: string; name: string; departmentName: string }[];
  recentDocuments: { id: string; title: string; fileName: string; mimeType: string; projectName: string; createdAt: string }[];
  knowledgeArticles: { id: string; title: string; category: string; createdAt: string }[];
  workspaceActivities: { type: string; description: string; actorName: string; timestamp: string }[];
}

export const workspaceService = {
  list: (params?: { search?: string; sort?: string; order?: string }) =>
    apiClient.get<WorkspaceSummaryResponse[]>('/workspaces', { params }),

  getById: (id: string) =>
    apiClient.get<WorkspaceResponse>(`/workspaces/${id}`),

  create: (data: CreateWorkspaceRequest) =>
    apiClient.post<WorkspaceResponse>('/workspaces', data),

  update: (id: string, data: UpdateWorkspaceRequest) =>
    apiClient.put<WorkspaceResponse>(`/workspaces/${id}`, data),

  delete: (id: string) =>
    apiClient.delete<void>(`/workspaces/${id}`),

  archive: (id: string) =>
    apiClient.put<WorkspaceResponse>(`/workspaces/${id}/archive`),

  restore: (id: string) =>
    apiClient.put<WorkspaceResponse>(`/workspaces/${id}/restore`),

  listArchived: () =>
    apiClient.get<WorkspaceSummaryResponse[]>('/workspaces/archived'),

  getDashboard: (workspaceId: string) =>
    apiClient.get<WorkspaceDashboardResponse>(`/workspaces/${workspaceId}/dashboard/workspace`),

  getPersonalDashboard: (workspaceId: string) =>
    apiClient.get<PersonalDashboardResponse>(`/workspaces/${workspaceId}/dashboard/me`),
};