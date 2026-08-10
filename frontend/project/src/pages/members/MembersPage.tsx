import { useState, useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Search,
  Plus,
  LayoutGrid,
  LayoutList,
  ChevronDown,
  Briefcase,
  Clock,
  Users,
  CheckCircle2,
  MoreHorizontal,
  Eye,
  UserPlus,
  Edit2,
  UserX,
  AlertCircle,
} from 'lucide-react';
import { Card, CardBody } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Select } from '../../components/ui/Select';
import { Modal } from '../../components/ui/Modal';
import { Badge, type Tone } from '../../components/ui/Badge';
import { Avatar } from '../../components/ui/Avatar';
import { IconButton } from '../../components/ui/IconButton';
import { Table } from '../../components/ui/Table';
import { Progress } from '../../components/ui/Progress';
import { Dropdown, type DropdownItem } from '../../components/ui/Dropdown';
import { EmptyState } from '../../components/ui/EmptyState';
import { Skeleton } from '../../components/ui/Skeleton';
import { useToast } from '../../components/ui/Toast';
import { useWorkspaceId } from '../../hooks/useWorkspaceId';
import {
  useUsersList,
  useDepartmentsList,
  useTeamsByDepartment,
  useUpdateUser,
  useDeleteUser,
  useActivateUser,
  useDeactivateUser,
  useAssignRoles,
} from '../../services/admin-hooks';
import { useWorkspaceTeams } from '../../services/team-hooks';
import { CreateUserModal } from '../Administration/Users Management/CreateUserModal';
import type { MemberProfile, MemberFilters } from './members-types';
import { mapUserToMemberProfile } from './members-utils';
import type { UserResponse } from '../../types';
import { RoleName, UserStatus } from '../../types';

type ViewMode = 'grid' | 'table';

interface ConfirmAction {
  type: 'delete' | 'activate' | 'deactivate';
  user: UserResponse;
}

const ROLE_OPTIONS = Object.values(RoleName).map((r) => ({ value: r, label: r }));

