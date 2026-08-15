import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  taskService,
  commentService,
  attachmentService,
  activityService,
  checklistService,
} from './task-service';
import type {
  TaskResponse,
  CreateTaskRequest,
  UpdateTaskRequest,
  BackendTaskStatus,
  CommentResponse,
  CreateCommentRequest,
  UpdateCommentRequest,
  AttachmentResponse,
  CreateAttachmentRequest,
  ActivityResponse,
  ChecklistResponse,
  CreateChecklistRequest,
  UpdateChecklistRequest,
  CreateChecklistItemRequest,
  UpdateChecklistItemRequest,
  Task,
} from '../pages/tasks/tasks-types';
import { mapTaskResponse } from '../pages/tasks/tasks-types';
import type { PageResponse } from '../types/api';
import { useAuth } from '../lib/auth-context';
import { hasPermission, isAdmin, isManager, isMember, isSuperAdmin } from '../lib/access';
import { useWorkspaceDetail } from './workspace-hooks';
import { useProjectList } from './project-hooks';
import { userService } from './user-service';

export {
  useTaskDepartmentContext,
  getTaskEmptyDescription,
  getTaskQueryErrorState,
} from './task-department-context';

const taskKeys = {
  all: ['tasks'] as const,
  list: (wsId: string, deptId: string, projId: string, params?: Record<string, unknown>) =>
    ['tasks', 'list', wsId, deptId, projId, params] as const,
  departmentList: (wsId: string, deptId: string, projId: string | undefined, params?: Record<string, unknown>) =>
    ['tasks', 'department-list', wsId, deptId, projId ?? 'all', params] as const,
  detail: (wsId: string, deptId: string, projId: string, taskId: string) =>
    ['tasks', 'detail', wsId, deptId, projId, taskId] as const,
  archived: (wsId: string, deptId: string, projId: string) =>
    ['tasks', 'archived', wsId, deptId, projId] as const,
  comments: (wsId: string, deptId: string, projId: string, taskId: string) =>
    ['tasks', 'comments', wsId, deptId, projId, taskId] as const,
  attachments: (wsId: string, deptId: string, projId: string, taskId: string) =>
    ['tasks', 'attachments', wsId, deptId, projId, taskId] as const,
  activities: (wsId: string, deptId: string, projId: string, taskId: string) =>
    ['tasks', 'activities', wsId, deptId, projId, taskId] as const,
  checklists: (wsId: string, deptId: string, projId: string, taskId: string) =>
    ['tasks', 'checklists', wsId, deptId, projId, taskId] as const,
};

export function useTasksList(
  wsId: string, deptId: string, projId: string,
  params?: { search?: string; status?: string; priority?: string; assigneeId?: string; page?: number; size?: number },
) {
  return useQuery<PageResponse<TaskResponse>>({
    queryKey: taskKeys.list(wsId, deptId, projId, params),
    queryFn: () => taskService.list(wsId, deptId, projId, params),
    enabled: !!wsId && !!deptId && !!projId,
  });
}

export function useDepartmentTasksList(
  wsId: string,
  deptId: string,
  projId: string | undefined,
  params?: { search?: string; status?: string; priority?: string; assigneeId?: string },
) {
  const { data: projects, isLoading: projectsLoading } = useProjectList(
    wsId || undefined,
    deptId || undefined,
    undefined,
    0,
  );

  return useQuery<{ content: Task[]; totalElements: number }>({
    queryKey: taskKeys.departmentList(wsId, deptId, projId, params),
    queryFn: async () => {
      const projectIds = projId
        ? [projId]
        : (projects?.content ?? []).map((p) => p.id);

      if (projectIds.length === 0) {
        return { content: [], totalElements: 0 };
      }

      const results = await Promise.allSettled(
        projectIds.map((pid) =>
          taskService.list(wsId, deptId, pid, { ...params, size: 200, page: 0 }),
        ),
      );

      const pages = results
        .filter((result): result is PromiseFulfilledResult<PageResponse<TaskResponse>> => result.status === 'fulfilled')
        .map((result) => result.value);

      if (pages.length === 0 && results.some((result) => result.status === 'rejected')) {
        const firstError = results.find((result) => result.status === 'rejected') as PromiseRejectedResult;
        throw firstError.reason;
      }

      const includeArchived = params?.status === 'archived';
      const content = pages
        .flatMap((page) => page.content ?? [])
        .map(mapTaskResponse)
        .filter((task) =>
          includeArchived
            ? task.status === 'archived'
            : task.status !== 'archived' && task.status !== 'cancelled',
        );

      return { content, totalElements: content.length };
    },
    enabled: !!wsId && !!deptId && !projectsLoading && (projId ? true : !!projects),
  });
}

