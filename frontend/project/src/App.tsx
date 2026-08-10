import { Suspense, lazy, useState, type ReactNode } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import type { LucideIcon } from 'lucide-react';
import { PageLoader } from './components/ui/PageLoader';
import { WorkspaceGuard } from './components/layout/WorkspaceGuard';
import {
  LayoutGrid,
  FolderKanban,
  CheckSquare,
  FileText,
  BookOpen,
  Bell,
  BarChart3,
  Users,
  Settings,
  Building2,
  ScrollText,
  MessageSquare,
  Network,
  Briefcase,
  LayoutList,
  Plus,
  Shield,
  Lock,
  User,
  Palette,
  Monitor,
  Clock,
  Sparkles,
  Hash,
  Search,
  Activity,
  CalendarDays,
  Archive,
} from 'lucide-react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider } from './lib/theme';
import { AuthProvider, useAuth } from './lib/auth-context';
import { isAdmin, isManager } from './lib/access';
import { useWorkspacesList } from './services/workspace-hooks';
import { ToastProvider } from './components/ui/Toast';
import { AppShell } from './components/layout/AppShell';
import { Card, CardBody } from './components/ui/Card';
import { ErrorBoundary } from './components/errors/ErrorBoundary';
import { ProtectedRoute, PublicRoute, GlobalAuthHandler } from './pages/auth';
import type { BreadcrumbItem } from './components/ui/Breadcrumbs';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 10 * 60 * 1000,
      retry: 2,
      refetchOnWindowFocus: false,
    },
  },
});

