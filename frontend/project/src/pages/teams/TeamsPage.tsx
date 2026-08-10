import { useMemo, useState } from 'react';
import {
  Users,
  Plus,
  Search,
  MoreHorizontal,
  ExternalLink,
  Settings,
  Archive,
  Trash2,
  FolderKanban,
  CheckSquare,
  Clock,
  Calendar,
  Repeat,
  type LucideIcon,
} from 'lucide-react';
import { Card, CardBody, SectionHeader, ViewToggle } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Avatar } from '../../components/ui/Avatar';
import { IconButton } from '../../components/ui/IconButton';
import { EmptyState } from '../../components/ui/EmptyState';
import { LoadingOverlay } from '../../components/ui/Skeleton';
import { Can } from '../../pages/auth';
import { cn } from '../../lib/cn';
import { useWorkspaceId } from '../../hooks/useWorkspaceId';
import { useWorkspaceAnalytics } from '../../services/department-hooks';
import type { Team, TeamStatus, ModalState } from './types';
import { statusBadge } from './types';
import { useTeamsData } from './data';
import { TeamDetailsPanel } from './TeamDetailsPanel';
import { TeamModal } from './TeamModals';

/* ============================================================   Types & helpers
============================================================ */

type SortKey = 'name' | 'members';

const statToneBg: Record<string, string> = {
  accent: 'bg-accent-50 text-accent-600 dark:bg-accent-100 dark:text-accent-300',
  success: 'bg-success-50 text-success-700 dark:bg-success-100 dark:text-success-500',
  warning: 'bg-warning-50 text-warning-700 dark:bg-warning-100 dark:text-warning-500',
  info: 'bg-info-50 text-info-700 dark:bg-info-100 dark:text-info-500',
  neutral: 'bg-surface-2 text-text-secondary',
};

/* ============================================================   KPI card
============================================================ */

function KpiCard({ label, value, sub, icon, tone }: {
  label: string;
  value: string | number;
  sub: string;
  icon: LucideIcon;
  tone: string;
}) {
  const Icon = icon;
  return (
    <Card className="hover:shadow-cx-md transition-shadow duration-200">
      <CardBody>
        <div className="flex items-start justify-between">
          <div className={cn('flex h-9 w-9 items-center justify-center rounded-lg [&>svg]:h-[18px] [&>svg]:w-[18px]', statToneBg[tone])}>
            <Icon />
          </div>
        </div>
        <p className="mt-3 text-2xs font-medium uppercase tracking-wide text-text-tertiary">{label}</p>
        <p className="mt-1 text-page font-semibold text-text-primary leading-tight">{value}</p>
        <p className="mt-1 text-2xs text-text-tertiary">{sub}</p>
      </CardBody>
    </Card>
  );
}

/* ============================================================   Action menu
============================================================ */

