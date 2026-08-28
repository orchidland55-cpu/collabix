import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  ArrowLeft, FolderKanban, Calendar, Clock, Users, AlertCircle, Edit2, Archive, RotateCcw, Plus,
  ListChecks, CheckSquare, Activity, Info, ShieldBan, Flag, Timer, Briefcase,
} from 'lucide-react';
import { Card, CardBody } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Tabs, type TabItem } from '../../components/ui/Tabs';
import { Skeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import { Breadcrumbs, type BreadcrumbItem } from '../../components/ui/Breadcrumbs';
import { StatCard } from '../../components/ui/StatCard';
import { Avatar } from '../../components/ui/Avatar';
import { Progress } from '../../components/ui/Progress';
import { Table, type TableColumn } from '../../components/ui/Table';
import { useProjectDetail, useProjectAccess, useProjectDepartmentContext, getProjectQueryErrorState } from '../../services/project-hooks';
import { useProjectTasks, useCreateTask, useDepartmentMembers, useTaskAccess } from '../../services/task-hooks';
import { useQueryClient } from '@tanstack/react-query';
import { useToast } from '../../components/ui/Toast';
import { EditProjectModal } from './modals/EditProjectModal';
import { ArchiveProjectModal } from './modals/ArchiveProjectModal';
import { RestoreProjectModal } from './modals/RestoreProjectModal';
import { HardDeleteProjectModal } from './modals/HardDeleteProjectModal';
import { TaskModal, type CreateTaskFormData, type TaskModalKind } from '../tasks/TaskModals';
import { mapToCreateRequest, isTaskOverdue, type Task } from '../tasks/tasks-types';
import type { ProjectResponse, ProjectPriority } from './projects-types';

interface ProjectDetailsPageProps {
  projectId: string;
  onBack: () => void;
}

const priorityColors: Record<ProjectPriority, 'danger' | 'warning' | 'info' | 'success'> = {
  CRITICAL: 'danger',
  HIGH: 'warning',
  MEDIUM: 'info',
  LOW: 'success',
};

const statusColors: Record<string, 'success' | 'neutral'> = {
  ACTIVE: 'success',
  ARCHIVED: 'neutral',
};

const taskStatusMeta: Record<Task['status'], { label: string; tone: 'neutral' | 'accent' | 'info' | 'danger' | 'success' }> = {
  'todo': { label: 'To Do', tone: 'neutral' },
  'in-progress': { label: 'In Progress', tone: 'accent' },
  'in-review': { label: 'In Review', tone: 'info' },
  blocked: { label: 'Blocked', tone: 'danger' },
  completed: { label: 'Done', tone: 'success' },
  archived: { label: 'Archived', tone: 'neutral' },
  cancelled: { label: 'Cancelled', tone: 'neutral' },
};

const taskPriorityMeta: Record<Task['priority'], { label: string; tone: 'danger' | 'warning' | 'info' | 'success' }> = {
  urgent: { label: 'Urgent', tone: 'danger' },
  high: { label: 'High', tone: 'warning' },
  medium: { label: 'Medium', tone: 'info' },
  low: { label: 'Low', tone: 'success' },
};

export function ProjectDetailsPage({ projectId, onBack }: ProjectDetailsPageProps) {
  const [searchParams] = useSearchParams();
  const urlWsId = searchParams.get('ws') ?? '';
  const urlDeptId = searchParams.get('dept') ?? '';
  const [activeTab, setActiveTab] = useState('overview');
  const [showEdit, setShowEdit] = useState(false);
  const [showArchive, setShowArchive] = useState(false);
  const [showRestore, setShowRestore] = useState(false);
  const [showHardDelete, setShowHardDelete] = useState(false);
  const [taskModal, setTaskModal] = useState<TaskModalKind>(null);

  const {
    workspaceId: contextWsId,
    departmentId: contextDeptId,
    canSelectDepartment,
    isScopedUser,
    hasAssignedDepartment,
    isLoading: contextLoading,
  } = useProjectDepartmentContext();

  const wsId = urlWsId || contextWsId;
  const deptId = canSelectDepartment ? urlDeptId : (contextDeptId ?? urlDeptId);

  const { data: project, isLoading, isError, error } = useProjectDetail(
    wsId || undefined,
    deptId || undefined,
    projectId,
  );

  const { canUpdate, canArchive, canRestore, canHardDelete } = useProjectAccess(wsId || undefined);
  const { canCreate: canCreateTask, canAssign } = useTaskAccess(wsId || undefined);

  const { data: tasks, isLoading: tasksLoading } = useProjectTasks(
    wsId || undefined,
    deptId || undefined,
    projectId,
  );
  const { data: departmentMembers } = useDepartmentMembers(wsId || undefined, deptId || undefined);
  const createTaskMutation = useCreateTask(wsId || undefined, deptId || undefined);
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const allTasks = tasks ?? [];
  const totalTasks = allTasks.length;
  const completedTasks = allTasks.filter((t) => t.status === 'completed').length;
  const inProgressTasks = allTasks.filter((t) => t.status === 'in-progress' || t.status === 'in-review' || t.status === 'blocked').length;
  const pendingTasks = allTasks.filter((t) => t.status === 'todo').length;
  const completion = totalTasks ? Math.round((completedTasks / totalTasks) * 100) : 0;
  const overdueTasks = allTasks.filter((t) => isTaskOverdue(t)).length;

  const members = buildMembers(project, allTasks);

  const tabItems: TabItem[] = [
    { id: 'overview', label: 'Overview', icon: <Info /> },
    { id: 'tasks', label: 'Tasks', icon: <CheckSquare />, count: totalTasks || undefined },
    { id: 'members', label: 'Members', icon: <Users />, count: members.length || undefined },
  ];

  if (contextLoading) {
    return <ProjectSkeleton />;
  }

  if (isScopedUser && !hasAssignedDepartment) {
    return (
      <Card>
        <CardBody className="py-16">
          <EmptyState
            icon={<AlertCircle className="h-6 w-6" />}
            title="No department assigned"
            description="No department is assigned to your account."
            action={<Button variant="outline" onClick={onBack}>Back to Projects</Button>}
          />
        </CardBody>
      </Card>
    );
  }

  if (canSelectDepartment && !deptId) {
    return (
      <Card>
        <CardBody className="py-16">
          <EmptyState
            icon={<FolderKanban className="h-6 w-6" />}
            title="Department required"
            description="Select a department from the projects list to open this project."
            action={<Button variant="outline" onClick={onBack}>Back to Projects</Button>}
          />
        </CardBody>
      </Card>
    );
  }

  if (isLoading) {
    return <ProjectSkeleton />;
  }

  if (isError) {
    const errorState = getProjectQueryErrorState(error, isScopedUser);
    return (
      <Card>
        <CardBody className="py-16">
          <EmptyState
            icon={errorState.isAccessDenied ? <ShieldBan className="h-6 w-6" /> : <AlertCircle className="h-6 w-6" />}
            title={errorState.title}
            description={errorState.description}
            action={<Button variant="outline" onClick={onBack}>Back to Projects</Button>}
          />
        </CardBody>
      </Card>
    );
  }

  if (!project) {
    return (
      <Card>
        <CardBody className="py-16">
          <EmptyState icon={<FolderKanban className="h-6 w-6" />} title="Project not found" description="The project you are looking for does not exist." />
        </CardBody>
      </Card>
    );
  }

  const actionItems: { key: string; label: string; icon: React.ReactNode; danger?: boolean; onClick: () => void }[] = [];
  if (project.status === 'ACTIVE' && canUpdate) {
    actionItems.push({ key: 'edit', label: 'Edit', icon: <Edit2 className="h-4 w-4" />, onClick: () => setShowEdit(true) });
  }
  if (project.status === 'ACTIVE' && canArchive) {
    actionItems.push({ key: 'archive', label: 'Delete', icon: <Archive className="h-4 w-4" />, danger: true, onClick: () => setShowArchive(true) });
  }
  if (project.status === 'ARCHIVED' && canRestore) {
    actionItems.push({ key: 'restore', label: 'Restore', icon: <RotateCcw className="h-4 w-4" />, onClick: () => setShowRestore(true) });
  }
  if (canHardDelete) {
    actionItems.push({ key: 'hard-delete', label: 'Delete Permanently', icon: <ShieldBan className="h-4 w-4" />, danger: true, onClick: () => setShowHardDelete(true) });
  }

  const handleCreateTask = async (data: CreateTaskFormData) => {
    try {
      await createTaskMutation.mutateAsync({
        projectId: data.projectId ?? project.id,
        data: mapToCreateRequest(data),
      });
      await queryClient.invalidateQueries({ queryKey: ['project-tasks', wsId, deptId, project.id] });
      toast({ title: 'Success', description: 'Task created successfully.', tone: 'success' });
      setTaskModal(null);
    } catch {
      toast({ title: 'Error', description: 'Failed to create task.', tone: 'danger' });
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <Breadcrumbs
        className="mb-1"
        items={buildBreadcrumbs(project.name, activeTab, onBack)}
      />

      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-4 flex-1 min-w-0">
          <button onClick={onBack} className="flex h-9 w-9 items-center justify-center rounded-lg border border-border-subtle text-text-secondary hover:bg-surface-2 hover:text-text-primary transition-colors mt-1 shrink-0" aria-label="Back to projects">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div className="flex-1 min-w-0">
            <div className="flex items-end gap-3 mb-2 flex-wrap">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg shrink-0" style={{ backgroundColor: project.color ?? '#e5e7eb' }}>
                <FolderKanban className="h-4 w-4 text-white" />
              </div>
              <h1 className="text-page font-semibold text-text-primary truncate">{project.name}</h1>
              <Badge tone={statusColors[project.status]} variant="soft" dot>
                {project.status === 'ACTIVE' ? 'Active' : 'Archived'}
              </Badge>
              {project.status === 'ARCHIVED' && (
                <Badge tone="neutral" variant="soft">Read-only</Badge>
              )}
            </div>
            <p className="text-body text-text-secondary line-clamp-2">{project.description ?? 'No description.'}</p>
            <div className="flex flex-wrap gap-2 mt-3">
              {project.priority && (
                <Badge tone={priorityColors[project.priority]} variant="soft" dot className="capitalize">
                  {project.priority.toLowerCase()}
                </Badge>
              )}
              {project.departmentName && (
                <Badge tone="accent" variant="soft">{project.departmentName}</Badge>
              )}
            </div>
          </div>
        </div>
        {actionItems.length > 0 && (
          <div className="flex flex-wrap items-center gap-2 shrink-0">
            {actionItems.map((a) => (
              <Button
                key={a.key}
                variant="outline"
                size="sm"
                leftIcon={a.icon}
                onClick={a.onClick}
                className={a.danger ? 'text-danger-600 border-danger-200 hover:bg-danger-50 dark:text-danger-400 dark:border-danger-100' : ''}
              >
                {a.label}
              </Button>
            ))}
          </div>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        <StatCard icon={<CheckSquare />} label="Total Tasks" value={totalTasks} tone="accent" />
        <StatCard icon={<ListChecks />} label="Completed" value={completedTasks} tone="success" />
        <StatCard icon={<Activity />} label="In Progress" value={inProgressTasks} tone="info" />
        <StatCard icon={<Timer />} label="Pending" value={pendingTasks} tone="warning" />
        <StatCard icon={<Flag />} label="Overdue" value={overdueTasks} tone="danger" />
      </div>

      {totalTasks > 0 && (
        <Card>
          <CardBody className="flex items-center gap-4">
            <div className="min-w-[120px]">
              <p className="text-2xs uppercase tracking-wide text-text-tertiary font-medium">Completion</p>
              <p className="text-xl font-bold text-text-primary">{completion}%</p>
            </div>
            <Progress value={completion} tone={completion === 100 ? 'success' : 'accent'} size="lg" className="flex-1" />
          </CardBody>
        </Card>
      )}

      <Tabs items={tabItems} active={activeTab} onChange={setActiveTab} />

      {activeTab === 'overview' && (
        <OverviewTab project={project} completion={completion} />
      )}

      {activeTab === 'tasks' && (
        <TasksTab
          tasks={allTasks}
          loading={tasksLoading}
          canCreateTask={canCreateTask}
          onOpenCreate={() => setTaskModal({ kind: 'create' })}
          onOpenTask={(task) => setTaskModal({ kind: 'view', task })}
        />
      )}

      {activeTab === 'members' && (
        <MembersTab members={members} loading={tasksLoading} />
      )}

      {showEdit && wsId && deptId && (
        <EditProjectModal open={showEdit} onClose={() => setShowEdit(false)} wsId={wsId} deptId={deptId} project={project} />
      )}
      {showArchive && wsId && deptId && (
        <ArchiveProjectModal open={showArchive} onClose={() => setShowArchive(false)} wsId={wsId} deptId={deptId} projectId={project.id} projectName={project.name} />
      )}
      {showRestore && wsId && deptId && (
        <RestoreProjectModal open={showRestore} onClose={() => setShowRestore(false)} wsId={wsId} deptId={deptId} projectId={project.id} projectName={project.name} />
      )}
      {showHardDelete && wsId && deptId && (
        <HardDeleteProjectModal open={showHardDelete} onClose={() => setShowHardDelete(false)} wsId={wsId} deptId={deptId} projectId={project.id} projectName={project.name} />
      )}

      {taskModal && (
        <TaskModal
          state={taskModal}
          onClose={() => setTaskModal(null)}
          onCreate={handleCreateTask}
          projects={[{ id: project.id, name: project.name }]}
          members={canAssign ? (departmentMembers ?? []).map((m) => ({ id: m.id, name: m.name })) : []}
          defaultProjectId={project.id}
          canAssign={canAssign}
          isSubmitting={createTaskMutation.isPending}
          wsId={wsId}
          deptId={deptId}
        />
      )}
    </div>
  );
}

function OverviewTab({ project, completion }: { project: ProjectResponse; completion: number }) {
  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
      <Card className="lg:col-span-2">
        <CardBody className="space-y-4">
          <p className="text-section font-semibold text-text-primary">Description</p>
          <p className="text-body text-text-secondary whitespace-pre-wrap">{project.description ?? 'No description provided.'}</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
            <InfoRow label="Project Name" value={project.name} />
            <InfoRow label="Status" value={project.status === 'ACTIVE' ? 'Active' : 'Archived'} />
            {project.priority && <InfoRow label="Priority" value={project.priority.toLowerCase()} />}
            {project.managerName && <InfoRow label="Manager" value={project.managerName} />}
            {project.departmentName && <InfoRow label="Department" value={project.departmentName} />}
            {project.startDate && <InfoRow label="Start Date" value={project.startDate} />}
            {project.endDate && <InfoRow label="End Date" value={project.endDate} />}
            {project.createdAt && <InfoRow label="Created" value={new Date(project.createdAt).toLocaleDateString()} />}
            {project.updatedAt && <InfoRow label="Updated" value={new Date(project.updatedAt).toLocaleDateString()} />}
          </div>
        </CardBody>
      </Card>
      <Card>
        <CardBody className="space-y-4">
          <p className="text-section font-semibold text-text-primary">Progress</p>
          <div>
            <div className="flex items-center justify-between mb-2">
              <span className="text-caption text-text-secondary">Completion</span>
              <span className="text-caption font-semibold text-text-primary">{completion}%</span>
            </div>
            <Progress value={completion} tone={completion === 100 ? 'success' : 'accent'} size="lg" />
          </div>
          <div className="space-y-3 pt-2">
            <MiniInfo icon={<Calendar />} label="Start" value={project.startDate ?? 'Not set'} />
            <MiniInfo icon={<Clock />} label="End" value={project.endDate ?? 'Not set'} />
            <MiniInfo icon={<Users />} label="Manager" value={project.managerName ?? 'Unassigned'} />
            <MiniInfo icon={<FolderKanban />} label="Department" value={project.departmentName ?? 'N/A'} />
          </div>
        </CardBody>
      </Card>
    </div>
  );
}

function TasksTab({
  tasks,
  loading,
  canCreateTask,
  onOpenCreate,
  onOpenTask,
}: {
  tasks: Task[];
  loading: boolean;
  canCreateTask: boolean;
  onOpenCreate: () => void;
  onOpenTask: (task: Task) => void;
}) {
  const columns: TableColumn<Task>[] = [
    {
      key: 'title',
      header: 'Task',
      render: (t) => (
        <button type="button" onClick={() => onOpenTask(t)} className="text-left font-medium text-text-primary hover:text-accent-600 transition-colors">
          {t.title}
        </button>
      ),
      sortValue: (t) => t.title,
    },
    {
      key: 'status',
      header: 'Status',
      sortable: true,
      render: (t) => {
        const meta = taskStatusMeta[t.status];
        return <Badge tone={meta.tone} variant="soft" dot>{meta.label}</Badge>;
      },
      sortValue: (t) => t.status,
    },
    {
      key: 'priority',
      header: 'Priority',
      sortable: true,
      render: (t) => {
        const meta = taskPriorityMeta[t.priority];
        return <Badge tone={meta.tone} variant="soft">{meta.label}</Badge>;
      },
      sortValue: (t) => t.priority,
    },
    {
      key: 'assignee',
      header: 'Assignee',
      render: (t) =>
        t.assigneeName ? (
          <span className="inline-flex items-center gap-2">
            <Avatar name={t.assigneeName} size="xs" />
            <span className="text-body text-text-secondary">{t.assigneeName}</span>
          </span>
        ) : (
          <span className="text-caption text-text-tertiary">Unassigned</span>
        ),
      sortValue: (t) => t.assigneeName ?? '',
    },
    {
      key: 'dueAt',
      header: 'Due',
      sortable: true,
      render: (t) =>
        t.dueAt ? (
          <span className={isTaskOverdue(t) ? 'text-caption font-medium text-danger-600 dark:text-danger-400' : 'text-caption text-text-secondary'}>
            {new Date(t.dueAt).toLocaleDateString()}
          </span>
        ) : (
          <span className="text-caption text-text-tertiary">—</span>
        ),
      sortValue: (t) => t.dueAt ?? '',
    },
  ];

  return (
    <Card>
      <CardBody className="space-y-4">
        <div className="flex items-center justify-between gap-3">
          <div>
            <h3 className="text-section font-semibold text-text-primary">Project Tasks</h3>
            <p className="text-caption text-text-tertiary">Track and manage tasks for this project.</p>
          </div>
          {canCreateTask && (
            <Button leftIcon={<Plus className="h-4 w-4" />} onClick={onOpenCreate}>Create Task</Button>
          )}
        </div>

        {loading ? (
          <div className="space-y-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full rounded-lg" />
            ))}
          </div>
        ) : tasks.length === 0 ? (
          <EmptyState
            icon={<CheckSquare className="h-6 w-6" />}
            title="No tasks in this project yet"
            description="Tasks you create will appear here."
          />
        ) : (
          <Table
            columns={columns}
            rows={tasks}
            rowKey={(t) => t.id}
            pageSize={8}
            searchable
            searchPlaceholder="Search tasks..."
            searchKeys={(t) => `${t.title} ${t.assigneeName ?? ''}`}
            emptyTitle="No tasks match"
          />
        )}
      </CardBody>
    </Card>
  );
}

