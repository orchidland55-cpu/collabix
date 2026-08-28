import { useState, useMemo, type ReactNode } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  LayoutGrid,
  FolderKanban,
  CheckSquare,
  FileText,
  BookOpen,
  Bell,
  BarChart3,
  Users,
  User as UserIcon,
  Settings,
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  ScrollText,
  ClipboardList,
  Building2,
  Network,
  MessageSquare,
  X,
  Briefcase,
  LayoutList,
  Plus,
  Shield,
  Lock,
  Sparkles,
  Activity,
  CalendarDays,
  Archive,
  Hash,
  Search,
  AlertTriangle,
} from 'lucide-react';
import { cn } from '../../lib/cn';
import { useAuth, type User } from '../../lib/auth-context';
import { useUnreadCount } from '../../services/notification-hooks';
import { useAlertUnreadCount } from '../../services/alert-hooks';
import { isAdmin, isManager, detectDeptType, deptTypeLabel } from '../../lib/access';
import { DEPT_TABS } from '../../pages/departments/department-tabs';

export interface NavItem {
  id: string;
  label: string;
  icon: ReactNode;
  badge?: number;
  children?: NavItem[];
}

export interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
  activeId: string;
  onNavigate: (id: string) => void;
}

const ADMIN_NAV: { title: string; items: NavItem[] }[] = [
  {
    title: '',
    items: [{ id: 'dashboard', label: 'Dashboard', icon: <LayoutGrid /> }],
  },
  {
    title: 'Workspace',
    items: [
      {
        id: 'workspace',
        label: 'Workspace',
        icon: <Briefcase />,
        children: [
          { id: 'workspace-overview', label: 'Overview', icon: <LayoutGrid /> },
          { id: 'workspace-members', label: 'Members', icon: <Users /> },
          { id: 'workspace-activity', label: 'Activity', icon: <Activity /> },
          { id: 'workspace-analytics', label: 'Analytics', icon: <BarChart3 /> },
          { id: 'workspace-reports', label: 'Reports', icon: <BarChart3 /> },
          { id: 'all-workspaces', label: 'All Workspaces', icon: <LayoutList /> },
          { id: 'archived-workspaces', label: 'Archived', icon: <Archive /> },
          { id: 'create-workspace', label: 'Create Workspace', icon: <Plus /> },
        ],
      },
      {
        id: 'projects',
        label: 'Projects',
        icon: <FolderKanban />,
        children: [
          { id: 'projects', label: 'Active', icon: <FolderKanban /> },
          { id: 'archived-projects', label: 'Archived', icon: <Archive /> },
        ],
      },
      { id: 'tasks', label: 'Tasks', icon: <CheckSquare /> },
      { id: 'collaboration', label: 'Collaboration', icon: <MessageSquare /> },
      {
        id: 'communication',
        label: 'Communication',
        icon: <MessageSquare />,
        children: [
          { id: 'communication', label: 'Dashboard', icon: <MessageSquare /> },
          { id: 'communication-channels', label: 'Channels', icon: <Hash /> },
          { id: 'communication-direct', label: 'Direct Messages', icon: <MessageSquare /> },
          { id: 'communication-announcements', label: 'Announcements', icon: <Bell /> },
          { id: 'communication-search', label: 'Search', icon: <Search /> },
          { id: 'communication-files', label: 'Shared Files', icon: <FileText /> },
        ],
      },
      { id: 'documents', label: 'Documents', icon: <FileText /> },
      { id: 'knowledge', label: 'Knowledge Base', icon: <BookOpen /> },
      { id: 'handover', label: 'Handover Journal', icon: <ScrollText /> },
      { id: 'ai', label: 'Collabix AI', icon: <Sparkles /> },
      { id: 'activity', label: 'Activity Center', icon: <Activity /> },
      { id: 'notifications', label: 'Notifications', icon: <Bell /> },
      { id: 'alerts', label: 'Alerts', icon: <AlertTriangle /> },
      { id: 'calendar', label: 'Calendar', icon: <CalendarDays /> },
      { id: 'reports', label: 'Reports', icon: <BarChart3 /> },
    ],
  },
  {
    title: 'Organization',
    items: [
      {
        id: 'organization',
        label: 'Organization',
        icon: <Building2 />,
        children: [
          { id: 'departments', label: 'Departments', icon: <Network /> },
          { id: 'teams', label: 'Teams', icon: <Users /> },
          { id: 'members', label: 'Members', icon: <Users /> },
        ],
      },
      { id: 'settings', label: 'Settings', icon: <Settings /> },
    ],
  },
  {
    title: 'Administration',
    items: [
      {
        id: 'admin',
        label: 'Administration',
        icon: <Shield />,
        children: [
          { id: 'admin-users', label: 'Users', icon: <Users /> },
          { id: 'admin-roles', label: 'Roles', icon: <Shield /> },
          { id: 'admin-permissions', label: 'Permissions', icon: <Lock /> },
          { id: 'admin-audit-logs', label: 'Audit Logs', icon: <ScrollText /> },
        ],
      },
    ],
  },
];

