import { useQuery } from '@tanstack/react-query';
import {
  adminDashboardService,
  type AdminAnalyticsPeriod,
} from './admin-dashboard-service';

export function useAdminActivityOverview(
  workspaceId: string | undefined,
  period: AdminAnalyticsPeriod = 'THIS_WEEK',
) {
  return useQuery({
    queryKey: ['admin', 'activity-overview', workspaceId, period],
    queryFn: async () => {
      const svc = adminDashboardService(workspaceId!);
      return svc.getActivityOverview(period);
    },
    enabled: !!workspaceId,
  });
}

export function useAdminProjectStatus(workspaceId: string | undefined) {
  return useQuery({
    queryKey: ['admin', 'project-status', workspaceId],
    queryFn: async () => {
      const svc = adminDashboardService(workspaceId!);
      return svc.getProjectStatus();
    },
    enabled: !!workspaceId,
  });
}

export type { AdminAnalyticsPeriod };
