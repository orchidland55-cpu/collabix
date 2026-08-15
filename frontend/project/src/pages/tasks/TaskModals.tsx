import { useState, type FormEvent } from 'react';
import { AlertTriangle, Plus, Archive, Trash2, RotateCcw, UserPlus, Eye, Calendar, Users, FolderKanban, ListChecks, MessageSquare, Paperclip } from 'lucide-react';
import { Modal } from '../../components/ui/Modal';
import { Button } from '../../components/ui/Button';
import { Textarea } from '../../components/ui/Textarea';
import { Select } from '../../components/ui/Select';
import { Badge } from '../../components/ui/Badge';
import type { Task, TaskPriority } from './tasks-types';
import { PRIORITY_OPTIONS, parseDatetimeLocalToInstant, isTaskOverdue } from './tasks-types';
import { useCommentsList, useChecklistsList, useAttachmentsList } from '../../services/task-hooks';

export type CreateTaskFormData = {
  title: string;
  description?: string;
  projectId?: string;
  assigneeId?: string;
  priority?: TaskPriority;
  dueAt?: string;
};

export type EditTaskFormData = {
  title: string;
  description?: string;
  priority?: TaskPriority;
  dueAt?: string;
};

export type TaskModalKind =
  | { kind: 'create' }
  | { kind: 'view'; task: Task }
  | { kind: 'edit'; task: { id: string; title: string; description?: string; priority: TaskPriority; dueAt?: string; projectId: string } }
  | { kind: 'assign'; task: { id: string; title: string; projectId: string; assigneeId?: string } }
  | { kind: 'archive'; task: { id: string; title: string; projectId: string } }
  | { kind: 'restore'; task: { id: string; title: string } }
  | { kind: 'delete'; task: { id: string; title: string; projectId: string } }
  | null;

