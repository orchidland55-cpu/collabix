import { useState, type FormEvent } from 'react';
import { AlertTriangle, Archive, RotateCcw, Shield, Trash2 } from 'lucide-react';
import { Modal } from '../../components/ui/Modal';
import { Button } from '../../components/ui/Button';
import { Avatar } from '../../components/ui/Avatar';
import { cn } from '../../lib/cn';
import { useWorkspaceId } from '../../hooks/useWorkspaceId';
import { useDepartmentList } from '../../services/department-hooks';
import { useToast } from '../../components/ui/Toast';
import { useWorkspaceUsers, useCreateTeam, useUpdateTeam, useArchiveTeam, useRestoreTeam, useDeleteTeamPermanently } from '../../services/team-hooks';
import type { Team, ModalState } from './types';

export function TeamModal({ state, onClose }: { state: ModalState; onClose: () => void }) {
  if (!state) return null;
  switch (state.kind) {
    case 'create':
      return <CreateModal onClose={onClose} />;
    case 'edit':
      return <EditModal key={state.team.id} team={state.team} onClose={onClose} />;
    case 'archive':
      return <ArchiveModal key={state.team.id} team={state.team} onClose={onClose} />;
    case 'restore':
      return <RestoreModal key={state.team.id} team={state.team} onClose={onClose} />;
    case 'delete':
      return <DeleteModal key={state.team.id} team={state.team} onClose={onClose} />;
    case 'change-manager':
      return <ChangeManagerModal key={state.team.id} team={state.team} onClose={onClose} />;
  }
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-caption font-medium text-text-secondary">{label}</label>
      {children}
    </div>
  );
}

const inputCls = 'cx-input h-10';

interface ManagerOption {
  id: string;
  name: string;
  avatar?: string;
}

function useManagerOptions(): ManagerOption[] {
  const wsId = useWorkspaceId();
  const { data: users } = useWorkspaceUsers(wsId);
  return (users ?? []).map((u) => ({
    id: u.id,
    name: `${u.firstName} ${u.lastName}`.trim(),
    avatar: u.profilePicture,
  }));
}

function CreateModal({ onClose }: { onClose: () => void }) {
  const wsId = useWorkspaceId();
  const { toast } = useToast();
  const { data: depts } = useDepartmentList(wsId);
  const managers = useManagerOptions();
  const create = useCreateTeam(wsId);

  const [name, setName] = useState('');
  const [deptId, setDeptId] = useState(depts?.[0]?.id ?? '');
  const [managerId, setManagerId] = useState('');
  const [desc, setDesc] = useState('');
  const [submitting, setSubmitting] = useState(false);

  function submit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim() || !deptId) return;
    setSubmitting(true);
    create.mutate(
      { departmentId: deptId, data: { name: name.trim(), description: desc.trim() || undefined, managerId: managerId || null } },
      {
        onSuccess: () => {
          toast({ title: 'Team created', tone: 'success' });
          onClose();
        },
        onError: (err) => {
          toast({ title: 'Failed to create team', description: err.message, tone: 'error' });
          setSubmitting(false);
        },
      },
    );
  }

  return (
    <Modal
      open
      onClose={onClose}
      title="Create Team"
      description="Create a new operational team within a department."
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button onClick={submit} disabled={!name.trim() || !deptId || submitting}>Create Team</Button>
        </>
      }
    >
      <form onSubmit={submit} className="flex flex-col gap-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="Team Name">
            <input className={inputCls} value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Backend Team" autoFocus />
          </Field>
          <Field label="Department">
            <select className={inputCls} value={deptId} onChange={(e) => setDeptId(e.target.value)}>
              {!deptId && <option value="">No departments available</option>}
              {(depts ?? []).map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
            </select>
          </Field>
        </div>
        <Field label="Team Manager">
          <select className={inputCls} value={managerId} onChange={(e) => setManagerId(e.target.value)}>
            <option value="">Unassigned</option>
            {managers.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
          </select>
        </Field>
        <Field label="Description">
          <textarea className="cx-input min-h-[80px] resize-none" value={desc} onChange={(e) => setDesc(e.target.value)} placeholder="Short description of the team's purpose..." />
        </Field>
      </form>
    </Modal>
  );
}

