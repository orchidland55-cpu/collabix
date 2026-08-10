import { apiClient } from '../lib/api';
import type { PageResponse } from '../types/api';

/* ---------- DTOs (mirroring backend) ---------- */

export type HandoverPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type HandoverShift = 'MORNING' | 'EVENING';
export type HandoverStatus = 'DRAFT' | 'SUBMITTED' | 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'COMPLETED' | 'ARCHIVED';

export interface UserSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface HandoverEntryResponse {
  id: string;
  workspaceId: string;
  departmentId: string;
  projectId: string;
  taskId?: string;
  sender: UserSummary;
  receiver?: UserSummary;
  title?: string;
  content?: string;
  priority: HandoverPriority;
  status: HandoverStatus;
  dueDate?: string;
  shift?: HandoverShift;
  entryDate?: string;
  completedTasks?: string;
  currentProgress?: string;
  pendingTasks?: string;
  blockers?: string;
  importantNotes?: string;
  estimatedRemainingWork?: string;
  mood?: string;
  submittedAt?: string;
  sentAt?: string;
  acceptedAt?: string;
  rejectedAt?: string;
  completedAt?: string;
  archivedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateHandoverEntryRequest {
  departmentId: string;
  projectId: string;
  taskId?: string;
  receiverId?: string;
  title?: string;
  content?: string;
  priority?: HandoverPriority;
  dueDate?: string;
  shift?: HandoverShift;
  entryDate?: string;
  completedTasks?: string;
  currentProgress?: string;
  pendingTasks?: string;
  blockers?: string;
  importantNotes?: string;
  estimatedRemainingWork?: string;
  mood?: string;
}

export interface UpdateHandoverEntryRequest {
  taskId?: string;
  receiverId?: string;
  title?: string;
  content?: string;
  priority?: HandoverPriority;
  dueDate?: string;
  shift?: HandoverShift;
  entryDate?: string;
  completedTasks?: string;
  currentProgress?: string;
  pendingTasks?: string;
  blockers?: string;
  importantNotes?: string;
  estimatedRemainingWork?: string;
  mood?: string;
}

export interface HandoverStatusUpdateRequest {
  reason?: string;
}

export interface HandoverCommentResponse {
  id: string;
  handoverEntryId: string;
  author: UserSummary;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateHandoverCommentRequest {
  content: string;
}

export interface HandoverAttachmentResponse {
  id: string;
  handoverEntryId: string;
  fileName: string;
  fileSize?: number;
  contentType?: string;
  storageKey: string;
  uploadedBy: UserSummary;
  createdAt: string;
}

export interface CreateHandoverAttachmentRequest {
  fileName: string;
  fileSize?: number;
  contentType?: string;
  storageKey: string;
}

export type TimelineEventType =
  | 'CREATED'
  | 'UPDATED'
  | 'SENT'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'COMPLETED'
  | 'ARCHIVED'
  | 'COMMENTED'
  | 'ATTACHMENT_ADDED'
  | 'ATTACHMENT_REMOVED'
  | 'REMINDER_SENT';

export interface HandoverTimelineEventResponse {
  id: string;
  handoverEntryId: string;
  eventType: TimelineEventType;
  description?: string;
  actorId?: string;
  occurredAt: string;
}

export interface HandoverJournalResponse {
  id: string;
  workspaceId: string;
  departmentId: string;
  projectId: string;
  journalDate: string;
  shift?: HandoverShift;
  journalVersion?: number;
  generatedBy?: string;
  departmentsIncluded?: string;
  entriesCount?: number;
  generatedSummary: string;
  mainDoneWork: string;
  mainRemainingWork: string;
  blockers: string;
  difficulties: string;
  recommendations: string;
  totalHandovers?: number;
  pendingHandovers?: number;
  completedHandovers?: number;
  rejectedHandovers?: number;
  urgentHandovers?: number;
  overdueHandovers?: number;
  generationStatus: 'PENDING' | 'GENERATED' | 'FAILED';
  generationDate?: string;
  generationProcessedBy?: string;
  status: 'ACTIVE' | 'ARCHIVED' | 'DELETED';
  createdAt: string;
  updatedAt: string;
}

export interface HandoverAIGenerateRequest {
  workspaceId: string;
  departmentId: string;
  projectId: string;
  date?: string;
  shift?: HandoverShift;
}

export interface HandoverAIEditRequest {
  workspaceId: string;
  departmentId: string;
  projectId: string;
  executiveSummary?: string;
  completedWork?: string;
  pendingWork?: string;
  criticalRisks?: string;
  blockedTasks?: string;
  recommendations?: string;
  priorityActions?: string;
  workContinuity?: string;
}

export interface HandoverAIResponse {
  journalId: string;
  workspaceId: string;
  departmentId: string;
  projectId: string;
  journalDate: string;
  shift?: HandoverShift;
  journalVersion?: number;
  departmentsIncluded?: string;
  entriesCount?: number;
  executiveSummary: string;
  completedWork: string;
  pendingWork: string;
  criticalRisks: string;
  blockedTasks: string;
  recommendations: string;
  priorityActions: string;
  workContinuity: string;
  generationStatus: 'PENDING' | 'GENERATED' | 'FAILED';
  generationDate: string;
  generatedBy: string;
  executionTime: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * Handover entry REST API.
 *
 * The backend exposes handovers at the WORKSPACE level:
 *   `/api/workspaces/{workspaceId}/handovers`
 * with sub-resources for inbox, sent, comments, attachments and timeline.
 */
export function handoverEntryService(workspaceId: string) {
  const base = `/workspaces/${workspaceId}/handovers`;

  return {
    list: (
      params?: { status?: string; priority?: string; projectId?: string; page?: number; size?: number },
    ) => {
      const q = new URLSearchParams();
      if (params?.status) q.set('status', params.status);
      if (params?.priority) q.set('priority', params.priority);
      if (params?.projectId) q.set('projectId', params.projectId);
      if (params?.page != null) q.set('page', String(params.page));
      if (params?.size != null) q.set('size', String(params.size));
      const qs = q.toString();
      return apiClient.get<PageResponse<HandoverEntryResponse>>(`${base}${qs ? `?${qs}` : ''}`);
    },

    inbox: (page?: number, size?: number) => {
      const q = new URLSearchParams();
      if (page != null) q.set('page', String(page));
      if (size != null) q.set('size', String(size));
      const qs = q.toString();
      return apiClient.get<PageResponse<HandoverEntryResponse>>(`${base}/inbox${qs ? `?${qs}` : ''}`);
    },

    sent: (page?: number, size?: number) => {
      const q = new URLSearchParams();
      if (page != null) q.set('page', String(page));
      if (size != null) q.set('size', String(size));
      const qs = q.toString();
      return apiClient.get<PageResponse<HandoverEntryResponse>>(`${base}/sent${qs ? `?${qs}` : ''}`);
    },

    myEntries: (params?: {
      status?: string;
      shift?: string;
      entryDate?: string;
      search?: string;
      page?: number;
      size?: number;
    }) => {
      const q = new URLSearchParams();
      if (params?.status) q.set('status', params.status);
      if (params?.shift) q.set('shift', params.shift);
      if (params?.entryDate) q.set('entryDate', params.entryDate);
      if (params?.search) q.set('search', params.search);
      if (params?.page != null) q.set('page', String(params.page));
      if (params?.size != null) q.set('size', String(params.size));
      const qs = q.toString();
      return apiClient.get<PageResponse<HandoverEntryResponse>>(`${base}/my-entries${qs ? `?${qs}` : ''}`);
    },

    getById: (entryId: string) =>
      apiClient.get<HandoverEntryResponse>(`${base}/${entryId}`),

    create: (data: CreateHandoverEntryRequest) =>
      apiClient.post<HandoverEntryResponse>(base, data),

    update: (entryId: string, data: UpdateHandoverEntryRequest) =>
      apiClient.put<HandoverEntryResponse>(`${base}/${entryId}`, data),

    delete: (entryId: string) =>
      apiClient.delete<void>(`${base}/${entryId}`),

    send: (entryId: string, data?: HandoverStatusUpdateRequest) =>
      apiClient.post<HandoverEntryResponse>(`${base}/${entryId}/send`, data),

    submit: (entryId: string, data?: HandoverStatusUpdateRequest) =>
      apiClient.post<HandoverEntryResponse>(`${base}/${entryId}/submit`, data),

    accept: (entryId: string, data?: HandoverStatusUpdateRequest) =>
      apiClient.post<HandoverEntryResponse>(`${base}/${entryId}/accept`, data),

    reject: (entryId: string, data?: HandoverStatusUpdateRequest) =>
      apiClient.post<HandoverEntryResponse>(`${base}/${entryId}/reject`, data),

    complete: (entryId: string, data?: HandoverStatusUpdateRequest) =>
      apiClient.post<HandoverEntryResponse>(`${base}/${entryId}/complete`, data),

    archive: (entryId: string, data?: HandoverStatusUpdateRequest) =>
      apiClient.post<HandoverEntryResponse>(`${base}/${entryId}/archive`, data),

    comments: (entryId: string) =>
      apiClient.get<HandoverCommentResponse[]>(`${base}/${entryId}/comments`),

    addComment: (entryId: string, data: CreateHandoverCommentRequest) =>
      apiClient.post<HandoverCommentResponse>(`${base}/${entryId}/comments`, data),

    updateComment: (entryId: string, commentId: string, data: CreateHandoverCommentRequest) =>
      apiClient.put<HandoverCommentResponse>(`${base}/${entryId}/comments/${commentId}`, data),

    deleteComment: (entryId: string, commentId: string) =>
      apiClient.delete<void>(`${base}/${entryId}/comments/${commentId}`),

    attachments: (entryId: string) =>
      apiClient.get<HandoverAttachmentResponse[]>(`${base}/${entryId}/attachments`),

    addAttachment: (entryId: string, data: CreateHandoverAttachmentRequest) =>
      apiClient.post<HandoverAttachmentResponse>(`${base}/${entryId}/attachments`, data),

    deleteAttachment: (entryId: string, attachmentId: string) =>
      apiClient.delete<void>(`${base}/${entryId}/attachments/${attachmentId}`),

    timeline: (entryId: string) =>
      apiClient.get<HandoverTimelineEventResponse[]>(`${base}/${entryId}/timeline`),
  };
}

/**
 * Handover journal REST API (nested under project).
 * @deprecated Use {@link handoverJournalAccessService} (department-scoped) for reads.
 */
export function handoverJournalService(workspaceId: string, departmentId: string, projectId: string) {
  const base = `/workspaces/${workspaceId}/departments/${departmentId}/projects/${projectId}/handover-logs`;

  return {
    list: (page?: number, size?: number) => {
      const params: Record<string, unknown> = {};
      if (page != null) params.page = page;
      if (size != null) params.size = size;
      return apiClient.get<PageResponse<HandoverJournalResponse>>(base, { params });
    },

    getById: (journalId: string) =>
      apiClient.get<HandoverJournalResponse>(`${base}/${journalId}`),

    generate: () =>
      apiClient.post<HandoverJournalResponse>(`${base}/generate`),

    regenerate: (journalId: string) =>
      apiClient.put<HandoverJournalResponse>(`${base}/${journalId}/regenerate`),

    delete: (journalId: string) =>
      apiClient.delete<void>(`${base}/${journalId}`),
  };
}

/**
 * Department-scoped handover journal service available to every workspace role.
 * The backend auto-scopes managers/members to their own department and returns 403
 * on cross-department reads.
 */
export function handoverJournalAccessService(workspaceId: string) {
  const base = `/workspaces/${workspaceId}/handover-journals`;

  return {
    list: (params?: {
      departmentId?: string;
      projectId?: string;
      shift?: string;
      date?: string;
      page?: number;
      size?: number;
    }) => {
      const q = new URLSearchParams();
      if (params?.departmentId) q.set('departmentId', params.departmentId);
      if (params?.projectId) q.set('projectId', params.projectId);
      if (params?.shift) q.set('shift', params.shift);
      if (params?.date) q.set('date', params.date);
      if (params?.page != null) q.set('page', String(params.page));
      if (params?.size != null) q.set('size', String(params.size));
      const qs = q.toString();
      return apiClient.get<PageResponse<HandoverJournalResponse>>(`${base}${qs ? `?${qs}` : ''}`);
    },

    getById: (journalId: string) =>
      apiClient.get<HandoverJournalResponse>(`${base}/${journalId}`),
  };
}

/**
 * Handover AI REST API.
 */
export function handoverAIService() {
  const base = '/handover/ai';

  return {
    generate: (data: HandoverAIGenerateRequest) =>
      apiClient.post<HandoverAIResponse>(`${base}/generate`, data),

    regenerate: (journalId: string, data: HandoverAIGenerateRequest) =>
      apiClient.post<HandoverAIResponse>(`${base}/regenerate/${journalId}`, data),

    edit: (journalId: string, data: HandoverAIEditRequest) =>
      apiClient.put<HandoverAIResponse>(`${base}/${journalId}`, data),

    approve: (journalId: string, data: HandoverAIGenerateRequest) =>
      apiClient.post<HandoverAIResponse>(`${base}/${journalId}/approve`, data),

    reject: (journalId: string, data: HandoverAIGenerateRequest) =>
      apiClient.post<HandoverAIResponse>(`${base}/${journalId}/reject`, data),
  };
}
