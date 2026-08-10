import { useSearchParams, useNavigate } from 'react-router-dom';
import {
  Briefcase,
  Users,
  FolderKanban,
  CheckSquare,
  Bell,
  Activity,
  AlertCircle,
  ArrowLeft,
  Settings,
  Network,
  FileText,
  BookOpen,
  ScrollText,
  BarChart3,
  UserCheck,
  CalendarClock,
  Sparkles,
  Building2,
  ChevronRight,
  Clock,
  Shield,
  Archive,
} from 'lucide-react';
import { Card, CardBody, CardHeader, CardTitle, SectionHeader } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Skeleton } from '../components/ui/Skeleton';
import { EmptyState } from '../components/ui/EmptyState';
import { Progress } from '../components/ui/Progress';
import { useWorkspaceDetail, useWorkspaceDashboard, usePersonalDashboard } from '../services/workspace-hooks';
import { useDepartmentsList, useUserStatistics, useTeamsByDepartment } from '../services/admin-hooks';
import { useDepartmentDashboard } from '../services/department-hooks';
import { useWorkspaceTeams } from '../services/team-hooks';
import { useWorkspaceDocuments } from '../services/document-hooks';
import { useNotificationsList } from '../services/notification-hooks';
import { useAccessibleHandoverJournals } from '../services/handover-hooks';
import { useAIReportHistory } from '../services/reporting-ai-hooks';
import { detectDeptType } from '../lib/access';
import { cn } from '../lib/cn';

const toneBg: Record<string, string> = {
  accent: 'bg-accent-50 text-accent-600 dark:bg-accent-100 dark:text-accent-300',
  success: 'bg-success-50 text-success-700 dark:bg-success-100 dark:text-success-500',
  warning: 'bg-warning-50 text-warning-700 dark:bg-warning-100 dark:text-warning-500',
  info: 'bg-info-50 text-info-700 dark:bg-info-100 dark:text-info-500',
  neutral: 'bg-surface-2 text-text-secondary',
  danger: 'bg-danger-50 text-danger-700 dark:bg-danger-100 dark:text-danger-500',
};

function KpiCard({ icon, label, value, sub, tone }: { icon: React.ReactNode; label: string; value: string | number; sub: string; tone: string }) {
  return (
    <Card className="hover:shadow-cx-md transition-shadow duration-200">
      <CardBody>
        <span className={cn('flex h-9 w-9 items-center justify-center rounded-lg [&>svg]:h-[18px] [&>svg]:w-[18px]', toneBg[tone])}>{icon}</span>
        <p className="mt-3 text-2xs font-medium uppercase tracking-wide text-text-tertiary">{label}</p>
        <p className="mt-1 text-page font-semibold text-text-primary leading-tight">{value}</p>
        <p className="mt-1 text-2xs text-text-tertiary">{sub}</p>
      </CardBody>
    </Card>
  );
}

function SectionRow({ title, description, action, children }: { title: string; description?: string; action?: React.ReactNode; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <SectionHeader title={title} description={description} />
        {action}
      </div>
      {children}
    </div>
  );
}

function ListRow({
  icon,
  title,
  subtitle,
  meta,
  onClick,
}: {
  icon?: React.ReactNode;
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  meta?: React.ReactNode;
  onClick?: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={!onClick}
      className={cn(
        'flex w-full items-start gap-3 rounded-lg px-2 py-2 text-left transition-colors',
        onClick ? 'hover:bg-surface-2' : 'cursor-default',
      )}
    >
      {icon && <span className="mt-0.5 shrink-0 text-text-tertiary [&>svg]:h-4 [&>svg]:w-4">{icon}</span>}
      <div className="min-w-0 flex-1">
        <p className="truncate text-body font-medium text-text-primary">{title}</p>
        {subtitle && <p className="mt-0.5 line-clamp-1 text-caption text-text-tertiary">{subtitle}</p>}
      </div>
      {meta && <span className="shrink-0 text-2xs text-text-tertiary">{meta}</span>}
    </button>
  );
}