function applyPermissionFilters(section: { title: string; items: NavItem[] }, perms: string[]): { title: string; items: NavItem[] } | null {
  const filteredItems = section.items
    .map((item) => {
      if (item.id === 'create-workspace' && !perms.includes('WORKSPACE_CREATE')) return null;
      if (item.id === 'settings' && !perms.includes('WORKSPACE_UPDATE')) return null;
      if (item.children) {
        const filteredChildren = item.children.filter((child) => {
          if (child.id === 'create-workspace' && !perms.includes('WORKSPACE_CREATE')) return false;
          return true;
        });
        return { ...item, children: filteredChildren };
      }
      return item;
    })
    .filter(Boolean) as NavItem[];
  if (filteredItems.length === 0) return null;
  return { ...section, items: filteredItems };
}

function deptModuleItems(user: User): NavItem[] {
  const type = detectDeptType(user.departmentName);
  const defs = DEPT_TABS[type] ?? DEPT_TABS.generic;
  return defs.map((tab) => ({ id: `dept-${tab.id}`, label: tab.label, icon: <tab.icon /> }));
}

const TOOL_ITEMS: NavItem[] = [
  { id: 'projects', label: 'Projects', icon: <FolderKanban /> },
  { id: 'tasks', label: 'My Tasks', icon: <CheckSquare /> },
  { id: 'collaboration', label: 'Collaboration', icon: <MessageSquare /> },
  { id: 'communication', label: 'Communication', icon: <MessageSquare /> },
  { id: 'documents', label: 'Documents', icon: <FileText /> },
  { id: 'knowledge', label: 'Knowledge Base', icon: <BookOpen /> },
  { id: 'handover-entries', label: 'Handover Entries', icon: <ClipboardList /> },
  { id: 'handover', label: 'Handover Journal', icon: <ScrollText /> },
  { id: 'ai', label: 'Collabix AI', icon: <Sparkles /> },
  { id: 'activity', label: 'Activity Center', icon: <Activity /> },
  { id: 'notifications', label: 'Notifications', icon: <Bell /> },
  { id: 'alerts', label: 'Alerts', icon: <AlertTriangle /> },
  { id: 'calendar', label: 'Calendar', icon: <CalendarDays /> },
];

const MEMBER_TOOL_ITEMS: NavItem[] = [
  { id: 'projects', label: 'Projects', icon: <FolderKanban /> },
  { id: 'tasks', label: 'My Tasks', icon: <CheckSquare /> },
  { id: 'communication', label: 'Communication', icon: <MessageSquare /> },
  { id: 'documents', label: 'Documents', icon: <FileText /> },
  { id: 'knowledge', label: 'Knowledge Base', icon: <BookOpen /> },
  { id: 'handover-entries', label: 'Handover Entries', icon: <ClipboardList /> },
  { id: 'handover', label: 'Handover Journal', icon: <ScrollText /> },
  { id: 'ai', label: 'Collabix AI', icon: <Sparkles /> },
  { id: 'notifications', label: 'Notifications', icon: <Bell /> },
  { id: 'alerts', label: 'Alerts', icon: <AlertTriangle /> },
  { id: 'calendar', label: 'Calendar', icon: <CalendarDays /> },
];

