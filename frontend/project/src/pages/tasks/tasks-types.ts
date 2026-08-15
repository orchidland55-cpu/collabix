import type { LucideIcon } from 'lucide-react';

export type TaskStatus = 'todo' | 'in-progress' | 'in-review' | 'blocked' | 'completed' | 'archived' | 'cancelled';
export type TaskPriority = 'low' | 'medium' | 'high' | 'urgent';

export type BackendTaskStatus = 'ACTIVE' | 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'BLOCKED' | 'COMPLETED' | 'ARCHIVED' | 'CANCELLED';
export type BackendTaskPriority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type BackendActivityStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED';
export type BackendCommentStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED';
export type BackendAttachmentStatus = 'ACTIVE' | 'DELETED';

export interface TaskResponse {
  id: string;
  projectId: string;
  title: string;
  description?: string;
  status: BackendTaskStatus;
  priority?: BackendTaskPriority;
  assigneeId?: string;
  assigneeName?: string;
  startDate?: string;
  dueAt?: string;
  projectName?: string;
  departmentName?: string;
  sprintId?: string;
  securityAuditId?: string;
  marketingCampaignId?: string;
  storyPoints?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  assigneeId?: string;
  priority?: BackendTaskPriority;
  startDate?: string;
  dueAt?: string;
  sprintId?: string;
  securityAuditId?: string;
  marketingCampaignId?: string;
  storyPoints?: number;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  assigneeId?: string;
  priority?: BackendTaskPriority;
  startDate?: string;
  dueAt?: string;
  sprintId?: string;
  securityAuditId?: string;
  marketingCampaignId?: string;
  storyPoints?: number;
  status?: BackendTaskStatus;
}

export interface CommentResponse {
  id: string;
  taskId: string;
  content: string;
  status: BackendCommentStatus;
  parentCommentId?: string;
  createdAt: string;
  updatedAt: string;
  attachments: AttachmentResponse[];
}

export interface CreateCommentRequest {
  content: string;
  parentCommentId?: string;
}

export interface UpdateCommentRequest {
  content: string;
}

export interface AttachmentResponse {
  id: string;
  taskId: string;
  commentId?: string;
  fileName: string;
  mimeType: string;
  fileSize: number;
  storagePath: string;
  status: BackendAttachmentStatus;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}

export interface CreateAttachmentRequest {
  fileName: string;
  mimeType: string;
  fileSize: number;
  storagePath: string;
  commentId?: string;
}

export interface UpdateAttachmentRequest {
  status: BackendAttachmentStatus;
}