export function useDepartmentMembers(wsId: string | undefined, deptId: string | undefined) {
  return useQuery({
    queryKey: ['tasks', 'department-members', wsId, deptId],
    queryFn: async () => {
      const page = await userService(wsId!).search(
        { departmentId: deptId, status: 'ACTIVE' },
        0,
        200,
      );
      return (page.content ?? []).map((user) => ({
        id: user.id,
        name: [user.firstName, user.lastName].filter(Boolean).join(' ') || user.email,
        email: user.email,
      }));
    },
    enabled: !!wsId && !!deptId,
  });
}

export function useArchivedTasksList(wsId: string, deptId: string, projId: string) {
  return useQuery<TaskResponse[]>({
    queryKey: taskKeys.archived(wsId, deptId, projId),
    queryFn: () => taskService.listArchived(wsId, deptId, projId),
    enabled: !!wsId && !!deptId && !!projId,
  });
}

export function useTaskDetail(wsId: string, deptId: string, projId: string, taskId: string | undefined) {
  return useQuery<TaskResponse>({
    queryKey: taskKeys.detail(wsId, deptId, projId, taskId ?? ''),
    queryFn: () => taskService.getById(wsId, deptId, projId, taskId!),
    enabled: !!wsId && !!deptId && !!projId && !!taskId,
  });
}

export function useCreateTask(wsId: string, deptId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, data }: { projectId: string; data: CreateTaskRequest }) =>
      taskService.create(wsId, deptId, projectId, data),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: taskKeys.list(wsId, deptId, variables.projectId) });
      qc.invalidateQueries({ queryKey: ['tasks', 'department-list', wsId, deptId] });
    },
  });
}

export function useUpdateTask(wsId: string, deptId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, taskId, data }: { projectId: string; taskId: string; data: UpdateTaskRequest }) =>
      taskService.update(wsId, deptId, projectId, taskId, data),
    onMutate: async ({ taskId, data }) => {
      await qc.cancelQueries({ queryKey: ['tasks', 'department-list', wsId, deptId] });
      const previous = qc.getQueriesData<{ content: Task[]; totalElements: number }>({
        queryKey: ['tasks', 'department-list', wsId, deptId],
      });
      previous.forEach(([key, cache]) => {
        if (!cache?.content) return;
        qc.setQueryData(key, {
          ...cache,
          content: cache.content.map((task) => {
            if (task.id !== taskId) return task;
            return {
              ...task,
              title: data.title ?? task.title,
              description: data.description ?? task.description,
              priority: data.priority
                ? ({ CRITICAL: 'urgent', HIGH: 'high', MEDIUM: 'medium', LOW: 'low' } as const)[data.priority] ?? task.priority
                : task.priority,
              dueAt: data.dueAt ?? task.dueAt,
              deadline: data.dueAt ? new Date(data.dueAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : task.deadline,
              status: data.status
                ? mapBackendStatusToFrontend(data.status)
                : task.status,
            };
          }),
        });
      });
      return { previous };
    },
    onError: (_err, _vars, context) => {
      context?.previous.forEach(([key, data]) => {
        qc.setQueryData(key, data);
      });
    },
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: taskKeys.detail(wsId, deptId, variables.projectId, variables.taskId) });
      qc.invalidateQueries({ queryKey: taskKeys.list(wsId, deptId, variables.projectId) });
      qc.invalidateQueries({ queryKey: ['tasks', 'department-list', wsId, deptId] });
    },
  });
}

export function useUpdateTaskStatus(wsId: string, deptId: string, defaultProjId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      taskId,
      projectId,
      status,
    }: {
      taskId: string;
      projectId: string;
      status: BackendTaskStatus;
    }) => taskService.update(wsId, deptId, projectId, taskId, { status }),
    onMutate: async ({ taskId, status }) => {
      await qc.cancelQueries({ queryKey: ['tasks', 'department-list', wsId, deptId] });
      const previous = qc.getQueriesData<{ content: Task[]; totalElements: number }>({
        queryKey: ['tasks', 'department-list', wsId, deptId],
      });
      previous.forEach(([key, data]) => {
        if (!data?.content) return;
        qc.setQueryData(key, {
          ...data,
          content: data.content.map((task) =>
            task.id === taskId
              ? { ...task, status: mapBackendStatusToFrontend(status) }
              : task,
          ),
        });
      });
      return { previous };
    },
    onError: (_err, _vars, context) => {
      context?.previous.forEach(([key, data]) => {
        qc.setQueryData(key, data);
      });
    },
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: taskKeys.detail(wsId, deptId, variables.projectId, variables.taskId) });
      qc.invalidateQueries({ queryKey: ['tasks', 'department-list', wsId, deptId] });
      qc.invalidateQueries({ queryKey: taskKeys.list(wsId, deptId, variables.projectId) });
    },
  });
}