function buildNavSections(
  user: User | null,
  notifCount: number | undefined,
  alertCount: number | undefined,
) {
  const roles = user?.roles ?? [];
  const perms = user?.permissions ?? [];

  if (isAdmin(roles)) {
    const sections = ADMIN_NAV
      .map((s) => applyPermissionFilters(s, perms))
      .filter(Boolean) as typeof ADMIN_NAV;
    return applyBadge(sections, notifCount, alertCount);
  }

  const canSeeDept = !!user?.departmentId;
  const dashboardId = canSeeDept ? 'dept-dashboard' : 'dashboard';
  const sections: { title: string; items: NavItem[] }[] = [
    {
      title: '',
      items: canSeeDept
        ? [
            { id: dashboardId, label: 'Department Dashboard', icon: <LayoutGrid /> },
            { id: 'my-dashboard', label: 'Personal Dashboard', icon: <UserIcon /> },
          ]
        : [{ id: dashboardId, label: 'Dashboard', icon: <LayoutGrid /> }],
    },
  ];

  if (canSeeDept) {
    const type = detectDeptType(user.departmentName);
    sections.push({
      title: user.departmentName ?? deptTypeLabel(type),
      items: deptModuleItems(user),
    });
  }

  sections.push({
    title: 'My Work',
    items: isManager(roles) ? TOOL_ITEMS : MEMBER_TOOL_ITEMS,
  });

  return applyBadge(sections, notifCount, alertCount);
}

function applyBadge(
  sections: { title: string; items: NavItem[] }[],
  notifCount: number | undefined,
  alertCount: number | undefined,
): { title: string; items: NavItem[] }[] {
  return sections.map((section) => ({
    ...section,
    items: section.items.map((item) => {
      let badge = item.badge;
      if (item.id === 'notifications' && notifCount !== undefined) {
        badge = notifCount;
      }
      if (item.id === 'alerts' && alertCount !== undefined) {
        badge = alertCount;
      }
      return { ...item, badge };
    }),
  }));
}

function isItemActive(item: NavItem, activeId: string): boolean {
  if (item.id === activeId) return true;
  if (activeId.startsWith('dept-')) {
    return item.children?.some((c) => c.id === 'departments') ?? false;
  }
  return item.children?.some((c) => c.id === activeId) ?? false;
}

