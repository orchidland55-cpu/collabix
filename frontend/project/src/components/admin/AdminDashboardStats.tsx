import { useState } from 'react';
import { Activity, FolderKanban, Loader2 } from 'lucide-react';
import { Card, CardBody, CardHeader, CardTitle } from '../ui/Card';
import { Skeleton } from '../ui/Skeleton';
import { EmptyState } from '../ui/EmptyState';
import { cn } from '../../lib/cn';
import {
  useAdminActivityOverview,
  useAdminProjectStatus,
  type AdminAnalyticsPeriod,
} from '../../services/admin-dashboard-hooks';
import { AdminActivityLineChart, AdminProjectDonutChart } from './AdminDashboardCharts';

const PERIOD_SUBTITLES: Record<AdminAnalyticsPeriod, string> = {
  THIS_WEEK: 'Tasks & activity this week',
  THIS_MONTH: 'Tasks & activity this month',
  LAST_7_DAYS: 'Tasks & activity — last 7 days',
  LAST_30_DAYS: 'Tasks & activity — last 30 days',
};
const PERIOD_OPTIONS: { value: AdminAnalyticsPeriod; label: string }[] = [
  { value: 'THIS_WEEK', label: 'This Week' },
  { value: 'THIS_MONTH', label: 'This Month' },
  { value: 'LAST_7_DAYS', label: 'Last 7 Days' },
  { value: 'LAST_30_DAYS', label: 'Last 30 Days' },
];

const SEGMENT_COLORS: Record<string, string> = {
  TO_DO: 'rgb(var(--text-tertiary))',
  IN_PROGRESS: 'rgb(var(--accent-500))',
  IN_REVIEW: 'rgb(var(--warning-500))',
  DONE: 'rgb(var(--success-500))',
};

interface AdminDashboardStatsProps {
  workspaceId: string;
}

export function AdminDashboardStats({ workspaceId }: AdminDashboardStatsProps) {
  const [period, setPeriod] = useState<AdminAnalyticsPeriod>('THIS_WEEK');

  const {
    data: activityData,
    isLoading: activityLoading,
    isError: activityError,
  } = useAdminActivityOverview(workspaceId, period);

  const {
    data: projectData,
    isLoading: projectLoading,
    isError: projectError,
  } = useAdminProjectStatus(workspaceId);

  const chartPoints = activityData?.points.map((p) => ({ label: p.label, value: p.value })) ?? [];
  const hasActivity = (activityData?.total ?? 0) > 0;

  const donutSegments =
    projectData?.segments.map((s) => ({
      label: s.label,
      value: s.count,
      color: SEGMENT_COLORS[s.status] ?? 'rgb(var(--border-strong))',
      percentage: s.percentage,
    })) ?? [];

  return (
    <div className="grid gap-4 xl:grid-cols-3">
      <Card className="xl:col-span-2">
        <CardHeader className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Activity className="h-4 w-4 text-accent-600 dark:text-accent-400" />
              Activity Overview
            </CardTitle>
            <p className="text-caption text-text-tertiary mt-1">{PERIOD_SUBTITLES[period]}</p>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {PERIOD_OPTIONS.map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => setPeriod(option.value)}
                className={cn(
                  'rounded-md px-2.5 py-1 text-2xs font-medium transition-colors',
                  period === option.value
                    ? 'bg-accent-50 text-accent-700 dark:bg-accent-100 dark:text-accent-300'
                    : 'text-text-tertiary hover:bg-surface-2 hover:text-text-secondary',
                )}
              >
                {option.label}
              </button>
            ))}
          </div>
        </CardHeader>
        <CardBody>
          {activityLoading ? (
            <Skeleton className="h-[260px] w-full" />
          ) : activityError ? (
            <EmptyState icon={<Activity className="h-5 w-5" />} title="Failed to load activity data" />
          ) : !hasActivity ? (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <Activity className="h-8 w-8 text-text-tertiary mb-3 opacity-60" />
              <p className="text-body font-medium text-text-secondary">No activity yet</p>
              <p className="text-caption text-text-tertiary mt-1 max-w-xs">
                Activity from tasks, comments, and events will appear here once your team gets started.
              </p>
            </div>
          ) : (
            <AdminActivityLineChart data={chartPoints} height={260} />
          )}
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FolderKanban className="h-4 w-4 text-accent-600 dark:text-accent-400" />
            Project Status
          </CardTitle>
          <p className="text-caption text-text-tertiary mt-1">
            {projectLoading ? (
              <span className="inline-flex items-center gap-1.5">
                <Loader2 className="h-3 w-3 animate-spin" /> Loading…
              </span>
            ) : (
              `${projectData?.activeProjectCount ?? 0} active projects`
            )}
          </p>
        </CardHeader>
        <CardBody>
          {projectLoading ? (
            <Skeleton className="h-[220px] w-full" />
          ) : projectError ? (
            <EmptyState icon={<FolderKanban className="h-5 w-5" />} title="Failed to load project status" />
          ) : (projectData?.activeProjectCount ?? 0) === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-center">
              <FolderKanban className="h-8 w-8 text-text-tertiary mb-3 opacity-60" />
              <p className="text-body font-medium text-text-secondary">No projects yet</p>
              <p className="text-caption text-text-tertiary mt-1 max-w-xs">
                Create projects across departments to see status distribution here.
              </p>
            </div>
          ) : (
            <AdminProjectDonutChart
              segments={donutSegments.filter((s) => s.value > 0)}
              legendSegments={donutSegments}
              size={156}
            />
          )}
        </CardBody>
      </Card>
    </div>
  );
}