// Lazy-loaded pages
const LoginPage = lazy(() => import('./pages/LoginPage').then((m) => ({ default: m.LoginPage })));
const ForgotPasswordPage = lazy(() => import('./pages/ForgotPasswordPage').then((m) => ({ default: m.ForgotPasswordPage })));
const ActivationPage = lazy(() => import('./pages/activate').then((m) => ({ default: m.ActivationPage })));
const ActivationSuccess = lazy(() => import('./pages/activate/ActivationSuccess').then((m) => ({ default: m.ActivationSuccess })));
const ActivationInvalid = lazy(() => import('./pages/activate/ActivationInvalid').then((m) => ({ default: m.ActivationInvalid })));
const ActivationExpired = lazy(() => import('./pages/activate/ActivationExpired').then((m) => ({ default: m.ActivationExpired })));
const ResetPasswordPage = lazy(() => import('./pages/reset-password').then((m) => ({ default: m.ResetPasswordPage })));
const ResetSuccess = lazy(() => import('./pages/reset-password/ResetSuccess').then((m) => ({ default: m.ResetSuccess })));
const ResetInvalid = lazy(() => import('./pages/reset-password/ResetInvalid').then((m) => ({ default: m.ResetInvalid })));
const ResetExpired = lazy(() => import('./pages/reset-password/ResetExpired').then((m) => ({ default: m.ResetExpired })));
const SessionExpiredPage = lazy(() => import('./pages/auth/SessionExpiredPage').then((m) => ({ default: m.SessionExpiredPage })));
const UnauthorizedPage = lazy(() => import('./pages/auth/UnauthorizedPage').then((m) => ({ default: m.UnauthorizedPage })));
const ForbiddenPage = lazy(() => import('./pages/auth/ForbiddenPage').then((m) => ({ default: m.ForbiddenPage })));
const DashboardPage = lazy(() => import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage })));
const AdminDashboardPage = lazy(() => import('./pages/AdminDashboardPage').then((m) => ({ default: m.AdminDashboardPage })));
const WorkspaceManagementPage = lazy(() => import('./pages/WorkspaceManagementPage').then((m) => ({ default: m.WorkspaceManagementPage })));
const OrganizationPage = lazy(() => import('./pages/OrganizationPage').then((m) => ({ default: m.OrganizationPage })));
const DepartmentsPage = lazy(() => import('./pages/DepartmentsPage').then((m) => ({ default: m.DepartmentsPage })));
const TeamsPage = lazy(() => import('./pages/teams/TeamsPage').then((m) => ({ default: m.TeamsPage })));
const MembersPage = lazy(() => import('./pages/members/MembersPage').then((m) => ({ default: m.MembersPage })));
const MemberDetailsPage = lazy(() => import('./pages/members/MemberDetailsPage').then((m) => ({ default: m.MemberDetailsPage })));
const ProjectsPage = lazy(() => import('./pages/projects/ProjectsPage').then((m) => ({ default: m.ProjectsPage })));
const ProjectDetailsPage = lazy(() => import('./pages/projects/ProjectDetailsPage').then((m) => ({ default: m.ProjectDetailsPage })));
const ArchivedProjectsPage = lazy(() => import('./pages/projects/ArchivedProjectsPage').then((m) => ({ default: m.ArchivedProjectsPage })));
const TasksPage = lazy(() => import('./pages/tasks/TasksPage').then((m) => ({ default: m.TasksPage })));
const TaskDetailsPage = lazy(() => import('./pages/tasks/TaskDetailsPage').then((m) => ({ default: m.TaskDetailsPage })));
const AIDashboardPage = lazy(() => import('./pages/ai/AIDashboardPage').then((m) => ({ default: m.AIDashboardPage })));
const PromptLibraryPage = lazy(() => import('./pages/ai/PromptLibraryPage').then((m) => ({ default: m.PromptLibraryPage })));
const HistoryPage = lazy(() => import('./pages/ai/HistoryPage').then((m) => ({ default: m.HistoryPage })));
const AnalyticsAIPage = lazy(() => import('./pages/ai/AnalyticsAIPage').then((m) => ({ default: m.AnalyticsAIPage })));
const HandoverAIPage = lazy(() => import('./pages/ai/HandoverAIPage').then((m) => ({ default: m.HandoverAIPage })));
const HandoverJournalPage = lazy(() => import('./pages/knowledge/components/HandoverJournalPage').then((m) => ({ default: m.HandoverJournalPage })));
const HandoverEntriesPage = lazy(() => import('./pages/handover/HandoverEntriesPage').then((m) => ({ default: m.HandoverEntriesPage })));
const KnowledgeAIPage = lazy(() => import('./pages/ai/KnowledgeAIPage').then((m) => ({ default: m.KnowledgeAIPage })));
const ReportAIPage = lazy(() => import('./pages/ai/ReportAIPage').then((m) => ({ default: m.ReportAIPage })));
const ConversationLayout = lazy(() => import('./components/ai/conversation').then((m) => ({ default: m.ConversationLayout })));
const ConversationPage = lazy(() => import('./components/ai/conversation').then((m) => ({ default: m.ConversationPage })));
const ConversationChatView = lazy(() => import('./components/ai/conversation').then((m) => ({ default: m.ConversationChatView })));
const ReportViewerPage = lazy(() => import('./pages/ai/ReportViewerPage').then((m) => ({ default: m.ReportViewerPage })));
const CollaborationPage = lazy(() => import('./pages/tasks/CollaborationPage').then((m) => ({ default: m.CollaborationPage })));
const AILayout = lazy(() => import('./components/ai/AILayout').then((m) => ({ default: m.AILayout })));
const ProfileLayout = lazy(() => import('./components/profile/ProfileLayout').then((m) => ({ default: m.ProfileLayout })));
const UsersManagementPage = lazy(() => import('./pages/Administration/Users Management/UsersManagementPage').then((m) => ({ default: m.UsersManagementPage })));
const UserDetailsPage = lazy(() => import('./pages/Administration/Users Management/UserDetailsPage').then((m) => ({ default: m.UserDetailsPage })));
const RolesManagementPage = lazy(() => import('./pages/Administration/Role Management/RolesManagementPage').then((m) => ({ default: m.RolesManagementPage })));
const RoleDetailsPage = lazy(() => import('./pages/Administration/Role Management/RoleDetailsPage').then((m) => ({ default: m.RoleDetailsPage })));
const PermissionsManagementPage = lazy(() => import('./pages/Administration/Permission Management/PermissionsManagementPage').then((m) => ({ default: m.PermissionsManagementPage })));
const AuditLogsPage = lazy(() => import('./pages/Administration/Audit Logs/AuditLogsPage').then((m) => ({ default: m.AuditLogsPage })));
const WorkspaceSettingsPage = lazy(() => import('./pages/settings/WorkspaceSettingsPage').then((m) => ({ default: m.WorkspaceSettingsPage })));
const MyProfilePage = lazy(() => import('./pages/profile/MyProfilePage').then((m) => ({ default: m.MyProfilePage })));
const AccountSettingsPage = lazy(() => import('./pages/profile/AccountSettingsPage').then((m) => ({ default: m.AccountSettingsPage })));
const SecurityPage = lazy(() => import('./pages/profile/SecurityPage').then((m) => ({ default: m.SecurityPage })));
const PreferencesPage = lazy(() => import('./pages/profile/PreferencesPage').then((m) => ({ default: m.PreferencesPage })));
const NotificationPreferencesPage = lazy(() => import('./pages/profile/NotificationPreferencesPage').then((m) => ({ default: m.NotificationPreferencesPage })));
const ActiveSessionsPage = lazy(() => import('./pages/profile/ActiveSessionsPage').then((m) => ({ default: m.ActiveSessionsPage })));
const ActivityTimelinePage = lazy(() => import('./pages/profile/ActivityTimelinePage').then((m) => ({ default: m.ActivityTimelinePage })));
const ActivityPage = lazy(() => import('./pages/activity/ActivityPage').then((m) => ({ default: m.ActivityCenterPage })));
const CalendarPage = lazy(() => import('./pages/calendar/CalendarPage').then((m) => ({ default: m.CalendarPage })));
const DocumentsPage = lazy(() => import('./pages/knowledge/components/DocumentsPage').then((m) => ({ default: m.DocumentsPage })));
const DocumentDetailPage = lazy(() => import('./pages/knowledge/components/DocumentDetailPage').then((m) => ({ default: m.DocumentDetailPage })));
const KnowledgeBasePage = lazy(() => import('./pages/knowledge/components/KnowledgeBasePage').then((m) => ({ default: m.KnowledgeBasePage })));
const NotificationsPage = lazy(() => import('./pages/productivity/Notifications/NotificationsPage').then((m) => ({ default: m.NotificationsPage })));
const DepartmentDetailPage = lazy(() => import('./pages/departments/DepartmentDetailPage').then((m) => ({ default: m.DepartmentDetailPage })));
const WorkspaceOverviewPage = lazy(() => import('./pages/WorkspaceOverviewPage').then((m) => ({ default: m.WorkspaceOverviewPage })));
const CreateWorkspacePage = lazy(() => import('./pages/CreateWorkspacePage').then((m) => ({ default: m.CreateWorkspacePage })));
const EditWorkspacePage = lazy(() => import('./pages/EditWorkspacePage').then((m) => ({ default: m.EditWorkspacePage })));
const ReportsPage = lazy(() => import('./pages/ReportsPage').then((m) => ({ default: m.ReportsPage })));
const WorkspaceMembersPage = lazy(() => import('./pages/workspace/WorkspaceMembersPage').then((m) => ({ default: m.WorkspaceMembersPage })));
const WorkspaceActivityPage = lazy(() => import('./pages/workspace/WorkspaceActivityPage').then((m) => ({ default: m.WorkspaceActivityPage })));
const WorkspaceAnalyticsPage = lazy(() => import('./pages/workspace/WorkspaceAnalyticsPage').then((m) => ({ default: m.WorkspaceAnalyticsPage })));
const WorkspaceReportsPage = lazy(() => import('./pages/workspace/WorkspaceReportsPage').then((m) => ({ default: m.WorkspaceReportsPage })));
const ArchivedWorkspacesPage = lazy(() => import('./pages/workspace/ArchivedWorkspacesPage').then((m) => ({ default: m.ArchivedWorkspacesPage })));
const NotFoundPage = lazy(() => import('./components/errors/NotFoundPage').then((m) => ({ default: m.NotFoundPage })));
const CommunicationLayout = lazy(() => import('./pages/communication').then((m) => ({ default: m.CommunicationLayout })));
const CommunicationDashboard = lazy(() => import('./pages/communication').then((m) => ({ default: m.CommunicationDashboard })));
const ConversationList = lazy(() => import('./pages/communication').then((m) => ({ default: m.ConversationList })));
const ChatWindow = lazy(() => import('./pages/communication').then((m) => ({ default: m.ChatWindow })));
const DirectMessages = lazy(() => import('./pages/communication').then((m) => ({ default: m.DirectMessages })));
const AnnouncementsPage = lazy(() => import('./pages/communication').then((m) => ({ default: m.AnnouncementsPage })));
const MessageSearch = lazy(() => import('./pages/communication').then((m) => ({ default: m.MessageSearch })));
const SharedFiles = lazy(() => import('./pages/communication').then((m) => ({ default: m.SharedFiles })));

