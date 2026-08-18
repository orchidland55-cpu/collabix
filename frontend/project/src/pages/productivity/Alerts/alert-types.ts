import type { AlertResponse, AlertSeverity, AlertType } from '../../../services/alert-service';

export function formatRelativeTime(dateStr: string): string {
  if (!dateStr) return '';
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  return new Date(dateStr).toLocaleDateString();
}

export function getAlertSeverityTone(severity: AlertSeverity): 'info' | 'warning' | 'danger' {
  switch (severity) {
    case 'CRITICAL':
      return 'danger';
    case 'WARNING':
      return 'warning';
    case 'INFO':
      return 'info';
  }
}

export function alertTypeLabel(type: AlertType): string {
  const labels: Record<AlertType, string> = {
    TASK_DEADLINE_APPROACHING: 'Deadline approaching',
    TASK_OVERDUE: 'Overdue task',
    TASK_BLOCKED: 'Blocked task',
    DOCUMENT_UPLOAD_FAILED: 'Document upload failed',
    AI_GENERATION_FAILED: 'AI generation failed',
    AI_GENERATION_REQUIRES_ATTENTION: 'AI needs attention',
    HANDOVER_GENERATION_FAILED: 'Handover generation failed',
    PERMISSION_DENIED: 'Permission denied',
    SYSTEM_ERROR: 'System error',
  };
  return labels[type] ?? type.replace(/_/g, ' ');
}

export function alertTypeIcon(type: AlertType): string {
  const icons: Record<AlertType, string> = {
    TASK_DEADLINE_APPROACHING: '⏰',
    TASK_OVERDUE: '🚨',
    TASK_BLOCKED: '⛔',
    DOCUMENT_UPLOAD_FAILED: '📄',
    AI_GENERATION_FAILED: '🤖',
    AI_GENERATION_REQUIRES_ATTENTION: '👀',
    HANDOVER_GENERATION_FAILED: '🔁',
    PERMISSION_DENIED: '🔒',
    SYSTEM_ERROR: '⚠️',
  };
  return icons[type] ?? '🔔';
}

export function alertResourceHref(alert: AlertResponse): string {
  const ws = alert.workspaceId;
  switch (alert.resourceType) {
    case 'TASK':
      return `/app/tasks/${alert.resourceId}?ws=${ws}`;
    case 'PROJECT':
      return `/app/projects?ws=${ws}`;
    case 'DOCUMENT':
      return `/app/documents?ws=${ws}`;
    case 'REPORT':
    case 'EXECUTIVE_REPORT':
    case 'ANALYTICS_REPORT':
      return `/app/reports?ws=${ws}`;
    case 'HANDOVER':
      return `/app/handovers?ws=${ws}`;
    default:
      return `/app/alerts?ws=${ws}`;
  }
}