function toDatetimeLocalValue(iso?: string): string {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

const statusLabels: Record<string, string> = {
  todo: 'To Do',
  'in-progress': 'In Progress',
  'in-review': 'In Review',
  blocked: 'Blocked',
  completed: 'Done',
  archived: 'Archived',
};

function Field({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-caption font-medium text-text-secondary">
        {label}
        {hint && <span className="ml-1 font-normal text-text-tertiary">({hint})</span>}
      </label>
      {children}
    </div>
  );
}

const inputCls = 'cx-input h-10';

export function TaskModal({
  state,
  onClose,
  onCreate,
  onEdit,
  onAssign,
  onConfirmAction,
  onOpenFullDetails,
  projects = [],
  members = [],
  defaultProjectId,
  canAssign = false,
  isSubmitting = false,
  wsId = '',
  deptId = '',
}: {
  state: TaskModalKind;
  onClose: () => void;
  onCreate?: (data: CreateTaskFormData) => void;
  onEdit?: (data: EditTaskFormData) => void;
  onAssign?: (assigneeId: string) => void;
  onConfirmAction?: () => void;
  onOpenFullDetails?: (task: Task) => void;
  projects?: { id: string; name: string }[];
  members?: { id: string; name: string }[];
  defaultProjectId?: string;
  canAssign?: boolean;
  isSubmitting?: boolean;
  wsId?: string;
  deptId?: string;
}) {
  if (!state) return null;
  switch (state.kind) {
    case 'create':
      return (
        <CreateModal
          onClose={onClose}
          onSubmit={onCreate!}
          projects={projects}
          members={canAssign ? members : []}
          defaultProjectId={defaultProjectId}
          isSubmitting={isSubmitting}
        />
      );
    case 'view':
      return (
        <ViewTaskModal
          task={state.task}
          wsId={wsId}
          deptId={deptId}
          onClose={onClose}
          onOpenFullDetails={onOpenFullDetails}
        />
      );
    case 'edit':
      return <EditModal task={state.task} onClose={onClose} onSubmit={onEdit!} isSubmitting={isSubmitting} />;
    case 'assign':
      return (
        <AssignModal
          task={state.task}
          onClose={onClose}
          onSubmit={onAssign!}
          members={members}
          isSubmitting={isSubmitting}
        />
      );
    case 'archive':
      return <ArchiveModal task={state.task} onClose={onClose} onConfirm={onConfirmAction!} isSubmitting={isSubmitting} />;
    case 'restore':
      return <RestoreModal task={state.task} onClose={onClose} onConfirm={onConfirmAction!} isSubmitting={isSubmitting} />;
    case 'delete':
      return <DeleteModal task={state.task} onClose={onClose} onConfirm={onConfirmAction!} isSubmitting={isSubmitting} />;
  }
}

function CreateModal({
  onClose,
  onSubmit,
  projects,
  members,
  defaultProjectId,
  isSubmitting,
}: {
  onClose: () => void;
  onSubmit: (data: CreateTaskFormData) => void;
  projects: { id: string; name: string }[];
  members: { id: string; name: string }[];
  defaultProjectId?: string;
  isSubmitting: boolean;
}) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [projectId, setProjectId] = useState(defaultProjectId ?? '');
  const [assigneeId, setAssigneeId] = useState('');
  const [priority, setPriority] = useState<TaskPriority | ''>('');
  const [deadline, setDeadline] = useState('');
  const [deadlineError, setDeadlineError] = useState('');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    let dueAt: string | undefined;
    if (deadline.trim()) {
      dueAt = parseDatetimeLocalToInstant(deadline);
      if (!dueAt) {
        setDeadlineError('Enter a valid date and time.');
        return;
      }
    }
    setDeadlineError('');

    onSubmit({
      title: title.trim(),
      description: description.trim() || undefined,
      projectId: projectId || undefined,
      assigneeId: assigneeId || undefined,
      priority: priority || undefined,
      dueAt,
    });
  };

  return (
    <Modal
      open
      onClose={onClose}
      title="Create Task"
      description="Add a new task. Only the title is required."
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>Cancel</Button>
          <Button
            onClick={handleSubmit}
            leftIcon={<Plus className="h-4 w-4" />}
            disabled={!title.trim() || isSubmitting}
            loading={isSubmitting}
          >
            Create Task
          </Button>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Field label="Title" hint="required">
          <input
            className={inputCls}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Enter task title"
            autoFocus
          />
        </Field>
        <Field label="Description" hint="optional">
          <Textarea value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Optional description..." />
        </Field>
        {projects.length > 0 && (
          <Field label="Project" hint="optional">
            <Select
              value={projectId}
              onChange={(e) => setProjectId(e.target.value)}
              options={[
                { value: '', label: 'No project selected' },
                ...projects.map((p) => ({ value: p.id, label: p.name })),
              ]}
            />
            {!projectId && defaultProjectId && (
              <p className="text-2xs text-text-tertiary">
                If left empty, the task will be created in the first available department project.
              </p>
            )}
          </Field>
        )}
        {members.length > 0 && (
          <Field label="Assigned member" hint="optional">
            <Select
              value={assigneeId}
              onChange={(e) => setAssigneeId(e.target.value)}
              options={[
                { value: '', label: 'Unassigned' },
                ...members.map((m) => ({ value: m.id, label: m.name })),
              ]}
            />
          </Field>
        )}
        <Field label="Priority" hint="optional">
          <Select
            value={priority}
            onChange={(e) => setPriority(e.target.value as TaskPriority | '')}
            options={[
              { value: '', label: 'Default' },
              ...PRIORITY_OPTIONS.map((p) => ({ value: p.value, label: p.label })),
            ]}
          />
        </Field>
        <Field label="Deadline" hint="optional">
          <input
            type="datetime-local"
            className={inputCls}
            value={deadline}
            onChange={(e) => {
              setDeadline(e.target.value);
              setDeadlineError('');
            }}
          />
          {deadlineError && <p className="text-2xs text-danger-600 dark:text-danger-400">{deadlineError}</p>}
        </Field>
      </form>
    </Modal>
  );
}

function AssignModal({
  task,
  onClose,
  onSubmit,
  members,
  isSubmitting,
}: {
  task: { id: string; title: string; assigneeId?: string };
  onClose: () => void;
  onSubmit: (assigneeId: string) => void;
  members: { id: string; name: string }[];
  isSubmitting: boolean;
}) {
  const [assigneeId, setAssigneeId] = useState(task.assigneeId ?? '');

  return (
    <Modal
      open
      onClose={onClose}
      title="Assign Member"
      description={`Choose a member for "${task.title}".`}
      size="md"
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>Cancel</Button>
          <Button
            onClick={() => assigneeId && onSubmit(assigneeId)}
            leftIcon={<UserPlus className="h-4 w-4" />}
            disabled={!assigneeId || isSubmitting}
            loading={isSubmitting}
          >
            {task.assigneeId ? 'Reassign' : 'Assign'}
          </Button>
        </>
      }
    >
      <Field label="Department member">
        <Select
          value={assigneeId}
          onChange={(e) => setAssigneeId(e.target.value)}
          options={[
            { value: '', label: 'Select a member' },
            ...members.map((m) => ({ value: m.id, label: m.name })),
          ]}
        />
      </Field>
    </Modal>
  );
}