type RouteMeta = { title: string; icon: LucideIcon; parent?: string };

const routeMeta: Record<string, RouteMeta> = {
  dashboard: { title: 'Dashboard', icon: LayoutGrid },
  'personal-dashboard': { title: 'Personal Dashboard', icon: LayoutGrid },
  ai: { title: 'Collabix AI', icon: Sparkles },
  workspace: { title: 'Workspace', icon: Briefcase },
  'workspace-overview': { title: 'Workspace Overview', icon: LayoutGrid, parent: 'workspace' },
  'all-workspaces': { title: 'All Workspaces', icon: LayoutList, parent: 'workspace' },
  'create-workspace': { title: 'Create Workspace', icon: Plus, parent: 'workspace' },
  projects: { title: 'Projects', icon: FolderKanban },
  'project-details': { title: 'Project Details', icon: FolderKanban, parent: 'projects' },
  'archived-projects': { title: 'Archived Projects', icon: Archive, parent: 'projects' },
  tasks: { title: 'Tasks', icon: CheckSquare },
  collaboration: { title: 'Collaboration', icon: MessageSquare },
  activity: { title: 'Activity Center', icon: Activity },
  'ai-chat': { title: 'AI Chat', icon: MessageSquare, parent: 'ai' },
  calendar: { title: 'Calendar', icon: CalendarDays },
  documents: { title: 'Documents', icon: FileText },
  'document-detail': { title: 'Document Detail', icon: FileText, parent: 'documents' },
  knowledge: { title: 'Knowledge Base', icon: BookOpen },
  handover: { title: 'Handover Journal', icon: ScrollText },
  'handover-entries': { title: 'Handover Entries', icon: ScrollText },
  notifications: { title: 'Notifications', icon: Bell },
  reports: { title: 'Reports', icon: BarChart3 },
  organization: { title: 'Organization', icon: Building2 },
  departments: { title: 'Departments', icon: Network, parent: 'organization' },
  teams: { title: 'Teams', icon: Users, parent: 'organization' },
  members: { title: 'Members', icon: Users, parent: 'organization' },
  'member-details': { title: 'Member Details', icon: Users, parent: 'members' },
  settings: { title: 'Settings', icon: Settings },
  admin: { title: 'Administration', icon: Shield },
  'admin-users': { title: 'Users', icon: Users, parent: 'admin' },
  'admin-user-details': { title: 'User Details', icon: Users, parent: 'admin' },
  'admin-roles': { title: 'Roles', icon: Shield, parent: 'admin' },
  'admin-role-details': { title: 'Role Details', icon: Shield, parent: 'admin' },
  'admin-permissions': { title: 'Permissions', icon: Lock, parent: 'admin' },
  'admin-audit-logs': { title: 'Audit Logs', icon: ScrollText, parent: 'admin' },
  profile: { title: 'My Profile', icon: User },
  'profile-account': { title: 'Account Settings', icon: Settings, parent: 'profile' },
  'profile-security': { title: 'Security', icon: Shield, parent: 'profile' },
  'profile-preferences': { title: 'Preferences', icon: Palette, parent: 'profile' },
  'profile-notifications': { title: 'Notifications', icon: Bell, parent: 'profile' },
  'profile-sessions': { title: 'Active Sessions', icon: Monitor, parent: 'profile' },
  'profile-activity': { title: 'Activity Timeline', icon: Clock, parent: 'profile' },
  'workspace-settings': { title: 'Settings', icon: Settings },
  'workspace-members': { title: 'Members', icon: Users, parent: 'workspace' },
  'workspace-activity': { title: 'Activity', icon: Activity, parent: 'workspace' },
  'workspace-analytics': { title: 'Analytics', icon: BarChart3, parent: 'workspace' },
  'workspace-reports': { title: 'Reports', icon: BarChart3, parent: 'workspace' },
  'archived-workspaces': { title: 'Archived', icon: Archive, parent: 'workspace' },
  communication: { title: 'Communication', icon: MessageSquare },
  'communication-channels': { title: 'Channels', icon: Hash, parent: 'communication' },
  'communication-direct': { title: 'Direct Messages', icon: MessageSquare, parent: 'communication' },
  'communication-announcements': { title: 'Announcements', icon: Bell, parent: 'communication' },
  'communication-search': { title: 'Search Messages', icon: Search, parent: 'communication' },
  'communication-files': { title: 'Shared Files', icon: FileText, parent: 'communication' },
};

