import { useState, useMemo, useCallback } from 'react';
import {
  Search,
  ChevronDown,
  Filter,
  MoreHorizontal,
  Eye,
  Edit,
  Power,
  Trash2,
  RotateCcw,
  UserPlus,
} from 'lucide-react';
import { Card, CardBody } from '../../../components/ui/Card';
import { Modal } from '../../../components/ui/Modal';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Badge } from '../../../components/ui/Badge';
import { Avatar } from '../../../components/ui/Avatar';
import { IconButton } from '../../../components/ui/IconButton';
import { Dropdown, type DropdownItem } from '../../../components/ui/Dropdown';
import { Checkbox } from '../../../components/ui/Checkbox';
import { EmptyState } from '../../../components/ui/EmptyState';
import { Pagination } from '../../../components/ui/Pagination';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Can } from '../../../pages/auth';
import { cn } from '../../../lib/cn';
import { useUsersList, useUserStatistics, useDeleteUser, useDeleteUserPermanent, useActivateUser, useDeactivateUser } from '../../../services/admin-hooks';
import { useToast } from '../../../components/ui/Toast';
import { EditUserModal } from './EditUserModal';
import type { UserResponse } from '../../../types';
import { UserStatus } from '../../../types';

const PAGE_SIZE = 10;

export function UsersManagementPage({
  onViewUser,
  onEditUser,
  onCreateUser,
}: {
  onViewUser?: (userId: string) => void;
  onEditUser?: (userId: string) => void;
  onCreateUser?: () => void;
}) {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string | undefined>();
  const [selectedUsers, setSelectedUsers] = useState<Set<string>>(new Set());
  const [page, setPage] = useState(1);
  const [confirmAction, setConfirmAction] = useState<{ type: 'delete' | 'remove' | 'deactivate' | 'activate'; userId?: string; label: string } | null>(null);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<UserResponse | null>(null);

  const { data: users, isLoading, isError, error } = useUsersList();
  const { data: stats } = useUserStatistics();
  const deleteUser = useDeleteUser();
  const removeUser = useDeleteUserPermanent();
  const activateUser = useActivateUser();
  const deactivateUser = useDeactivateUser();
  const { toast } = useToast();

  const filteredUsers = useMemo(() => {
    if (!users) return [];
    let result = [...users];

    if (search) {
      const q = search.toLowerCase();
      result = result.filter(
        (u) =>
          u.firstName.toLowerCase().includes(q) ||
          u.lastName.toLowerCase().includes(q) ||
          u.email.toLowerCase().includes(q),
      );
    }

    if (statusFilter) {
      result = result.filter((u) => u.status === statusFilter);
    }

    return result;
  }, [users, search, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredUsers.length / PAGE_SIZE));
  const paginatedUsers = filteredUsers.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const statuses = users ? Array.from(new Set(users.map((u) => u.status))) : [];

  const displayStats = {
    total: stats?.totalUsers ?? users?.length ?? 0,
    active: stats?.activeUsers ?? (users ? users.filter((u) => u.status === UserStatus.ACTIVE).length : 0),
    inactive: stats?.inactiveUsers ?? (users ? users.filter((u) => u.status === UserStatus.INACTIVE).length : 0),
    pending: stats?.pendingActivationUsers ?? (users ? users.filter((u) => u.status === UserStatus.PENDING_ACTIVATION).length : 0),
  };

  const toggleUserSelection = (userId: string) => {
    const newSelected = new Set(selectedUsers);
    if (newSelected.has(userId)) {
      newSelected.delete(userId);
    } else {
      newSelected.add(userId);
    }
    setSelectedUsers(newSelected);
  };

  const toggleSelectAll = () => {
    if (selectedUsers.size === paginatedUsers.length) {
      setSelectedUsers(new Set());
    } else {
      setSelectedUsers(new Set(paginatedUsers.map((u) => u.id)));
    }
  };

  const handleConfirmAction = useCallback(async () => {
    if (!confirmAction) return;
    try {
      if (confirmAction.type === 'delete') {
        if (confirmAction.userId) {
          await deleteUser.mutateAsync(confirmAction.userId);
        } else {
          await Promise.all(Array.from(selectedUsers).map((id) => deleteUser.mutateAsync(id)));
          setSelectedUsers(new Set());
        }
        toast({ title: confirmAction.label, tone: 'success' });
      } else if (confirmAction.type === 'remove') {
        if (confirmAction.userId) {
          await removeUser.mutateAsync(confirmAction.userId);
        } else {
          await Promise.all(Array.from(selectedUsers).map((id) => removeUser.mutateAsync(id)));
          setSelectedUsers(new Set());
        }
        toast({ title: confirmAction.label, tone: 'success' });
      } else if (confirmAction.type === 'activate') {
        if (confirmAction.userId) {
          await activateUser.mutateAsync(confirmAction.userId);
        }
        toast({ title: confirmAction.label, tone: 'success' });
      } else if (confirmAction.type === 'deactivate') {
        if (confirmAction.userId) {
          await deactivateUser.mutateAsync(confirmAction.userId);
        } else {
          await Promise.all(Array.from(selectedUsers).map((id) => deactivateUser.mutateAsync(id)));
          setSelectedUsers(new Set());
        }
        toast({ title: confirmAction.label, tone: 'success' });
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Action failed';
      toast({ title: msg, tone: 'danger' });
    }
    setConfirmAction(null);
  }, [confirmAction, deleteUser, removeUser, activateUser, deactivateUser, selectedUsers, toast]);

  const handleEditUser = (userId: string) => {
    const user = users?.find((u) => u.id === userId);
    if (user) {
      setEditingUser(user);
      setEditModalOpen(true);
    }
  };

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {[1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-20 rounded-lg" />)}
        </div>
        <Skeleton className="h-96 rounded-xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <EmptyState
        icon={<Filter />}
        title="Failed to load users"
        description={error?.message ?? 'An error occurred while fetching users.'}
      />
    );
  }

  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-page font-semibold text-text-primary">Users</h1>
        <p className="text-body text-text-secondary">
          Manage all users within the current workspace.
        </p>
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard label="Total Users" value={displayStats.total} tone="accent" />
        <StatCard label="Active" value={displayStats.active} tone="success" />
        <StatCard label="Inactive" value={displayStats.inactive} tone="warning" />
        <StatCard label="Pending" value={displayStats.pending} tone="info" />
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 flex-col gap-2 sm:flex-row sm:gap-2">
          <div className="flex-1">
            <Input
              placeholder="Search users by name or email..."
              leftIcon={<Search />}
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(1); }}
            />
          </div>

          <Dropdown
            trigger={
              <Button variant="outline">
                Status
                <ChevronDown className="h-3.5 w-3.5" />
              </Button>
            }
            items={[
              { label: 'All Statuses', onClick: () => setStatusFilter(undefined) },
              { divider: true },
              ...statuses.map((s) => ({
                label: formatStatus(s),
                onClick: () => setStatusFilter(s),
              })),
            ]}
          />
        </div>

        <Can permission="USER_CREATE">
          <Button leftIcon={<UserPlus />} onClick={onCreateUser}>
            Invite User
          </Button>
        </Can>
      </div>

      {paginatedUsers.length === 0 ? (
        <EmptyState
          icon={<Filter />}
          title="No users found"
          description="Try adjusting your search or filters."
        />
      ) : (
        <Card>
          <CardBody className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full" role="table" aria-label="Users table">
                <thead>
                  <tr className="border-b border-border-subtle">
                    <th className="px-4 py-3 text-left w-10">
                      <Checkbox
                        checked={selectedUsers.size === paginatedUsers.length && paginatedUsers.length > 0}
                        onChange={toggleSelectAll}
                        aria-label="Select all users"
                      />
                    </th>
                    <th className="px-4 py-3 text-left text-caption font-semibold text-text-secondary">Name</th>
                    <th className="px-4 py-3 text-left text-caption font-semibold text-text-secondary">Email</th>
                    <th className="px-4 py-3 text-left text-caption font-semibold text-text-secondary">Department</th>
                    <th className="px-4 py-3 text-left text-caption font-semibold text-text-secondary">Team</th>
                    <th className="px-4 py-3 text-left text-caption font-semibold text-text-secondary">Role</th>
                    <th className="px-4 py-3 text-left text-caption font-semibold text-text-secondary">Status</th>
                    <th className="px-4 py-3 text-left text-caption font-semibold text-text-secondary">Last Login</th>
                    <th className="px-4 py-3 text-right text-caption font-semibold text-text-secondary">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedUsers.map((user) => (
                    <UserTableRow
                      key={user.id}
                      user={user}
                      isSelected={selectedUsers.has(user.id)}
                      onSelect={() => toggleUserSelection(user.id)}
                      onView={() => onViewUser?.(user.id)}
                      onEdit={() => handleEditUser(user.id)}
                      onToggleStatus={(id, action) => setConfirmAction({
                        type: action,
                        userId: id,
                        label: action === 'activate' ? 'User activated' : 'User deactivated',
                      })}
                      onDelete={(id) => setConfirmAction({
                        type: 'delete',
                        userId: id,
                        label: 'User deleted',
                      })}
                      onRemove={(id) => setConfirmAction({
                        type: 'remove',
                        userId: id,
                        label: 'User permanently removed',
                      })}
                    />
                  ))}
                </tbody>
              </table>
            </div>
            {totalPages > 1 && (
              <div className="flex items-center justify-between px-4 py-3 border-t border-border-subtle">
                <p className="text-caption text-text-tertiary">
                  Showing {(page - 1) * PAGE_SIZE + 1}–{Math.min(page * PAGE_SIZE, filteredUsers.length)} of {filteredUsers.length}
                </p>
                <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
              </div>
            )}
          </CardBody>
        </Card>
      )}

      {selectedUsers.size > 0 && (
        <div className="flex items-center gap-3 p-4 rounded-lg border border-border-subtle bg-accent-50 dark:bg-accent-100">
          <span className="text-body font-medium text-text-primary">
            {selectedUsers.size} user{selectedUsers.size !== 1 ? 's' : ''} selected
          </span>
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setConfirmAction({ type: 'deactivate', label: `${selectedUsers.size} user(s) deactivated` })}>
              Deactivate
            </Button>
            <Button variant="danger" onClick={() => setConfirmAction({ type: 'remove', label: `${selectedUsers.size} user(s) permanently removed` })}>
              Remove permanently
            </Button>
          </div>
        </div>
      )}

      {confirmAction && (
        <Modal open={!!confirmAction} onClose={() => setConfirmAction(null)} title="Confirm Action" size="sm">
          <p className="text-body text-text-secondary">
            {confirmAction.type === 'remove'
              ? `This will permanently remove ${confirmAction.userId ? 'this user' : `${selectedUsers.size} user(s)`} from the database. This action cannot be undone.`
              : `Are you sure you want to ${confirmAction.type === 'delete' ? 'delete' : confirmAction.type === 'activate' ? 'activate' : 'deactivate'} ${confirmAction.userId ? 'this user' : `${selectedUsers.size} user(s)`}?`}
          </p>
          <div className="flex items-center justify-end gap-3 mt-6 pt-5 border-t border-border-subtle">
            <Button variant="outline" onClick={() => setConfirmAction(null)}>Cancel</Button>
            <Button variant={confirmAction.type === 'delete' || confirmAction.type === 'remove' ? 'danger' : 'primary'} onClick={handleConfirmAction}>
              Confirm
            </Button>
          </div>
        </Modal>
      )}

      <EditUserModal
        open={editModalOpen}
        onClose={() => { setEditModalOpen(false); setEditingUser(null); }}
        user={editingUser}
      />
    </div>
  );
}