interface MemberRow {
  id?: string;
  name: string;
  role: string;
  taskCount: number;
  completedCount: number;
}

function MembersTab({ members, loading }: { members: MemberRow[]; loading: boolean }) {
  if (loading) {
    return (
      <Card>
        <CardBody className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full rounded-lg" />
          ))}
        </CardBody>
      </Card>
    );
  }

  if (members.length === 0) {
    return (
      <Card>
        <CardBody className="py-16">
          <EmptyState icon={<Users className="h-6 w-6" />} title="No members assigned to this project" description="Tasks assigned to members will list them here." />
        </CardBody>
      </Card>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {members.map((m, idx) => (
        <Card key={m.id ?? idx}>
          <CardBody className="flex items-center gap-3">
            <Avatar name={m.name} size="md" tone={idx % 6} />
            <div className="min-w-0 flex-1">
              <p className="text-body font-medium text-text-primary truncate">{m.name}</p>
              <p className="text-2xs text-text-tertiary">{m.role}</p>
            </div>
            <div className="text-right shrink-0">
              <p className="text-body font-semibold text-text-primary">{m.taskCount}</p>
              <p className="text-2xs text-text-tertiary">{m.completedCount} done</p>
            </div>
          </CardBody>
        </Card>
      ))}
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="pb-3 border-b border-border-subtle last:pb-0 last:border-b-0">
      <p className="text-2xs text-text-tertiary mb-1">{label}</p>
      <p className="text-body font-medium text-text-primary capitalize">{value}</p>
    </div>
  );
}

