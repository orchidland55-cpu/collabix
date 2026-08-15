import { useState } from 'react';
import {
  X,
  Users,
  Shield,
  Archive,
  RotateCcw,
  AlertTriangle,
  Trash2,
  UserPlus,
  MoreHorizontal,
  UserMinus,
} from 'lucide-react';
import { createPortal } from 'react-dom';
import { Card, CardBody } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Avatar } from '../../components/ui/Avatar';
import { IconButton } from '../../components/ui/IconButton';
import { Button } from '../../components/ui/Button';
import { Modal } from '../../components/ui/Modal';
import { EmptyState } from '../../components/ui/EmptyState';
import { LoadingOverlay } from '../../components/ui/Skeleton';
import { Dropdown } from '../../components/ui/Dropdown';
import { cn } from '../../lib/cn';
import { useToast } from '../../components/ui/Toast';
import { Can } from '../auth';
import { useWorkspaceId } from '../../hooks/useWorkspaceId';
import { useWorkspaceUsers, useAssignMemberToTeam, useRemoveMemberFromTeam } from '../../services/team-hooks';
import { UserStatus } from '../../types';
import type { UserResponse } from '../../types';
import type { Team } from './types';
import { statusBadge } from './types';

const statToneBg: Record<string, string> = {
  accent: 'bg-accent-50 text-accent-600 dark:bg-accent-100 dark:text-accent-300',
  success: 'bg-success-50 text-success-700 dark:bg-success-100 dark:text-success-500',
  warning: 'bg-warning-50 text-warning-700 dark:bg-warning-100 dark:text-warning-500',
  info: 'bg-info-50 text-info-700 dark:bg-info-100 dark:text-info-500',
  neutral: 'bg-surface-2 text-text-secondary',
};

interface Props {
  team: Team;
  onClose: () => void;
  onAction: (kind: 'edit' | 'archive' | 'restore' | 'change-manager' | 'delete', team: Team) => void;
}