function formatStatus(status: string): string {
  return status
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());
}

function StatCard({ label, value, tone }: { label: string; value: number; tone: string }) {
  const bgColor: Record<string, string> = {
    accent: 'bg-accent-50 dark:bg-accent-100 text-accent-700 dark:text-accent-200',
    success: 'bg-success-50 dark:bg-success-100 text-success-700 dark:text-success-200',
    warning: 'bg-warning-50 dark:bg-warning-100 text-warning-700 dark:text-warning-200',
    info: 'bg-info-50 dark:bg-info-100 text-info-700 dark:text-info-200',
  };

  return (
    <div className={cn('rounded-lg border border-border-subtle p-3', bgColor[tone])}>
      <p className="text-2xs font-medium opacity-75">{label}</p>
      <p className="text-section font-semibold mt-1">{value}</p>
    </div>
  );
}

function UserTableRow({
  user,
  isSelected,
  onSelect,
  onView,
  onEdit,
  onToggleStatus,
  onDelete,
  onRemove,
}: {
  user: UserResponse;
  isSelected: boolean;
  onSelect: () => void;
  onView: () => void;
  onEdit: () => void;
  onToggleStatus: (id: string, action: 'activate' | 'deactivate') => void;
  onDelete: (id: string) => void;
  onRemove: (id: string) => void;
}) {
  const statusColor: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    ACTIVE: 'success',
    INACTIVE: 'warning',
    PENDING_ACTIVATION: 'info',
    SUSPENDED: 'danger',
    LOCKED: 'danger',
    ARCHIVED: 'warning',
    SOFT_DELETED: 'danger',
  };

  const actionItems: DropdownItem[] = [
    { label: 'View', icon: <Eye className="h-4 w-4" />, onClick: onView },
    { label: 'Edit', icon: <Edit className="h-4 w-4" />, onClick: onEdit },
    { divider: true },
    {
      label: user.status === 'ACTIVE' ? 'Deactivate' : 'Activate',
      icon: <Power className="h-4 w-4" />,
      onClick: () => onToggleStatus(user.id, user.status === 'ACTIVE' ? 'deactivate' : 'activate'),
    },
    { label: 'Reset Password', icon: <RotateCcw className="h-4 w-4" /> },
    { divider: true },
    { label: 'Delete', icon: <Trash2 className="h-4 w-4" />, danger: true, onClick: () => onDelete(user.id) },
    { label: 'Remove permanently', icon: <Trash2 className="h-4 w-4" />, danger: true, onClick: () => onRemove(user.id) },
  ];

  return (
    <tr className="border-b border-border-subtle hover:bg-surface-2 transition-colors">
      <td className="px-4 py-3">
        <Checkbox checked={isSelected} onChange={onSelect} aria-label={`Select ${user.firstName} ${user.lastName}`} />
      </td>
      <td className="px-4 py-3">
        <button type="button" onClick={onView} className="flex items-center gap-3 hover:opacity-80 transition-opacity">
          <Avatar name={`${user.firstName} ${user.lastName}`} />
          <div className="text-left">
            <p className="text-body font-medium text-text-primary">
              {user.firstName} {user.lastName}
            </p>
          </div>
        </button>
      </td>
      <td className="px-4 py-3">
        <p className="text-body text-text-secondary">{user.email}</p>
      </td>
      <td className="px-4 py-3">
        <p className="text-body text-text-secondary">{user.departmentName ?? <span className="italic text-text-tertiary">Not Assigned</span>}</p>
      </td>
      <td className="px-4 py-3">
        <p className="text-body text-text-secondary">{user.teamName ?? <span className="italic text-text-tertiary">Not Assigned</span>}</p>
      </td>
      <td className="px-4 py-3">
        <Badge tone="info" variant="soft">
          {user.role || '—'}
        </Badge>
      </td>
      <td className="px-4 py-3">
        <Badge tone={statusColor[user.status] ?? 'info'} variant="soft">
          {formatStatus(user.status)}
        </Badge>
      </td>
      <td className="px-4 py-3">
        <p className="text-body text-text-secondary">{user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleDateString() : 'Never'}</p>
      </td>
      <td className="px-4 py-3 text-right">
        <Dropdown
          trigger={<IconButton label="Actions" variant="ghost"><MoreHorizontal className="h-4 w-4" /></IconButton>}
          items={actionItems}
          align="right"
        />
      </td>
    </tr>
  );
}
