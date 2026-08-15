import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  ArrowLeft, FolderKanban, Calendar, Clock, Users, AlertCircle, Edit2, Archive, Activity, Settings, Info, ShieldBan,
} from 'lucide-react';
import { Card, CardBody } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Tabs, type TabItem } from '../../components/ui/Tabs';
import { Skeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import { IconButton } from '../../components/ui/IconButton';
import { Dropdown, type DropdownItem } from '../../components/ui/Dropdown';
import { useProjectDetail, useProjectAccess, useProjectDepartmentContext, getProjectQueryErrorState } from '../../services/project-hooks';
import { EditProjectModal } from './modals/EditProjectModal';
import { ArchiveProjectModal } from './modals/ArchiveProjectModal';
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

export function ProjectDetailsPage({ projectId, onBack }: ProjectDetailsPageProps) {
  const [searchParams] = useSearchParams();
  const urlWsId = searchParams.get('ws') ?? '';
  const urlDeptId = searchParams.get('dept') ?? '';
  const [activeTab, setActiveTab] = useState('overview');
  const [showEdit, setShowEdit] = useState(false);
  const [showArchive, setShowArchive] = useState(false);

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

  const { canUpdate, canArchive } = useProjectAccess(wsId || undefined);

  const tabItems: TabItem[] = [
    { id: 'overview', label: 'Overview', icon: <Info /> },
    { id: 'activity', label: 'Activity', icon: <Activity /> },
    { id: 'settings', label: 'Settings', icon: <Settings /> },
  ];

  if (contextLoading) {
    return (
      <div className="flex flex-col gap-6 animate-fade-in">
        <Skeleton className="h-7 w-48" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    );
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
    return (
      <div className="flex flex-col gap-6 animate-fade-in">
        <div className="flex items-center gap-3">
          <Skeleton className="h-9 w-9 rounded-lg" />
          <div>
            <Skeleton className="h-6 w-48" />
            <Skeleton className="h-4 w-64 mt-1" />
          </div>
        </div>
        <Skeleton className="h-10 w-full rounded-lg" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    );
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

  const actionItems: DropdownItem[] = [
    ...(canUpdate ? [{ label: 'Edit', icon: <Edit2 className="h-4 w-4" />, onClick: () => setShowEdit(true) }] : []),
    ...(canUpdate && canArchive ? [{ divider: true }] : []),
    ...(canArchive ? [{ label: 'Archive', icon: <Archive className="h-4 w-4" />, danger: true, onClick: () => setShowArchive(true) }] : []),
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-4 flex-1 min-w-0">
          <button onClick={onBack} className="flex h-9 w-9 items-center justify-center rounded-lg border border-border-subtle text-text-secondary hover:bg-surface-2 hover:text-text-primary transition-colors mt-1 shrink-0">
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div className="flex-1 min-w-0">
            <div className="flex items-end gap-3 mb-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg shrink-0" style={{ backgroundColor: project.color ?? '#e5e7eb' }}>
                <FolderKanban className="h-4 w-4 text-white" />
              </div>
              <h1 className="text-page font-semibold text-text-primary truncate">{project.name}</h1>
              <Badge tone={statusColors[project.status]} variant="soft" dot>
                {project.status === 'ACTIVE' ? 'Active' : 'Archived'}
              </Badge>
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
          <Dropdown trigger={<IconButton label="Actions" variant="ghost"><Edit2 className="h-4 w-4" /></IconButton>} items={actionItems} align="right" />
        )}
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <InfoBox icon={<Users />} label="Manager" value={project.managerName ?? 'Unassigned'} />
        <InfoBox icon={<Calendar />} label="Start Date" value={project.startDate ?? 'Not set'} />
        <InfoBox icon={<Clock />} label="End Date" value={project.endDate ?? 'Not set'} />
        <InfoBox icon={<FolderKanban />} label="Department" value={project.departmentName ?? 'N/A'} />
      </div>

      <Tabs items={tabItems} active={activeTab} onChange={setActiveTab} />

      {activeTab === 'overview' && <OverviewTab project={project} />}
      {activeTab === 'activity' && <ActivityTab />}
      {activeTab === 'settings' && <SettingsTab project={project} canEdit={canUpdate} canArchive={canArchive} onEdit={() => setShowEdit(true)} onArchive={() => setShowArchive(true)} />}

      {showEdit && wsId && deptId && (
        <EditProjectModal open={showEdit} onClose={() => setShowEdit(false)} wsId={wsId} deptId={deptId} project={project} />
      )}
      {showArchive && wsId && deptId && (
        <ArchiveProjectModal open={showArchive} onClose={() => setShowArchive(false)} wsId={wsId} deptId={deptId} projectId={project.id} projectName={project.name} />
      )}
    </div>
  );
}