export function MembersPage() {
  const { toast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState<MemberFilters>({});
  const [sortBy, setSortBy] = useState<'name' | 'joinedDate' | 'workload'>('name');
  const [inviteOpen, setInviteOpen] = useState(false);
  const [assignUser, setAssignUser] = useState<UserResponse | null>(null);
  const [confirmAction, setConfirmAction] = useState<ConfirmAction | null>(null);

  const { data: users, isLoading, isError, error } = useUsersList();
  const deleteUser = useDeleteUser();
  const activateUser = useActivateUser();
  const deactivateUser = useDeactivateUser();

  const profiles = useMemo(
    () => (users ?? [])
      .filter((u) => u.status !== UserStatus.ARCHIVED && u.status !== UserStatus.SOFT_DELETED)
      .map(mapUserToMemberProfile),
    [users],
  );

  const filteredMembers = useMemo(() => {
    let result = profiles;

    if (search) {
      const q = search.toLowerCase();
      result = result.filter(
        (m) =>
          m.name.toLowerCase().includes(q) ||
          m.email.toLowerCase().includes(q) ||
          m.jobTitle.toLowerCase().includes(q),
      );
    }

    if (filters.department) {
      result = result.filter((m) => m.department === filters.department);
    }
    if (filters.team) {
      result = result.filter((m) => m.team === filters.team);
    }
    if (filters.role) {
      result = result.filter((m) => m.role === filters.role);
    }
    if (filters.status) {
      result = result.filter((m) => m.status === filters.status);
    }
    if (filters.employmentType) {
      result = result.filter((m) => m.employmentType === filters.employmentType);
    }

    result.sort((a, b) => {
      switch (sortBy) {
        case 'joinedDate':
          return new Date(b.joinedDate).getTime() - new Date(a.joinedDate).getTime();
        case 'workload':
          return b.workload - a.workload;
        case 'name':
        default:
          return a.name.localeCompare(b.name);
      }
    });

    return result;
  }, [profiles, search, filters, sortBy]);

  const departments = useMemo(() => Array.from(new Set(profiles.map((m) => m.department))), [profiles]);
  const teams = useMemo(() => Array.from(new Set(profiles.map((m) => m.team))), [profiles]);
  const roles = useMemo(() => Array.from(new Set(profiles.map((m) => m.role))), [profiles]);

  const openMember = (id: string) => navigate(`/app/members/${id}${location.search}`);

  const handleConfirmAction = async () => {
    if (!confirmAction) return;
    const user = confirmAction.user;
    try {
      if (confirmAction.type === 'delete') {
        await deleteUser.mutateAsync(user.id);
        toast({ title: 'Member removed', tone: 'success' });
      } else if (confirmAction.type === 'deactivate') {
        await deactivateUser.mutateAsync(user.id);
        toast({ title: 'Member deactivated', tone: 'success' });
      } else {
        await activateUser.mutateAsync(user.id);
        toast({ title: 'Member activated', tone: 'success' });
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Action failed';
      toast({ title: msg, tone: 'danger' });
    }
    setConfirmAction(null);
  };

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-8 w-40" />
        <div className="flex items-center justify-between gap-4">
          <Skeleton className="h-10 w-full max-w-sm" />
          <Skeleton className="h-10 w-32" />
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => <Skeleton key={i} className="h-56 rounded-xl" />)}
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <EmptyState
        icon={<AlertCircle className="h-6 w-6" />}
        title="Failed to load members"
        description={error?.message ?? 'An error occurred while fetching workspace members.'}
      />
    );
  }

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col gap-1.5">
        <h1 className="text-page font-semibold text-text-primary">Members</h1>
        <p className="text-body text-text-secondary">
          Manage all members inside the current workspace.
        </p>
      </div>

      {/* Toolbar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 flex-col gap-2 sm:flex-row sm:gap-2">
          <div className="flex-1">
            <Input
              placeholder="Search by name, email, title..."
              leftIcon={<Search />}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              containerClassName="w-full"
            />
          </div>

          <Dropdown
            trigger={
              <Button variant="outline" size="md">
                Department
                <ChevronDown className="h-3.5 w-3.5" />
              </Button>
            }
            items={[
              { label: 'All Departments', onClick: () => setFilters((f) => ({ ...f, department: undefined })) },
              { divider: true },
              ...departments.map((d) => ({
                label: d,
                onClick: () => setFilters((f) => ({ ...f, department: d })),
              })),
            ]}
          />

          <Dropdown
            trigger={
              <Button variant="outline" size="md">
                Team
                <ChevronDown className="h-3.5 w-3.5" />
              </Button>
            }
            items={[
              { label: 'All Teams', onClick: () => setFilters((f) => ({ ...f, team: undefined })) },
              { divider: true },
              ...teams.map((t) => ({
                label: t,
                onClick: () => setFilters((f) => ({ ...f, team: t })),
              })),
            ]}
          />

          <Dropdown
            trigger={
              <Button variant="outline" size="md">
                Role
                <ChevronDown className="h-3.5 w-3.5" />
              </Button>
            }
            items={[
              { label: 'All Roles', onClick: () => setFilters((f) => ({ ...f, role: undefined })) },
              { divider: true },
              ...roles.map((r) => ({
                label: r.charAt(0).toUpperCase() + r.slice(1),
                onClick: () => setFilters((f) => ({ ...f, role: r })),
              })),
            ]}
          />

          <Dropdown
            trigger={
              <Button variant="outline" size="md">
                Sort
                <ChevronDown className="h-3.5 w-3.5" />
              </Button>
            }
            items={[
              { label: 'Name (A-Z)', onClick: () => setSortBy('name') },
              { label: 'Recently Joined', onClick: () => setSortBy('joinedDate') },
              { label: 'Highest Workload', onClick: () => setSortBy('workload') },
            ]}
          />
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <div className="flex items-center gap-1 border border-border-subtle rounded-lg p-1">
            <IconButton
              label="Grid view"
              variant={viewMode === 'grid' ? 'solid' : 'ghost'}
              size="sm"
              onClick={() => setViewMode('grid')}
              className="h-8 w-8"
            >
              <LayoutGrid className="h-4 w-4" />
            </IconButton>
            <IconButton
              label="Table view"
              variant={viewMode === 'table' ? 'solid' : 'ghost'}
              size="sm"
              onClick={() => setViewMode('table')}
              className="h-8 w-8"
            >
              <LayoutList className="h-4 w-4" />
            </IconButton>
          </div>

          <Button leftIcon={<Plus />} onClick={() => setInviteOpen(true)}>Invite Member</Button>
        </div>
      </div>

      {/* Content */}
      {filteredMembers.length === 0 ? (
        <EmptyState
          icon={<Users />}
          title="No members found"
          description="Try adjusting your search or filters to find members."
        />
      ) : viewMode === 'grid' ? (
        <MembersGridView
          members={filteredMembers}
          onView={(m) => openMember(m.id)}
          onAssign={(m) => setAssignUser(users?.find((u) => u.id === m.id) ?? null)}
          onDeactivate={(m) => {
            const user = users?.find((u) => u.id === m.id);
            if (user) setConfirmAction({ type: 'deactivate', user });
          }}
        />
      ) : (
        <MembersTableView
          members={filteredMembers}
          onView={(m) => openMember(m.id)}
          onAssign={(m) => setAssignUser(users?.find((u) => u.id === m.id) ?? null)}
          onToggleStatus={(m) => {
            const user = users?.find((u) => u.id === m.id);
            if (user) setConfirmAction({ type: m.status === 'active' ? 'deactivate' : 'activate', user });
          }}
          onDelete={(m) => {
            const user = users?.find((u) => u.id === m.id);
            if (user) setConfirmAction({ type: 'delete', user });
          }}
        />
      )}

      {/* Invite modal */}
      <CreateUserModal open={inviteOpen} onClose={() => setInviteOpen(false)} />

      {/* Assign / edit member */}
      {assignUser && <AssignMemberModal user={assignUser} onClose={() => setAssignUser(null)} />}

      {/* Confirm action */}
      {confirmAction && (
        <Modal open={!!confirmAction} onClose={() => setConfirmAction(null)} title="Confirm Action" size="sm">
          <p className="text-body text-text-secondary">
            Are you sure you want to {confirmAction.type === 'delete' ? 'remove this member' : confirmAction.type === 'deactivate' ? 'deactivate this member' : 'activate this member'}?
          </p>
          <div className="flex items-center justify-end gap-3 mt-6 pt-5 border-t border-border-subtle">
            <Button variant="outline" onClick={() => setConfirmAction(null)}>Cancel</Button>
            <Button variant={confirmAction.type === 'delete' ? 'danger' : 'primary'} onClick={handleConfirmAction}>
              Confirm
            </Button>
          </div>
        </Modal>
      )}
    </div>
  );
}

