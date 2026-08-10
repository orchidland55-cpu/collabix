import { useState } from 'react';
import {
  ArrowLeft,
  Mail,
  Calendar,
  Briefcase,
  Users,
  CheckCircle2,
  Clock,
  MoreHorizontal,
  Edit2,
  Share2,
  UserX,
  AlertCircle,
} from 'lucide-react';
import { Card, CardBody, CardHeader, CardTitle } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge, type Tone } from '../../components/ui/Badge';
import { Avatar } from '../../components/ui/Avatar';
import { IconButton } from '../../components/ui/IconButton';
import { Modal } from '../../components/ui/Modal';
import { EmptyState } from '../../components/ui/EmptyState';
import { Skeleton } from '../../components/ui/Skeleton';
import { Tabs, type TabItem } from '../../components/ui/Tabs';
import { Dropdown, type DropdownItem } from '../../components/ui/Dropdown';
import { cn } from '../../lib/cn';
import { useToast } from '../../components/ui/Toast';
import { useUserDetail, useActivateUser, useDeactivateUser, useDeleteUser } from '../../services/admin-hooks';
import type { UserResponse } from '../../types';
import { UserStatus } from '../../types';

interface MemberDetailsPageProps {
  memberId: string;
  onBack: () => void;
}

const statusTone: Record<string, Tone> = {
  ACTIVE: 'success',
  INACTIVE: 'warning',
  PENDING_ACTIVATION: 'info',
  SUSPENDED: 'danger',
  LOCKED: 'danger',
  ARCHIVED: 'warning',
  SOFT_DELETED: 'danger',
};

function formatStatus(status: string): string {
  return status
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());
}

function formatTitleCase(value: string): string {
  return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}