function DepartmentCard({ wsId, dept, onOpen }: { wsId: string; dept: { id: string; name: string; status: string; teamCount?: number }; onOpen: () => void }) {
  const { data: dash, isLoading } = useDepartmentDashboard(wsId, dept.id);
  const { data: teams } = useTeamsByDepartment(wsId, dept.id);
  const type = detectDeptType(dept.name);

  const typeTone: Record<string, string> = {
    hr: 'bg-accent-50 text-accent-600 dark:bg-accent-100 dark:text-accent-300',
    development: 'bg-info-50 text-info-600 dark:bg-info-100 dark:text-info-500',
    ai: 'bg-warning-50 text-warning-600 dark:bg-warning-100 dark:text-warning-500',
    marketing: 'bg-success-50 text-success-700 dark:bg-success-100 dark:text-success-500',
    cybersecurity: 'bg-danger-50 text-danger-700 dark:bg-danger-100 dark:text-danger-500',
    generic: 'bg-surface-2 text-text-secondary',
  };

  return (
    <Card className="hover:shadow-cx-md transition-shadow duration-200">
      <CardBody className="flex flex-col gap-3">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2.5 min-w-0">
            <span className={cn('flex h-8 w-8 shrink-0 items-center justify-center rounded-lg [&>svg]:h-4 [&>svg]:w-4', typeTone[type])}>
              <Building2 />
            </span>
            <p className="truncate text-body font-semibold text-text-primary">{dept.name}</p>
          </div>
          <Badge tone={dept.status === 'ACTIVE' || dept.status?.startsWith('ACTIVE') ? 'success' : 'warning'} variant="soft">
            {dept.status}
          </Badge>
        </div>

        {isLoading ? (
          <Skeleton className="h-16 w-full" />
        ) : (
          <div className="grid grid-cols-4 gap-2">
            <div>
              <p className="text-2xs text-text-tertiary">Members</p>
              <p className="text-body font-semibold text-text-primary">{dash?.overview?.totalMembers ?? 0}</p>
            </div>
            <div>
              <p className="text-2xs text-text-tertiary">Teams</p>
              <p className="text-body font-semibold text-text-primary">{teams?.length ?? dash?.overview?.totalTeams ?? 0}</p>
            </div>
            <div>
              <p className="text-2xs text-text-tertiary">Projects</p>
              <p className="text-body font-semibold text-text-primary">{dash?.overview?.activeProjects ?? 0}</p>
            </div>
            <div>
              <p className="text-2xs text-text-tertiary">Tasks</p>
              <p className="text-body font-semibold text-text-primary">{dash?.taskSummary?.activeTasks ?? 0}</p>
            </div>
          </div>
        )}

        <Button variant="outline" size="sm" fullWidth onClick={onOpen} rightIcon={<ChevronRight className="h-4 w-4" />}>
          Open department
        </Button>
      </CardBody>
    </Card>
  );
}

function formatDate(instant?: string): string {
  if (!instant) return '—';
  try {
    return new Date(instant).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  } catch {
    return instant;
  }
}

function formatDateTime(instant?: string): string {
  if (!instant) return '—';
  try {
    return new Date(instant).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  } catch {
    return instant;
  }
}