function extractNavKey(pathname: string, search = ''): string {
  const parts = pathname.replace('/app/', '').split('/');
  if (parts[0] === 'admin' && parts.length > 1) {
    return `admin-${parts[1]}`;
  }
  if (parts[0] === 'departments' && parts.length > 1) {
    const tab = new URLSearchParams(search).get('tab');
    return tab ? `dept-${tab}` : 'departments';
  }
  if (parts[0] === 'profile' && parts.length > 1) {
    return `profile-${parts[1]}`;
  }
  if (parts[0] === 'ai' && parts.length > 1) {
    return `ai-${parts[1]}`;
  }
  if (parts[0] === 'communication' && parts.length > 1) {
    return `communication-${parts[1]}`;
  }
  if (parts[0] === 'settings') {
    return 'workspace-settings';
  }
  return parts[0] || 'dashboard';
}

function buildBreadcrumbs(navKey: string): BreadcrumbItem[] {
  const meta = routeMeta[navKey];
  if (navKey.startsWith('dept-')) {
    const label = navKey === 'dept-dashboard' ? 'Dashboard' : navKey.charAt(5).toUpperCase() + navKey.slice(6);
    return [{ label: 'Departments', icon: <Network className="h-3.5 w-3.5" /> }, { label }];
  }
  if (!meta) return [{ label: 'Dashboard' }];
  if (meta.parent) {
    const parentMeta = routeMeta[meta.parent];
    const parentIcon = parentMeta ? <parentMeta.icon className="h-3.5 w-3.5" /> : undefined;
    return [
      { label: meta.parent.charAt(0).toUpperCase() + meta.parent.slice(1), icon: parentIcon },
      { label: meta.title },
    ];
  }
  return [{ label: meta.title }];
}

function AppLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const navKey = extractNavKey(location.pathname, location.search);

  return (
    <AppShell
      activeNav={navKey}
      onNavigate={(id) => {
        if (id.startsWith('dept-')) {
          const tab = id.replace('dept-', '');
          const deptId = user?.departmentId;
          const ws = new URLSearchParams(location.search).get('ws');
          if (!deptId) {
            navigate('/app/dashboard');
            return;
          }
          navigate(`/app/departments/${deptId}?tab=${tab}${ws ? `&ws=${ws}` : ''}`);
        } else if (id.startsWith('admin-')) {
          navigate(`/app/admin/${id.replace('admin-', '')}`);
        } else if (id.startsWith('profile-')) {
          navigate(`/app/profile/${id.replace('profile-', '')}`);
        } else if (id === 'workspace-overview' || id === 'workspace') {
          const params = new URLSearchParams(location.search);
          const ws = params.get('ws');
          navigate(`/app/${id}${ws ? `?ws=${ws}` : ''}`);
        } else if (id === 'my-dashboard') {
          const ws = new URLSearchParams(location.search).get('ws');
          navigate(`/app/personal-dashboard${ws ? `?ws=${ws}` : ''}`);
        } else {
          navigate(`/app/${id}`);
        }
      }}
      breadcrumbs={buildBreadcrumbs(navKey)}
    >
      <WorkspaceGuard routeKey={navKey}>
        <Suspense
          fallback={
            <div className="flex items-center justify-center py-20">
              <div className="h-8 w-8 animate-cx-spin rounded-full border-2 border-accent-600 border-t-transparent" />
            </div>
          }
        >
          <Outlet />
        </Suspense>
      </WorkspaceGuard>
    </AppShell>
  );
}

function MemberDetailsRoute() {
  const { memberId } = useParams();
  const navigate = useNavigate();
  if (!memberId) return <Navigate to="/app/members" replace />;
  return <MemberDetailsPage memberId={memberId} onBack={() => navigate('/app/members')} />;
}

function AdminUserDetailsRoute() {
  const { userId } = useParams();
  const navigate = useNavigate();
  if (!userId) return <Navigate to="/app/admin/users" replace />;
  return <UserDetailsPage userId={userId} onBack={() => navigate('/app/admin/users')} />;
}

function AdminRoleDetailsRoute() {
  const { roleId } = useParams();
  const navigate = useNavigate();
  if (!roleId) return <Navigate to="/app/admin/roles" replace />;
  return <RoleDetailsPage roleId={roleId} onBack={() => navigate('/app/admin/roles')} />;
}

function DepartmentDetailRoute() {
  const { departmentId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const isAdminUser = isAdmin(user?.roles ?? []);
  if (!departmentId) return <Navigate to={isAdminUser ? '/app/departments' : '/app/dashboard'} replace />;
  return (
    <Suspense fallback={<PageLoader />}>
      <DepartmentDetailPage
        departmentId={departmentId}
        onBack={() => navigate(isAdminUser ? '/app/departments' : '/app/dashboard')}
      />
    </Suspense>
  );
}

function AdminUsersPage() {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const navigate = useNavigate();
  return (
    <>
      <Suspense fallback={<PageLoader />}>
        {showCreateModal && <CreateUserModal open={showCreateModal} onClose={() => setShowCreateModal(false)} />}
      </Suspense>
      <UsersManagementPage
        onViewUser={(id) => navigate(`/app/admin/users/${id}`)}
        onEditUser={(id) => navigate(`/app/admin/users/${id}`)}
        onCreateUser={() => setShowCreateModal(true)}
      />
    </>
  );
}

const CreateUserModal = lazy(() => import('./pages/Administration/Users Management/CreateUserModal').then((m) => ({ default: m.CreateUserModal })));

function AdminRolesPage() {
  const navigate = useNavigate();
  return (
    <RolesManagementPage
      onCreateRole={() => {}}
      onEditRole={(id) => navigate(`/app/admin/roles/${id}`)}
    />
  );
}

function ProjectDetailsRoute() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  if (!projectId) return <Navigate to="/app/projects" replace />;
  return <ProjectDetailsPage projectId={projectId} onBack={() => navigate('/app/projects')} />;
}

function TasksRoute() {
  const [searchParams] = useSearchParams();
  return (
    <TasksPage
      workspaceId={searchParams.get('ws') ?? ''}
      departmentId={searchParams.get('dept') ?? ''}
      projectId={searchParams.get('proj') ?? ''}
    />
  );
}