export function MemberDetailsPage({ memberId, onBack }: MemberDetailsPageProps) {
  const { toast } = useToast();
  const { data: user, isLoading, isError } = useUserDetail(memberId);
  const activateUser = useActivateUser();
  const deactivateUser = useDeactivateUser();
  const deleteUser = useDeleteUser();
  const [activeTab, setActiveTab] = useState('overview');
  const [confirmAction, setConfirmAction] = useState<'activate' | 'deactivate' | 'delete' | null>(null);

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <div className="flex items-start gap-4">
          <Skeleton className="h-9 w-9 rounded-lg" />
          <Skeleton className="h-12 w-12 rounded-full" />
          <div className="flex flex-col gap-2">
            <Skeleton className="h-7 w-56" />
            <Skeleton className="h-4 w-32" />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          {[1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-20 rounded-lg" />)}
        </div>
        <Skeleton className="h-96 rounded-xl" />
      </div>
    );
  }

  if (isError || !user) {
    return (
      <div className="flex flex-col gap-6">
        <button
          onClick={onBack}
          className="flex h-9 w-9 items-center justify-center rounded-lg border border-border-subtle text-text-secondary hover:bg-surface-2 hover:text-text-primary transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <EmptyState
          icon={<AlertCircle className="h-6 w-6" />}
          title="Member not found"
          description="This member could not be loaded. They may have been removed or you may not have access."
        />
      </div>
    );
  }

  const memberName = `${user.firstName} ${user.lastName}`.trim();
  const isActive = user.status === UserStatus.ACTIVE;

  const tabItems: TabItem[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'projects', label: 'Projects', count: 0 },
    { id: 'tasks', label: 'Tasks', count: 0 },
    { id: 'documents', label: 'Documents', count: 0 },
    { id: 'activity', label: 'Activity' },
    { id: 'performance', label: 'Performance' },
  ];

  const actionItems: DropdownItem[] = [
    { label: 'Edit Profile', icon: <Edit2 className="h-4 w-4" />, onClick: () => toast({ title: 'Coming soon', tone: 'info' }) },
    { label: 'Share Profile', icon: <Share2 className="h-4 w-4" />, onClick: () => toast({ title: 'Coming soon', tone: 'info' }) },
    { divider: true },
    {
      label: isActive ? 'Deactivate Member' : 'Reactivate Member',
      icon: <UserX className="h-4 w-4" />,
      danger: isActive,
      onClick: () => setConfirmAction(isActive ? 'deactivate' : 'activate'),
    },
    { label: 'Remove Member', icon: <UserX className="h-4 w-4" />, danger: true, onClick: () => setConfirmAction('delete') },
  ];

  const handleConfirmAction = async () => {
    if (!confirmAction) return;
    try {
      if (confirmAction === 'delete') {
        await deleteUser.mutateAsync(user.id);
        toast({ title: 'Member removed', tone: 'success' });
      } else if (confirmAction === 'deactivate') {
        await deactivateUser.mutateAsync(user.id);
        toast({ title: 'Member deactivated', tone: 'success' });
      } else {
        await activateUser.mutateAsync(user.id);
        toast({ title: 'Member activated', tone: 'success' });
      }
      onBack();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Action failed';
      toast({ title: msg, tone: 'danger' });
    }
    setConfirmAction(null);
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-4">
          <button
            onClick={onBack}
            className="flex h-9 w-9 items-center justify-center rounded-lg border border-border-subtle text-text-secondary hover:bg-surface-2 hover:text-text-primary transition-colors mt-1"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>

          <div className="flex flex-col gap-3">
            <div className="flex items-end gap-4">
              <Avatar name={memberName} size="lg" src={user.profilePicture} />
              <div>
                <h1 className="text-page font-semibold text-text-primary">{memberName}</h1>
                <p className="text-body text-text-secondary">{user.email}</p>
                <div className="flex flex-wrap items-center gap-2 mt-2">
                  <Badge tone="accent" variant="soft">{user.departmentName ?? 'No Department'}</Badge>
                  <Badge tone={statusTone[user.status] ?? 'neutral'} variant="soft" dot>
                    {formatStatus(user.status)}
                  </Badge>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <Button variant="outline" size="md" onClick={() => toast({ title: 'Coming soon', tone: 'info' })}>Share</Button>
          <Dropdown trigger={<IconButton label="Actions" variant="ghost"><MoreHorizontal /></IconButton>} items={actionItems} align="right" />
        </div>
      </div>

      {/* Quick Stats */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatCard icon={<Briefcase />} label="Department" value={user.departmentName ?? 'Not Assigned'} tone="accent" />
        <StatCard icon={<Users />} label="Team" value={user.teamName ?? 'Not Assigned'} tone="info" />
        <StatCard icon={<CheckCircle2 />} label="Role" value={formatTitleCase(user.role)} tone="info" />
        <StatCard icon={<Calendar />} label="Member Since" value={user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'} tone="success" />
      </div>

      {/* Tabs */}
      <Tabs items={tabItems} active={activeTab} onChange={setActiveTab} />

      {/* Tab Content */}
      {activeTab === 'overview' && <OverviewTab user={user} />}
      {activeTab === 'projects' && (
        <NotAvailableTab title="No projects yet" description="Assigned projects will appear here when this member is added to a project." />
      )}
      {activeTab === 'tasks' && (
        <NotAvailableTab title="No tasks yet" description="Assigned tasks will appear here once tasks are linked to this member." />
      )}
      {activeTab === 'documents' && (
        <NotAvailableTab title="No documents" description="Documents shared with this member will appear here when available." />
      )}
      {activeTab === 'activity' && (
        <NotAvailableTab title="No recent activity" description="Activity events for this member will appear here when available." />
      )}
      {activeTab === 'performance' && (
        <NotAvailableTab title="No performance data" description="Performance metrics will appear here when tracking is enabled for this member." />
      )}

      {confirmAction && (
        <Modal open={!!confirmAction} onClose={() => setConfirmAction(null)} title="Confirm Action" size="sm">
          <p className="text-body text-text-secondary">
            Are you sure you want to {confirmAction === 'delete' ? 'remove this member' : confirmAction === 'deactivate' ? 'deactivate this member' : 'activate this member'}?
          </p>
          <div className="flex items-center justify-end gap-3 mt-6 pt-5 border-t border-border-subtle">
            <Button variant="outline" onClick={() => setConfirmAction(null)}>Cancel</Button>
            <Button variant={confirmAction === 'delete' ? 'danger' : 'primary'} onClick={handleConfirmAction}>
              Confirm
            </Button>
          </div>
        </Modal>
      )}
    </div>
  );
}

function StatCard({
  icon,
  label,
  value,
  tone,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  tone: string;
}) {
  const bgColor: Record<string, string> = {
    accent: 'bg-accent-50 dark:bg-accent-100 text-accent-700 dark:text-accent-200',
    success: 'bg-success-50 dark:bg-success-100 text-success-700 dark:text-success-200',
    info: 'bg-info-50 dark:bg-info-100 text-info-700 dark:text-info-200',
    warning: 'bg-warning-50 dark:bg-warning-100 text-warning-700 dark:text-warning-200',
  };

  return (
    <div className={cn('rounded-lg border border-border-subtle p-3', bgColor[tone])}>
      <div className="flex items-center gap-2 mb-2">
        <span className="shrink-0 [&>svg]:h-4 [&>svg]:w-4">{icon}</span>
        <p className="text-2xs font-medium opacity-75">{label}</p>
      </div>
      <p className="text-section font-semibold">{value}</p>
    </div>
  );
}

function OverviewTab({ user }: { user: UserResponse }) {
  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
      <div className="lg:col-span-2 flex flex-col gap-6">
        {/* Contact information */}
        <Card>
          <CardHeader>
            <CardTitle>Contact Information</CardTitle>
          </CardHeader>
          <CardBody className="space-y-4">
            <InfoRow label="Email" value={user.email} icon={<Mail />} />
          </CardBody>
        </Card>

        {/* Professional information */}
        <Card>
          <CardHeader>
            <CardTitle>Professional Information</CardTitle>
          </CardHeader>
          <CardBody className="space-y-4">
            <InfoRow label="Department" value={user.departmentName ?? 'Not Assigned'} />
            <InfoRow label="Team" value={user.teamName ?? 'Not Assigned'} />
            <InfoRow label="Role" value={formatTitleCase(user.role)} />
            <InfoRow label="Member Type" value={formatTitleCase(user.memberType)} />
            <InfoRow label="Joined" value={user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'} icon={<Calendar />} />
          </CardBody>
        </Card>
      </div>

      {/* Sidebar */}
      <div className="flex flex-col gap-6">
        {/* Statistics */}
        <Card>
          <CardHeader>
            <CardTitle className="text-section">Statistics</CardTitle>
          </CardHeader>
          <CardBody className="space-y-3">
            <StatItem label="Role" value={formatTitleCase(user.role)} />
            <StatItem label="Member Type" value={formatTitleCase(user.memberType)} />
            <StatItem label="Status" value={formatStatus(user.status)} />
            <StatItem label="Last Login" value={user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : 'Never'} />
            <StatItem label="Created" value={user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'} />
          </CardBody>
        </Card>

        {/* Status */}
        <Card>
          <CardHeader>
            <CardTitle className="text-section">Status</CardTitle>
          </CardHeader>
          <CardBody className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-caption text-text-secondary">Current Status</span>
              <Badge tone={statusTone[user.status] ?? 'neutral'} variant="soft" dot>
                {formatStatus(user.status)}
              </Badge>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-caption text-text-secondary">Last Active</span>
              <span className="text-caption font-medium text-text-tertiary">
                {user.lastLoginAt ? <span className="flex items-center gap-1.5"><Clock className="h-3.5 w-3.5" /> {new Date(user.lastLoginAt).toLocaleDateString()}</span> : 'Never'}
              </span>
            </div>
          </CardBody>
        </Card>
      </div>
    </div>
  );
}

function InfoRow({ label, value, icon }: { label: string; value: string; icon?: React.ReactNode }) {
  return (
    <div className="flex items-center gap-3">
      {icon && <span className="shrink-0 text-text-tertiary [&>svg]:h-4 [&>svg]:w-4">{icon}</span>}
      <div>
        <p className="text-2xs text-text-tertiary">{label}</p>
        <p className="text-body font-medium text-text-primary">{value}</p>
      </div>
    </div>
  );
}

function StatItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-caption text-text-secondary">{label}</span>
      <span className="text-body font-semibold text-text-primary">{value}</span>
    </div>
  );
}

function NotAvailableTab({ title, description }: { title: string; description: string }) {
  return (
    <div className="py-10">
      <EmptyState icon={<Clock className="h-6 w-6" />} title={title} description={description} />
    </div>
  );
}