function MiniInfo({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-center gap-2">
      <span className="text-text-tertiary [&>svg]:h-4 [&>svg]:w-4">{icon}</span>
      <span className="text-2xs text-text-tertiary w-16">{label}</span>
      <span className="text-body font-medium text-text-primary truncate">{value}</span>
    </div>
  );
}

function ProjectSkeleton() {
  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <div className="flex items-center gap-4">
        <Skeleton className="h-9 w-9 rounded-lg" />
        <div className="flex-1">
          <Skeleton className="h-6 w-48" />
          <Skeleton className="h-4 w-64 mt-2" />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-20 rounded-xl" />
        ))}
      </div>
      <Skeleton className="h-10 w-full rounded-lg" />
      <Skeleton className="h-64 w-full rounded-xl" />
    </div>
  );
}

function buildMembers(project: ProjectResponse | undefined, tasks: Task[]): MemberRow[] {
  if (!project) return [];
  const map = new Map<string, MemberRow>();

  if (project.managerName) {
    map.set('__manager__', {
      id: project.managerId,
      name: project.managerName,
      role: 'Project Manager',
      taskCount: 0,
      completedCount: 0,
    });
  }

  for (const t of tasks) {
    if (!t.assigneeId || !t.assigneeName) continue;
    const existing = map.get(t.assigneeId);
    if (existing) {
      existing.taskCount += 1;
      if (t.status === 'completed') existing.completedCount += 1;
    } else {
      map.set(t.assigneeId, {
        id: t.assigneeId,
        name: t.assigneeName,
        role: 'Member',
        taskCount: 1,
        completedCount: t.status === 'completed' ? 1 : 0,
      });
    }
  }

  return Array.from(map.values());
}

function buildBreadcrumbs(projectName: string, activeTab: string, onBack: () => void): BreadcrumbItem[] {
  const items: BreadcrumbItem[] = [
    { label: 'Projects', icon: <Briefcase />, onClick: onBack },
    { label: projectName },
  ];
  if (activeTab === 'tasks') items.push({ label: 'Tasks' });
  else if (activeTab === 'members') items.push({ label: 'Members' });
  return items;
}
