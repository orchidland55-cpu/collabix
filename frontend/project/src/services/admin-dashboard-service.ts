import { apiClient } from '../lib/api';

export type AdminAnalyticsPeriod = 'THIS_WEEK' | 'THIS_MONTH' | 'LAST_7_DAYS' | 'LAST_30_DAYS';

export interface ActivityOverviewPoint {
  date: string;
  label: string;
  value: number;
}

export interface ActivityOverviewResponse {
  period: AdminAnalyticsPeriod;
  points: ActivityOverviewPoint[];
  total: number;
}

export interface ProjectStatusSegment {
  status: string;
  label: string;
  count: number;
  percentage: number;
}

export interface AdminProjectStatusResponse {
  activeProjectCount: number;
  segments: ProjectStatusSegment[];
}

export function adminDashboardService(workspaceId: string) {
  const base = `/workspaces/${workspaceId}/analytics/admin`;

  return {
    getActivityOverview: (period: AdminAnalyticsPeriod = 'THIS_WEEK') =>
      apiClient.get<ActivityOverviewResponse>(`${base}/activity-overview`, { params: { period } }),

    getProjectStatus: () =>
      apiClient.get<AdminProjectStatusResponse>(`${base}/project-status`),
  };
}