function TaskDetailsRoute() {
  const { taskId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  if (!taskId) return <Navigate to="/app/tasks" replace />;
  return (
    <TaskDetailsPage
      taskId={taskId}
      workspaceId={searchParams.get('ws') ?? ''}
      departmentId={searchParams.get('dept') ?? ''}
      projectId={searchParams.get('proj') ?? ''}
      onBack={() => navigate('/app/tasks')}
    />
  );
}

function WorkspaceMembersRoute() {
  const [searchParams] = useSearchParams();
  return <WorkspaceMembersPage workspaceId={searchParams.get('ws') ?? ''} />;
}

function WorkspaceActivityRoute() {
  const [searchParams] = useSearchParams();
  return <WorkspaceActivityPage workspaceId={searchParams.get('ws') ?? ''} />;
}

function WorkspaceAnalyticsRoute() {
  const [searchParams] = useSearchParams();
  return <WorkspaceAnalyticsPage workspaceId={searchParams.get('ws') ?? ''} />;
}

function WorkspaceReportsRoute() {
  const [searchParams] = useSearchParams();
  return <WorkspaceReportsPage workspaceId={searchParams.get('ws') ?? ''} />;
}

function PlaceholderPage({ navKey }: { navKey: string }) {
  const meta = routeMeta[navKey];
  const Icon = meta?.icon ?? LayoutGrid;
  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-3">
        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent-50 text-accent-600 dark:bg-accent-100 dark:text-accent-300 [&>svg]:h-5 [&>svg]:w-5">
          <Icon />
        </span>
        <div>
          <h1 className="text-page font-semibold text-text-primary leading-tight">{meta?.title ?? navKey}</h1>
          <p className="text-caption text-text-tertiary mt-0.5">
            This is where page content will be rendered.
          </p>
        </div>
      </div>
      <Card>
        <CardBody className="py-16">
          <div className="flex flex-col items-center justify-center text-center gap-3">
            <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-surface-2 text-text-tertiary [&>svg]:h-6 [&>svg]:w-6">
              <Icon />
            </span>
            <p className="text-body font-medium text-text-secondary">{meta?.title ?? navKey} page</p>
            <p className="max-w-sm text-caption text-text-tertiary">
              This is a placeholder. The {(meta?.title ?? navKey).toLowerCase()} page content will be built in a future prompt and rendered inside this Application Shell.
            </p>
          </div>
        </CardBody>
      </Card>
    </div>
  );
}

function AdminOnly({ children }: { children: ReactNode }) {
  return (
    <ProtectedRoute requiredRoles={['SUPER_ADMIN', 'ADMIN']} requireAll={false}>
      {children}
    </ProtectedRoute>
  );
}