function InfoBox({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="rounded-lg border border-border-subtle bg-surface p-3">
      <div className="flex items-start gap-2 mb-2">
        <span className="shrink-0 text-text-tertiary [&>svg]:h-4 [&>svg]:w-4">{icon}</span>
        <p className="text-2xs text-text-tertiary font-medium">{label}</p>
      </div>
      <p className="text-section font-semibold text-text-primary capitalize">{value}</p>
    </div>
  );
}

function OverviewTab({ project }: { project: ProjectResponse }) {
  return (
    <Card>
      <CardBody className="space-y-4">
        <InfoRow label="Project Name" value={project.name} />
        <InfoRow label="Description" value={project.description ?? 'No description provided.'} />
        <InfoRow label="Status" value={project.status === 'ACTIVE' ? 'Active' : 'Archived'} />
        {project.priority && <InfoRow label="Priority" value={project.priority.toLowerCase()} />}
        {project.startDate && <InfoRow label="Start Date" value={project.startDate} />}
        {project.endDate && <InfoRow label="End Date" value={project.endDate} />}
        {project.managerName && <InfoRow label="Manager" value={project.managerName} />}
        {project.departmentName && <InfoRow label="Department" value={project.departmentName} />}
        {project.createdAt && <InfoRow label="Created" value={new Date(project.createdAt).toLocaleDateString()} />}
        {project.updatedAt && <InfoRow label="Updated" value={new Date(project.updatedAt).toLocaleDateString()} />}
      </CardBody>
    </Card>
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

function ActivityTab() {
  return (
    <Card>
      <CardBody className="py-12 text-center">
        <Activity className="h-10 w-10 text-text-tertiary mx-auto mb-3" />
        <p className="text-body font-medium text-text-primary">No recent activity</p>
        <p className="text-caption text-text-tertiary mt-1">Project activity tracking coming in the Tasks sprint.</p>
      </CardBody>
    </Card>
  );
}

function SettingsTab({ project, canEdit, canArchive, onEdit, onArchive }: { project: ProjectResponse; canEdit: boolean; canArchive: boolean; onEdit: () => void; onArchive: () => void }) {
  return (
    <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
      <Card>
        <CardBody className="space-y-4">
          <SettingRow label="Project Name" value={project.name} />
          <SettingRow label="Department" value={project.departmentName ?? 'N/A'} />
          <SettingRow label="Manager" value={project.managerName ?? 'Unassigned'} />
          <SettingRow label="Status" value={project.status === 'ACTIVE' ? 'Active' : 'Archived'} />
          {project.priority && <SettingRow label="Priority" value={project.priority.toLowerCase()} />}
          {canEdit && (
            <Button variant="outline" fullWidth onClick={onEdit}>Edit Information</Button>
          )}
        </CardBody>
      </Card>
      {project.status === 'ACTIVE' && canArchive && (
        <Card className="border-danger-200 dark:border-danger-100">
          <CardBody className="space-y-3">
            <p className="text-section font-semibold text-danger-600 dark:text-danger-400">Danger Zone</p>
            <p className="text-caption text-text-secondary">Archive this project to remove it from active projects.</p>
            <Button variant="danger" fullWidth onClick={onArchive}>Archive Project</Button>
          </CardBody>
        </Card>
      )}
    </div>
  );
}

function SettingRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="pb-3 border-b border-border-subtle last:pb-0 last:border-b-0">
      <p className="text-2xs text-text-tertiary mb-1">{label}</p>
      <p className="text-body font-medium text-text-primary capitalize">{value}</p>
    </div>
  );
}
