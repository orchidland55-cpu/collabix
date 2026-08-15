import { useState, useMemo } from 'react';
import { useToast } from '../../components/ui/Toast';
import {
  ArrowLeft,
  AlertCircle,
  Clock,
  Users,
  FileText,
  Calendar,
  MoreHorizontal,
  Edit2,
  Archive,
  Plus,
  Send,
  X,
  Trash2,
  ListChecks,
  ShieldBan,
} from 'lucide-react';
import { Card, CardBody, CardHeader, CardTitle } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge, type Tone } from '../../components/ui/Badge';
import { Avatar } from '../../components/ui/Avatar';
import { IconButton } from '../../components/ui/IconButton';
import { Progress } from '../../components/ui/Progress';
import { Textarea } from '../../components/ui/Textarea';
import { Checkbox } from '../../components/ui/Checkbox';
import { Tabs, type TabItem } from '../../components/ui/Tabs';
import { Dropdown, type DropdownItem } from '../../components/ui/Dropdown';
import { Timeline, type TimelineItem } from '../../components/ui/Timeline';
import { Skeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import { Input } from '../../components/ui/Input';
import {
  useTaskDetail,
  useUpdateTask,
  useUpdateTaskStatus,
  useDeleteTask,
  useCommentsList,
  useCreateComment,
  useDeleteComment,
  useAttachmentsList,
  useActivitiesList,
  useChecklistsList,
  useCreateChecklist,
  useDeleteChecklist,
  useCreateChecklistItem,
  useUpdateChecklistItem,
  useDeleteChecklistItem,
  useTaskDepartmentContext,
  getTaskQueryErrorState,
  useTaskAccess,
  useDepartmentMembers,
  canDragTask,
} from '../../services/task-hooks';
import {
  mapTaskResponse,
  mapCommentResponse,
  mapAttachmentResponse,
  mapActivityResponse,
  mapToUpdateRequest,
  FRONTEND_STATUS_MAP,
} from './tasks-types';
import type { Task, TaskStatus, TaskPriority } from './tasks-types';
import { TaskModal, type TaskModalKind, type EditTaskFormData } from './TaskModals';
import { useAuth } from '../../lib/auth-context';

const statusColor: Record<string, Tone> = {
  todo: 'info',
  'in-progress': 'accent',
  'in-review': 'warning',
  blocked: 'danger',
  completed: 'success',
  archived: 'neutral',
};

const statusLabels: Record<string, string> = {
  todo: 'To Do',
  'in-progress': 'In Progress',
  'in-review': 'In Review',
  blocked: 'Blocked',
  completed: 'Completed',
  archived: 'Archived',
};

const statusOptions: { value: TaskStatus; label: string }[] = [
  { value: 'todo', label: statusLabels.todo },
  { value: 'in-progress', label: statusLabels['in-progress'] },
  { value: 'in-review', label: statusLabels['in-review'] },
  { value: 'blocked', label: statusLabels.blocked },
  { value: 'completed', label: statusLabels.completed },
];

const priorityOptions: { value: TaskPriority; label: string }[] = [
  { value: 'low', label: 'Low' },
  { value: 'medium', label: 'Medium' },
  { value: 'high', label: 'High' },
  { value: 'urgent', label: 'Urgent' },
];

function priorityTone(p: TaskPriority): Tone {
  return p === 'urgent' ? 'danger' : p === 'high' ? 'warning' : p === 'medium' ? 'info' : 'success';
}

interface TaskDetailsPageProps {
  taskId: string;
  workspaceId?: string;
  departmentId?: string;
  projectId?: string;
  onBack?: () => void;
}

export function TaskDetailsPage({ taskId, workspaceId = '', departmentId = '', projectId = '', onBack }: TaskDetailsPageProps) {
  const {
    workspaceId: contextWsId,
    departmentId: contextDeptId,
    canSelectDepartment,
    isScopedUser,
    hasAssignedDepartment,
    isLoading: contextLoading,
  } = useTaskDepartmentContext();

  const effectiveWsId = workspaceId || contextWsId;
  const effectiveDeptId = canSelectDepartment ? departmentId : (contextDeptId ?? departmentId);
  const effectiveProjId = projectId;

  const [activeTab, setActiveTab] = useState('overview');
  const [commentText, setCommentText] = useState('');
  const [newChecklistTitle, setNewChecklistTitle] = useState('');
  const [newItemText, setNewItemText] = useState<Record<string, string>>({});
  const [editingItem, setEditingItem] = useState<Record<string, string>>({});
  const [modal, setModal] = useState<TaskModalKind>(null);
  const { toast } = useToast();
  const { user } = useAuth();

  const { data: taskData, isLoading: taskLoading, isError: taskError, error: taskFetchError } = useTaskDetail(effectiveWsId, effectiveDeptId, effectiveProjId, taskId);
  const { data: commentsPage } = useCommentsList(effectiveWsId, effectiveDeptId, effectiveProjId, taskId);
  const { data: attachmentsPage } = useAttachmentsList(effectiveWsId, effectiveDeptId, effectiveProjId, taskId);
  const { data: activitiesPage } = useActivitiesList(effectiveWsId, effectiveDeptId, effectiveProjId, taskId);
  const { data: checklistsData } = useChecklistsList(effectiveWsId, effectiveDeptId, effectiveProjId, taskId);

  const updateTask = useUpdateTask(effectiveWsId, effectiveDeptId);
  const updateStatus = useUpdateTaskStatus(effectiveWsId, effectiveDeptId, effectiveProjId);
  const deleteTask = useDeleteTask(effectiveWsId, effectiveDeptId);
  const createComment = useCreateComment(effectiveWsId, effectiveDeptId, effectiveProjId, taskId);
  const deleteComment = useDeleteComment(effectiveWsId, effectiveDeptId, effectiveProjId, taskId);
  const createChecklist = useCreateChecklist(effectiveWsId, effectiveDeptId, effectiveProjId, taskId);
  const deleteChecklist = useDeleteChecklist(effectiveWsId, effectiveDeptId, effectiveProjId, taskId);
  const createItem = useCreateChecklistItem(effectiveWsId, effectiveDeptId, effectiveProjId, taskId);
  const updateItem = useUpdateChecklistItem(effectiveWsId, effectiveDeptId, effectiveProjId, taskId);
  const deleteItem = useDeleteChecklistItem(effectiveWsId, effectiveDeptId, effectiveProjId, taskId);

  const { canUpdate, canDelete, canAssign } = useTaskAccess(effectiveWsId || undefined);
  const { data: departmentMembers } = useDepartmentMembers(
    canAssign ? effectiveWsId || undefined : undefined,
    canAssign ? effectiveDeptId || undefined : undefined,
  );
  const canManageTasks = canAssign;

  const task: Task | null = useMemo(() => {
    if (!taskData) return null;
    return mapTaskResponse(taskData);
  }, [taskData]);

  const canUpdateWorkflowStatus = !!task && canDragTask(task, user?.id, canManageTasks);

  const comments = useMemo(() => {
    if (!commentsPage?.content) return [];
    return commentsPage.content.map(mapCommentResponse);
  }, [commentsPage]);

  const attachments = useMemo(() => {
    if (!attachmentsPage?.content) return [];
    return attachmentsPage.content.map(mapAttachmentResponse);
  }, [attachmentsPage]);

  const activities = useMemo(() => {
    if (!activitiesPage?.content) return [];
    return activitiesPage.content.map(mapActivityResponse);
  }, [activitiesPage]);

  const checklists = useMemo(() => checklistsData?.content ?? [], [checklistsData]);

  const timelineItems: TimelineItem[] = useMemo(() => {
    return activities.map((a) => ({
      id: a.id,
      title: a.description,
      timestamp: a.timestamp,
      tone: 'neutral' as const,
    }));
  }, [activities]);

  const tabItems: TabItem[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'checklist', label: 'Checklist', count: checklists.length },
    { id: 'activity', label: 'Activity' },
    { id: 'comments', label: 'Comments', count: comments.length },
    { id: 'attachments', label: 'Attachments', count: attachments.length },
  ];

  const handleAddComment = () => {
    if (!commentText.trim()) return;
    createComment.mutate({ content: commentText.trim() }, {
      onSuccess: () => { setCommentText(''); toast({ title: 'Comment added', tone: 'success' }); },
      onError: () => toast({ title: 'Failed to add comment', tone: 'danger' }),
    });
  };

  const handleAddChecklist = () => {
    if (!newChecklistTitle.trim()) return;
    createChecklist.mutate({ title: newChecklistTitle.trim() }, {
      onSuccess: () => { setNewChecklistTitle(''); toast({ title: 'Checklist created', tone: 'success' }); },
      onError: () => toast({ title: 'Failed to create checklist', tone: 'danger' }),
    });
  };

  const handleAddItem = (checklistId: string) => {
    const text = newItemText[checklistId]?.trim();
    if (!text) return;
    createItem.mutate({ checklistId, data: { content: text } }, {
      onSuccess: () => { setNewItemText((prev) => ({ ...prev, [checklistId]: '' })); },
      onError: () => toast({ title: 'Failed to add item', tone: 'danger' }),
    });
  };

  const handleToggleItem = (checklistId: string, itemId: string, completed: boolean) => {
    updateItem.mutate({ checklistId, itemId, data: { completed: !completed } });
  };

  const handleSaveItemTitle = (checklistId: string, itemId: string) => {
    const text = editingItem[`${itemId}-content`]?.trim();
    if (!text) return;
    updateItem.mutate({ checklistId, itemId, data: { content: text } }, {
      onSuccess: () => setEditingItem((prev) => ({ ...prev, [`${itemId}-title`]: '' })),
    });
  };

  if (contextLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 rounded-lg" />
      </div>
    );
  }

  if (isScopedUser && !hasAssignedDepartment) {
    return (
      <EmptyState
        icon={<AlertCircle />}
        title="No department assigned"
        description="No department is assigned to your account."
      />
    );
  }

  if (taskLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-6 w-48" />
        <Skeleton className="h-96 rounded-lg" />
      </div>
    );
  }

  if (taskError || !task) {
    const errState = getTaskQueryErrorState(taskFetchError, isScopedUser);
    return (
      <EmptyState
        icon={errState.isAccessDenied ? <ShieldBan /> : <AlertCircle />}
        title={errState.title}
        description={errState.description}
      />
    );
  }

  const detailActionItems: DropdownItem[] = [
    ...(canAssign ? [{ label: task.assigneeId ? 'Reassign member' : 'Assign member', icon: <Users className="h-4 w-4" />, onClick: () => setModal({ kind: 'assign', task: { id: task.id, title: task.title, projectId: task.projectId, assigneeId: task.assigneeId } }) }] : []),
    ...(canUpdate ? [{ label: 'Edit task', icon: <Edit2 className="h-4 w-4" />, onClick: () => setModal({ kind: 'edit', task: { id: task.id, title: task.title, description: task.description, priority: task.priority, dueAt: task.dueAt, projectId: task.projectId } }) }] : []),
    ...(canDelete ? [{ divider: true, label: 'Danger zone' }, { label: 'Delete task', icon: <Trash2 className="h-4 w-4" />, onClick: () => setModal({ kind: 'delete', task: { id: task.id, title: task.title, projectId: task.projectId } }) }] : []),
  ];

  return (
    <div className="flex flex-col gap-6">
      <TaskModal
        state={modal}
        onClose={() => setModal(null)}
        onEdit={(data: EditTaskFormData) => {
          if (!modal || modal.kind !== 'edit') return;
          updateTask.mutate(
            { projectId: effectiveProjId, taskId: modal.task.id, data: mapToUpdateRequest(data) },
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
        }}
        onAssign={(assigneeId) => {
          if (!modal || modal.kind !== 'assign') return;
          updateTask.mutate(
            { projectId: modal.task.projectId, taskId: modal.task.id, data: { assigneeId } },
            {
              onSuccess: () => {
                setModal(null);
                toast({ title: 'Task assigned', tone: 'success' });
              },
              onError: () => toast({ title: 'Failed to assign task', tone: 'danger' }),
            },
          );
        }}
        onConfirmAction={() => {
          if (!modal || modal.kind !== 'delete') return;
          deleteTask.mutate(
            { projectId: effectiveProjId, taskId: modal.task.id },
            {
              onSuccess: () => {
                setModal(null);
                toast({ title: 'Task deleted', tone: 'success' });
                if (onBack) onBack();
              },
              onError: (err: unknown) => {
                const message = typeof err === 'object' && err !== null && 'message' in err
                  ? String((err as { message: string }).message)
                  : 'Failed to delete task';
                toast({ title: 'Failed to delete task', description: message, tone: 'danger' });
              },
            },
          );
        }}
        members={departmentMembers ?? []}
        isSubmitting={updateTask.isPending || deleteTask.isPending}
      />

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          {onBack && (
            <IconButton label="Back" variant="ghost" size="sm" onClick={onBack}>
              <ArrowLeft className="h-4 w-4" />
            </IconButton>
          )}
          <div className="flex flex-col">
            <div className="flex items-center gap-3">
              <h1 className="text-page font-semibold text-text-primary">{task.title}</h1>
              <Badge tone={statusColor[task.status]} variant="soft" dot>
                {statusLabels[task.status]}
              </Badge>
              <Badge tone={priorityTone(task.priority)} variant="soft">
                {task.priority}
              </Badge>
            </div>
            <p className="text-body text-text-secondary mt-1">
              {task.projectName || 'No project'}{task.departmentName ? ` · ${task.departmentName}` : ''}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Dropdown
            trigger={
              <Button variant="outline" size="md">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            }
            items={detailActionItems}
            align="right"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <Tabs tabs={tabItems} activeTab={activeTab} onChange={setActiveTab} />

          <div className="mt-4">
            {activeTab === 'overview' && (
              <Card>
                <CardBody className="space-y-4">
                  <div>
                    <h3 className="text-caption font-semibold text-text-primary mb-1">Description</h3>
                    <p className="text-body text-text-secondary">{task.description || 'No description provided.'}</p>
                  </div>
                  <div className="grid grid-cols-2 gap-4 pt-2 border-t border-border-subtle">
                    <InfoRow icon={<Calendar />} label="Created" value={task.createdAt} />
                    <InfoRow icon={<Clock />} label="Deadline" value={task.deadline || 'No deadline'} />
                    <InfoRow icon={<Users />} label="Assignee" value={task.assigneeName || 'Unassigned'} />
                    {task.departmentName && <InfoRow icon={<FileText />} label="Department" value={task.departmentName} />}
                    {task.startDate && <InfoRow icon={<Clock />} label="Start Date" value={task.startDate} />}
                  </div>
                </CardBody>
              </Card>
            )}

            {activeTab === 'checklist' && (
              <div className="space-y-4">
                <div className="flex items-center gap-2">
                  <Input
                    placeholder="New checklist title..."
                    value={newChecklistTitle}
                    onChange={(e) => setNewChecklistTitle(e.target.value)}
                    containerClassName="flex-1"
                  />
                  <Button onClick={handleAddChecklist} disabled={!newChecklistTitle.trim()} leftIcon={<Plus className="h-4 w-4" />}>Add</Button>
                </div>
                {checklists.length === 0 ? (
                  <Card><CardBody className="py-8 text-center"><p className="text-body text-text-secondary">No checklists yet. Create one above.</p></CardBody></Card>
                ) : (
                  checklists.map((cl) => (
                    <Card key={cl.id}>
                         <CardHeader className="flex items-center justify-between">
                           <div className="flex items-center gap-2">
                             <ListChecks className="h-4 w-4 text-text-tertiary" />
                             <CardTitle>{cl.title}</CardTitle>
                             <Badge tone="neutral" variant="soft">{cl.completedItems}/{cl.totalItems}</Badge>
                           </div>
                           <div className="flex items-center gap-2">
                             <Progress value={cl.completionPercentage} size="sm" className="w-20" />
                             <IconButton label="Delete checklist" variant="ghost" size="sm" onClick={() => deleteChecklist.mutate(cl.id, { onSuccess: () => toast({ title: 'Checklist deleted', tone: 'success' }) })}>
                               <Trash2 className="h-4 w-4 text-danger-500" />
                             </IconButton>
                           </div>
                         </CardHeader>
                       <CardBody className="space-y-1">
                         {cl.items?.map((item) => (
                           <div key={item.id} className="flex items-center gap-2 py-1">
                             <Checkbox checked={item.completed} onChange={() => handleToggleItem(cl.id, item.id, item.completed)} />
                             {editingItem[`${item.id}-content`] != null && editingItem[`${item.id}-content`] !== undefined ? (
                               <Input
                                 value={editingItem[`${item.id}-content`] ?? item.content}
                                 onChange={(e) => setEditingItem((prev) => ({ ...prev, [`${item.id}-content`]: e.target.value }))}
                                 onBlur={() => handleSaveItemTitle(cl.id, item.id)}
                                 onKeyDown={(e) => { if (e.key === 'Enter') handleSaveItemTitle(cl.id, item.id); }}
                                 autoFocus
                                 containerClassName="flex-1"
                               />
                             ) : (
                               <span
                                 className={`flex-1 text-body ${item.completed ? 'line-through text-text-tertiary' : 'text-text-primary'}`}
                                 onDoubleClick={() => setEditingItem((prev) => ({ ...prev, [`${item.id}-content`]: item.content }))}
                               >
                                 {item.content}
                               </span>
                             )}
                            <IconButton label="Delete item" variant="ghost" size="sm" onClick={() => deleteItem.mutate({ checklistId: cl.id, itemId: item.id })}>
                              <X className="h-3 w-3 text-text-tertiary" />
                            </IconButton>
                          </div>
                        ))}
                        <div className="flex items-center gap-2 pt-2">
                          <Input
                            placeholder="Add item..."
                            value={newItemText[cl.id] ?? ''}
                            onChange={(e) => setNewItemText((prev) => ({ ...prev, [cl.id]: e.target.value }))}
                            onKeyDown={(e) => { if (e.key === 'Enter') handleAddItem(cl.id); }}
                            containerClassName="flex-1"
                          />
                          <IconButton label="Add item" variant="ghost" size="sm" onClick={() => handleAddItem(cl.id)}>
                            <Plus className="h-4 w-4" />
                          </IconButton>
                        </div>
                      </CardBody>
                    </Card>
                  ))
                )}
              </div>
            )}

            {activeTab === 'activity' && (
              <Card>
                <CardBody>
                  {timelineItems.length === 0 ? (
                    <p className="text-body text-text-secondary py-4 text-center">No activity recorded yet.</p>
                  ) : (
                    <Timeline items={timelineItems} />
                  )}
                </CardBody>
              </Card>
            )}

            {activeTab === 'comments' && (
              <Card>
                <CardBody className="space-y-4">
                  <div className="flex gap-2">
                    <Textarea
                      placeholder="Write a comment..."
                      value={commentText}
                      onChange={(e) => setCommentText(e.target.value)}
                      className="flex-1"
                    />
                    <Button onClick={handleAddComment} disabled={!commentText.trim()} leftIcon={<Send className="h-4 w-4" />}>Send</Button>
                  </div>
                  {comments.length === 0 ? (
                    <p className="text-body text-text-secondary py-4 text-center">No comments yet.</p>
                  ) : (
                    comments.map((c) => (
                      <div key={c.id} className="flex gap-3 py-3 border-b border-border-subtle last:border-0">
                        <Avatar name={c.author || 'User'} size="sm" tone={0} />
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2">
                            <span className="text-caption font-medium text-text-primary">{c.author || 'User'}</span>
                            <span className="text-2xs text-text-tertiary">{c.timestamp}</span>
                          </div>
                          <p className="text-body text-text-secondary mt-1">{c.content}</p>
                        </div>
                        <IconButton label="Delete comment" variant="ghost" size="sm" onClick={() => deleteComment.mutate(c.id)}>
                          <X className="h-3 w-3 text-text-tertiary" />
                        </IconButton>
                      </div>
                    ))
                  )}
                </CardBody>
              </Card>
            )}

            {activeTab === 'attachments' && (
              <Card>
                <CardBody>
                  {attachments.length === 0 ? (
                    <p className="text-body text-text-secondary py-4 text-center">No attachments yet.</p>
                  ) : (
                    <div className="grid grid-cols-2 gap-3">
                      {attachments.map((a) => (
                        <div key={a.id} className="flex items-center gap-3 p-3 rounded-lg border border-border-subtle">
                          <FileText className="h-8 w-8 text-text-tertiary" />
                          <div className="min-w-0">
                            <p className="text-caption font-medium text-text-primary truncate">{a.name}</p>
                            <p className="text-2xs text-text-tertiary">{a.type}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </CardBody>
              </Card>
            )}
          </div>
        </div>

        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Details</CardTitle>
            </CardHeader>
            <CardBody className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-caption text-text-tertiary">Status</span>
                <Dropdown
                  trigger={
                    <Badge tone={statusColor[task.status]} variant="soft" dot>{statusLabels[task.status]}</Badge>
                  }
                  align="right"
                  items={canUpdateWorkflowStatus ? statusOptions.map((o) => ({
                    label: o.label,
                    disabled: o.value === task.status,
                    onClick: () => updateStatus.mutate({
                      taskId: task.id,
                      projectId: task.projectId,
                      status: FRONTEND_STATUS_MAP[o.value],
                    }, {
                      onSuccess: () => toast({ title: 'Status updated', tone: 'success' }),
                      onError: () => toast({ title: 'Failed to update status', tone: 'danger' }),
                    }),
                  })) : []}
                />
              </div>
              <div className="flex items-center justify-between">
                <span className="text-caption text-text-tertiary">Priority</span>
                <Dropdown
                  trigger={
                    <Badge tone={priorityTone(task.priority)} variant="soft">{task.priority}</Badge>
                  }
                  align="right"
                  items={canUpdate ? priorityOptions.map((o) => ({
                    label: o.label,
                    disabled: o.value === task.priority,
                    onClick: () => updateTask.mutate({
                      projectId: task.projectId,
                      taskId: task.id,
                      data: mapToUpdateRequest({ priority: o.value }),
                    }),
                  })) : []}
                />
              </div>
              <div className="flex items-center justify-between">
                <span className="text-caption text-text-tertiary">Assignee</span>
                <span className="text-caption font-medium text-text-primary">{task.assigneeName || 'Unassigned'}</span>
              </div>
              {task.departmentName && (
                <div className="flex items-center justify-between">
                  <span className="text-caption text-text-tertiary">Department</span>
                  <span className="text-caption text-text-primary">{task.departmentName}</span>
                </div>
              )}
              {task.projectName && (
                <div className="flex items-center justify-between">
                  <span className="text-caption text-text-tertiary">Project</span>
                  <span className="text-caption text-text-primary">{task.projectName}</span>
                </div>
              )}
              <div className="flex items-center justify-between">
                <span className="text-caption text-text-tertiary">Created</span>
                <span className="text-caption text-text-primary">{task.createdAt}</span>
              </div>
              {task.deadline && (
                <div className="flex items-center justify-between">
                  <span className="text-caption text-text-tertiary">Due</span>
                  <span className="text-caption text-text-primary">{task.deadline}</span>
                </div>
              )}
            </CardBody>
          </Card>
        </div>
      </div>
    </div>
  );
}

function InfoRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-center gap-2">
      <span className="text-text-tertiary">{icon}</span>
      <div>
        <p className="text-2xs text-text-tertiary">{label}</p>
        <p className="text-caption text-text-primary">{value}</p>
      </div>
    </div>
  );
}
