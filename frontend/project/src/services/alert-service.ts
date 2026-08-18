import { apiClient } from '../lib/api';
import type { PageResponse } from '../types/api';

export type AlertType =
  | 'TASK_DEADLINE_APPROACHING'
  | 'TASK_OVERDUE'
  | 'TASK_BLOCKED'
  | 'DOCUMENT_UPLOAD_FAILED'
  | 'AI_GENERATION_FAILED'
  | 'AI_GENERATION_REQUIRES_ATTENTION'
  | 'HANDOVER_GENERATION_FAILED'
  | 'PERMISSION_DENIED'
  | 'SYSTEM_ERROR';

export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

export type AlertStatus = 'UNREAD' | 'READ' | 'ARCHIVED';

export interface AlertResponse {
  id: string;
  workspaceId: string;
  recipientId: string;
  departmentId?: string;
  type: AlertType;
  severity: AlertSeverity;
  status: AlertStatus;
  title: string;
  message?: string;
  resourceType?: string;
  resourceId?: string;
  readAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AlertSearchCriteria {
  status?: AlertStatus;
  type?: AlertType;
  severity?: AlertSeverity;
}

function base(wsId: string) {
  return `/workspaces/${wsId}/alerts`;
}

export const alertService = {
  list: (wsId: string, criteria?: AlertSearchCriteria) =>
    apiClient.get<PageResponse<AlertResponse>>(`${base(wsId)}`, {
      params: {
        ...(criteria?.status ? { status: criteria.status } : {}),
        ...(criteria?.type ? { type: criteria.type } : {}),
        ...(criteria?.severity ? { severity: criteria.severity } : {}),
      },
    }),

  getById: (wsId: string, alertId: string) =>
    apiClient.get<AlertResponse>(`${base(wsId)}/${alertId}`),

  unreadCount: (wsId: string) =>
    apiClient.get<number>(`${base(wsId)}/unread/count`),

  markAsRead: (wsId: string, alertId: string) =>
    apiClient.put<AlertResponse>(`${base(wsId)}/${alertId}/read`),

  markAllAsRead: (wsId: string) =>
    apiClient.put<void>(`${base(wsId)}/read-all`),

  dismiss: (wsId: string, alertId: string) =>
    apiClient.delete<void>(`${base(wsId)}/${alertId}`),
};