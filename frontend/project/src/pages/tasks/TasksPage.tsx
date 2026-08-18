import { useState, useMemo, useCallback, useEffect } from 'react';
import {
  Search,
  Plus,
  LayoutGrid,
  LayoutList,
  Calendar,
  ChevronDown,
  Clock,
  FolderKanban,
  MoreHorizontal,
  Eye,
  Edit2,
  Trash2,
  Briefcase,
  Network,
  AlertCircle,
  ShieldBan,
} from 'lucide-react';
import { Card, CardBody } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Badge, type Tone } from '../../components/ui/Badge';
import { IconButton } from '../../components/ui/IconButton';
import { Dropdown, type DropdownItem } from '../../components/ui/Dropdown';
import { Select } from '../../components/ui/Select';
import { EmptyState } from '../../components/ui/EmptyState';
import { Skeleton } from '../../components/ui/Skeleton';
import { Avatar } from '../../components/ui/Avatar';
import { useToast } from '../../components/ui/Toast';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { cn } from '../../lib/cn';
import { useWorkspacesList } from '../../services/workspace-hooks';
import { useDepartmentList } from '../../services/department-hooks';
import { useProjectList } from '../../services/project-hooks';
import { useDepartmentTasksList, useCreateTask, useUpdateTask, useUpdateTaskStatus, useDeleteTask, useTaskDepartmentContext, useTaskAccess, useDepartmentMembers, canDragTask, getTaskQueryErrorState, getTaskEmptyDescription } from '../../services/task-hooks';
import { useAuth } from '../../lib/auth-context';
import { isMember } from '../../lib/access';
import { FRONTEND_STATUS_MAP, mapToCreateRequest, mapToUpdateRequest, isTaskOverdue } from './tasks-types';
import type { Task, TaskStatus } from './tasks-types';
import { TaskModal, type TaskModalKind, type CreateTaskFormData, type EditTaskFormData } from './TaskModals';
import { useQueryClient } from '@tanstack/react-query';

type ViewMode = 'kanban' | 'list' | 'calendar';

const taskStatuses = ['todo', 'in-progress', 'in-review', 'blocked', 'completed', 'archived'];
const statusLabels: Record<string, string> = {
  todo: 'To Do',
  'in-progress': 'In Progress',
  'in-review': 'In Review',
  blocked: 'Blocked',
  completed: 'Completed',
  archived: 'Archived',
};
const statusColors: Record<string, Tone> = {
  todo: 'info',
  'in-progress': 'accent',
  'in-review': 'warning',
  blocked: 'danger',
  completed: 'success',
  archived: 'neutral',
};

interface TasksPageProps {
  workspaceId?: string;
  departmentId?: string;
  projectId?: string;
}