export function TeamDetailsPanel({ team, onClose, onAction }: Props) {
  const status = statusBadge[team.status];
  const wsId = useWorkspaceId();
  const { data: users, isLoading: usersLoading } = useWorkspaceUsers(wsId);
  const removeMember = useRemoveMemberFromTeam(wsId);
  const { toast } = useToast();
  const [addMemberOpen, setAddMemberOpen] = useState(false);
  const [removeTarget, setRemoveTarget] = useState<{ id: string; name: string } | null>(null);

  const memberRows = (users ?? [])
    .filter((u) => u.teamId === team.id)
    .map((u) => ({
      id: u.id,
      name: `${u.firstName} ${u.lastName}`.trim(),
      role: u.role,
      email: u.email,
      avatar: u.profilePicture,
    }));

  const handleRemove = async () => {
    if (!removeTarget) return;
    try {
      await removeMember.mutateAsync({
        departmentId: team.departmentId,
        teamId: team.id,
        userId: removeTarget.id,
      });
      toast({ title: 'Member removed from team', tone: 'success' });
      setRemoveTarget(null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to remove member';
      toast({ title: msg, tone: 'danger' });
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex justify-end">
      <div className="absolute inset-0 bg-text-primary/40 dark:bg-black/60 backdrop-blur-[2px] animate-fade-in" onClick={onClose} />
      <div className="relative h-full w-full max-w-xl bg-canvas border-l border-border-subtle shadow-cx-xl animate-slide-in-right flex flex-col">
        {/* Header */}
        <div className="flex items-start justify-between gap-3 border-b border-border-subtle px-5 py-4">
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <h2 className="text-page font-semibold text-text-primary truncate">{team.name}</h2>
              <Badge tone={status.tone} variant="soft" dot>{status.label}</Badge>
            </div>
            <p className="mt-0.5 text-caption text-text-tertiary truncate">{team.department || 'No department'} · Created {team.createdAt || 'No data'}</p>
          </div>
          <IconButton label="Close" variant="ghost" className="h-8 w-8 shrink-0" onClick={onClose}>
            <X className="h-4 w-4" />
          </IconButton>
        </div>

        {/* Scrollable content */}
        <div className="flex-1 overflow-y-auto px-5 py-5 flex flex-col gap-5">
          {/* General info */}
          <section>
            <h3 className="mb-2 text-caption font-semibold uppercase tracking-wide text-text-tertiary">General Information</h3>
            <p className="text-body text-text-secondary leading-relaxed">{team.description || 'No description provided.'}</p>
            <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="rounded-lg border border-border-subtle bg-surface px-3 py-2">
                <p className="text-2xs text-text-tertiary">Department</p>
                <p className="text-caption font-medium text-text-primary">{team.department || 'No data'}</p>
              </div>
              <div className="rounded-lg border border-border-subtle bg-surface px-3 py-2">
                <p className="text-2xs text-text-tertiary">Manager</p>
                <div className="flex items-center gap-1.5">
                  <Avatar name={team.manager} />
                  <span className="text-caption font-medium text-text-primary truncate">{team.manager}</span>
                </div>
              </div>
            </div>
          </section>

          {/* Statistics */}
          <section>
            <h3 className="mb-2 text-caption font-semibold uppercase tracking-wide text-text-tertiary">Statistics</h3>
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              <StatTile icon={Users} tone="accent" label="Members" value={memberRows.length} />
              <StatTile icon={Shield} tone="info" label="Created" value={team.createdAt || 'No data'} />
            </div>
          </section>

          {/* Member list */}
          <section>
            <h3 className="mb-2 text-caption font-semibold uppercase tracking-wide text-text-tertiary">Members</h3>
            {usersLoading ? (
              <Card variant="inner">
                <CardBody>
                  <LoadingOverlay label="Loading members..." />
                </CardBody>
              </Card>
            ) : memberRows.length === 0 ? (
              <Card variant="inner">
                <CardBody className="py-8">
                  <EmptyState
                    icon={<Users className="h-5 w-5" />}
                    title="No members"
                    description="This team has no members assigned yet."
                  />
                </CardBody>
              </Card>
            ) : (
              <div className="flex flex-col gap-2">
                {memberRows.map((m) => (
                  <div key={m.id} className="flex items-center gap-3 rounded-lg border border-border-subtle bg-surface px-3 py-2.5">
                    <Avatar name={m.name} src={m.avatar} />
                    <div className="min-w-0 flex-1">
                      <p className="text-body font-medium text-text-primary truncate">{m.name}</p>
                      <p className="text-2xs text-text-tertiary truncate">{m.role} · {m.email}</p>
                    </div>
                    <Can permission="TEAM_MEMBER_REMOVE">
                      <Dropdown
                        trigger={<IconButton label="Member actions" variant="ghost"><MoreHorizontal className="h-4 w-4" /></IconButton>}
                        items={[
                          {
                            label: 'Remove from team',
                            icon: <UserMinus className="h-4 w-4" />,
                            danger: true,
                            onClick: () => setRemoveTarget({ id: m.id, name: m.name }),
                          },
                        ]}
                        align="right"
                      />
                    </Can>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>

        {/* Footer actions */}
        <div className="flex items-center justify-between gap-2 border-t border-border-subtle px-5 py-3">
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              onClick={() => onAction(team.status === 'archived' ? 'restore' : 'archive', team)}
              leftIcon={team.status === 'archived' ? <RotateCcw className="h-4 w-4" /> : <Archive className="h-4 w-4" />}
            >
              {team.status === 'archived' ? 'Restore' : 'Archive'}
            </Button>
            <Button variant="danger" onClick={() => onAction('delete', team)} leftIcon={<Trash2 className="h-4 w-4" />}>
              Delete
            </Button>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              onClick={() => setAddMemberOpen(true)}
              leftIcon={<UserPlus className="h-4 w-4" />}
            >
              Add Member
            </Button>
            <Button variant="outline" onClick={() => onAction('change-manager', team)} leftIcon={<Shield className="h-4 w-4" />}>
              Change Manager
            </Button>
            <Button onClick={() => onAction('edit', team)} leftIcon={<AlertTriangle className="h-4 w-4" />}>
              Edit
            </Button>
          </div>
        </div>
      </div>
      {addMemberOpen && <AddMemberModal team={team} onClose={() => setAddMemberOpen(false)} />}
      {removeTarget && (
        <Modal open onClose={() => setRemoveTarget(null)} title="Remove member from team?" size="sm">
          <p className="text-body text-text-secondary">
            <span className="font-medium text-text-primary">{removeTarget.name}</span> will be removed from{' '}
            <span className="font-medium text-text-primary">{team.name}</span>. Their Collabix account and workspace
            membership will not be affected.
          </p>
          <div className="mt-5 flex items-center justify-end gap-3 border-t border-border-subtle pt-4">
            <Button variant="outline" onClick={() => setRemoveTarget(null)}>Cancel</Button>
            <Button variant="danger" onClick={handleRemove} disabled={removeMember.isPending} leftIcon={<UserMinus className="h-4 w-4" />}>
              Remove from team
            </Button>
          </div>
        </Modal>
      )}
    </div>,
    document.body,
  );
}

function AddMemberModal({ team, onClose }: { team: Team; onClose: () => void }) {
  const wsId = useWorkspaceId();
  const { data: users, isLoading } = useWorkspaceUsers(wsId);
  const addMember = useAssignMemberToTeam(wsId);
  const { toast } = useToast();
  const [selectedId, setSelectedId] = useState<string>('');

  const candidates = (users ?? [])
    .filter((u) => u.status === UserStatus.ACTIVE && u.teamId !== team.id);

  const handleAdd = async () => {
    if (!selectedId) return;
    try {
      await addMember.mutateAsync({ userId: selectedId, teamId: team.id });
      toast({ title: 'Member added to team', tone: 'success' });
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to add member';
      toast({ title: msg, tone: 'danger' });
    }
  };

  return (
    <Modal open onClose={onClose} title={`Add member to ${team.name}`} size="sm">
      <div className="flex flex-col gap-3">
        {isLoading ? (
          <LoadingOverlay label="Loading members..." />
        ) : candidates.length === 0 ? (
          <EmptyState
            icon={<Users className="h-5 w-5" />}
            title="No members to add"
            description="All active workspace members are already assigned to this team."
          />
        ) : (
          <div className="flex max-h-80 flex-col gap-2 overflow-y-auto pr-1">
            {candidates.map((u: UserResponse) => (
              <MemberOption key={u.id} user={u} selected={selectedId === u.id} onSelect={() => setSelectedId(u.id)} />
            ))}
          </div>
        )}
      </div>
      <div className="mt-5 flex items-center justify-end gap-3 border-t border-border-subtle pt-4">
        <Button variant="outline" onClick={onClose}>Cancel</Button>
        <Button variant="primary" onClick={handleAdd} disabled={!selectedId} leftIcon={<UserPlus className="h-4 w-4" />}>
          Add Member
        </Button>
      </div>
    </Modal>
  );
}

function MemberOption({ user, selected, onSelect }: { user: UserResponse; selected: boolean; onSelect: () => void }) {
  const name = `${user.firstName} ${user.lastName}`.trim();
  return (
    <button
      type="button"
      onClick={onSelect}
      className={cn(
        'flex items-center gap-3 rounded-lg border px-3 py-2.5 text-left transition-colors',
        selected ? 'border-accent-500 bg-accent-50' : 'border-border-subtle bg-surface hover:border-border-default',
      )}
    >
      <Avatar name={name} src={user.profilePicture} />
      <div className="min-w-0 flex-1">
        <p className="text-body font-medium text-text-primary truncate">{name}</p>
        <p className="text-2xs text-text-tertiary truncate">{user.email} · {user.departmentName ?? 'No department'}</p>
      </div>
    </button>
  );
}

function StatTile({ icon: Icon, tone, label, value }: { icon: typeof Users; tone: string; label: string; value: string | number }) {
  return (
    <div className="rounded-lg border border-border-subtle bg-surface px-3 py-2.5">
      <span className={cn('flex h-7 w-7 items-center justify-center rounded-lg [&>svg]:h-3.5 [&>svg]:w-3.5', statToneBg[tone])}>
        <Icon />
      </span>
      <p className="mt-2 text-2xs text-text-tertiary">{label}</p>
      <p className="text-body font-semibold text-text-primary">{value}</p>
    </div>
  );
}