function ViewTaskModal({
  task,
  wsId,
  deptId,
  onClose,
  onOpenFullDetails,
}: {
  task: Task;
  wsId: string;
  deptId: string;
  onClose: () => void;
  onOpenFullDetails?: (task: Task) => void;
}) {
  const { data: commentsPage } = useCommentsList(wsId, deptId, task.projectId, task.id);
  const { data: checklistsPage } = useChecklistsList(wsId, deptId, task.projectId, task.id);
  const { data: attachmentsPage } = useAttachmentsList(wsId, deptId, task.projectId, task.id);
  const commentCount = commentsPage?.content?.length ?? 0;
  const checklistCount = checklistsPage?.content?.length ?? 0;
  const attachmentCount = attachmentsPage?.content?.length ?? 0;
  const overdue = isTaskOverdue(task);

  return (
    <Modal
      open
      onClose={onClose}
      title={task.title}
      description="Task details"
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Close</Button>
          {onOpenFullDetails && (
            <Button variant="outline" leftIcon={<Eye className="h-4 w-4" />} onClick={() => onOpenFullDetails(task)}>
              Open full page
            </Button>
          )}
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap gap-2">
          <Badge tone="info" variant="soft">{statusLabels[task.status] ?? task.status}</Badge>
          <Badge tone="neutral" variant="soft">{task.priority}</Badge>
          {overdue && <Badge tone="danger" variant="soft">Overdue</Badge>}
        </div>
        <Field label="Description">
          <p className="text-body text-text-secondary whitespace-pre-wrap">{task.description || 'No description provided.'}</p>
        </Field>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <DetailItem icon={<FolderKanban className="h-4 w-4" />} label="Project" value={task.projectName || '—'} />
          <DetailItem icon={<Users className="h-4 w-4" />} label="Department" value={task.departmentName || '—'} />
          <DetailItem icon={<Users className="h-4 w-4" />} label="Assigned member" value={task.assigneeName || 'Unassigned'} />
          <DetailItem icon={<Calendar className="h-4 w-4" />} label="Deadline" value={task.deadline || 'No deadline'} />
          <DetailItem icon={<Calendar className="h-4 w-4" />} label="Created" value={task.createdAt} />
        </div>
        <div className="flex flex-wrap gap-4 pt-2 border-t border-border-subtle text-caption text-text-secondary">
          <span className="inline-flex items-center gap-1.5"><ListChecks className="h-4 w-4" /> {checklistCount} checklist{checklistCount !== 1 ? 's' : ''}</span>
          <span className="inline-flex items-center gap-1.5"><MessageSquare className="h-4 w-4" /> {commentCount} comment{commentCount !== 1 ? 's' : ''}</span>
          <span className="inline-flex items-center gap-1.5"><Paperclip className="h-4 w-4" /> {attachmentCount} attachment{attachmentCount !== 1 ? 's' : ''}</span>
        </div>
      </div>
    </Modal>
  );
}

function DetailItem({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-start gap-2 rounded-lg border border-border-subtle bg-surface-2/50 p-3">
      <span className="text-text-tertiary mt-0.5">{icon}</span>
      <div>
        <p className="text-2xs text-text-tertiary">{label}</p>
        <p className="text-caption font-medium text-text-primary">{value}</p>
      </div>
    </div>
  );
}