export function TasksPage(_props: TasksPageProps = {}) {
  const { toast } = useToast();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { user } = useAuth();
  const urlWs = searchParams.get('ws') ?? '';
  const urlDept = searchParams.get('dept') ?? '';
  const urlProj = searchParams.get('proj') ?? '';

  const [viewMode, setViewMode] = useState<ViewMode>('kanban');
  const [search, setSearch] = useState('');
  const [sortBy, setSortBy] = useState<'priority' | 'deadline' | 'progress'>('priority');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [modal, setModal] = useState<TaskModalKind>(null);

  const {
    workspaceId: contextWsId,
    departmentId: contextDeptId,
    departmentName,
    projectId: contextProjId,
    canSelectDepartment,
    isScopedUser,
    hasAssignedDepartment,
    isLoading: contextLoading,
  } = useTaskDepartmentContext();

  const { data: workspaces } = useWorkspacesList();
  const effectiveWsId = urlWs || contextWsId;
  const effectiveDeptId = canSelectDepartment ? urlDept : contextDeptId;
  const effectiveProjId = urlProj || contextProjId || '';

  const { data: departments } = useDepartmentList(canSelectDepartment ? (effectiveWsId || undefined) : undefined);
  const { data: projects } = useProjectList(effectiveWsId || undefined, effectiveDeptId || undefined, undefined, 0);

  const hasBoardContext = !!effectiveWsId && !!effectiveDeptId;
  const hasProjectFilter = !!effectiveProjId;
  const createProjectId = effectiveProjId || projects?.content?.[0]?.id || '';
  const hasProjects = (projects?.content?.length ?? 0) > 0;
  const isMemberUser = isMember(user?.roles);
  const { canCreate, canUpdate, canDelete, canAssign } = useTaskAccess(effectiveWsId || undefined);
  const canManageTasks = canCreate;
  const currentUserId = user?.id;
  const queryClient = useQueryClient();

  const { data: departmentMembers } = useDepartmentMembers(
    canAssign ? effectiveWsId || undefined : undefined,
    canAssign ? effectiveDeptId || undefined : undefined,
  );

  useEffect(() => {
    if (contextLoading || canSelectDepartment || !hasAssignedDepartment) return;
    if (!effectiveWsId || !contextDeptId) return;

    const needsSync =
      urlWs !== effectiveWsId ||
      urlDept !== contextDeptId;

    if (needsSync) {
      const params: Record<string, string> = { ws: effectiveWsId, dept: contextDeptId };
      if (urlProj) params.proj = urlProj;
      setSearchParams(params, { replace: true });
    }
  }, [
    canSelectDepartment,
    contextLoading,
    contextDeptId,
    effectiveWsId,
    hasAssignedDepartment,
    setSearchParams,
    urlDept,
    urlProj,
    urlWs,
  ]);

  const handleSelectWs = (ws: string) => {
    if (!ws) {
      setSearchParams({});
      return;
    }
    setSearchParams({ ws, dept: '', proj: '' });
  };

  const handleSelectDept = (dept: string) => {
    if (!dept) {
      setSearchParams({ ws: effectiveWsId, dept: '', proj: '' });
      return;
    }
    setSearchParams({ ws: effectiveWsId, dept, proj: '' });
  };

  const handleSelectProj = (proj: string) => {
    setSearchParams({ ws: effectiveWsId, dept: effectiveDeptId ?? '', proj });
  };

  const selectedDepartmentName = canSelectDepartment
    ? departments?.find((d) => d.id === effectiveDeptId)?.name
    : departmentName;

  const pageTitle = isScopedUser && selectedDepartmentName
    ? `${selectedDepartmentName} Tasks`
    : 'Tasks';

  const { data: tasksResult, isLoading, isError, error } = useDepartmentTasksList(
    effectiveWsId,
    effectiveDeptId ?? '',
    hasProjectFilter ? effectiveProjId : undefined,
    { search: search || undefined, status: statusFilter || undefined },
  );

  const createTask = useCreateTask(effectiveWsId, effectiveDeptId ?? '');
  const updateTask = useUpdateTask(effectiveWsId, effectiveDeptId ?? '');
  const updateStatus = useUpdateTaskStatus(effectiveWsId, effectiveDeptId ?? '', createProjectId);
  const deleteTask = useDeleteTask(effectiveWsId, effectiveDeptId ?? '');

  const tasks: Task[] = useMemo(() => tasksResult?.content ?? [], [tasksResult]);

  const filteredTasks = useMemo(() => {
    let result = tasks;
    if (search) {
      const q = search.toLowerCase();
      result = result.filter((t) => t.title.toLowerCase().includes(q) || t.description?.toLowerCase().includes(q));
    }
    result.sort((a, b) => {
      switch (sortBy) {
        case 'deadline':
          if (!a.deadline || !b.deadline) return 0;
          return new Date(a.deadline).getTime() - new Date(b.deadline).getTime();
        case 'progress':
          return (b.progress ?? 0) - (a.progress ?? 0);
        case 'priority':
        default: {
          const priorityOrder = { urgent: 0, high: 1, medium: 2, low: 3 };
          return priorityOrder[a.priority] - priorityOrder[b.priority];
        }
      }
    });
    return result;
  }, [tasks, search, sortBy]);

  const stats = useMemo(() => ({
    total: tasks.length,
    inProgress: tasks.filter((t) => t.status === 'in-progress').length,
    completed: tasks.filter((t) => t.status === 'completed').length,
    blocked: tasks.filter((t) => t.status === 'blocked').length,
  }), [tasks]);

  const handleCreate = useCallback((data: CreateTaskFormData) => {
    const projectId = data.projectId || createProjectId;
    if (!projectId) {
      toast({ title: 'No project available', description: 'Create a project in this department before adding tasks.', tone: 'danger' });
      return;
    }
    createTask.mutate(
      { projectId, data: mapToCreateRequest(data) },
      {
        onSuccess: () => {
          setModal(null);
          toast({ title: 'Task created', tone: 'success' });
        },
        onError: (err: unknown) => {
          const message = typeof err === 'object' && err !== null && 'message' in err
            ? String((err as { message: string }).message)
            : 'Failed to create task';
          toast({ title: 'Failed to create task', description: message, tone: 'danger' });
        },
      },
    );
  }, [createProjectId, createTask, toast]);

  const handleEdit = useCallback((data: EditTaskFormData) => {
    if (!modal || modal.kind !== 'edit') return;
    const task = tasks.find((t) => t.id === modal.task.id);
    if (!task) return;
    updateTask.mutate(
      { projectId: task.projectId, taskId: modal.task.id, data: mapToUpdateRequest(data) },
      {
        onSuccess: () => {
          setModal(null);
          toast({ title: 'Task updated', tone: 'success' });
        },
        onError: (err: unknown) => {
          const message = typeof err === 'object' && err !== null && 'message' in err
            ? String((err as { message: string }).message)
            : 'Failed to update task';
          toast({ title: 'Failed to update task', description: message, tone: 'danger' });
        },
      },
    );
  }, [modal, tasks, updateTask, toast]);

  const handleAssign = useCallback((assigneeId: string) => {
    if (!modal || modal.kind !== 'assign') return;
    updateTask.mutate(
      { projectId: modal.task.projectId, taskId: modal.task.id, data: { assigneeId } },
      {
        onSuccess: () => {
          setModal(null);
          toast({ title: 'Task assigned', tone: 'success' });
        },
        onError: (err: unknown) => {
          const message = typeof err === 'object' && err !== null && 'message' in err
            ? String((err as { message: string }).message)
            : 'Failed to assign task';
          toast({ title: 'Failed to assign task', description: message, tone: 'danger' });
        },
      },
    );
  }, [modal, updateTask, toast]);

  const handleConfirmAction = useCallback(() => {
    if (!modal || modal.kind !== 'delete') return;
    const task = tasks.find((t) => t.id === modal.task.id);
    if (!task) return;
    deleteTask.mutate(
      { projectId: task.projectId, taskId: modal.task.id },
      {
        onSuccess: () => {
          setModal(null);
          toast({ title: 'Task deleted', tone: 'success' });
        },
        onError: (err: unknown) => {
          const message = typeof err === 'object' && err !== null && 'message' in err
            ? String((err as { message: string }).message)
            : 'Failed to delete task';
          toast({ title: 'Failed to delete task', description: message, tone: 'danger' });
        },
      },
    );
  }, [modal, tasks, deleteTask, toast]);

  const handleOpenFullDetails = useCallback((task: Task) => {
    setModal(null);
    navigate(`/app/tasks/${task.id}?ws=${effectiveWsId}&dept=${effectiveDeptId}&proj=${task.projectId}`);
  }, [effectiveWsId, effectiveDeptId, navigate]);

if (contextLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-96 rounded-lg" />
      </div>
    );
  }

  if (isScopedUser && !hasAssignedDepartment) {
    return (
      <div className="flex flex-col gap-6">
        <h1 className="text-page font-semibold text-text-primary">Tasks</h1>
        <Card>
          <CardBody className="py-16">
            <EmptyState
              icon={<AlertCircle className="h-6 w-6" />}
              title="No department assigned"
              description="No department is assigned to your account. Contact your administrator to get access to tasks."
            />
          </CardBody>
        </Card>
      </div>
    );
  }

  if (!hasBoardContext) {
    return (
      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-page font-semibold text-text-primary">{pageTitle}</h1>
          <p className="text-body text-text-secondary">
            {canSelectDepartment
              ? 'Select a workspace and department to open the task board.'
              : 'Your department context is loading.'}
          </p>
        </div>
        {canSelectDepartment && (
          <Card>
            <CardBody className="space-y-4">
              <div className="flex items-center gap-2 text-text-secondary">
                <Briefcase className="h-4 w-4" />
                <span className="text-body font-medium text-text-primary">Workspace</span>
              </div>
              <Select
                value={effectiveWsId}
                onChange={(e) => handleSelectWs(e.target.value)}
                options={[
                  { value: '', label: 'Select a workspace' },
                  ...(workspaces ?? []).map((w) => ({ value: w.id, label: w.name })),
                ]}
              />
              {effectiveWsId && (
                <>
                  <div className="flex items-center gap-2 text-text-secondary">
                    <Network className="h-4 w-4" />
                    <span className="text-body font-medium text-text-primary">Department</span>
                  </div>
                  <Select
                    value={urlDept}
                    onChange={(e) => handleSelectDept(e.target.value)}
                    options={[
                      { value: '', label: 'Select a department' },
                      ...(departments ?? []).map((d) => ({ value: d.id, label: d.name })),
                    ]}
                  />
                </>
              )}
            </CardBody>
          </Card>
        )}
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-6 w-72" />
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-24 rounded-lg" />)}
        </div>
        <Skeleton className="h-96 rounded-lg" />
      </div>
    );
  }

  if (isError) {
    const errState = getTaskQueryErrorState(error, isScopedUser);
    return (
      <EmptyState
        icon={errState.isAccessDenied ? <ShieldBan /> : <AlertCircle />}
        title={errState.title}
        description={errState.description}
        action={
          <Button
            variant="outline"
            onClick={() => queryClient.invalidateQueries({ queryKey: ['tasks', 'department-list', effectiveWsId, effectiveDeptId] })}
          >
            Retry
          </Button>
        }
      />
    );
  }


  return (
    <div className="flex flex-col gap-6">
      <TaskModal
        state={modal}
        onClose={() => setModal(null)}
        onCreate={handleCreate}
        onEdit={handleEdit}
        onAssign={handleAssign}
        onConfirmAction={handleConfirmAction}
        onOpenFullDetails={handleOpenFullDetails}
        projects={(projects?.content ?? []).map((p) => ({ id: p.id, name: p.name }))}
        members={departmentMembers ?? []}
        defaultProjectId={createProjectId}
        canAssign={canAssign}
        isSubmitting={createTask.isPending || updateTask.isPending || deleteTask.isPending}
        wsId={effectiveWsId}
        deptId={effectiveDeptId ?? ''}
      />

      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-page font-semibold text-text-primary">{pageTitle}</h1>
          <p className="text-body text-text-secondary">
            {isMemberUser
              ? 'Your assigned tasks across the department.'
              : isScopedUser && selectedDepartmentName
                ? `Kanban board for ${selectedDepartmentName}.`
                : 'Manage and track tasks across departments.'}
          </p>
        </div>
        {canSelectDepartment && (
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <Select
              value={urlDept}
              onChange={(e) => handleSelectDept(e.target.value)}
              options={[
                { value: '', label: 'All departments' },
                ...(departments ?? []).map((d) => ({ value: d.id, label: d.name })),
              ]}
            />
            <Select
              value={urlProj}
              onChange={(e) => handleSelectProj(e.target.value)}
              options={[
                { value: '', label: 'All projects' },
                ...(projects?.content ?? []).map((p) => ({ value: p.id, label: p.name })),
              ]}
            />
          </div>
        )}
        {isScopedUser && selectedDepartmentName && (
          <Badge tone="info" variant="soft">{selectedDepartmentName}</Badge>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatCard label="Total Tasks" value={stats.total} tone="accent" />
        <StatCard label="In Progress" value={stats.inProgress} tone="info" />
        <StatCard label="Completed" value={stats.completed} tone="success" />
        <StatCard label="Blocked" value={stats.blocked} tone="danger" />
      </div>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 flex-col gap-2 sm:flex-row sm:gap-2">
          <div className="flex-1">
            <Input
              placeholder="Search tasks..."
              leftIcon={<Search />}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              containerClassName="w-full"
            />
          </div>
          <Select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            options={[
              { value: '', label: 'All Statuses' },
              ...taskStatuses.map((s) => ({ value: s, label: statusLabels[s] })),
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
              { label: 'Priority', onClick: () => setSortBy('priority') },
              { label: 'Deadline', onClick: () => setSortBy('deadline') },
              { label: 'Progress', onClick: () => setSortBy('progress') },
            ]}
          />
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <div className="flex items-center gap-1 border border-border-subtle rounded-lg p-1">
            <IconButton label="Kanban view" variant={viewMode === 'kanban' ? 'solid' : 'ghost'} onClick={() => setViewMode('kanban')} className="h-8 w-8">
              <LayoutGrid className="h-4 w-4" />
            </IconButton>
            <IconButton label="List view" variant={viewMode === 'list' ? 'solid' : 'ghost'} onClick={() => setViewMode('list')} className="h-8 w-8">
              <LayoutList className="h-4 w-4" />
            </IconButton>
            <IconButton label="Calendar view" variant={viewMode === 'calendar' ? 'solid' : 'ghost'} onClick={() => setViewMode('calendar')} className="h-8 w-8">
              <Calendar className="h-4 w-4" />
            </IconButton>
          </div>
          {canCreate && hasProjects && (
            <Button leftIcon={<Plus />} onClick={() => setModal({ kind: 'create' })}>Create Task</Button>
          )}
        </div>
      </div>

      {filteredTasks.length === 0 && !isLoading ? (
        <EmptyState
          icon={<FolderKanban />}
          title="No tasks found"
          description={getTaskEmptyDescription(isScopedUser, isMemberUser, true, !!search, !!statusFilter)}
        />
      ) : viewMode === 'kanban' ? (
        <KanbanView
          tasks={filteredTasks}
          updateStatus={updateStatus}
          currentUserId={currentUserId}
          canManageTasks={canManageTasks}
          canUpdate={canUpdate}
          canDelete={canDelete}
          onView={(task) => setModal({ kind: 'view', task })}
          onEdit={(task) => setModal({
            kind: 'edit',
            task: {
              id: task.id,
              title: task.title,
              description: task.description,
              priority: task.priority,
              dueAt: task.dueAt,
              projectId: task.projectId,
            },
          })}
          onDelete={(task) => setModal({ kind: 'delete', task: { id: task.id, title: task.title, projectId: task.projectId } })}
          onTaskClick={(task) => setModal({ kind: 'view', task })}
        />
      ) : viewMode === 'list' ? (
        <ListView
          tasks={filteredTasks}
          canUpdate={canUpdate}
          canDelete={canDelete}
          onView={(task) => setModal({ kind: 'view', task })}
          onEdit={(task) => setModal({
            kind: 'edit',
            task: {
              id: task.id,
              title: task.title,
              description: task.description,
              priority: task.priority,
              dueAt: task.dueAt,
              projectId: task.projectId,
            },
          })}
          onDelete={(task) => setModal({ kind: 'delete', task: { id: task.id, title: task.title, projectId: task.projectId } })}
        />
      ) : (
        <CalendarView tasks={filteredTasks} onTaskClick={(task) => setModal({ kind: 'view', task })} />
      )}
    </div>
  );
}

function StatCard({ label, value, tone }: { label: string; value: number; tone: string }) {
  const bgColor: Record<string, string> = {
    accent: 'bg-accent-50 text-accent-700 border-accent-200/50 dark:bg-accent-500/10 dark:text-accent-300 dark:border-accent-500/20',
    success: 'bg-success-50 text-success-700 border-success-200/50 dark:bg-success-500/10 dark:text-success-300 dark:border-success-500/20',
    info: 'bg-info-50 text-info-700 border-info-200/50 dark:bg-info-500/10 dark:text-info-300 dark:border-info-500/20',
    warning: 'bg-warning-50 text-warning-700 border-warning-200/50 dark:bg-warning-500/10 dark:text-warning-300 dark:border-warning-500/20',
    danger: 'bg-danger-50 text-danger-700 border-danger-200/50 dark:bg-danger-500/10 dark:text-danger-300 dark:border-danger-500/20',
  };
  return (
    <div className={cn('rounded-lg border border-border-subtle p-3', bgColor[tone] ?? bgColor.accent)}>
      <p className="text-2xs font-medium opacity-80">{label}</p>
      <p className="text-section font-semibold mt-1">{value}</p>
    </div>
  );
}

const KANBAN_COLUMNS: Array<{ id: TaskStatus; label: string; tone: Tone }> = [
  { id: 'todo', label: 'To Do', tone: 'info' },
  { id: 'in-progress', label: 'In Progress', tone: 'accent' },
  { id: 'in-review', label: 'In Review', tone: 'warning' },
  { id: 'blocked', label: 'Blocked', tone: 'danger' },
  { id: 'completed', label: 'Done', tone: 'success' },
];

const priorityToneMap: Record<string, Tone> = {
  urgent: 'danger',
  high: 'warning',
  medium: 'info',
  low: 'success',
};

function KanbanView({
  tasks,
  updateStatus,
  currentUserId,
  canManageTasks,
  canUpdate,
  canDelete,
  onView,
  onEdit,
  onDelete,
  onTaskClick,
}: {
  tasks: Task[];
  updateStatus: ReturnType<typeof useUpdateTaskStatus>;
  currentUserId: string | undefined;
  canManageTasks: boolean;
  canUpdate: boolean;
  canDelete: boolean;
  onView: (task: Task) => void;
  onEdit: (task: Task) => void;
  onDelete: (task: Task) => void;
  onTaskClick: (task: Task) => void;
}) {
  const { toast } = useToast();
  const [draggedId, setDraggedId] = useState<string | null>(null);
  const [dropTarget, setDropTarget] = useState<TaskStatus | null>(null);

  const handleDrop = (targetStatus: TaskStatus) => (e: React.DragEvent) => {
    e.preventDefault();
    setDropTarget(null);
    const taskId = draggedId;
    setDraggedId(null);
    if (!taskId) return;
    const task = tasks.find((t) => t.id === taskId);
    if (!task || task.status === targetStatus) return;

    if (!canDragTask(task, currentUserId, canManageTasks)) {
      toast({
        title: 'Cannot move task',
        description: canManageTasks
          ? 'You do not have permission to move this task.'
          : 'Only the assigned member or a manager can move this task.',
        tone: 'danger',
      });
      return;
    }

    updateStatus.mutate(
      { taskId, projectId: task.projectId, status: FRONTEND_STATUS_MAP[targetStatus] },
      {
        onSuccess: () => toast({ title: 'Task status updated', tone: 'success' }),
        onError: (err: unknown) => {
          const message = typeof err === 'object' && err !== null && 'message' in err
            ? String((err as { message: string }).message)
            : 'Failed to update task status';
          toast({ title: 'Cannot move task', description: message, tone: 'danger' });
        },
      },
    );
  };

  return (
    <div className="-mx-1 overflow-x-auto pb-2">
      <div className="flex gap-4 min-w-max px-1">
        {KANBAN_COLUMNS.map((col) => {
          const colTasks = tasks.filter((t) => t.status === col.id);
          const isDropTarget = dropTarget === col.id;
          return (
            <div key={col.id} className="flex w-[280px] shrink-0 flex-col gap-3">
              <div className="flex items-center justify-between rounded-lg border border-border-subtle bg-surface-2 px-3 py-2">
                <div className="flex items-center gap-2">
                  <Badge tone={col.tone} variant="soft" dot />
                  <span className="text-caption font-semibold text-text-primary">{col.label}</span>
                </div>
                <Badge tone="neutral" variant="soft">{colTasks.length}</Badge>
              </div>
              <div
                className={cn(
                  'flex min-h-[120px] flex-col gap-2 rounded-lg border-2 border-dashed p-2 transition-colors',
                  isDropTarget
                    ? 'border-accent-400 bg-accent-50/50 dark:border-accent-500 dark:bg-accent-500/10'
                    : 'border-border-subtle bg-surface/50',
                )}
                onDragOver={(e) => {
                  e.preventDefault();
                  setDropTarget(col.id);
                }}
                onDragLeave={() => setDropTarget((prev) => (prev === col.id ? null : prev))}
                onDrop={handleDrop(col.id)}
              >
                {colTasks.length === 0 ? (
                  <div className="flex flex-1 items-center justify-center rounded-lg p-4 text-center">
                    <p className="text-2xs text-text-tertiary">Drop tasks here</p>
                  </div>
                ) : (
                  colTasks.map((task) => (
                    <TaskCard
                      key={task.id}
                      task={task}
                      draggable={canDragTask(task, currentUserId, canManageTasks)}
                      onDragStart={() => setDraggedId(task.id)}
                      onDragEnd={() => {
                        setDraggedId(null);
                        setDropTarget(null);
                      }}
                      onClick={() => onTaskClick(task)}
                      onView={() => onView(task)}
                      onEdit={canUpdate ? () => onEdit(task) : undefined}
                      onDelete={canDelete ? () => onDelete(task) : undefined}
                      isDragging={draggedId === task.id}
                    />
                  ))
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function TaskCard({
  task,
  draggable: isDraggable,
  onDragStart,
  onDragEnd,
  onClick,
  onView,
  onEdit,
  onDelete,
  isDragging,
}: {
  task: Task;
  draggable: boolean;
  onDragStart: () => void;
  onDragEnd: () => void;
  onClick: () => void;
  onView: () => void;
  onEdit?: () => void;
  onDelete?: () => void;
  isDragging: boolean;
}) {
  const overdue = isTaskOverdue(task);
  const actionItems: DropdownItem[] = [
    { label: 'View task', icon: <Eye className="h-4 w-4" />, onClick: onView },
    ...(onEdit ? [{ label: 'Edit task', icon: <Edit2 className="h-4 w-4" />, onClick: onEdit }] : []),
    ...(onDelete ? [{ divider: true as const }, { label: 'Delete task', icon: <Trash2 className="h-4 w-4" />, onClick: onDelete, danger: true }] : []),
  ];
  return (
    <div
      draggable={isDraggable}
      onDragStart={(e: React.DragEvent) => {
        if (!isDraggable) {
          e.preventDefault();
          return;
        }
        onDragStart();
        e.dataTransfer.effectAllowed = 'move';
      }}
      onDragEnd={onDragEnd}
      onClick={onClick}
      className={cn(
        'rounded-lg border bg-surface p-3 shadow-cx-sm transition-all cursor-pointer group',
        isDragging
          ? 'border-accent-400 opacity-60 ring-2 ring-accent-300 dark:border-accent-500 dark:ring-accent-500/40'
          : 'border-border-subtle hover:border-border-default hover:shadow-cx-md',
        !isDraggable && 'cursor-default',
      )}
    >
      <div className="flex items-start justify-between gap-3 mb-2">
        <h4 className="text-body font-medium text-text-primary line-clamp-2 flex-1">{task.title}</h4>
        <Dropdown
          trigger={
            <IconButton
              label="Task actions"
              variant="ghost"
              size="sm"
              className="h-7 w-7 shrink-0"
            >
              <MoreHorizontal className="h-4 w-4" />
            </IconButton>
          }
          items={actionItems}
          align="right"
        />
      </div>
      <div className="flex flex-wrap items-center gap-2 mb-2">
        <Badge tone={statusColors[task.status] ?? 'neutral'} variant="soft" dot>
          {statusLabels[task.status]}
        </Badge>
        {task.priority && (
          <Badge tone={priorityToneMap[task.priority] ?? 'info'} variant="soft">
            {task.priority}
          </Badge>
        )}
      </div>
      {task.deadline && (
        <div className={cn(
          'flex items-center gap-1 text-2xs mb-2',
          overdue ? 'text-danger-600 dark:text-danger-400 font-medium' : 'text-text-tertiary',
        )}>
          <Clock className="h-3 w-3" />
          <span>{overdue ? 'Overdue · ' : ''}{task.deadline}</span>
        </div>
      )}
      <div className="flex items-center justify-between pt-2 border-t border-border-subtle gap-2">
        {task.assigneeName ? (
          <div className="flex items-center gap-1.5 min-w-0">
            <Avatar name={task.assigneeName} size="xs" />
            <span className="text-2xs text-text-secondary truncate">{task.assigneeName}</span>
          </div>
        ) : (
          <Badge tone="neutral" variant="outline">Unassigned</Badge>
        )}
        {task.projectName ? (
          <Badge tone="neutral" variant="soft" className="truncate max-w-[110px]">{task.projectName}</Badge>
        ) : null}
      </div>
    </div>
  );
}

function ListView({
  tasks,
  canUpdate,
  canDelete,
  onView,
  onEdit,
  onDelete,
}: {
  tasks: Task[];
  canUpdate: boolean;
  canDelete: boolean;
  onView: (task: Task) => void;
  onEdit: (task: Task) => void;
  onDelete: (task: Task) => void;
}) {
  return (
    <div className="space-y-2">
      {tasks.filter(t => t.status !== 'archived').map((task) => (
        <TaskListRow
          key={task.id}
          task={task}
          onView={() => onView(task)}
          onEdit={canUpdate ? () => onEdit(task) : undefined}
          onDelete={canDelete ? () => onDelete(task) : undefined}
        />
      ))}
    </div>
  );
}

function TaskListRow({ task, onView, onEdit, onDelete }: { task: Task; onView: () => void; onEdit?: () => void; onDelete?: () => void }) {
  const priorityColor: Record<string, Tone> = { urgent: 'danger', high: 'warning', medium: 'info', low: 'success' };
  const statusColor: Record<string, Tone> = { todo: 'info', 'in-progress': 'accent', 'in-review': 'warning', blocked: 'danger', completed: 'success', archived: 'neutral' };
  const actionItems: DropdownItem[] = [
    { label: 'View task', icon: <Eye className="h-4 w-4" />, onClick: onView },
    ...(onEdit ? [{ label: 'Edit task', icon: <Edit2 className="h-4 w-4" />, onClick: onEdit }] : []),
    ...(onDelete ? [{ divider: true as const }, { label: 'Delete task', icon: <Trash2 className="h-4 w-4" />, onClick: onDelete, danger: true }] : []),
  ];
  return (
    <div className="flex items-center gap-4 rounded-lg border border-border-subtle bg-surface p-3 hover:bg-surface-2 transition-colors group">
      <div className="flex-1 min-w-0 cursor-pointer" onClick={onView}>
        <div className="flex items-center gap-2 mb-2">
          <h4 className="text-body font-medium text-text-primary truncate flex-1">{task.title}</h4>
          <Badge tone={statusColor[task.status]} variant="soft" dot>{statusLabels[task.status]}</Badge>
        </div>
        <div className="flex items-center gap-2 text-caption text-text-secondary">
          {task.deadline && <span>{task.deadline}</span>}
        </div>
      </div>
      <div className="flex items-center gap-3">
        <Badge tone={priorityColor[task.priority]} variant="soft">{task.priority}</Badge>
        <Dropdown
          trigger={
            <IconButton label="Task actions" variant="ghost" size="sm">
              <MoreHorizontal className="h-4 w-4" />
            </IconButton>
          }
          items={actionItems}
          align="right"
        />
      </div>
    </div>
  );
}

function CalendarView({ tasks, onTaskClick }: { tasks: Task[]; onTaskClick: (task: Task) => void }) {
  const tasksByDate: Record<string, Task[]> = {};
  tasks.forEach((task) => {
    if (task.deadline) {
      if (!tasksByDate[task.deadline]) tasksByDate[task.deadline] = [];
      tasksByDate[task.deadline].push(task);
    }
  });
  const sortedDates = Object.keys(tasksByDate).sort();
  if (sortedDates.length === 0) {
    return (
      <Card><CardBody className="py-8 text-center"><p className="text-body text-text-secondary">No upcoming deadlines</p></CardBody></Card>
    );
  }
  return (
    <div className="space-y-4">
      {sortedDates.map((date) => (
        <div key={date}>
          <div className="flex items-center gap-3 mb-2 px-2">
            <Calendar className="h-4 w-4 text-text-tertiary" />
            <h3 className="text-caption font-semibold text-text-primary">{date}</h3>
            <Badge tone="neutral" variant="soft">{tasksByDate[date].length} task{tasksByDate[date].length !== 1 ? 's' : ''}</Badge>
          </div>
          <div className="space-y-2">
            {tasksByDate[date].map((task) => (
              <div key={task.id} className="flex items-center gap-4 rounded-lg border border-border-subtle bg-surface p-3 hover:bg-surface-2 transition-colors group cursor-pointer" onClick={() => onTaskClick(task)}>
                <div className="flex-1 min-w-0">
                  <h4 className="text-body font-medium text-text-primary truncate">{task.title}</h4>
                </div>
                <Badge tone={statusColors[task.status]} variant="soft" dot>{statusLabels[task.status]}</Badge>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