export interface ActivityResponse {
  id: string;
  taskId: string;
  actorId: string;
  actorName?: string;
  description: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateActivityRequest {
  description: string;
}

export interface ChecklistResponse {
  id: string;
  taskId: string;
  title: string;
  sortOrder: number;
  status: BackendActivityStatus;
  createdAt: string;
  updatedAt: string;
  totalItems: number;
  completedItems: number;
  completionPercentage: number;
  items: ChecklistItemResponse[];
}

export interface ChecklistItemResponse {
  id: string;
  checklistId: string;
  content: string;
  completed: boolean;
  sortOrder: number;
  status: BackendActivityStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateChecklistRequest {
  title: string;
}

export interface UpdateChecklistRequest {
  title?: string;
  sortOrder?: number;
}

export interface CreateChecklistItemRequest {
  content: string;
}

export interface UpdateChecklistItemRequest {
  content?: string;
  completed?: boolean;
  sortOrder?: number;
}

const BACKEND_STATUS_MAP: Record<BackendTaskStatus, TaskStatus> = {
  ACTIVE: 'todo',
  TODO: 'todo',
  IN_PROGRESS: 'in-progress',
  IN_REVIEW: 'in-review',
  BLOCKED: 'blocked',
  COMPLETED: 'completed',
  ARCHIVED: 'archived',
  CANCELLED: 'cancelled',
};

export const FRONTEND_STATUS_MAP: Record<TaskStatus, BackendTaskStatus> = {
  'todo': 'ACTIVE',
  'in-progress': 'IN_PROGRESS',
  'in-review': 'IN_REVIEW',
  'blocked': 'BLOCKED',
  'completed': 'COMPLETED',
  'archived': 'ARCHIVED',
  'cancelled': 'CANCELLED',
};

const BACKEND_PRIORITY_MAP: Record<BackendTaskPriority, TaskPriority> = {
  CRITICAL: 'urgent',
  HIGH: 'high',
  MEDIUM: 'medium',
  LOW: 'low',
};

const FRONTEND_PRIORITY_MAP: Record<TaskPriority, BackendTaskPriority> = {
  urgent: 'CRITICAL',
  high: 'HIGH',
  medium: 'MEDIUM',
  low: 'LOW',
};

export function mapTaskResponse(t: TaskResponse): Task {
  return {
    id: t.id,
    title: t.title,
    description: t.description ?? '',
    status: BACKEND_STATUS_MAP[t.status] ?? 'todo',
    priority: t.priority ? BACKEND_PRIORITY_MAP[t.priority] ?? 'medium' : 'medium',
    projectId: t.projectId,
    projectName: t.projectName ?? '',
    departmentName: t.departmentName ?? '',
    assigneeId: t.assigneeId,
    assigneeName: t.assigneeName,
    startDate: t.startDate ? formatInstant(t.startDate) : undefined,
    dueAt: t.dueAt,
    createdAt: formatInstant(t.createdAt),
    deadline: t.dueAt ? formatInstant(t.dueAt) : undefined,
    estimatedTime: undefined,
    actualTime: undefined,
    comments: [],
    attachments: [],
    activity: [],
    labels: [],
    tags: [],
  };
}

export function mapToCreateRequest(data: {
  title: string;
  description?: string;
  assigneeId?: string;
  priority?: TaskPriority;
  startDate?: string;
  dueAt?: string;
}): CreateTaskRequest {
  return {
    title: data.title,
    description: data.description,
    assigneeId: data.assigneeId,
    priority: data.priority ? FRONTEND_PRIORITY_MAP[data.priority] : undefined,
    startDate: data.startDate,
    dueAt: data.dueAt,
  };
}

export function mapToUpdateRequest(data: {
  title?: string;
  description?: string;
  status?: TaskStatus;
  assigneeId?: string;
  priority?: TaskPriority;
  startDate?: string;
  dueAt?: string;
}): UpdateTaskRequest {
  return {
    title: data.title,
    description: data.description,
    status: data.status ? FRONTEND_STATUS_MAP[data.status] : undefined,
    assigneeId: data.assigneeId,
    priority: data.priority ? FRONTEND_PRIORITY_MAP[data.priority] : undefined,
    startDate: data.startDate,
    dueAt: data.dueAt,
  };
}

export function mapCommentResponse(c: CommentResponse) {
  return {
    id: c.id,
    author: '',
    content: c.content,
    timestamp: formatInstant(c.createdAt),
    attachments: c.attachments.map(mapAttachmentResponse),
  };
}

export function mapAttachmentResponse(a: AttachmentResponse) {
  return {
    id: a.id,
    name: a.fileName,
    type: a.mimeType,
    size: a.fileSize,
    uploadedBy: a.createdBy,
    uploadedAt: formatInstant(a.createdAt),
    url: a.storagePath,
  };
}

export function mapActivityResponse(a: ActivityResponse) {
  return {
    id: a.id,
    type: 'updated' as const,
    actor: a.actorName ?? a.actorId,
    description: a.description,
    timestamp: formatInstant(a.createdAt),
  };
}

function formatInstant(instant: string): string {
  if (!instant) return '';
  try {
    const date = new Date(instant);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  } catch {
    return instant;
  }
}

export function parseDatetimeLocalToInstant(value: string): string | undefined {
  if (!value.trim()) return undefined;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return undefined;
  return date.toISOString();
}

export function isTaskOverdue(task: Pick<Task, 'dueAt' | 'status'>): boolean {
  if (!task.dueAt || task.status === 'completed' || task.status === 'archived' || task.status === 'cancelled') {
    return false;
  }
  return new Date(task.dueAt).getTime() < Date.now();
}

export const PRIORITY_OPTIONS: { value: TaskPriority; label: string }[] = [
  { value: 'low', label: 'Low' },
  { value: 'medium', label: 'Medium' },
  { value: 'high', label: 'High' },
  { value: 'urgent', label: 'Urgent' },
];

export interface Comment {
  id: string;
  author: string;
  content: string;
  timestamp: string;
  avatar?: string;
  reactions?: Record<string, number>;
  replies?: Comment[];
  edited?: boolean;
  pinned?: boolean;
}

export interface Attachment {
  id: string;
  name: string;
  type: string;
  size: number;
  uploadedBy: string;
  uploadedAt: string;
  url?: string;
  preview?: string;
}

export interface ActivityEvent {
  id: string;
  type: string;
  actor: string;
  description: string;
  timestamp: string;
  icon?: LucideIcon;
  tone?: 'accent' | 'success' | 'warning' | 'danger' | 'info' | 'neutral';
}

export interface Task {
  id: string;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  projectId: string;
  projectName: string;
  departmentName: string;
  assigneeId?: string;
  assigneeName?: string;
  startDate?: string;
  dueAt?: string;
  createdAt: string;
  deadline?: string;
  estimatedTime?: number;
  actualTime?: number;
  progress?: number;
  comments: Comment[];
  attachments: Attachment[];
  activity: ActivityEvent[];
  labels: string[];
  tags: string[];
}

export interface TaskFilters {
  search?: string;
  status?: TaskStatus;
  priority?: TaskPriority;
  assigneeId?: string;
}

export type TaskViewMode = 'kanban' | 'list' | 'table' | 'calendar' | 'timeline';