function NavLink({
  item,
  collapsed,
  activeId,
  onNavigate,
  expandedSections,
  setExpandedSections,
}: {
  item: NavItem;
  collapsed: boolean;
  activeId: string;
  onNavigate: (id: string) => void;
  expandedSections: Record<string, boolean>;
  setExpandedSections: (id: string, v: boolean) => void;
}) {
  const isActive = isItemActive(item, activeId);
  const hasChildren = !!item.children?.length;
  const isExpandable = item.id === 'projects' || item.id === 'organization' || item.id === 'workspace' || item.id === 'admin';
  const isExpanded = expandedSections[item.id] ?? true;

  if (hasChildren && isExpandable) {
    return (
      <div className="w-full">
        <button
          type="button"
          onClick={() => !collapsed && setExpandedSections(item.id, !isExpanded)}
          title={collapsed ? item.label : undefined}
          className={cn(
            'group relative flex w-full items-center gap-3 rounded-lg px-2.5 py-2 text-body font-medium transition-all duration-150 ease-cx',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent-500',
            isActive
              ? 'bg-gradient-to-r from-accent-50 to-accent-50/40 text-accent-700 dark:from-accent-200/15 dark:to-transparent dark:text-accent-300'
              : 'text-text-secondary hover:bg-surface-2 hover:text-text-primary',
            collapsed && 'justify-center',
          )}
        >
          {isActive && !collapsed && (
            <span className="absolute left-0 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-full bg-gradient-to-b from-accent-400 to-accent-600" />
          )}
          <span className={cn('shrink-0 [&>svg]:h-[18px] [&>svg]:w-[18px]', isActive && 'text-accent-600 dark:text-accent-300')}>
            {item.icon}
          </span>
          {!collapsed && (
            <>
              <span className="flex-1 text-left truncate">{item.label}</span>
              <ChevronDown
                className={cn(
                  'h-4 w-4 text-text-tertiary transition-transform duration-150',
                  isExpanded && 'rotate-180',
                )}
              />
            </>
          )}
        </button>
        {!collapsed && isExpanded && (
          <nav className="mt-0.5 ml-3 flex flex-col gap-0.5 border-l border-border-subtle pl-3 animate-fade-in">
            {item.children!.map((child) => {
              const childActive =
                child.id === activeId ||
                (child.id === 'departments' && activeId.startsWith('dept-'));
              return (
                <button
                  key={child.id}
                  type="button"
                  onClick={() => onNavigate(child.id)}
                  className={cn(
                    'flex items-center gap-2.5 rounded-lg px-2.5 py-1.5 text-body transition-all duration-150 ease-cx',
                    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent-500',
                    childActive
                      ? 'bg-accent-50 text-accent-700 dark:bg-accent-200/15 dark:text-accent-300 font-medium'
                      : 'text-text-tertiary hover:bg-surface-2 hover:text-text-primary',
                  )}
                >
                  <span className="shrink-0 [&>svg]:h-4 [&>svg]:w-4">{child.icon}</span>
                  <span className="text-left truncate">{child.label}</span>
                </button>
              );
            })}
          </nav>
        )}
      </div>
    );
  }

  return (
    <button
      type="button"
      onClick={() => onNavigate(item.id)}
      title={collapsed ? item.label : undefined}
      className={cn(
        'group relative flex w-full items-center gap-3 rounded-lg px-2.5 py-2 text-body font-medium transition-all duration-150 ease-cx',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent-500',
        isActive
          ? 'bg-gradient-to-r from-accent-50 to-accent-50/40 text-accent-700 dark:from-accent-200/15 dark:to-transparent dark:text-accent-300'
          : 'text-text-secondary hover:bg-surface-2 hover:text-text-primary',
        collapsed && 'justify-center',
      )}
    >
      {isActive && !collapsed && (
        <span className="absolute left-0 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-full bg-gradient-to-b from-accent-400 to-accent-600" />
      )}
      <span className={cn('shrink-0 [&>svg]:h-[18px] [&>svg]:w-[18px]', isActive && 'text-accent-600 dark:text-accent-300')}>
        {item.icon}
      </span>
      {!collapsed && (
        <>
          <span className="flex-1 text-left truncate">{item.label}</span>
          {item.badge !== undefined && (
            <span
              className={cn(
                'inline-flex items-center justify-center rounded-full px-1.5 py-0.5 text-2xs font-semibold',
                isActive
                  ? 'bg-accent-200 text-accent-700 dark:bg-accent-200 dark:text-accent-900'
                  : item.badge > 0
                    ? 'bg-danger-100 text-danger-700 dark:bg-danger-100 dark:text-danger-700'
                    : 'bg-surface-2 text-text-tertiary',
              )}
            >
              {item.badge}
            </span>
          )}
        </>
      )}
      {collapsed && item.badge !== undefined && item.badge > 0 && (
        <span className="absolute right-1.5 top-1.5 h-1.5 w-1.5 rounded-full bg-danger-500" />
      )}
    </button>
  );
}

function SidebarContent({
  collapsed,
  activeId,
  onNavigate,
  expandedSections,
  setExpandedSections,
}: {
  collapsed: boolean;
  activeId: string;
  onNavigate: (id: string) => void;
  expandedSections: Record<string, boolean>;
  setExpandedSections: (id: string, v: boolean) => void;
}) {
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const wsId = searchParams.get('ws') ?? '';
  const { data: notifCount } = useUnreadCount(wsId);
  const { data: alertCount } = useAlertUnreadCount(wsId);

  const visibleSections = useMemo(
    () => buildNavSections(user ?? null, notifCount, alertCount),
    [user, notifCount, alertCount],
  );

  return (
    <div className="flex-1 overflow-y-auto py-3 flex flex-col gap-4">
      {visibleSections.map((section, si) => (
        <div key={si} className="px-3">
          {!collapsed && section.title && (
            <p className="px-2 py-2 text-[11px] font-semibold uppercase tracking-[0.08em] text-text-tertiary">
              {section.title}
            </p>
          )}
          <nav className="flex flex-col gap-0.5">
            {section.items.map((item) => (
              <NavLink
                key={item.id}
                item={item}
                collapsed={collapsed}
                activeId={activeId}
                onNavigate={onNavigate}
                expandedSections={expandedSections}
                setExpandedSections={setExpandedSections}
              />
            ))}
          </nav>
        </div>
      ))}
    </div>
  );
}