function MembersGridView({
  members,
  onView,
  onAssign,
  onDeactivate,
}: {
  members: MemberProfile[];
  onView: (member: MemberProfile) => void;
  onAssign: (member: MemberProfile) => void;
  onDeactivate: (member: MemberProfile) => void;
}) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {members.map((member) => (
        <MemberCard key={member.id} member={member} onView={() => onView(member)} onAssign={() => onAssign(member)} onDeactivate={() => onDeactivate(member)} />
      ))}
    </div>
  );
}

function MemberCard({
  member,
  onView,
  onAssign,
  onDeactivate,
}: {
  member: MemberProfile;
  onView: () => void;
  onAssign: () => void;
  onDeactivate: () => void;
}) {
  const statusColor: Record<typeof member.status, string> = {
    active: 'success',
    away: 'warning',
    offline: 'neutral',
    inactive: 'danger',
  };

  const availabilityColor: Record<typeof member.availability, string> = {
    available: 'success',
    busy: 'danger',
    away: 'warning',
    offline: 'neutral',
  };

  const actionItems: DropdownItem[] = [
    { label: 'View Profile', icon: <Eye className="h-4 w-4" />, onClick: onView },
    { label: 'Assign Team / Department', icon: <UserPlus className="h-4 w-4" />, onClick: onAssign },
    { divider: true },
    { label: 'Edit Member', icon: <Edit2 className="h-4 w-4" />, onClick: onAssign },
    { label: 'Deactivate', icon: <UserX className="h-4 w-4" />, danger: true, onClick: onDeactivate },
  ];

  return (
    <Card className="flex flex-col overflow-hidden hover:border-border-default transition-colors group">
      <CardBody className="flex flex-col gap-4">
        {/* Header with avatar and actions */}
        <div className="flex items-start justify-between gap-3">
          <Avatar name={member.name} size="lg" tone={member.tone} src={member.avatar} />
          <Dropdown trigger={<IconButton label="Actions" variant="ghost" size="sm"><MoreHorizontal className="h-4 w-4" /></IconButton>} items={actionItems} align="right" />
        </div>

        {/* Member info */}
        <div className="flex flex-col gap-1">
          <div>
            <h3 className="text-body font-semibold text-text-primary">{member.name}</h3>
            <p className="text-caption text-text-secondary">{member.jobTitle}</p>
          </div>

          {/* Badges */}
          <div className="flex flex-wrap gap-1.5">
            <Badge tone="accent" variant="soft" dot>
              {member.department}
            </Badge>
            <Badge tone="info" variant="soft" dot>
              {member.role}
            </Badge>
          </div>
        </div>

        {/* Details */}
        <div className="space-y-2 border-t border-border-subtle pt-3">
          <DetailRow icon={<Briefcase />} label={member.team} />
          <DetailRow icon={<Clock />} label={member.lastActive} />
          <DetailRow icon={<CheckCircle2 />} label={`${member.currentTasks} tasks`} />
        </div>

        {/* Workload */}
        <div className="space-y-1.5 border-t border-border-subtle pt-3">
          <div className="flex items-center justify-between">
            <span className="text-2xs font-medium text-text-tertiary">Workload</span>
            <span className="text-2xs font-semibold text-text-primary">{member.workload}%</span>
          </div>
          <Progress value={member.workload} size="sm" />
        </div>

        {/* Status badges */}
        <div className="flex items-center gap-2 border-t border-border-subtle pt-3">
          <Badge tone={statusColor[member.status] as Tone} variant="soft" dot>
            {member.status}
          </Badge>
          <Badge tone={availabilityColor[member.availability] as Tone} variant="soft" dot>
            {member.availability}
          </Badge>
        </div>
      </CardBody>
    </Card>
  );
}