export function WorkspaceOverviewPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const workspaceId = searchParams.get('ws') ?? '';

  const { data: ws, isLoading: wsLoading, isError: wsError } = useWorkspaceDetail(workspaceId || undefined);
  const { data: dash, isLoading: dashLoading } = useWorkspaceDashboard(workspaceId || undefined);
  const { data: personal, isLoading: personalLoading } = usePersonalDashboard(workspaceId || undefined);
  const { data: departments, isLoading: deptLoading } = useDepartmentsList();
  const { data: teams, isLoading: teamsLoading } = useWorkspaceTeams(workspaceId || undefined);
  const { data: documents } = useWorkspaceDocuments(workspaceId);
  const { data: notifPage } = useNotificationsList(workspaceId);
  const { data: journalPage } = useAccessibleHandoverJournals(workspaceId, { page: 0, size: 5 });
  const { data: reportPage } = useAIReportHistory(workspaceId, 0, 5);
  const { data: userStats } = useUserStatistics();

  const wsPath = workspaceId ? `?ws=${workspaceId}` : '';

  if (wsLoading || dashLoading) {
    return (
      <div className="flex flex-col gap-6 animate-fade-in">
        <div className="flex items-center gap-3">
          <Skeleton className="h-9 w-9 rounded-lg" />
          <div><Skeleton className="h-6 w-52" /><Skeleton className="h-4 w-80 mt-1" /></div>
        </div>
        <div className="grid gap-4 grid-cols-2 sm:grid-cols-3 lg:grid-cols-6">
          {[1, 2, 3, 4, 5, 6].map((i) => <Skeleton key={i} className="h-28 rounded-xl" />)}
        </div>
        <div className="grid gap-6 lg:grid-cols-2">
          <Skeleton className="h-64 rounded-xl" />
          <Skeleton className="h-64 rounded-xl" />
        </div>
      </div>
    );
  }

  if (wsError || !ws) {
    return (
      <div className="flex flex-col items-center justify-center py-20">
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-danger-50 text-danger-500">
          <AlertCircle className="h-6 w-6" />
        </div>
        <h3 className="text-section font-semibold text-text-primary">Workspace not found</h3>
        <p className="mt-1 text-body text-text-tertiary">Select a workspace from the sidebar to view its overview.</p>
        <Button variant="secondary" className="mt-4" onClick={() => navigate('/app/all-workspaces')}>View All Workspaces</Button>
      </div>
    );
  }

  const s = dash?.workspaceSummary;
  const t = dash?.taskSummary;
  const p = dash?.projectSummary;
  const m = dash?.memberSummary;
  const n = dash?.notificationSummary;

  const activeDepts = (departments ?? []).filter((d) => d.status === 'ACTIVE' || d.status?.startsWith('ACTIVE')).length;
  const archivedDepts = (departments ?? []).filter((d) => d.status === 'ARCHIVED').length;
  const archivedProjects = Math.max(0, (p?.totalCount ?? 0) - (p?.activeCount ?? 0) - (p?.completedCount ?? 0));

  const managers = userStats?.usersPerRole?.['MANAGER'] ?? 0;
  const employees = userStats?.usersPerRole?.['MEMBER'] ?? 0;

  const openDocs = (documents?.content ?? []).filter((d) => d.status !== 'ARCHIVED' && d.status !== 'DELETED');
  const knowledgeCount = personal?.knowledgeArticles?.length ?? 0;
  const handoverCount = journalPage?.page?.totalElements ?? 0;
  const reportCount = reportPage?.page?.totalElements ?? 0;
  const todaysHandovers = personal?.todaysHandovers ?? [];

  const now = Date.now();
  const upcomingDeadlines = (personal?.myTasks ?? [])
    .filter((task) => task.dueAt && new Date(task.dueAt).getTime() >= now && task.status !== 'COMPLETED' && task.status !== 'CANCELLED' && task.status !== 'ARCHIVED')
    .sort((a, b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime())
    .slice(0, 5);

  return (
    <div className="flex flex-col gap-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="sm" onClick={() => navigate(-1)}><ArrowLeft className="h-4 w-4" /></Button>
            <h1 className="text-display font-semibold text-text-primary">{ws.name}</h1>
            <Badge tone={ws.status === 'ACTIVE' ? 'success' : 'neutral'} variant="soft" dot>{ws.status ?? 'Active'}</Badge>
          </div>
          <p className="mt-1 text-body text-text-secondary ml-11">{ws.description ?? 'No description'}</p>
          <div className="mt-3 flex flex-wrap items-center gap-2 ml-11">
            <Badge tone="neutral" variant="soft">{s?.memberCount ?? ws.memberCount} members</Badge>
            <Badge tone="neutral" variant="soft">{s?.teamCount ?? ws.teamCount} teams</Badge>
            <Badge tone="neutral" variant="soft">{departments?.length ?? 0} departments</Badge>
          </div>
        </div>
        <Button variant="outline" leftIcon={<Settings />} onClick={() => navigate(`/app/settings${wsPath}`)}>Settings</Button>
      </div>

      {/* Quick actions */}
      <div className="flex flex-wrap gap-2">
        <Button variant="outline" size="sm" leftIcon={<Users />} onClick={() => navigate(`/app/workspace-members${wsPath}`)}>Members</Button>
        <Button variant="outline" size="sm" leftIcon={<Network />} onClick={() => navigate(`/app/departments${wsPath}`)}>Departments</Button>
        <Button variant="outline" size="sm" leftIcon={<BarChart3 />} onClick={() => navigate(`/app/workspace-analytics${wsPath}`)}>Analytics</Button>
        <Button variant="outline" size="sm" leftIcon={<FileText />} onClick={() => navigate(`/app/workspace-reports${wsPath}`)}>Reports</Button>
        <Button variant="outline" size="sm" leftIcon={<Activity />} onClick={() => navigate(`/app/workspace-activity${wsPath}`)}>Activity</Button>
        <Button variant="outline" size="sm" leftIcon={<Archive />} onClick={() => navigate(`/app/archived-workspaces`)}>Archived</Button>
      </div>

      {/* KPIs */}
      <SectionRow title="Workspace Overview" description="Complete workspace statistics at a glance">
        <div className="grid gap-4 grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-7">
          <KpiCard icon={<Users />} label="Members" value={m?.totalCount ?? s?.memberCount ?? 0} sub={`${m?.activeCount ?? 0} active`} tone="info" />
          <KpiCard icon={<UserCheck />} label="Managers" value={managers} sub="Department managers" tone="accent" />
          <KpiCard icon={<Briefcase />} label="Employees" value={employees} sub="Team members" tone="success" />
          <KpiCard icon={<Network />} label="Departments" value={departments?.length ?? 0} sub={`${activeDepts} active · ${archivedDepts} archived`} tone="neutral" />
          <KpiCard icon={<Shield />} label="Teams" value={teams?.length ?? dash?.teamSummary?.totalCount ?? 0} sub={`${dash?.teamSummary?.activeCount ?? 0} active`} tone="warning" />
          <KpiCard icon={<FolderKanban />} label="Projects" value={p?.totalCount ?? 0} sub={`${p?.activeCount ?? 0} active · ${archivedProjects} archived`} tone="accent" />
          <KpiCard icon={<CheckSquare />} label="Tasks" value={t?.totalTasks ?? 0} sub={`${t?.completedTasks ?? 0} completed`} tone="success" />
          <KpiCard icon={<Clock />} label="Overdue Tasks" value={t?.overdueTasks ?? 0} sub="Past due" tone="danger" />
          <KpiCard icon={<FileText />} label="Documents" value={openDocs.length} sub="Open documents" tone="info" />
          <KpiCard icon={<BookOpen />} label="Knowledge" value={knowledgeCount} sub="Articles" tone="accent" />
          <KpiCard icon={<ScrollText />} label="Handover Journals" value={handoverCount} sub="Generated" tone="warning" />
          <KpiCard icon={<Sparkles />} label="AI Reports" value={reportCount} sub="Generated" tone="success" />
          <KpiCard icon={<Bell />} label="Notifications" value={n?.unread ?? 0} sub={`${n?.total ?? 0} total`} tone="danger" />
          <KpiCard icon={<CalendarClock />} label="Today's Handovers" value={todaysHandovers.length} sub="Due today" tone="neutral" />
        </div>
      </SectionRow>

      {/* Departments */}
      <SectionRow
        title="Departments"
        description="Department summary with live metrics"
        action={<Button variant="ghost" size="sm" onClick={() => navigate(`/app/departments${wsPath}`)} rightIcon={<ChevronRight className="h-4 w-4" />}>View all</Button>}
      >
        {deptLoading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
            {[1, 2, 3, 4, 5].map((i) => <Skeleton key={i} className="h-40 rounded-xl" />)}
          </div>
        ) : !departments?.length ? (
          <Card><CardBody className="py-10"><EmptyState icon={<Network className="h-6 w-6" />} title="No departments yet" description="Departments will appear here once they are created." /></CardBody></Card>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
            {departments.slice(0, 10).map((dept) => (
              <DepartmentCard
                key={dept.id}
                wsId={workspaceId}
                dept={dept}
                onOpen={() => navigate(`/app/departments/${dept.id}?tab=dashboard&ws=${workspaceId}`)}
              />
            ))}
          </div>
        )}
      </SectionRow>

      {/* Recent Projects + Teams */}
      <div className="grid gap-6 lg:grid-cols-2">
        <SectionRow title="Recent Projects" description="Latest projects in the workspace">
          <Card>
            <CardBody className="p-2">
              {!personal?.recentWorkspaceProjects?.length ? (
                <div className="py-6"><EmptyState icon={<FolderKanban className="h-5 w-5" />} title="No projects yet" /></div>
              ) : (
                personal.recentWorkspaceProjects.slice(0, 6).map((proj) => (
                  <ListRow
                    key={proj.id}
                    icon={<FolderKanban />}
                    title={proj.name}
                    subtitle={proj.departmentName ?? 'No department'}
                  />
                ))
              )}
            </CardBody>
          </Card>
        </SectionRow>

        <SectionRow title="Teams" description="Teams across all departments">
          <Card>
            <CardBody className="p-2">
              {teamsLoading ? (
                <div className="flex flex-col gap-2 p-2">{[1, 2, 3].map((i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
              ) : !teams?.length ? (
                <div className="py-6"><EmptyState icon={<Shield className="h-5 w-5" />} title="No teams yet" /></div>
              ) : (
                teams.slice(0, 6).map((team) => (
                  <ListRow
                    key={team.id}
                    icon={<Briefcase />}
                    title={team.name}
                    subtitle={team.departmentName}
                    meta={`${team.memberCount ?? 0} members`}
                  />
                ))
              )}
            </CardBody>
          </Card>
        </SectionRow>
      </div>

      {/* Recent Documents + Notifications */}
      <div className="grid gap-6 lg:grid-cols-2">
        <SectionRow title="Recent Documents" description="Latest documents uploaded">
          <Card>
            <CardBody className="p-2">
              {!personal?.recentDocuments?.length ? (
                <div className="py-6"><EmptyState icon={<FileText className="h-5 w-5" />} title="No documents yet" /></div>
              ) : (
                personal.recentDocuments.slice(0, 6).map((doc) => (
                  <ListRow
                    key={doc.id}
                    icon={<FileText />}
                    title={doc.title || doc.fileName}
                    subtitle={doc.projectName ?? '—'}
                    meta={formatDate(doc.createdAt)}
                  />
                ))
              )}
            </CardBody>
          </Card>
        </SectionRow>

        <SectionRow title="Recent Notifications" description="Latest workspace notifications">
          <Card>
            <CardBody className="p-2">
              {!notifPage?.content?.length ? (
                <div className="py-6"><EmptyState icon={<Bell className="h-5 w-5" />} title="No notifications" /></div>
              ) : (
                notifPage.content.slice(0, 6).map((notif) => (
                  <ListRow
                    key={notif.id}
                    icon={<Bell />}
                    title={notif.title}
                    subtitle={notif.body ?? ''}
                    meta={formatDateTime(notif.createdAt)}
                    onClick={notif.linkUrl ? () => navigate(notif.linkUrl!) : undefined}
                  />
                ))
              )}
            </CardBody>
          </Card>
        </SectionRow>
      </div>

      {/* Handover journals + AI reports */}
      <div className="grid gap-6 lg:grid-cols-2">
        <SectionRow title="Recent Handover Journals" description="Latest generated handover journals">
          <Card>
            <CardBody className="p-2">
              {!journalPage?.content?.length ? (
                <div className="py-6"><EmptyState icon={<ScrollText className="h-5 w-5" />} title="No journals yet" /></div>
              ) : (
                journalPage.content.slice(0, 5).map((journal) => (
                  <ListRow
                    key={journal.id}
                    icon={<ScrollText />}
                    title={`Journal · ${formatDate(journal.journalDate)}`}
                    subtitle={journal.generatedSummary || journal.mainDoneWork}
                    meta={journal.generationStatus}
                  />
                ))
              )}
            </CardBody>
          </Card>
        </SectionRow>

        <SectionRow title="Recent AI Reports" description="Latest generated executive reports">
          <Card>
            <CardBody className="p-2">
              {!reportPage?.content?.length ? (
                <div className="py-6"><EmptyState icon={<Sparkles className="h-5 w-5" />} title="No AI reports yet" description="Generate reports from the Reports center." /></div>
              ) : (
                reportPage.content.slice(0, 5).map((report) => (
                  <ListRow
                    key={report.reportId}
                    icon={<Sparkles />}
                    title={report.title}
                    subtitle={`${report.reportType.replace(/_/g, ' ')} · ${report.generatedBy ?? ''}`}
                    meta={formatDate(report.generationDate)}
                    onClick={() => navigate(`/app/ai/report/${report.reportId}`)}
                  />
                ))
              )}
            </CardBody>
          </Card>
        </SectionRow>
      </div>

      {/* Deadlines + Activity */}
      <div className="grid gap-6 lg:grid-cols-2">
        <SectionRow title="Upcoming Deadlines" description="Tasks due soon">
          <Card>
            <CardBody className="p-2">
              {!upcomingDeadlines.length ? (
                <div className="py-6"><EmptyState icon={<CalendarClock className="h-5 w-5" />} title="No upcoming deadlines" description="Tasks with upcoming due dates will appear here." /></div>
              ) : (
                upcomingDeadlines.map((task) => (
                  <ListRow
                    key={task.id}
                    icon={<CalendarClock />}
                    title={task.title}
                    subtitle={task.projectName ?? '—'}
                    meta={formatDate(task.dueAt)}
                  />
                ))
              )}
            </CardBody>
          </Card>
        </SectionRow>

        <SectionRow title="Recent Activity" description="Latest workspace activity">
          <Card>
            <CardBody className="p-2">
              {!dash?.recentActivities?.length ? (
                <div className="py-6"><EmptyState icon={<Activity className="h-5 w-5" />} title="No recent activity" /></div>
              ) : (
                dash.recentActivities.slice(0, 8).map((a) => (
                  <ListRow
                    key={a.id}
                    icon={<Activity />}
                    title={a.description}
                    subtitle={a.actorName ? `${a.actorName}${a.projectName ? ` · ${a.projectName}` : ''}` : a.projectName}
                    meta={formatDateTime(a.createdAt)}
                  />
                ))
              )}
            </CardBody>
          </Card>
        </SectionRow>
      </div>

      {/* Task completion */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2"><CheckSquare className="h-4 w-4 text-accent-600" /> Task Completion</CardTitle>
        </CardHeader>
        <CardBody>
          {personalLoading ? (
            <div className="flex flex-col gap-2"><Skeleton className="h-4 w-40" /><Skeleton className="h-2.5 w-full rounded-full" /></div>
          ) : (
            <>
              <div className="flex items-center justify-between mb-1.5">
                <p className="text-caption text-text-tertiary">Workspace completion rate</p>
                <p className="text-caption font-semibold text-text-primary">{Math.round(((t?.completedTasks ?? 0) / Math.max(1, t?.totalTasks ?? 0)) * 100)}%</p>
              </div>
              <Progress value={((t?.completedTasks ?? 0) / Math.max(1, t?.totalTasks ?? 0)) * 100} tone="accent" />
            </>
          )}
        </CardBody>
      </Card>
    </div>
  );
}

export default WorkspaceOverviewPage;