function Logo({ collapsed }: { collapsed: boolean }) {
  return (
    <div className={cn('flex h-16 items-center border-b border-border-subtle px-4', collapsed && 'justify-center px-0')}>
      <div className="flex items-center gap-2.5">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent-600 text-white shrink-0">
          <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="3" width="7" height="7" rx="1.5" />
            <rect x="14" y="3" width="7" height="7" rx="1.5" />
            <rect x="3" y="14" width="7" height="7" rx="1.5" />
            <rect x="14" y="14" width="7" height="7" rx="1.5" />
          </svg>
        </div>
        {!collapsed && (
          <div className="min-w-0">
            <p className="text-section font-bold text-text-primary leading-none tracking-tight">Collabix</p>
            <p className="text-2xs text-text-tertiary mt-0.5">Enterprise Workspace</p>
          </div>
        )}
      </div>
    </div>
  );
}

export function Sidebar({ collapsed, onToggle, activeId, onNavigate }: SidebarProps) {
  const [expandedSections, setExpandedSectionsState] = useState<Record<string, boolean>>({
    projects: true,
    workspace: true,
    organization: true,
    admin: true,
  });
  const setExpandedSections = (id: string, v: boolean) =>
    setExpandedSectionsState((prev) => ({ ...prev, [id]: v }));

  return (
    <aside
      aria-label="Primary navigation"
      className={cn(
        'relative z-30 hidden lg:flex flex-col border-r border-border-subtle bg-sidebar-bg transition-[width] duration-200 ease-cx',
        collapsed ? 'w-[68px]' : 'w-[248px]',
      )}
    >
      <Logo collapsed={collapsed} />
      <SidebarContent
        collapsed={collapsed}
        activeId={activeId}
        onNavigate={onNavigate}
        expandedSections={expandedSections}
        setExpandedSections={setExpandedSections}
      />
      <button
        type="button"
        onClick={onToggle}
        aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        className="absolute -right-3 top-20 z-40 flex h-6 w-6 items-center justify-center rounded-full border border-border-subtle bg-elevated text-text-tertiary shadow-cx-sm hover:text-text-primary hover:border-border-default transition-colors"
      >
        {collapsed ? <ChevronRight className="h-3.5 w-3.5" /> : <ChevronLeft className="h-3.5 w-3.5" />}
      </button>
    </aside>
  );
}

export function MobileSidebar({
  open,
  onClose,
  activeId,
  onNavigate,
}: {
  open: boolean;
  onClose: () => void;
  activeId: string;
  onNavigate: (id: string) => void;
}) {
  const [expandedSections, setExpandedSectionsState] = useState<Record<string, boolean>>({
    projects: true,
    workspace: true,
    organization: true,
    admin: true,
  });
  const setExpandedSections = (id: string, v: boolean) =>
    setExpandedSectionsState((prev) => ({ ...prev, [id]: v }));
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 lg:hidden">
      <div className="absolute inset-0 bg-text-primary/40 dark:bg-black/60 backdrop-blur-[2px] animate-fade-in" onClick={onClose} />
      <aside role="dialog" aria-modal="true" aria-label="Navigation menu" className="absolute left-0 top-0 h-full w-[260px] max-w-[85vw] border-r border-border-subtle bg-sidebar-bg animate-slide-in-right flex flex-col">
        <div className="flex h-16 items-center justify-between border-b border-border-subtle px-4">
          <Logo collapsed={false} />
          <button
            type="button"
            onClick={onClose}
            aria-label="Close menu"
            className="flex h-8 w-8 items-center justify-center rounded-lg text-text-tertiary hover:bg-surface-2 hover:text-text-primary transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        <SidebarContent
          collapsed={false}
          activeId={activeId}
          onNavigate={(id) => { onNavigate(id); onClose(); }}
          expandedSections={expandedSections}
          setExpandedSections={setExpandedSections}
        />
      </aside>
    </div>
  );
}