function DetailRow({ icon, label }: { icon: React.ReactNode; label: string }) {
  return (
    <div className="flex items-center gap-2 text-caption text-text-secondary">
      <span className="shrink-0 text-text-tertiary [&>svg]:h-3.5 [&>svg]:w-3.5">{icon}</span>
      <span className="truncate">{label}</span>
    </div>
  );
}

function MembersTableView({
  members,
  onView,
  onAssign,
  onToggleStatus,
  onDelete,
}: {
  members: MemberProfile[];
  onView: (member: MemberProfile) => void;
  onAssign: (member: MemberProfile) => void;
  onToggleStatus: (member: MemberProfile) => void;
  onDelete: (member: MemberProfile) => void;
}) {
  const statusTones: Record<string, Tone> = {
    active: 'success',
    away: 'warning',
    offline: 'neutral',
    inactive: 'danger',
  };

  return (
    <Table
      columns={[
        {
          key: 'name',
          header: 'Member',
          sortable: true,
          width: '240px',
          render: (row: MemberProfile) => (
            <button type="button" onClick={() => onView(row)} className="flex items-center gap-2 text-left hover:opacity-80 transition-opacity">
              <Avatar name={row.name} size="sm" tone={row.tone} src={row.avatar} />
              <div className="min-w-0">
                <p className="text-body font-medium text-text-primary truncate">{row.name}</p>
                <p className="text-caption text-text-tertiary truncate">{row.jobTitle}</p>
              </div>
            </button>
          ),
          sortValue: (row: MemberProfile) => row.name,
        },
        {
          key: 'department',
          header: 'Department',
          width: '140px',
          render: (row: MemberProfile) => <Badge tone="accent" variant="soft">{row.department}</Badge>,
        },
        {
          key: 'team',
          header: 'Team',
          width: '140px',
          render: (row: MemberProfile) => <span className="text-body text-text-secondary">{row.team}</span>,
        },
        {
          key: 'role',
          header: 'Role',
          width: '120px',
          render: (row: MemberProfile) => (
            <Badge tone="info" variant="soft">
              {row.role.charAt(0).toUpperCase() + row.role.slice(1)}
            </Badge>
          ),
        },
        {
          key: 'projects',
          header: 'Projects',
          width: '100px',
          align: 'center',
          render: (row: MemberProfile) => (
            <Badge tone="neutral" variant="soft">
              {row.currentProjects}
            </Badge>
          ),
        },
        {
          key: 'tasks',
          header: 'Tasks',
          width: '100px',
          align: 'center',
          render: (row: MemberProfile) => (
            <Badge tone="neutral" variant="soft">
              {row.currentTasks}
            </Badge>
          ),
        },
        {
          key: 'workload',
          header: 'Workload',
          width: '140px',
          sortable: true,
          render: (row: MemberProfile) => (
            <div className="flex items-center gap-2">
              <Progress value={row.workload} size="sm" className="w-16" />
              <span className="text-caption font-medium text-text-tertiary min-w-[2.5rem] text-right">
                {row.workload}%
              </span>
            </div>
          ),
          sortValue: (row: MemberProfile) => row.workload,
        },
        {
          key: 'status',
          header: 'Status',
          width: '120px',
          render: (row: MemberProfile) => (
            <Badge tone={statusTones[row.status]} variant="soft" dot>
              {row.status}
            </Badge>
          ),
        },
        {
          key: 'lastActive',
          header: 'Last Active',
          width: '120px',
          render: (row: MemberProfile) => <span className="text-caption text-text-tertiary">{row.lastActive}</span>,
        },
        {
          key: 'actions',
          header: '',
          width: '80px',
          align: 'right',
          render: (row: MemberProfile) => (
            <div className="flex justify-end">
              <Dropdown
                trigger={<IconButton label="Actions" variant="ghost" size="sm"><MoreHorizontal className="h-4 w-4" /></IconButton>}
                items={[
                  { label: 'View Profile', icon: <Eye className="h-4 w-4" />, onClick: () => onView(row) },
                  { label: 'Assign Team / Department', icon: <UserPlus className="h-4 w-4" />, onClick: () => onAssign(row) },
                  { divider: true },
                  {
                    label: row.status === 'active' ? 'Deactivate' : 'Reactivate',
                    icon: <UserX className="h-4 w-4" />,
                    danger: row.status === 'active',
                    onClick: () => onToggleStatus(row),
                  },
                  { label: 'Remove', icon: <Edit2 className="h-4 w-4" />, danger: true, onClick: () => onDelete(row) },
                ]}
                align="right"
              />
            </div>
          ),
        },
      ]}
      rows={members}
      rowKey={(m) => m.id}
      pageSize={15}
      stickyHeader
      maxHeight="600px"
    />
  );
}

