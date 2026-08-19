import { apiClient } from '../lib/api';
import type { PageResponse } from '../types/api';

/* ============================================================
   Department Overview DTOs
============================================================ */

export interface DepartmentOverviewWidget {
  departmentName: string;
  description: string;
  totalMembers: number;
  activeMembers: number;
  totalTeams: number;
  activeProjects: number;
  archivedProjects: number;
}

export interface DepartmentMemberWidget {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  teamName: string;
  memberStatus: string;
}

export interface DepartmentProjectWidget {
  id: string;
  name: string;
  status: string;
  taskCount: number;
}

export interface TaskSummaryWidget {
  totalTasks: number;
  activeTasks: number;
  archivedTasks: number;
  overdueTasks: number;
  tasksDueToday: number;
  tasksDueThisWeek: number;
}

export interface DepartmentActivityWidget {
  id: string;
  description: string;
  actorName: string;
  projectName: string;
  createdAt: string;
}

export interface DepartmentNotificationWidget {
  id: string;
  title: string;
  notificationType: string;
  projectName: string;
  status: string;
  createdAt: string;
}

export interface DepartmentAIModelWidget {
  totalModels: number;
  modelsInTraining: number;
  readyModels: number;
  deployedModels: number;
}

export interface DepartmentTaskWidget {
  id: string;
  title: string;
  status: string;
  projectName: string;
  dueAt?: string;
}

export interface PersonalDocumentWidget {
  id: string;
  title: string;
  fileName: string;
  mimeType: string;
  projectName: string;
  createdAt: string;
}

export interface PersonalKnowledgeArticleWidget {
  id: string;
  title: string;
  category: string;
  createdAt: string;
}

export interface DepartmentDashboardResponse {
  overview: DepartmentOverviewWidget;
  taskSummary: TaskSummaryWidget;
  activeProjects: DepartmentProjectWidget[];
  recentProjects: DepartmentProjectWidget[];
  recentlyUpdatedProjects: DepartmentProjectWidget[];
  departmentTasks: DepartmentTaskWidget[];
  departmentMembers: DepartmentMemberWidget[];
  recentDocuments: PersonalDocumentWidget[];
  recentKnowledgeArticles: PersonalKnowledgeArticleWidget[];
  departmentActivities: DepartmentActivityWidget[];
  departmentNotifications: DepartmentNotificationWidget[];
  unreadNotificationCount: number;
  aiModelSummary: DepartmentAIModelWidget;
}

/* ============================================================
   Department-specific Statistics DTOs
============================================================ */

export interface SprintStatistics {
  totalSprints: number;
  activeSprints: number;
  completedSprints: number;
  plannedSprints: number;
  cancelledSprints: number;
  averageDurationDays: number;
  averageCompletionRate: number;
  averageVelocity: number;
  averageTasksPerSprint: number;
}

export interface MarketingCampaignStatistics {
  totalCampaigns: number;
  activeCampaigns: number;
  completedCampaigns: number;
  plannedCampaigns: number;
  cancelledCampaigns: number;
  archivedCampaigns: number;
  averageCompletionPercentage: number;
  averageDurationDays: number;
}

export interface SecurityAuditStatistics {
  totalAudits: number;
  activeAudits: number;
  completedAudits: number;
  plannedAudits: number;
  archivedAudits: number;
  averageCompletionPercentage: number;
  averageCompletionTimeDays: number;
}

export interface AIModelStatistics {
  totalModels: number;
  trainingModels: number;
  readyModels: number;
  deployedModels: number;
  archivedModels: number;
  averageAccuracy: number;
}

/* ============================================================
   Analytics DTOs
============================================================ */

export interface ChartData {
  chartId: string;
  title: string;
  type: 'BAR' | 'LINE' | 'PIE' | 'DONUT' | 'AREA';
  series: ChartSeries[];
  labels: string[];
}

export interface ChartSeries {
  name: string;
  points: ChartPoint[];
  color: string;
}

export interface ChartPoint {
  label: string;
  value: number;
  timestamp: string;
  category: string;
}

export interface TaskMetrics {
  activeCount: number;
  archivedCount: number;
  overdueCount: number;
  dueTodayCount: number;
  dueThisWeekCount: number;
  completionRate: number;
  velocity: number;
}