function HomeDashboard() {
  const { user } = useAuth();
  const location = useLocation();
  const roles = user?.roles ?? [];
  const { data: workspaces } = useWorkspacesList();

  if (isAdmin(roles)) {
    return (
      <Suspense fallback={<PageLoader />}>
        <AdminDashboardPage />
      </Suspense>
    );
  }

  if (isManager(roles) && user?.departmentId && (workspaces?.length ?? 0) > 0) {
    const ws = new URLSearchParams(location.search).get('ws');
    return (
      <Navigate to={`/app/departments/${user.departmentId}?tab=dashboard${ws ? `&ws=${ws}` : ''}`} replace />
    );
  }

  return <DashboardPage />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/app/dashboard" replace />} />
      <Route path="/login" element={
        <PublicRoute><Suspense fallback={<PageLoader />}><LoginPage /></Suspense></PublicRoute>
      } />
      <Route path="/activate" element={
        <PublicRoute><Suspense fallback={<PageLoader />}><ActivationPage /></Suspense></PublicRoute>
      } />
      <Route path="/activate/success" element={
        <PublicRoute><Suspense fallback={<PageLoader />}><ActivationSuccess /></Suspense></PublicRoute>
      } />
      <Route path="/activate/invalid" element={
        <PublicRoute><Suspense fallback={<PageLoader />}><ActivationInvalid /></Suspense></PublicRoute>
      } />
      <Route path="/activate/expired" element={
        <PublicRoute><Suspense fallback={<PageLoader />}><ActivationExpired /></Suspense></PublicRoute>
      } />
      <Route path="/forgot-password" element={
        <PublicRoute><Suspense fallback={<PageLoader />}><ForgotPasswordPage /></Suspense></PublicRoute>
      } />
      <Route path="/reset-password" element={
        <PublicRoute><Suspense fallback={<PageLoader />}><ResetPasswordPage /></Suspense></PublicRoute>
      } />
      <Route path="/reset-password/success" element={
        <PublicRoute><Suspense fallback={<PageLoader />}><ResetSuccess /></Suspense></PublicRoute>
      } />
      <Route path="/reset-password/invalid" element={
        <PublicRoute><Suspense fallback={<PageLoader />}><ResetInvalid /></Suspense></PublicRoute>
      } />
      <Route path="/reset-password/expired" element={
        <PublicRoute><Suspense fallback={<PageLoader />}><ResetExpired /></Suspense></PublicRoute>
      } />
      <Route path="/session-expired" element={
        <Suspense fallback={<PageLoader />}><SessionExpiredPage /></Suspense>
      } />
      <Route path="/401" element={
        <Suspense fallback={<PageLoader />}><UnauthorizedPage /></Suspense>
      } />
      <Route path="/403" element={
        <Suspense fallback={<PageLoader />}><ForbiddenPage /></Suspense>
      } />

      <Route path="/app" element={
        <ProtectedRoute>
          <AppLayout />
        </ProtectedRoute>
      }>
        <Route index element={<Navigate to="dashboard" replace />} />
        <Route path="dashboard" element={<HomeDashboard />} />
        <Route path="personal-dashboard" element={<Suspense fallback={<PageLoader />}><DashboardPage /></Suspense>} />
        <Route path="ai" element={<Suspense fallback={<PageLoader />}><AILayout /></Suspense>}>
          <Route index element={<AIDashboardPage />} />
          <Route path="prompts" element={<PromptLibraryPage />} />
          <Route path="history" element={<HistoryPage />} />
          <Route path="analytics" element={<AnalyticsAIPage />} />
          <Route path="handover" element={<HandoverAIPage />} />
          <Route path="knowledge" element={<KnowledgeAIPage />} />
          <Route path="reports" element={<ReportAIPage />} />
        </Route>
        <Route path="ai/chat" element={<Suspense fallback={<PageLoader />}><ConversationLayout /></Suspense>}>
          <Route index element={<Suspense fallback={<PageLoader />}><ConversationPage /></Suspense>} />
          <Route path=":conversationId" element={<Suspense fallback={<PageLoader />}><ConversationChatView /></Suspense>} />
        </Route>
        <Route path="ai/report/:reportId" element={<Suspense fallback={<PageLoader />}><ReportViewerPage /></Suspense>} />
        <Route path="activity" element={<Suspense fallback={<PageLoader />}><ActivityPage /></Suspense>} />
        <Route path="calendar" element={<Suspense fallback={<PageLoader />}><CalendarPage /></Suspense>} />
        <Route path="workspace-overview" element={<AdminOnly><Suspense fallback={<PageLoader />}><WorkspaceOverviewPage /></Suspense></AdminOnly>} />
        <Route path="all-workspaces" element={<AdminOnly><Suspense fallback={<PageLoader />}><WorkspaceManagementPage /></Suspense></AdminOnly>} />
        <Route path="create-workspace" element={<AdminOnly><Suspense fallback={<PageLoader />}><CreateWorkspacePage /></Suspense></AdminOnly>} />
        <Route path="edit-workspace/:workspaceId" element={<AdminOnly><Suspense fallback={<PageLoader />}><EditWorkspacePage /></Suspense></AdminOnly>} />
        <Route path="workspace-members" element={<AdminOnly><Suspense fallback={<PageLoader />}><WorkspaceMembersRoute /></Suspense></AdminOnly>} />
        <Route path="workspace-activity" element={<AdminOnly><Suspense fallback={<PageLoader />}><WorkspaceActivityRoute /></Suspense></AdminOnly>} />
        <Route path="workspace-analytics" element={<AdminOnly><Suspense fallback={<PageLoader />}><WorkspaceAnalyticsRoute /></Suspense></AdminOnly>} />
        <Route path="workspace-reports" element={<AdminOnly><Suspense fallback={<PageLoader />}><WorkspaceReportsRoute /></Suspense></AdminOnly>} />
        <Route path="archived-workspaces" element={<AdminOnly><Suspense fallback={<PageLoader />}><ArchivedWorkspacesPage /></Suspense></AdminOnly>} />
        <Route path="projects" element={<Suspense fallback={<PageLoader />}><ProjectsPage /></Suspense>} />
        <Route path="projects/:projectId" element={<ProjectDetailsRoute />} />
        <Route path="archived-projects" element={<Suspense fallback={<PageLoader />}><ArchivedProjectsPage /></Suspense>} />
        <Route path="departments/:departmentId" element={<DepartmentDetailRoute />} />
        <Route path="tasks" element={<TasksRoute />} />
        <Route path="tasks/:taskId" element={<TaskDetailsRoute />} />
        <Route path="collaboration" element={<Suspense fallback={<PageLoader />}><CollaborationPage /></Suspense>} />
        <Route path="documents" element={<Suspense fallback={<PageLoader />}><DocumentsPage /></Suspense>} />
        <Route path="documents/:documentId" element={<Suspense fallback={<PageLoader />}><DocumentDetailPage /></Suspense>} />
        <Route path="knowledge" element={<Suspense fallback={<PageLoader />}><KnowledgeBasePage /></Suspense>} />
        <Route path="handover" element={<Suspense fallback={<PageLoader />}><HandoverJournalPage /></Suspense>} />
        <Route path="handover-entries" element={<Suspense fallback={<PageLoader />}><HandoverEntriesPage /></Suspense>} />
        <Route path="notifications" element={<Suspense fallback={<PageLoader />}><NotificationsPage /></Suspense>} />
        <Route path="reports" element={<Suspense fallback={<PageLoader />}><ReportsPage /></Suspense>} />
        <Route path="organization" element={<AdminOnly><Suspense fallback={<PageLoader />}><OrganizationPage /></Suspense></AdminOnly>} />
        <Route path="departments" element={<AdminOnly><Suspense fallback={<PageLoader />}><DepartmentsPage /></Suspense></AdminOnly>} />
        <Route path="teams" element={<AdminOnly><Suspense fallback={<PageLoader />}><TeamsPage /></Suspense></AdminOnly>} />
        <Route path="members" element={<AdminOnly><Suspense fallback={<PageLoader />}><MembersPage /></Suspense></AdminOnly>} />
        <Route path="members/:memberId" element={<AdminOnly><MemberDetailsRoute /></AdminOnly>} />
        <Route path="settings" element={
          <ProtectedRoute requiredPermissions={['WORKSPACE_UPDATE']}>
            <Suspense fallback={<PageLoader />}><WorkspaceSettingsPage /></Suspense>
          </ProtectedRoute>
        } />
        <Route path="profile" element={<Suspense fallback={<PageLoader />}><ProfileLayout /></Suspense>}>
          <Route index element={<MyProfilePage />} />
          <Route path="account" element={<AccountSettingsPage />} />
          <Route path="security" element={<SecurityPage />} />
          <Route path="preferences" element={<PreferencesPage />} />
          <Route path="notifications" element={<NotificationPreferencesPage />} />
          <Route path="sessions" element={<ActiveSessionsPage />} />
          <Route path="activity" element={<ActivityTimelinePage />} />
        </Route>
        <Route path="admin/users" element={<ProtectedRoute requiredRoles={['SUPER_ADMIN', 'ADMIN']} requireAll={false}><AdminUsersPage /></ProtectedRoute>} />
        <Route path="admin/users/:userId" element={<ProtectedRoute requiredRoles={['SUPER_ADMIN', 'ADMIN']} requireAll={false}><AdminUserDetailsRoute /></ProtectedRoute>} />
        <Route path="admin/roles" element={<ProtectedRoute requiredRoles={['SUPER_ADMIN', 'ADMIN']} requireAll={false}><AdminRolesPage /></ProtectedRoute>} />
        <Route path="admin/roles/:roleId" element={<ProtectedRoute requiredRoles={['SUPER_ADMIN', 'ADMIN']} requireAll={false}><AdminRoleDetailsRoute /></ProtectedRoute>} />
        <Route path="admin/permissions" element={<ProtectedRoute requiredRoles={['SUPER_ADMIN', 'ADMIN']} requireAll={false}><PermissionsManagementPage /></ProtectedRoute>} />
        <Route path="admin/audit-logs" element={<ProtectedRoute requiredRoles={['SUPER_ADMIN', 'ADMIN']} requireAll={false}><AuditLogsPage /></ProtectedRoute>} />
        <Route path="communication" element={<Suspense fallback={<PageLoader />}><CommunicationLayout /></Suspense>}>
          <Route index element={<CommunicationDashboard />} />
          <Route path="conversations" element={<ConversationList />} />
          <Route path="chat/:conversationId" element={<ChatWindow />} />
          <Route path="direct-messages" element={<DirectMessages />} />
          <Route path="announcements" element={<AnnouncementsPage />} />
          <Route path="search" element={<MessageSearch />} />
          <Route path="files" element={<SharedFiles />} />
        </Route>
      </Route>

      <Route path="/404" element={<Suspense fallback={<PageLoader />}><NotFoundPage /></Suspense>} />
      <Route path="*" element={<Navigate to="/404" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <ThemeProvider>
        <QueryClientProvider client={queryClient}>
          <AuthProvider>
            <ToastProvider>
              <GlobalAuthHandler />
              <ErrorBoundary>
                <AppRoutes />
              </ErrorBoundary>
            </ToastProvider>
          </AuthProvider>
        </QueryClientProvider>
      </ThemeProvider>
    </BrowserRouter>
  );
}