function mapBackendStatusToFrontend(status: BackendTaskStatus): Task['status'] {
  const map: Record<BackendTaskStatus, Task['status']> = {
    ACTIVE: 'todo',
    TODO: 'todo',
    IN_PROGRESS: 'in-progress',
    IN_REVIEW: 'in-review',
    BLOCKED: 'blocked',
    COMPLETED: 'completed',
    ARCHIVED: 'archived',
    CANCELLED: 'cancelled',
  };
  return map[status] ?? 'todo';
}

export function useDeleteTask(wsId: string, deptId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ projectId, taskId }: { projectId: string; taskId: string }) =>
      taskService.delete(wsId, deptId, projectId, taskId),
    onMutate: async ({ taskId }) => {
      await qc.cancelQueries({ queryKey: ['tasks', 'department-list', wsId, deptId] });
      const previous = qc.getQueriesData<{ content: Task[]; totalElements: number }>({
        queryKey: ['tasks', 'department-list', wsId, deptId],
      });
      previous.forEach(([key, cache]) => {
        if (!cache?.content) return;
        const content = cache.content.filter((task) => task.id !== taskId);
        qc.setQueryData(key, { ...cache, content, totalElements: content.length });
      });
      return { previous };
    },
    onError: (_err, _vars, context) => {
      context?.previous.forEach(([key, data]) => {
        qc.setQueryData(key, data);
      });
    },
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: taskKeys.list(wsId, deptId, variables.projectId) });
      qc.invalidateQueries({ queryKey: ['tasks', 'department-list', wsId, deptId] });
    },
  });
}

export function useRestoreTask(wsId: string, deptId: string, projId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (taskId: string) => taskService.restore(wsId, deptId, projId, taskId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: taskKeys.list(wsId, deptId, projId) });
      qc.invalidateQueries({ queryKey: taskKeys.archived(wsId, deptId, projId) });
    },
  });
}

export function useCommentsList(wsId: string, deptId: string, projId: string, taskId: string) {
  return useQuery<PageResponse<CommentResponse>>({
    queryKey: taskKeys.comments(wsId, deptId, projId, taskId),
    queryFn: () => commentService.list(wsId, deptId, projId, taskId),
    enabled: !!wsId && !!deptId && !!projId && !!taskId,
  });
}

export function useCreateComment(wsId: string, deptId: string, projId: string, taskId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateCommentRequest) => commentService.create(wsId, deptId, projId, taskId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: taskKeys.comments(wsId, deptId, projId, taskId) });
    },
  });
}

export function useUpdateComment(wsId: string, deptId: string, projId: string, taskId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ commentId, data }: { commentId: string; data: UpdateCommentRequest }) =>
      commentService.update(wsId, deptId, projId, taskId, commentId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: taskKeys.comments(wsId, deptId, projId, taskId) });
    },
  });
}

export function useDeleteComment(wsId: string, deptId: string, projId: string, taskId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (commentId: string) => commentService.delete(wsId, deptId, projId, taskId, commentId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: taskKeys.comments(wsId, deptId, projId, taskId) });
    },
  });
}

export function useAttachmentsList(wsId: string, deptId: string, projId: string, taskId: string) {
  return useQuery<PageResponse<AttachmentResponse>>({
    queryKey: taskKeys.attachments(wsId, deptId, projId, taskId),
    queryFn: () => attachmentService.list(wsId, deptId, projId, taskId),
    enabled: !!wsId && !!deptId && !!projId && !!taskId,
  });
}

export function useCreateAttachment(wsId: string, deptId: string, projId: string, taskId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateAttachmentRequest) => attachmentService.create(wsId, deptId, projId, taskId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: taskKeys.attachments(wsId, deptId, projId, taskId) });
    },
  });
}

export function useDeleteAttachment(wsId: string, deptId: string, projId: string, taskId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (attachmentId: string) => attachmentService.delete(wsId, deptId, projId, taskId, attachmentId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: taskKeys.attachments(wsId, deptId, projId, taskId) });
    },
  });
}

export function useActivitiesList(wsId: string, deptId: string, projId: string, taskId: string) {
  return useQuery<PageResponse<ActivityResponse>>({
    queryKey: taskKeys.activities(wsId, deptId, projId, taskId),
    queryFn: () => activityService.list(wsId, deptId, projId, taskId),
    enabled: !!wsId && !!deptId && !!projId && !!taskId,
  });
}