export interface ActivityMetrics {
  totalCount: number;
}

export interface DocumentMetrics {
  documentCount: number;
  knowledgeBaseCount: number;
  totalSizeBytes: number;
}

export interface NotificationMetrics {
  totalCount: number;
  unreadCount: number;
  todayCount: number;
}

export interface WorkspaceAnalyticsResponse {
  tasks: TaskMetrics;
  activities: ActivityMetrics;
  documents: DocumentMetrics;
  notifications: NotificationMetrics;
  commentCount: number;
  memberCount: number;
  projectCount: number;
  charts: ChartData[];
}

/* ============================================================
   Sprint DTOs
============================================================ */

export interface SprintResponse {
  id: string;
  name: string;
  departmentId: string;
  status: string;
  startDate: string;
  endDate: string;
  goal: string;
  velocity: number;
  completionRate: number;
  totalTasks: number;
  completedTasks: number;
  createdAt: string;
}

export interface DepartmentSummary {
  id: string;
  workspaceId: string;
  name: string;
  status: string;
  teamCount?: number;
}

export interface DepartmentResponse {
  id: string;
  workspaceId: string;
  name: string;
  description?: string;
  status: string;
  teamCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDepartmentRequest {
  name: string;
  description?: string;
}

export interface UpdateDepartmentRequest {
  name?: string;
  description?: string;
  status?: string;
}

export function listDepartments(workspaceId: string, includeArchived = false) {
  return apiClient.get<DepartmentSummary[]>(`/workspaces/${workspaceId}/departments`, {
    params: includeArchived ? { includeArchived: true } : undefined,
  });
}

export function getDepartmentById(workspaceId: string, departmentId: string) {
  return apiClient.get<DepartmentResponse>(`/workspaces/${workspaceId}/departments/${departmentId}`);
}

export function createDepartment(workspaceId: string, data: CreateDepartmentRequest) {
  return apiClient.post<DepartmentResponse>(`/workspaces/${workspaceId}/departments`, data);
}

export function updateDepartment(workspaceId: string, departmentId: string, data: UpdateDepartmentRequest) {
  return apiClient.put<DepartmentResponse>(`/workspaces/${workspaceId}/departments/${departmentId}`, data);
}

export function archiveDepartment(workspaceId: string, departmentId: string) {
  return apiClient.delete<void>(`/workspaces/${workspaceId}/departments/${departmentId}`);
}

export function deleteDepartmentPermanently(workspaceId: string, departmentId: string) {
  return apiClient.delete<void>(`/workspaces/${workspaceId}/departments/${departmentId}/permanent`);
}

export function restoreDepartment(workspaceId: string, departmentId: string) {
  return apiClient.put<DepartmentResponse>(`/workspaces/${workspaceId}/departments/${departmentId}/restore`);
}

/* ============================================================
   Service factory
============================================================ */

export function departmentService(workspaceId: string, departmentId: string) {
  const dashboardBase = `/workspaces/${workspaceId}/departments/${departmentId}`;
  const wsAnalyticsBase = `/workspaces/${workspaceId}/analytics`;

  return {
    /* Department Dashboard */
    getDashboard: () =>
      apiClient.get<DepartmentDashboardResponse>(`${dashboardBase}/dashboard`),

    /* Sprint (Development) */
    getSprintStats: () =>
      apiClient.get<SprintStatistics>(`${dashboardBase}/sprints/stats`),

    listSprints: () =>
      apiClient.get<PageResponse<SprintResponse>>(`${dashboardBase}/sprints`),

    /* Marketing Campaigns */
    getCampaignStats: () =>
      apiClient.get<MarketingCampaignStatistics>(`${dashboardBase}/campaigns/stats`),

    /* Security Audits (Cybersecurity) */
    getAuditStats: () =>
      apiClient.get<SecurityAuditStatistics>(`${dashboardBase}/audits/stats`),

    /* AI Models */
    getModelStats: () =>
      apiClient.get<AIModelStatistics>(`${dashboardBase}/models/stats`),

    /* Analytics */
    getAnalytics: () =>
      apiClient.get<WorkspaceAnalyticsResponse>(wsAnalyticsBase),
  };
}
