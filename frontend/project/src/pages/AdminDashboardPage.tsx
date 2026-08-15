import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Building2,
  Network,
  Users,
  UserCheck,
  AlertCircle,
  Plus,
  ShieldCheck,
  UserPlus,
  Boxes,
  Briefcase,
  type LucideIcon,
} from 'lucide-react';
import { useAuth } from '../lib/auth-context';
import { useWorkspaceId } from '../hooks/useWorkspaceId';
import { useWorkspacesList } from '../services/workspace-hooks';
import { useDepartmentsList, useUserStatistics } from '../services/admin-hooks';
import { AdminDashboardStats } from '../components/admin/AdminDashboardStats';
import { Card, CardHeader, CardTitle, CardBody } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Skeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';
import { cn } from '../lib/cn';

const statToneBg: Record<string, string> = {
  accent: 'bg-accent-50 text-accent-600 dark:bg-accent-100 dark:text-accent-300',
  success: 'bg-success-50 text-success-700 dark:bg-success-100 dark:text-success-500',
  warning: 'bg-warning-50 text-warning-700 dark:bg-warning-100 dark:text-warning-500',
  danger: 'bg-danger-50 text-danger-700 dark:bg-danger-100 dark:text-danger-500',
  info: 'bg-info-50 text-info-700 dark:bg-info-100 dark:text-info-500',
  neutral: 'bg-surface-2 text-text-secondary',
};

function KpiCard({
  label,
  value,
  sub,
  icon: Icon,
  tone,
  loading,
}: {
  label: string;
  value: number | string;
  sub?: string;
  icon: LucideIcon;
  tone: string;
  loading?: boolean;
}) {
  return (
    <Card>
      <CardBody className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-caption text-text-tertiary">{label}</p>
          {loading ? (
            <Skeleton className="h-8 w-16 mt-1" />
          ) : (
            <p className="text-2xl font-bold text-text-primary mt-1 leading-none">{value}</p>
          )}
          {sub && <p className="text-2xs text-text-tertiary mt-1.5">{sub}</p>}
        </div>
        <span className={cn('flex h-10 w-10 shrink-0 items-center justify-center rounded-xl [&>svg]:h-5 [&>svg]:w-5', statToneBg[tone])}>
          <Icon />
        </span>
      </CardBody>
    </Card>
  );
}

function QuickAction({
  label,
  icon: Icon,
  onClick,
}: {
  label: string;
  icon: LucideIcon;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center gap-2.5 rounded-lg border border-border-subtle bg-surface px-3 py-2 text-body font-medium text-text-secondary hover:border-accent-500/40 hover:text-accent-600 dark:hover:text-accent-400 transition-colors"
    >
      <span className="shrink-0 [&>svg]:h-4 [&>svg]:w-4 text-accent-600 dark:text-accent-400">
        <Icon />
      </span>
      {label}
    </button>
  );
}

function formatDate(instant: string | undefined): string {
  if (!instant) return '—';
  try {
    return new Date(instant).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  } catch {
    return instant;
  }
}

const statusTone: Record<string, 'success' | 'warning' | 'neutral'> = {
  ACTIVE: 'success',
  ACTIVE_IN_PROGRESS: 'success',
  ACTIVE_PENDING: 'success',
  INACTIVE: 'warning',
  ARCHIVED: 'neutral',
  CLOSED: 'neutral',
};