function EditModal({
  task,
  onClose,
  onSubmit,
  isSubmitting,
}: {
  task: { id: string; title: string; description?: string; priority: TaskPriority; dueAt?: string };
  onClose: () => void;
  onSubmit: (data: EditTaskFormData) => void;
  isSubmitting: boolean;
}) {
  const [title, setTitle] = useState(task.title);
  const [description, setDescription] = useState(task.description ?? '');
  const [priority, setPriority] = useState<TaskPriority>(task.priority);
  const [deadline, setDeadline] = useState(toDatetimeLocalValue(task.dueAt));
  const [deadlineError, setDeadlineError] = useState('');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    let dueAt: string | undefined;
    if (deadline.trim()) {
      dueAt = parseDatetimeLocalToInstant(deadline);
      if (!dueAt) {
        setDeadlineError('Enter a valid date and time.');
        return;
      }
    }
    setDeadlineError('');
    onSubmit({
      title: title.trim(),
      description: description.trim() || undefined,
      priority,
      dueAt,
    });
  };

  return (
    <Modal
      open
      onClose={onClose}
      title="Edit Task"
      description="Update task details."
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>Cancel</Button>
          <Button onClick={handleSubmit} disabled={!title.trim() || isSubmitting} loading={isSubmitting}>
            Save Changes
          </Button>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Field label="Title">
          <input className={inputCls} value={title} onChange={(e) => setTitle(e.target.value)} autoFocus />
        </Field>
        <Field label="Description">
          <Textarea value={description} onChange={(e) => setDescription(e.target.value)} />
        </Field>
        <Field label="Priority">
          <Select
            value={priority}
            onChange={(e) => setPriority(e.target.value as TaskPriority)}
            options={PRIORITY_OPTIONS.map((p) => ({ value: p.value, label: p.label }))}
          />
        </Field>
        <Field label="Deadline" hint="optional">
          <input
            type="datetime-local"
            className={inputCls}
            value={deadline}
            onChange={(e) => {
              setDeadline(e.target.value);
              setDeadlineError('');
            }}
          />
          {deadlineError && <p className="text-2xs text-danger-600 dark:text-danger-400">{deadlineError}</p>}
        </Field>
      </form>
    </Modal>
  );
}

function ArchiveModal({
  task,
  onClose,
  onConfirm,
  isSubmitting,
}: {
  task: { id: string; title: string };
  onClose: () => void;
  onConfirm: () => void;
  isSubmitting: boolean;
}) {
  return (
    <Modal
      open
      onClose={onClose}
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>Cancel</Button>
          <Button variant="danger" onClick={onConfirm} leftIcon={<Archive className="h-4 w-4" />} loading={isSubmitting}>
            Archive Task
          </Button>
        </>
      }
    >
      <div className="flex flex-col items-center gap-3 text-center py-2">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-warning-50 text-warning-600 dark:bg-warning-500/15 dark:text-warning-400">
          <AlertTriangle className="h-6 w-6" />
        </span>
        <h3 className="text-page font-semibold text-text-primary">Archive "{task.title}"?</h3>
        <p className="text-body text-text-secondary max-w-sm">
          The task will be archived and hidden from active views. You can restore it later.
        </p>
      </div>
    </Modal>
  );
}

function RestoreModal({
  task,
  onClose,
  onConfirm,
  isSubmitting,
}: {
  task: { id: string; title: string };
  onClose: () => void;
  onConfirm: () => void;
  isSubmitting: boolean;
}) {
  return (
    <Modal
      open
      onClose={onClose}
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>Cancel</Button>
          <Button onClick={onConfirm} leftIcon={<RotateCcw className="h-4 w-4" />} loading={isSubmitting}>
            Restore Task
          </Button>
        </>
      }
    >
      <div className="flex flex-col items-center gap-3 text-center py-2">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-accent-50 text-accent-600 dark:bg-accent-500/15 dark:text-accent-400">
          <RotateCcw className="h-6 w-6" />
        </span>
        <h3 className="text-page font-semibold text-text-primary">Restore "{task.title}"?</h3>
        <p className="text-body text-text-secondary max-w-sm">
          This task will be moved back to the active task list.
        </p>
      </div>
    </Modal>
  );
}

function DeleteModal({
  task,
  onClose,
  onConfirm,
  isSubmitting,
}: {
  task: { id: string; title: string };
  onClose: () => void;
  onConfirm: () => void;
  isSubmitting: boolean;
}) {
  return (
    <Modal
      open
      onClose={onClose}
      title="Delete task?"
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={isSubmitting}>Cancel</Button>
          <Button variant="danger" onClick={onConfirm} leftIcon={<Trash2 className="h-4 w-4" />} loading={isSubmitting}>
            Delete
          </Button>
        </>
      }
    >
      <div className="flex flex-col items-center gap-3 text-center py-2">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-danger-50 text-danger-600 dark:bg-danger-500/15 dark:text-danger-400">
          <Trash2 className="h-6 w-6" />
        </span>
        <p className="text-body text-text-secondary max-w-sm">
          Are you sure you want to delete <strong className="text-text-primary">{task.title}</strong>? This action cannot be undone.
        </p>
      </div>
    </Modal>
  );
}