function TeamActionMenu({ team, onAction }: { team: Team; onAction: (kind: 'view' | 'edit' | 'archive' | 'delete', team: Team) => void }) {
  const [open, setOpen] = useState(false);
  const isArchived = team.status === 'archived';
  const actions = [
    { id: 'view' as const, label: 'View Team', icon: ExternalLink },
    { id: 'edit' as const, label: 'Manage Team', icon: Settings },
    { id: 'archive' as const, label: isArchived ? 'Unarchive Team' : 'Archive Team', icon: Archive },
    { id: 'delete' as const, label: 'Delete Team', icon: Trash2, danger: true },
  ];
  return (
    <div className="relative">
      <IconButton label="Actions" variant="ghost" size="sm" className="h-8 w-8" onClick={() => setOpen((v) => !v)}>
        <MoreHorizontal className="h-4 w-4" />
      </IconButton>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <div className="absolute right-0 top-9 z-20 w-48 rounded-lg border border-border-default bg-elevated shadow-cx-lg py-1">
            {actions.map((a) => {
              const Icon = a.icon;
              return (
                <button
                  key={a.id}
                  type="button"
                  onClick={() => { onAction(a.id, team); setOpen(false); }}
                  className={cn(
                    'flex w-full items-center gap-2.5 px-3 py-2 text-body transition-colors',
                    a.danger
                      ? 'text-danger-600 hover:bg-danger-50 dark:text-danger-400 dark:hover:bg-danger-500/10'
                      : 'text-text-secondary hover:bg-surface-2 hover:text-text-primary',
                  )}
                >
                  <Icon className="h-4 w-4 shrink-0" />
                  {a.label}
                </button>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}

/* ============================================================   Team card
============================================================ */

function TeamCard({ team, onAction, onSelect }: {
  team: Team;
  onAction: (kind: 'view' | 'edit' | 'archive' | 'delete', team: Team) => void;
  onSelect: (team: Team) => void;
}) {
  const status = statusBadge[team.status];
  return (
    <Card className="group hover:shadow-cx-md transition-shadow duration-200">
      <CardBody className="flex flex-col gap-4">
        <div className="flex items-start justify-between gap-3">
          <button onClick={() => onSelect(team)} className="flex items-start gap-3 min-w-0 text-left">
            <span className={cn('flex h-10 w-10 items-center justify-center rounded-xl shrink-0 [&>svg]:h-5 [&>svg]:w-5', statToneBg.neutral)}>
              <Users />
            </span>
            <div className="min-w-0">
              <p className="font-semibold text-text-primary truncate group-hover:text-accent-600 dark:group-hover:text-accent-400 transition-colors">{team.name}</p>
              <p className="mt-0.5 text-caption text-text-tertiary line-clamp-2">{team.description}</p>
            </div>
          </button>
          <TeamActionMenu team={team} onAction={onAction} />
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Badge tone={status.tone} variant="soft" dot>{status.label}</Badge>
          <Badge tone="neutral" variant="soft">{team.department}</Badge>
        </div>

        <div className="flex items-center gap-2">
          <Avatar name={team.manager} size="xs" />
          <div>
            <p className="text-2xs text-text-tertiary">Team Manager</p>
            <p className="text-caption font-medium text-text-secondary">{team.manager}</p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 border-t border-border-subtle pt-3">
          <div>
            <p className="text-2xs text-text-tertiary">Members</p>
            <p className="text-body font-semibold text-text-primary">{team.memberCount}</p>
          </div>
          <div>
            <p className="text-2xs text-text-tertiary">Created</p>
            <p className="text-body font-semibold text-text-primary">{team.createdAt || 'No data'}</p>
          </div>
        </div>
      </CardBody>
    </Card>
  );
}

/* ============================================================   Table view
============================================================ */

function TeamTable({ teams, onAction, onSelect }: {
  teams: Team[];
  onAction: (kind: 'view' | 'edit' | 'archive' | 'delete', team: Team) => void;
  onSelect: (team: Team) => void;
}) {
  return (
    <Card>
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border-subtle">
              {['Team', 'Department', 'Manager', 'Members', 'Status', 'Created', 'Actions'].map((h) => (
                <th key={h} className="px-4 py-3 text-left text-2xs font-semibold uppercase tracking-wide text-text-tertiary whitespace-nowrap">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {teams.map((team) => {
              const status = statusBadge[team.status];
              return (
                <tr key={team.id} className="border-b border-border-subtle last:border-0 hover:bg-surface-2 transition-colors">
                  <td className="px-4 py-3">
                    <button onClick={() => onSelect(team)} className="flex items-center gap-2.5 text-left min-w-0">
                      <span className={cn('flex h-8 w-8 items-center justify-center rounded-lg shrink-0 [&>svg]:h-4 [&>svg]:w-4', statToneBg.neutral)}>
                        <Users />
                      </span>
                      <div className="min-w-0">
                        <p className="text-body font-medium text-text-primary truncate hover:text-accent-600 dark:hover:text-accent-400 transition-colors">{team.name}</p>
                        <p className="text-2xs text-text-tertiary truncate max-w-[200px]">{team.description}</p>
                      </div>
                    </button>
                  </td>
                  <td className="px-4 py-3"><Badge tone="neutral" variant="soft">{team.department}</Badge></td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <Avatar name={team.manager} size="xs" />
                      <span className="text-caption text-text-secondary truncate">{team.manager}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-caption text-text-secondary">{team.memberCount}</td>
                  <td className="px-4 py-3"><Badge tone={status.tone} variant="soft" dot>{status.label}</Badge></td>
                  <td className="px-4 py-3 text-caption text-text-tertiary whitespace-nowrap">{team.createdAt || 'No data'}</td>
                  <td className="px-4 py-3"><TeamActionMenu team={team} onAction={onAction} /></td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

/* ============================================================
   Page
============================================================ */

export function TeamsPage() {
  const [query, setQuery] = useState('');
  const [deptFilter, setDeptFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState<'all' | TeamStatus>('all');
  const [managerFilter, setManagerFilter] = useState('all');
  const [sortBy, setSortBy] = useState<SortKey>('name');
  const [view, setView] = useState<'grid' | 'table'>('grid');
  const [selected, setSelected] = useState<Team | null>(null);
  const [modal, setModal] = useState<ModalState>(null);
  const { teams: allTeams, departments, managers, isLoading, isError, refetch } = useTeamsData();
  const analytics = useWorkspaceAnalytics(useWorkspaceId()).data;

  const filtered = useMemo(() => {
    let list = allTeams.filter((t) => {
      const q = query.toLowerCase();
      const matchesQuery = !query || t.name.toLowerCase().includes(q) || t.description.toLowerCase().includes(q);
      const matchesDept = deptFilter === 'all' || t.department === deptFilter;
      const matchesStatus = statusFilter === 'all' || t.status === statusFilter;
      const matchesManager =
        managerFilter === 'all' ||
        (managerFilter === 'unassigned' ? !t.managerId : t.managerId === managerFilter);
      return matchesQuery && matchesDept && matchesStatus && matchesManager;
    });
    list = [...list].sort((a, b) => {
      switch (sortBy) {
        case 'name': return a.name.localeCompare(b.name);
        case 'members': return b.memberCount - a.memberCount;
        default: return 0;
      }
    });
    return list;
  }, [query, deptFilter, statusFilter, managerFilter, sortBy, allTeams]);

  const totalMembers = allTeams.reduce((a, t) => a + t.memberCount, 0);
  const taskMetrics = analytics?.tasks;
  const avgCompletion = taskMetrics?.completionRate ?? 0;

  function handleAction(kind: 'view' | 'edit' | 'archive' | 'delete', team: Team) {
    if (kind === 'view') setSelected(team);
    else if (kind === 'edit') setModal({ kind: 'edit', team });
    else if (kind === 'archive') {
      if (team.status === 'archived') setModal({ kind: 'restore', team });
      else setModal({ kind: 'archive', team });
    } else if (kind === 'delete') {
      setModal({ kind: 'delete', team });
    }
  }

  function handlePanelAction(kind: 'edit' | 'archive' | 'restore' | 'change-manager' | 'delete', team: Team) {
    setSelected(null);
    setModal({ kind, team });
  }

  return (
    <div className="flex flex-col gap-8 animate-fade-in">
      {/* Page header */}
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <h1 className="text-display font-semibold text-text-primary">Teams</h1>
          <p className="mt-1 text-body text-text-secondary">Manage operational teams within your organization.</p>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Badge tone="neutral" variant="soft">{allTeams.length} teams</Badge>
            <Badge tone="neutral" variant="soft">{totalMembers} members</Badge>
            <Badge tone="neutral" variant="soft">{departments.length} departments</Badge>
          </div>
        </div>
        <div className="flex flex-wrap gap-2 shrink-0">
          <Can permission="TEAM_CREATE">
            <Button leftIcon={<Plus />} onClick={() => setModal({ kind: 'create' })}>Create Team</Button>
          </Can>
        </div>
      </div>

      {/* KPI cards */}
      <div>
        <SectionHeader title="Team Statistics" description="Key metrics across the workspace" />
        <div className="grid gap-4 grid-cols-2 sm:grid-cols-3 lg:grid-cols-4">
          <KpiCard label="Teams" value={allTeams.length} sub="in workspace" icon={Users} tone="accent" />
          <KpiCard label="Members" value={totalMembers} sub="across all teams" icon={Users} tone="info" />
          <KpiCard label="Projects" value={analytics?.projectCount ?? 0} sub="in workspace" icon={FolderKanban} tone="neutral" />
          <KpiCard label="Open Tasks" value={taskMetrics?.activeCount ?? 0} sub="in workspace" icon={CheckSquare} tone="warning" />
          <KpiCard label="Overdue" value={taskMetrics?.overdueCount ?? 0} sub="tasks overdue" icon={Calendar} tone="warning" />
          <KpiCard label="Due Today" value={taskMetrics?.dueTodayCount ?? 0} sub="tasks due today" icon={Clock} tone="info" />
          <KpiCard label="Avg Completion" value={`${avgCompletion}%`} sub="task completion rate" icon={Repeat} tone="success" />
        </div>
      </div>

      {/* Teams + toolbar */}
      <div>
        <SectionHeader title="All Teams" description="Browse and manage all teams" action={<ViewToggle mode={view} onChange={setView} modes={[{ id: 'grid', label: 'Grid' }, { id: 'table', label: 'Table' }]} />} />

        {/* Toolbar */}
        <div className="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="relative w-full lg:max-w-xs">
            <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-tertiary" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search teams..."
              className="cx-input h-10 pl-9 w-full"
            />
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <FilterSelect label="Department" value={deptFilter} onChange={setDeptFilter} options={[{ value: 'all', label: 'All' }, ...departments.map((d) => ({ value: d, label: d }))]} />
            <FilterSelect label="Status" value={statusFilter} onChange={(v) => setStatusFilter(v as 'all' | TeamStatus)} options={[{ value: 'all', label: 'All' }, { value: 'active', label: 'Active' }, { value: 'archived', label: 'Archived' }]} />
            <FilterSelect label="Manager" value={managerFilter} onChange={setManagerFilter} options={[{ value: 'all', label: 'All' }, { value: 'unassigned', label: 'Unassigned' }, ...managers.map((m) => ({ value: m.id, label: m.name }))]} />
            <FilterSelect label="Sort" value={sortBy} onChange={(v) => setSortBy(v as SortKey)} options={[{ value: 'name', label: 'Name' }, { value: 'members', label: 'Members' }]} />
          </div>
        </div>

        {isLoading ? (
          <Card>
            <CardBody>
              <LoadingOverlay label="Loading teams..." />
            </CardBody>
          </Card>
        ) : isError ? (
          <Card>
            <CardBody className="py-16">
              <EmptyState
                icon={<Users className="h-6 w-6" />}
                title="Unable to load teams."
                description="There was an error loading your teams. Please try again."
                action={<Button variant="outline" onClick={refetch}>Retry</Button>}
              />
            </CardBody>
          </Card>
        ) : filtered.length === 0 ? (
          <Card>
            <CardBody className="py-16">
              <EmptyState
                icon={<Users className="h-6 w-6" />}
                title={allTeams.length === 0 ? 'No teams have been created yet.' : 'No teams match your filters.'}
                description={allTeams.length === 0 ? 'Create your first team to start organizing members and projects within your departments.' : 'Try adjusting your search or filter criteria.'}
                action={
                  allTeams.length === 0 ? (
                    <Can permission="TEAM_CREATE">
                      <Button leftIcon={<Plus />} onClick={() => setModal({ kind: 'create' })}>Create Team</Button>
                    </Can>
                  ) : undefined
                }
              />
            </CardBody>
          </Card>
        ) : view === 'grid' ? (
          <div className="grid gap-4 sm:grid-cols-2 2xl:grid-cols-3">
            {filtered.map((t) => (
              <TeamCard key={t.id} team={t} onAction={handleAction} onSelect={setSelected} />
            ))}
          </div>
        ) : (
          <TeamTable teams={filtered} onAction={handleAction} onSelect={setSelected} />
        )}
      </div>

      {/* Details panel */}
      {selected && (
        <TeamDetailsPanel team={selected} onClose={() => setSelected(null)} onAction={handlePanelAction} />
      )}

      {/* Modals */}
      <TeamModal state={modal} onClose={() => setModal(null)} />
    </div>
  );
}

function FilterSelect({ label, value, onChange, options }: { label: string; value: string; onChange: (v: string) => void; options: Array<{ value: string; label: string }> }) {
  return (
    <div className="flex items-center gap-1 rounded-lg border border-border-subtle bg-surface p-0.5">
      <span className="px-2 py-1 text-2xs font-medium text-text-tertiary">{label}</span>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="rounded-md bg-transparent px-2 py-1 text-2xs capitalize text-text-secondary outline-none cursor-pointer"
      >
        {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
    </div>
  );
}