function AssignMemberModal({ user, onClose }: { user: UserResponse; onClose: () => void }) {
  const wsId = useWorkspaceId();
  const { toast } = useToast();
  const { data: departments } = useDepartmentsList();
  const { data: workspaceTeams } = useWorkspaceTeams(wsId || undefined);
  const updateUser = useUpdateUser();
  const assignRoles = useAssignRoles();
  const [deptId, setDeptId] = useState<string>(user.departmentId ?? '');
  const [teamId, setTeamId] = useState<string>(user.teamId ?? '');
  const [role, setRole] = useState<RoleName>(user.role);
  const [saving, setSaving] = useState(false);

  const { data: deptTeams } = useTeamsByDepartment(wsId, deptId || undefined);

  const availableTeams = deptId
    ? deptTeams ?? (workspaceTeams ?? []).filter((t) => t.departmentId === deptId)
    : workspaceTeams ?? [];

  const handleSave = async () => {
    setSaving(true);
    try {
      if (role && role !== user.role) {
        await assignRoles.mutateAsync({ id: user.id, data: { roles: [role] } });
      }
      await updateUser.mutateAsync({
        id: user.id,
        data: {
          departmentId: deptId || null,
          ...(teamId ? { teamId } : { removeTeam: true }),
        },
      });
      toast({ title: 'Member updated', tone: 'success' });
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update member';
      toast({ title: msg, tone: 'danger' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open onClose={onClose} title={`Assign ${user.firstName} ${user.lastName}`} size="sm">
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-2">
          <Select
            label="Department"
            value={deptId}
            onChange={(e) => { setDeptId(e.target.value); setTeamId(''); }}
            options={[
              { value: '', label: 'Not Assigned' },
              ...(departments ?? []).map((d) => ({ value: d.id, label: d.name })),
            ]}
          />
          <Select
            label="Team"
            value={teamId}
            onChange={(e) => setTeamId(e.target.value)}
            options={[
              { value: '', label: 'Not Assigned' },
              ...availableTeams.map((t) => ({ value: t.id, label: t.name })),
            ]}
          />
          <Select
            label="Role"
            value={role}
            onChange={(e) => setRole(e.target.value as RoleName)}
            options={ROLE_OPTIONS}
          />
          <p className="text-caption text-text-tertiary">
            Selecting a department updates the available team options accordingly.
          </p>
        </div>
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-border-subtle">
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button variant="primary" onClick={handleSave} loading={saving}>Save</Button>
        </div>
      </div>
    </Modal>
  );
}