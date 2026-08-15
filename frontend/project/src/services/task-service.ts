import { apiClient } from '../lib/api';
import type { PageResponse } from '../types/api';
import type {
  TaskResponse,
  CreateTaskRequest,
  UpdateTaskRequest,
  CommentResponse,
  CreateCommentRequest,
  UpdateCommentRequest,
  AttachmentResponse,
  CreateAttachmentRequest,
  UpdateAttachmentRequest,
  ActivityResponse,
  CreateActivityRequest,
  ChecklistResponse,
  CreateChecklistRequest,
  UpdateChecklistRequest,
  ChecklistItemResponse,
  CreateChecklistItemRequest,
  UpdateChecklistItemRequest,
} from '../pages/tasks/tasks-types';

function taskBase(wsId: string, deptId: string, projId: string) {
  return `/workspaces/${wsId}/departments/${deptId}/projects/${projId}/tasks`;
}

function toBackendStatusFilter(status?: string): string | undefined {
  if (!status) return undefined;
  const map: Record<string, string> = {
    todo: 'ACTIVE',
    'in-progress': 'IN_PROGRESS',
    'in-review': 'IN_REVIEW',
    blocked: 'BLOCKED',
    completed: 'COMPLETED',
    archived: 'ARCHIVED',
    cancelled: 'CANCELLED',
  };
  return map[status] ?? status.toUpperCase().replace(/-/g, '_');
}

export const taskService = {
  list: (wsId: string, deptId: string, projId: string, params?: {
    search?: string; status?: string; priority?: string; assigneeId?: string; page?: number; size?: number;
  }) => {
    const query = new URLSearchParams();
    if (params?.search) query.set('search', params.search);
    if (params?.status) query.set('status', toBackendStatusFilter(params.status)!);
    if (params?.priority) query.set('priority', params.priority);
    if (params?.assigneeId) query.set('assignee', params.assigneeId);
    if (params?.page != null) query.set('page', String(params.page));
    if (params?.size != null) query.set('size', String(params.size));
    const qs = query.toString();
    return apiClient.get<PageResponse<TaskResponse>>(`${taskBase(wsId, deptId, projId)}${qs ? `?${qs}` : ''}`);
  },

  getById: (wsId: string, deptId: string, projId: string, taskId: string) =>
    apiClient.get<TaskResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}`),

  create: (wsId: string, deptId: string, projId: string, data: CreateTaskRequest) =>
    apiClient.post<TaskResponse>(`${taskBase(wsId, deptId, projId)}`, data),

  update: (wsId: string, deptId: string, projId: string, taskId: string, data: UpdateTaskRequest) =>
    apiClient.put<TaskResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}`, data),

  delete: (wsId: string, deptId: string, projId: string, taskId: string) =>
    apiClient.delete<void>(`${taskBase(wsId, deptId, projId)}/${taskId}`),

  restore: (wsId: string, deptId: string, projId: string, taskId: string) =>
    apiClient.put<TaskResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/restore`),

  listArchived: (wsId: string, deptId: string, projId: string) =>
    apiClient.get<TaskResponse[]>(`${taskBase(wsId, deptId, projId)}/archived`),
};

export const checklistService = {
  list: (wsId: string, deptId: string, projId: string, taskId: string) =>
    apiClient.get<PageResponse<ChecklistResponse>>(`${taskBase(wsId, deptId, projId)}/${taskId}/checklists`),

  getById: (wsId: string, deptId: string, projId: string, taskId: string, checklistId: string) =>
    apiClient.get<ChecklistResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/checklists/${checklistId}`),

  create: (wsId: string, deptId: string, projId: string, taskId: string, data: CreateChecklistRequest) =>
    apiClient.post<ChecklistResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/checklists`, data),

  update: (wsId: string, deptId: string, projId: string, taskId: string, checklistId: string, data: UpdateChecklistRequest) =>
    apiClient.put<ChecklistResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/checklists/${checklistId}`, data),

  delete: (wsId: string, deptId: string, projId: string, taskId: string, checklistId: string) =>
    apiClient.delete<void>(`${taskBase(wsId, deptId, projId)}/${taskId}/checklists/${checklistId}`),

  createItem: (wsId: string, deptId: string, projId: string, taskId: string, checklistId: string, data: CreateChecklistItemRequest) =>
    apiClient.post<ChecklistItemResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/checklists/${checklistId}/items`, data),

  updateItem: (wsId: string, deptId: string, projId: string, taskId: string, checklistId: string, itemId: string, data: UpdateChecklistItemRequest) =>
    apiClient.put<ChecklistItemResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/checklists/${checklistId}/items/${itemId}`, data),

  deleteItem: (wsId: string, deptId: string, projId: string, taskId: string, checklistId: string, itemId: string) =>
    apiClient.delete<void>(`${taskBase(wsId, deptId, projId)}/${taskId}/checklists/${checklistId}/items/${itemId}`),
};

export const commentService = {
  list: (wsId: string, deptId: string, projId: string, taskId: string) =>
    apiClient.get<PageResponse<CommentResponse>>(`${taskBase(wsId, deptId, projId)}/${taskId}/comments`),

  getById: (wsId: string, deptId: string, projId: string, taskId: string, commentId: string) =>
    apiClient.get<CommentResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/comments/${commentId}`),

  create: (wsId: string, deptId: string, projId: string, taskId: string, data: CreateCommentRequest) =>
    apiClient.post<CommentResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/comments`, data),

  update: (wsId: string, deptId: string, projId: string, taskId: string, commentId: string, data: UpdateCommentRequest) =>
    apiClient.put<CommentResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/comments/${commentId}`, data),

  delete: (wsId: string, deptId: string, projId: string, taskId: string, commentId: string) =>
    apiClient.delete<void>(`${taskBase(wsId, deptId, projId)}/${taskId}/comments/${commentId}`),
};

export const attachmentService = {
  list: (wsId: string, deptId: string, projId: string, taskId: string) =>
    apiClient.get<PageResponse<AttachmentResponse>>(`${taskBase(wsId, deptId, projId)}/${taskId}/attachments`),

  getById: (wsId: string, deptId: string, projId: string, taskId: string, attachmentId: string) =>
    apiClient.get<AttachmentResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/attachments/${attachmentId}`),

  create: (wsId: string, deptId: string, projId: string, taskId: string, data: CreateAttachmentRequest) =>
    apiClient.post<AttachmentResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/attachments`, data),

  update: (wsId: string, deptId: string, projId: string, taskId: string, attachmentId: string, data: UpdateAttachmentRequest) =>
    apiClient.put<AttachmentResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/attachments/${attachmentId}`, data),

  delete: (wsId: string, deptId: string, projId: string, taskId: string, attachmentId: string) =>
    apiClient.delete<void>(`${taskBase(wsId, deptId, projId)}/${taskId}/attachments/${attachmentId}`),
};

export const activityService = {
  list: (wsId: string, deptId: string, projId: string, taskId: string) =>
    apiClient.get<PageResponse<ActivityResponse>>(`${taskBase(wsId, deptId, projId)}/${taskId}/activities`),

  getById: (wsId: string, deptId: string, projId: string, taskId: string, activityId: string) =>
    apiClient.get<ActivityResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/activities/${activityId}`),

  create: (wsId: string, deptId: string, projId: string, taskId: string, data: CreateActivityRequest) =>
    apiClient.post<ActivityResponse>(`${taskBase(wsId, deptId, projId)}/${taskId}/activities`, data),
};