export function useChecklistsList(wsId: string, deptId: string, projId: string, taskId: string) {
  return useQuery<PageResponse<ChecklistResponse>>({
    queryKey: taskKeys.checklists(wsId, deptId, projId, taskId),
    queryFn: () => checklistService.list(wsId, deptId, projId, taskId),
    enabled: !!wsId && !!deptId && !!projId && !!taskId,
  });
}

export function useCreateChecklist(wsId: string, deptId: string, projId: string, taskId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateChecklistRequest) => checklistService.create(wsId, deptId, projId, taskId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: taskKeys.checklists(wsId, deptId, projId, taskId) });
    },
  });
}

export function useUpdateChecklist(wsId: string, deptId: string, projId: string, taskId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ checklistId, data }: { checklistId: string; data: UpdateChecklistRequest }) =>
      checklistService.update(wsId, deptId, projId, taskId, checklistId, data),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: taskKeys.checklists(wsId, deptId, projId, taskId) });
    },
  });
}

export function useDeleteChecklist(wsId: string, deptId: string, projId: string, taskId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (checklistId: string) => checklistService.delete(wsId, deptId, projId, taskId, checklistId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: taskKeys.checklists(wsId, deptId, projId, taskId) });
    },
  });
}

export function useCreateChecklistItem(wsId: string, deptId: string, projId: string, taskId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ checklistId, data }: { checklistId: string; data: CreateChecklistItemRequest }) =>
      checklistService.createItem(wsId, deptId, projId, taskId, checklistId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: taskKeys.checklists(wsId, deptId, projId, taskId) });
    },
  });
}

export function useUpdateChecklistItem(wsId: string, deptId: string, projId: string, taskId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ checklistId, itemId, data }: { checklistId: string; itemId: string; data: UpdateChecklistItemRequest }) =>
      checklistService.updateItem(wsId, deptId, projId, taskId, checklistId, itemId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: taskKeys.checklists(wsId, deptId, projId, taskId) });
    },
  });
}

export function useDeleteChecklistItem(wsId: string, deptId: string, projId: string, taskId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ checklistId, itemId }: { checklistId: string; itemId: string }) =>
      checklistService.deleteItem(wsId, deptId, projId, taskId, checklistId, itemId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: taskKeys.checklists(wsId, deptId, projId, taskId) });
    },
  });
}

export interface TaskAccess {
  canCreate: boolean;
  canUpdate: boolean;
  canUpdateStatus: boolean;
  canAssign: boolean;
  canDelete: boolean;
  /** @deprecated use canDelete */
  canArchive: boolean;
  isLoading: boolean;
}

export function isTaskAssignee(task: Pick<Task, 'assigneeId'>, userId: string | undefined): boolean {
  return !!userId && !!task.assigneeId && task.assigneeId === userId;
}

export function canDragTask(
  task: Pick<Task, 'assigneeId'>,
  userId: string | undefined,
  canManageTasks: boolean,
): boolean {
  if (canManageTasks) return true;
  return isTaskAssignee(task, userId);
}

export function useTaskAccess(wsId: string | undefined): TaskAccess {
  const { user } = useAuth();
  const { data: workspace, isLoading } = useWorkspaceDetail(wsId || undefined);

  const roles = user?.roles ?? [];
  const superAdmin = isSuperAdmin(roles);
  const globalAdmin = isAdmin(roles);
  const globalManager = isManager(roles);
  const memberUser = isMember(roles);
  const wsRole = workspace?.myRole ?? null;
  const isWorkspaceManager = wsRole === 'OWNER' || wsRole === 'ADMIN';
  const isWorkspaceOwner = wsRole === 'OWNER';
  const canManageTasks = superAdmin || globalAdmin || isWorkspaceManager || globalManager;

  const canManageWithPermission = !!user && hasPermission(user, 'TASK_UPDATE') && canManageTasks;
  const canSelfServiceUpdate = !!user && memberUser && hasPermission(user, 'TASK_READ');
  const canDeleteTasks = !!user && hasPermission(user, 'TASK_DELETE') && canManageTasks;

  return {
    canCreate: !!user && hasPermission(user, 'TASK_CREATE') && canManageTasks,
    canUpdate: canManageWithPermission,
    canUpdateStatus: canSelfServiceUpdate,
    canAssign: !!user && hasPermission(user, 'TASK_CREATE') && canManageTasks,
    canDelete: canDeleteTasks,
    canArchive: canDeleteTasks,
    isLoading,
  };
}
