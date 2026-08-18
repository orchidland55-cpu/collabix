import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Link } from 'react-router-dom';
import { Bell, Check, Trash2, MoreHorizontal, ChevronDown, Loader2, ExternalLink } from 'lucide-react';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Badge } from '../../../components/ui/Badge';
import { IconButton } from '../../../components/ui/IconButton';
import { Dropdown, type DropdownItem } from '../../../components/ui/Dropdown';
import { EmptyState } from '../../../components/ui/EmptyState';
import { Tabs, type TabItem } from '../../../components/ui/Tabs';
import { cn } from '../../../lib/cn';
import {
  useAlertsList,
  useAlertMarkAsRead,
  useAlertMarkAllAsRead,
  useAlertDismiss,
  useAlertUnreadCount,
} from '../../../services/alert-hooks';
import type { AlertResponse, AlertSearchCriteria, AlertSeverity } from '../../../services/alert-service';
import {
  formatRelativeTime,
  getAlertSeverityTone,
  alertTypeLabel,
  alertTypeIcon,
  alertResourceHref,
} from './alert-types';

export function AlertsPage() {
  const [searchParams] = useSearchParams();
  const wsId = searchParams.get('ws') ?? '';
  const [search, setSearch] = useState('');
  const [criteria, setCriteria] = useState<AlertSearchCriteria>({});
  const [activeTab, setActiveTab] = useState('all');

  const { data: alertsData, isLoading, isError } = useAlertsList(wsId, criteria);
  const { data: unreadCountData } = useAlertUnreadCount(wsId);
  const markAsRead = useAlertMarkAsRead(wsId);
  const markAllAsRead = useAlertMarkAllAsRead(wsId);
  const dismiss = useAlertDismiss(wsId);

  const alerts = useMemo(() => (alertsData?.content ?? []) as AlertResponse[], [alertsData]);

  const filteredAlerts = useMemo(() => {
    if (!search) return alerts;
    const q = search.toLowerCase();
    return alerts.filter(
      (a) =>
        a.title.toLowerCase().includes(q) ||
        (a.message ?? '').toLowerCase().includes(q) ||
        (a.resourceType ?? '').toLowerCase().includes(q),
    );
  }, [alerts, search]);

  const unreadCount = unreadCountData ?? 0;
  const readCount = alerts.filter((a) => a.status === 'READ').length;
  const criticalCount = alerts.filter((a) => a.severity === 'CRITICAL').length;
  const totalCount = alerts.length;

  const stats = { total: totalCount, unread: unreadCount, read: readCount, critical: criticalCount };

  const tabItems: TabItem[] = [
    { id: 'all', label: 'All', count: stats.total },
    { id: 'unread', label: 'Unread', count: stats.unread },
    { id: 'read', label: 'Read', count: stats.read },
  ];

  const severityOptions: Array<{ value: AlertSeverity | undefined; label: string }> = [
    { value: undefined, label: 'All severities' },
    { value: 'CRITICAL', label: 'Critical' },
    { value: 'WARNING', label: 'Warning' },
    { value: 'INFO', label: 'Info' },
  ];

  const handleTabChange = (id: string) => {
    setActiveTab(id);
    if (id === 'unread') setCriteria((c) => ({ ...c, status: 'UNREAD' }));
    else if (id === 'read') setCriteria((c) => ({ ...c, status: 'READ' }));
    else setCriteria((c) => ({ ...c, status: undefined }));
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-text-tertiary" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-3">
        <p className="text-body font-medium text-danger-600">Failed to load alerts</p>
        <p className="text-caption text-text-tertiary">Please try again later.</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-page font-semibold text-text-primary">Alerts</h1>
        <p className="text-body text-text-secondary">
          Events that require your attention across tasks, documents, and AI services.
        </p>
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatCard label="Total" value={stats.total} tone="accent" />
        <StatCard label="Unread" value={stats.unread} tone="warning" />
        <StatCard label="Read" value={stats.read} tone="success" />
        <StatCard label="Critical" value={stats.critical} tone="danger" />
      </div>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-1 flex-col gap-2 sm:flex-row sm:gap-2">
          <div className="flex-1">
            <Input
              placeholder="Search alerts..."
              leftIcon={<Bell />}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              containerClassName="w-full"
            />
          </div>

          <Dropdown
            trigger={
              <Button variant="outline">
                Severity
                <ChevronDown className="h-3.5 w-3.5" />
              </Button>
            }
            items={severityOptions.map((opt) => ({
              label: opt.label,
              onClick: () => setCriteria((c) => ({ ...c, severity: opt.value })),
            }))}
          />
        </div>

        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => markAllAsRead.mutate()}>
            Mark All Read
          </Button>
        </div>
      </div>

      <Tabs items={tabItems} activeTab={activeTab} onChange={handleTabChange} />

      {filteredAlerts.length === 0 ? (
        <EmptyState
          icon={<Bell />}
          title="No alerts"
          description="You have no alerts right now. You're all clear!"
        />
      ) : (
        <div className="space-y-2">
          {filteredAlerts.map((alert) => (
            <AlertItem
              key={alert.id}
              alert={alert}
              onMarkRead={() => markAsRead.mutate(alert.id)}
              onDismiss={() => dismiss.mutate(alert.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function StatCard({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone: string;
}) {
  const bgColor: Record<string, string> = {
    accent: 'bg-accent-50 dark:bg-accent-100 text-accent-700 dark:text-accent-200',
    success: 'bg-success-50 dark:bg-success-100 text-success-700 dark:text-success-200',
    warning: 'bg-warning-50 dark:bg-warning-100 text-warning-700 dark:text-warning-200',
    danger: 'bg-danger-50 dark:bg-danger-100 text-danger-700 dark:text-danger-200',
  };

  return (
    <div className={cn('rounded-lg border border-border-subtle p-3', bgColor[tone])}>
      <p className="text-2xs font-medium opacity-75">{label}</p>
      <p className="text-section font-semibold mt-1">{value}</p>
    </div>
  );
}

function AlertItem({
  alert,
  onMarkRead,
  onDismiss,
}: {
  alert: AlertResponse;
  onMarkRead: () => void;
  onDismiss: () => void;
}) {
  const severityTone = getAlertSeverityTone(alert.severity);

  const actionItems: DropdownItem[] = [
    { label: 'Mark as read', icon: <Check className="h-4 w-4" />, onClick: onMarkRead },
    { divider: true },
    { label: 'Dismiss', icon: <Trash2 className="h-4 w-4" />, danger: true, onClick: onDismiss },
  ];

  const isUnread = alert.status === 'UNREAD';
  const href = alertResourceHref(alert);

  return (
    <div
      className={cn(
        'flex items-start gap-3 p-4 rounded-lg border transition-all',
        isUnread
          ? 'border-warning-200 dark:border-warning-800 bg-warning-50 dark:bg-warning-900'
          : 'border-border-subtle bg-surface hover:bg-surface-2',
      )}
    >
      <div className="text-2xl shrink-0">{alertTypeIcon(alert.type)}</div>

      <div className="flex-1 min-w-0">
        <div className="flex items-start gap-2 mb-1">
          <h4 className="text-body font-semibold text-text-primary flex-1">{alert.title}</h4>
          {isUnread && <div className="h-2 w-2 rounded-full bg-warning-500 shrink-0 mt-1.5" />}
        </div>
        {alert.message && (
          <p className="text-caption text-text-secondary mb-2">{alert.message}</p>
        )}
        <div className="flex flex-wrap gap-2 items-center">
          <Badge tone={severityTone} variant="soft">{alert.severity}</Badge>
          <Badge tone="neutral" variant="soft">{alertTypeLabel(alert.type)}</Badge>
          {alert.resourceType && (
            <Link
              to={href}
              className="inline-flex items-center gap-1 text-2xs font-medium text-accent-600 hover:text-accent-700"
            >
              <ExternalLink className="h-3 w-3" />
              {alert.resourceType.toLowerCase()}
            </Link>
          )}
          <span className="text-2xs text-text-tertiary ml-auto">
            {formatRelativeTime(alert.createdAt)}
          </span>
        </div>
      </div>

      <Dropdown
        trigger={
          <IconButton label="Actions" variant="ghost">
            <MoreHorizontal className="h-4 w-4" />
          </IconButton>
        }
        items={actionItems}
        align="right"
      />
    </div>
  );
}