export function AdminDashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const wsId = useWorkspaceId();

  const { data: workspaces, isLoading: wsLoading, isError: wsError } = useWorkspacesList();
  const { data: departments, isLoading: deptLoading } = useDepartmentsList();
  const { data: userStats, isLoading: statsLoading } = useUserStatistics();

  const adminName = user ? `${user.firstName} ${user.lastName}` : 'Admin';
  const now = new Date();
  const dateStr = now.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });

  const activeWorkspaces = useMemo(
    () => (workspaces ?? []).filter((ws) => ws.status === 'ACTIVE' || ws.status?.startsWith('ACTIVE')).length,
    [workspaces],
  );

  const rolesBreakdown = useMemo(() => {
    const map = userStats?.usersPerRole ?? {};
    const labels: Record<string, string> = {
      SUPER_ADMIN: 'Super Admins',
      ADMIN: 'Admins',
      MANAGER: 'Managers',
      MEMBER: 'Members',
    };
    return Object.entries(map).map(([role, count]) => ({ role, count, label: labels[role] ?? role }));
  }, [userStats?.usersPerRole]);

  const isLoadingAny = wsLoading && deptLoading && statsLoading;

  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <div className="flex flex-col gap-4">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent-50 text-accent-600 dark:bg-accent-100 dark:text-accent-300 [&>svg]:h-5 [&>svg]:w-5">
            <ShieldCheck />
          </span>
          <div>
            <h1 className="text-page font-semibold text-text-primary">Company Overview</h1>
            <p className="text-caption text-text-tertiary mt-0.5">
              {dateStr} &middot; Welcome back, {adminName}
            </p>
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <QuickAction label="Create Workspace" icon={Plus} onClick={() => navigate('/app/create-workspace')} />
          <QuickAction label="Invite User" icon={UserPlus} onClick={() => navigate('/app/admin/users')} />
          <QuickAction label="Departments" icon={Network} onClick={() => navigate('/app/departments')} />
          <QuickAction label="All Workspaces" icon={Boxes} onClick={() => navigate('/app/all-workspaces')} />
        </div>
      </div>

      {isLoadingAny ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Card key={i}><CardBody><Skeleton className="h-16 w-full" /></CardBody></Card>
          ))}
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <KpiCard
            label="Workspaces"
            value={workspaces?.length ?? 0}
            sub={`${activeWorkspaces} active`}
            icon={Building2}
            tone="accent"
            loading={wsLoading}
          />
          <KpiCard
            label="Departments"
            value={departments?.length ?? 0}
            icon={Network}
            tone="info"
            loading={deptLoading}
          />
          <KpiCard
            label="Members"
            value={userStats?.totalUsers ?? 0}
            sub={`${userStats?.recentHires ?? 0} recent hires`}
            icon={Users}
            tone="success"
            loading={statsLoading}
          />
          <KpiCard
            label="Active Members"
            value={userStats?.activeUsers ?? 0}
            icon={UserCheck}
            tone="warning"
            loading={statsLoading}
          />
        </div>
      )}

      {wsId && <AdminDashboardStats workspaceId={wsId} />}

      <div className="grid gap-6 xl:grid-cols-2">
        <Card>
          <CardHeader className="flex items-center justify-between">
            <CardTitle>Workspaces</CardTitle>
            <Badge tone="neutral" variant="soft">{workspaces?.length ?? 0}</Badge>
          </CardHeader>
          <CardBody className="p-0">
            {wsLoading ? (
              <div className="flex flex-col gap-2 p-4">
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
              </div>
            ) : wsError ? (
              <EmptyState icon={<AlertCircle className="h-5 w-5" />} title="Failed to load workspaces" />
            ) : !workspaces?.length ? (
              <EmptyState icon={<Building2 className="h-5 w-5" />} title="No workspaces yet" />
            ) : (
              <div className="divide-y divide-border-subtle">
                {workspaces.slice(0, 6).map((ws) => (
                  <button
                    key={ws.id}
                    type="button"
                    onClick={() => navigate(`/app/workspace-overview?ws=${ws.id}`)}
                    className="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-surface-2 transition-colors"
                  >
                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-accent-600 text-white text-2xs font-bold">
                      {ws.name.slice(0, 2).toUpperCase()}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="text-body font-medium text-text-primary truncate">{ws.name}</p>
                      <p className="text-2xs text-text-tertiary mt-0.5">
                        {ws.memberCount} members &middot; {ws.projectCount} projects
                      </p>
                    </div>
                    <Badge tone={statusTone[ws.status] ?? 'neutral'} variant="soft">{ws.status}</Badge>
                  </button>
                ))}
              </div>
            )}
          </CardBody>
        </Card>

        <Card>
          <CardHeader className="flex items-center justify-between">
            <CardTitle>Departments</CardTitle>
            <Badge tone="neutral" variant="soft">{departments?.length ?? 0}</Badge>
          </CardHeader>
          <CardBody className="p-0">
            {deptLoading ? (
              <div className="flex flex-col gap-2 p-4">
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
              </div>
            ) : !departments?.length ? (
              <EmptyState icon={<Network className="h-5 w-5" />} title="No departments yet" />
            ) : (
              <div className="divide-y divide-border-subtle">
                {departments.slice(0, 6).map((dept) => (
                  <button
                    key={dept.id}
                    type="button"
                    onClick={() => navigate(`/app/departments/${dept.id}?ws=${dept.workspaceId}`)}
                    className="flex w-full items-center gap-3 px-4 py-3 text-left hover:bg-surface-2 transition-colors"
                  >
                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-info-50 text-info-600 dark:bg-info-100 dark:text-info-500 [&>svg]:h-4 [&>svg]:w-4">
                      <Briefcase />
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="text-body font-medium text-text-primary truncate">{dept.name}</p>
                      <p className="text-2xs text-text-tertiary mt-0.5">{dept.teamCount ?? 0} teams</p>
                    </div>
                    <Badge tone={statusTone[dept.status] ?? 'neutral'} variant="soft">{dept.status}</Badge>
                  </button>
                ))}
              </div>
            )}
          </CardBody>
        </Card>
      </div>

      {rolesBreakdown.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><Users /> Members by role</CardTitle>
          </CardHeader>
          <CardBody>
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              {rolesBreakdown.map((r) => (
                <div key={r.role} className="flex items-center justify-between rounded-lg border border-border-subtle bg-surface px-3.5 py-3">
                  <span className="text-caption font-medium text-text-secondary">{r.label}</span>
                  <span className="text-body font-bold text-text-primary">{r.count}</span>
                </div>
              ))}
            </div>
          </CardBody>
        </Card>
      )}
    </div>
  );
}

export default AdminDashboardPage;