function EditModal({ team, onClose }: { team: Team; onClose: () => void }) {
  const wsId = useWorkspaceId();
  const { toast } = useToast();
  const managers = useManagerOptions();
  const update = useUpdateTeam(wsId);

  const [name, setName] = useState(team.name);
  const [managerId, setManagerId] = useState(team.managerId ?? '');
  const [desc, setDesc] = useState(team.description);
  const [submitting, setSubmitting] = useState(false);

  function submit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    setSubmitting(true);
    update.mutate(
      {
        departmentId: team.departmentId,
        teamId: team.id,
        data: {
          name: name.trim(),
          description: desc.trim() || undefined,
          managerId: managerId || null,
          ...(managerId ? {} : { clearManager: true }),
        },
      },
      {
        onSuccess: () => {
          toast({ title: 'Team updated', tone: 'success' });
          onClose();
        },
        onError: (err) => {
          toast({ title: 'Failed to update team', description: err.message, tone: 'error' });
          setSubmitting(false);
        },
      },
    );
  }

  return (
    <Modal
      open
      onClose={onClose}
      title="Edit Team"
      description="Update team information."
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button onClick={submit} disabled={!name.trim() || submitting}>Save Changes</Button>
        </>
      }
    >
      <form onSubmit={submit} className="flex flex-col gap-4">
        <Field label="Team Name">
          <input className={inputCls} value={name} onChange={(e) => setName(e.target.value)} autoFocus />
        </Field>
        <Field label="Team Manager">
          <select className={inputCls} value={managerId} onChange={(e) => setManagerId(e.target.value)}>
            <option value="">Unassigned</option>
            {managers.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
          </select>
        </Field>
        <Field label="Description">
          <textarea className="cx-input min-h-[80px] resize-none" value={desc} onChange={(e) => setDesc(e.target.value)} />
        </Field>
      </form>
    </Modal>
  );
}

function ArchiveModal({ team, onClose }: { team: Team; onClose: () => void }) {
  const wsId = useWorkspaceId();
  const { toast } = useToast();
  const archive = useArchiveTeam(wsId);
  const [submitting, setSubmitting] = useState(false);

  function confirm() {
    setSubmitting(true);
    archive.mutate(
      { departmentId: team.departmentId, teamId: team.id },
      {
        onSuccess: () => {
          toast({ title: 'Team archived', tone: 'success' });
          onClose();
        },
        onError: (err) => {
          toast({ title: 'Failed to archive team', description: err.message, tone: 'error' });
          setSubmitting(false);
        },
      },
    );
  }

  return (
    <Modal
      open
      onClose={onClose}
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button variant="danger" onClick={confirm} leftIcon={<Archive className="h-4 w-4" />} disabled={submitting}>Archive Team</Button>
        </>
      }
    >
      <div className="flex flex-col items-center gap-3 text-center py-2">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-warning-50 text-warning-600 dark:bg-warning-100 dark:text-warning-500">
          <AlertTriangle className="h-6 w-6" />
        </span>
        <h3 className="text-page font-semibold text-text-primary">Archive "{team.name}"?</h3>
        <p className="text-body text-text-secondary max-w-sm">
          The team will be moved to archived status. Members remain assigned, but the team will no longer appear in active lists.
        </p>
      </div>
    </Modal>
  );
}

function RestoreModal({ team, onClose }: { team: Team; onClose: () => void }) {
  const wsId = useWorkspaceId();
  const { toast } = useToast();
  const restore = useRestoreTeam(wsId);
  const [submitting, setSubmitting] = useState(false);

  function confirm() {
    setSubmitting(true);
    restore.mutate(
      { departmentId: team.departmentId, teamId: team.id },
      {
        onSuccess: () => {
          toast({ title: 'Team restored', tone: 'success' });
          onClose();
        },
        onError: (err) => {
          toast({ title: 'Failed to restore team', description: err.message, tone: 'error' });
          setSubmitting(false);
        },
      },
    );
  }

  return (
    <Modal
      open
      onClose={onClose}
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button onClick={confirm} leftIcon={<RotateCcw className="h-4 w-4" />} disabled={submitting}>Restore Team</Button>
        </>
      }
    >
      <div className="flex flex-col items-center gap-3 text-center py-2">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-accent-50 text-accent-600 dark:bg-accent-100 dark:text-accent-300">
          <RotateCcw className="h-6 w-6" />
        </span>
        <h3 className="text-page font-semibold text-text-primary">Restore "{team.name}"?</h3>
        <p className="text-body text-text-secondary max-w-sm">
          The team will be restored to active status and will appear in active lists again.
        </p>
      </div>
    </Modal>
  );
}

function DeleteModal({ team, onClose }: { team: Team; onClose: () => void }) {
  const wsId = useWorkspaceId();
  const { toast } = useToast();
  const deletePermanently = useDeleteTeamPermanently(wsId);
  const [submitting, setSubmitting] = useState(false);

  function confirm() {
    setSubmitting(true);
    deletePermanently.mutate(
      { departmentId: team.departmentId, teamId: team.id },
      {
        onSuccess: () => {
          toast({ title: 'Team permanently deleted', tone: 'success' });
          onClose();
        },
        onError: (err) => {
          toast({ title: 'Failed to delete team', description: err.message, tone: 'error' });
          setSubmitting(false);
        },
      },
    );
  }

  return (
    <Modal
      open
      onClose={onClose}
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button variant="danger" onClick={confirm} leftIcon={<Trash2 className="h-4 w-4" />} disabled={submitting}>
            Delete Permanently
          </Button>
        </>
      }
    >
      <div className="flex flex-col items-center gap-3 text-center py-2">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-danger-50 text-danger-600 dark:bg-danger-100 dark:text-danger-500">
          <AlertTriangle className="h-6 w-6" />
        </span>
        <h3 className="text-page font-semibold text-text-primary">Delete "{team.name}"?</h3>
        <p className="text-body text-text-secondary max-w-sm">
          This action permanently deletes this team and cannot be undone. All team memberships will be removed, and the team will no longer appear in any list.
        </p>
      </div>
    </Modal>
  );
}

function ChangeManagerModal({ team, onClose }: { team: Team; onClose: () => void }) {
  const wsId = useWorkspaceId();
  const { toast } = useToast();
  const managers = useManagerOptions();
  const update = useUpdateTeam(wsId);

  const [selected, setSelected] = useState(team.managerId ?? '');
  const [submitting, setSubmitting] = useState(false);

  function confirm() {
    setSubmitting(true);
    update.mutate(
      {
        departmentId: team.departmentId,
        teamId: team.id,
        data: selected ? { managerId: selected } : { managerId: null, clearManager: true },
      },
      {
        onSuccess: () => {
          toast({ title: selected ? 'Manager updated' : 'Manager removed', tone: 'success' });
          onClose();
        },
        onError: (err) => {
          toast({ title: 'Failed to update manager', description: err.message, tone: 'error' });
          setSubmitting(false);
        },
      },
    );
  }

  const unassignedRow = (
    <button
      type="button"
      onClick={() => setSelected('')}
      className={cn(
        'flex items-center gap-3 rounded-lg border px-3 py-2.5 text-left transition-colors',
        selected === '' ? 'border-accent-300 bg-accent-50 dark:border-accent-100/50 dark:bg-accent-100/10' : 'border-border-subtle hover:bg-surface-2',
      )}
    >
      <Avatar name="Unassigned" size="sm" />
      <div className="min-w-0 flex-1">
        <p className="text-body font-medium text-text-primary truncate">Unassigned</p>
        <p className="text-2xs text-text-tertiary">{team.managerId ? 'Remove the current manager' : 'Current state'}</p>
      </div>
      {selected === '' && <span className="text-caption font-medium text-accent-600">Selected</span>}
    </button>
  );

  return (
    <Modal
      open
      onClose={onClose}
      title="Change Team Manager"
      description={`Assign a new manager to "${team.name}".`}
      size="md"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button onClick={confirm} leftIcon={<Shield className="h-4 w-4" />} disabled={submitting}>
            {selected ? 'Assign Manager' : 'Unassign Manager'}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <p className="text-caption text-text-tertiary">Select a new manager from the workspace users below, or set the team to Unassigned.</p>
        <div className="flex flex-col gap-2 max-h-64 overflow-y-auto -mx-1 px-1">
          {unassignedRow}
          {managers.map((m, i) => {
            const isSelected = selected === m.id;
            return (
              <button
                key={m.id}
                type="button"
                onClick={() => setSelected(m.id)}
                className={cn(
                  'flex items-center gap-3 rounded-lg border px-3 py-2.5 text-left transition-colors',
                  isSelected ? 'border-accent-300 bg-accent-50 dark:border-accent-100/50 dark:bg-accent-100/10' : 'border-border-subtle hover:bg-surface-2',
                )}
              >
                <Avatar name={m.name} src={m.avatar} size="sm" tone={i} />
                <div className="min-w-0 flex-1">
                  <p className="text-body font-medium text-text-primary truncate">{m.name}</p>
                  <p className="text-2xs text-text-tertiary">{m.id === team.managerId ? 'Current manager' : 'Workspace user'}</p>
                </div>
                {isSelected && <span className="text-caption font-medium text-accent-600">Selected</span>}
              </button>
            );
          })}
        </div>
      </div>
    </Modal>
  );
}