import { useEffect, useState } from 'react';
import { Search, Plus, FolderKanban, AlertCircle, Briefcase, Network, ShieldBan } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Card, CardBody } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Select } from '../../components/ui/Select';
import { Badge } from '../../components/ui/Badge';
import { Pagination } from '../../components/ui/Pagination';
import { EmptyState } from '../../components/ui/EmptyState';
import { Skeleton } from '../../components/ui/Skeleton';
import { useWorkspacesList } from '../../services/workspace-hooks';
import { useDepartmentList } from '../../services/department-hooks';
import {
  useProjectList,
  useProjectAccess,
  useProjectDepartmentContext,
  getProjectQueryErrorState,
  getProjectEmptyDescription,
} from '../../services/project-hooks';
import { useAuth } from '../../lib/auth-context';
import { isMember } from '../../lib/access';
import { CreateProjectModal } from './modals/CreateProjectModal';
import type { ProjectResponse, ProjectPriority } from './projects-types';

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

export function ProjectsPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const wsId = searchParams.get('ws') ?? '';
  const deptId = searchParams.get('dept') ?? '';
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [showCreate, setShowCreate] = useState(false);

  const {
    workspaceId: contextWsId,
    departmentId: contextDeptId,
    departmentName,
    canSelectDepartment,
    isScopedUser,
    hasAssignedDepartment,
    isLoading: contextLoading,
  } = useProjectDepartmentContext();

  const { data: workspaces } = useWorkspacesList();
  const effectiveWsId = wsId || contextWsId;
  const { data: departments } = useDepartmentList(canSelectDepartment ? (effectiveWsId || undefined) : undefined);

  const effectiveDeptId = canSelectDepartment ? deptId : contextDeptId;
  const { data, isLoading, isError, error } = useProjectList(
    effectiveWsId || undefined,
    effectiveDeptId || undefined,
    search || undefined,
    page,
  );

  const { canCreate } = useProjectAccess(effectiveWsId || undefined);
  const isMemberUser = isMember(user?.roles);

  // Keep URL in sync for scoped users so links and refreshes stay consistent.
  useEffect(() => {
    if (contextLoading || canSelectDepartment || !hasAssignedDepartment) return;
    if (!effectiveWsId || !contextDeptId) return;

    const needsSync = wsId !== effectiveWsId || deptId !== contextDeptId;
    if (needsSync) {
      setSearchParams({ ws: effectiveWsId, dept: contextDeptId }, { replace: true });
    }
  }, [
    canSelectDepartment,
    contextLoading,
    contextDeptId,
    deptId,
    effectiveWsId,
    hasAssignedDepartment,
    setSearchParams,
    wsId,
  ]);

  const handleSelectWs = (ws: string) => {
    if (!ws) {
      setSearchParams({});
      return;
    }
    setSearchParams({ ws, dept: '' });
  };

  const handleSelectDept = (dept: string) => {
    if (!dept) {
      setSearchParams({ ws: effectiveWsId, dept: '' });
      return;
    }
    setSearchParams({ ws: effectiveWsId, dept });
  };

  const selectedDepartmentName = canSelectDepartment
    ? departments?.find((d) => d.id === effectiveDeptId)?.name
    : departmentName;

  const hasContext = !!effectiveWsId && !!effectiveDeptId;
  const pageTitle = isScopedUser && selectedDepartmentName
    ? `${selectedDepartmentName} Projects`
    : 'Projects';

  if (contextLoading) {
    return (
      <div className="flex flex-col gap-6 animate-fade-in">
        <Skeleton className="h-7 w-40" />
        <Skeleton className="h-4 w-64" />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-48 rounded-xl" />
          ))}
        </div>
      </div>
    );
  }

  if (isScopedUser && !hasAssignedDepartment) {
    return (
      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-page font-semibold text-text-primary">Projects</h1>
        </div>
        <Card>
          <CardBody className="py-16">
            <EmptyState
              icon={<AlertCircle className="h-6 w-6" />}
              title="No department assigned"
              description="No department is assigned to your account. Contact your administrator to get access to projects."
            />
          </CardBody>
        </Card>
      </div>
    );
  }

  if (canSelectDepartment && !hasContext) {
    return (
      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-page font-semibold text-text-primary">Projects</h1>
          <p className="text-body text-text-secondary">Select a workspace and department to manage projects.</p>
        </div>
        <Card>
          <CardBody className="space-y-4">
            <div className="flex items-center gap-2 text-text-secondary">
              <Briefcase className="h-4 w-4" />
              <span className="text-body font-medium text-text-primary">Workspace</span>
            </div>
            <Select
              value={effectiveWsId}
              onChange={(e) => handleSelectWs(e.target.value)}
              options={[
                { value: '', label: 'Select a workspace' },
                ...(workspaces ?? []).map((w) => ({ value: w.id, label: w.name })),
              ]}
            />
            {effectiveWsId && (
              <>
                <div className="flex items-center gap-2 text-text-secondary">
                  <Network className="h-4 w-4" />
                  <span className="text-body font-medium text-text-primary">Department</span>
                </div>
                <Select
                  value={deptId}
                  onChange={(e) => handleSelectDept(e.target.value)}
                  options={[
                    { value: '', label: 'Select a department' },
                    ...(departments ?? []).map((d) => ({ value: d.id, label: d.name })),
                  ]}
                />
              </>
            )}
            {(!workspaces || workspaces.length === 0) && (
              <p className="text-caption text-text-tertiary">
                No workspaces available. Please create a workspace first.
              </p>
            )}
          </CardBody>
        </Card>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6 animate-fade-in">
        <div className="flex flex-col gap-1.5">
          <Skeleton className="h-7 w-40" />
          <Skeleton className="h-4 w-64" />
        </div>
        <div className="flex gap-4">
          <Skeleton className="h-9 flex-1" />
          <Skeleton className="h-9 w-24" />
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-48 rounded-xl" />
          ))}
        </div>
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
            action={
              errorState.isAccessDenied ? (
                <Button variant="outline" onClick={() => navigate('/app/projects')}>Back to Projects</Button>
              ) : (
                <Button variant="outline" onClick={() => window.location.reload()}>Retry</Button>
              )
            }
          />
        </CardBody>
      </Card>
    );
  }

  const projects = data?.content ?? [];
  const totalPages = data?.page?.totalPages ?? 0;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-page font-semibold text-text-primary">{pageTitle}</h1>
        <p className="text-body text-text-secondary">
          {canSelectDepartment
            ? 'Manage projects across your departments.'
            : 'Projects in your department.'}
        </p>
        {isScopedUser && (
          <div className="flex flex-wrap items-center gap-2 pt-1">
            {workspaces && workspaces.length > 1 && effectiveWsId && (
              <Badge tone="neutral" variant="soft">
                Workspace: {workspaces.find((w) => w.id === effectiveWsId)?.name ?? effectiveWsId}
              </Badge>
            )}
            {selectedDepartmentName && (
              <Badge tone="accent" variant="soft">
                Department: {selectedDepartmentName}
              </Badge>
            )}
          </div>
        )}
      </div>

      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex-1">
          <Input
            placeholder="Search projects..."
            leftIcon={<Search />}
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            containerClassName="w-full sm:max-w-xs"
          />
        </div>
        {canCreate && (
          <Button leftIcon={<Plus />} onClick={() => setShowCreate(true)}>Create Project</Button>
        )}
      </div>

      {projects.length === 0 ? (
        <Card>
          <CardBody className="py-16">
            <EmptyState
              icon={<FolderKanban className="h-6 w-6" />}
              title="No projects found"
              description={getProjectEmptyDescription(isScopedUser, isMemberUser, !!search)}
              action={!search && canCreate ? (
                <Button leftIcon={<Plus />} onClick={() => setShowCreate(true)}>Create Project</Button>
              ) : undefined}
            />
          </CardBody>
        </Card>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {projects.map((project) => (
              <ProjectCard
                key={project.id}
                project={project}
                onClick={() => navigate(`/app/projects/${project.id}?ws=${effectiveWsId}&dept=${effectiveDeptId}`)}
              />
            ))}
          </div>
          {totalPages > 1 && (
            <Pagination page={page + 1} totalPages={totalPages} onPageChange={(p) => setPage(p - 1)} className="self-center" />
          )}
        </>
      )}

      {showCreate && effectiveWsId && effectiveDeptId && (
        <CreateProjectModal
          open={showCreate}
          onClose={() => setShowCreate(false)}
          wsId={effectiveWsId}
          deptId={effectiveDeptId}
          departmentName={selectedDepartmentName}
          departmentLocked={isScopedUser}
        />
      )}
    </div>
  );
}

function ProjectCard({ project, onClick }: { project: ProjectResponse; onClick: () => void }) {
  return (
    <button type="button" onClick={onClick} className="text-left w-full rounded-xl border border-border-subtle bg-surface p-4 hover:border-border-default hover:shadow-cx-sm transition-all group">
      <div className="flex items-start justify-between gap-3 mb-3">
        <div className="flex items-center gap-2.5 min-w-0">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg" style={{ backgroundColor: project.color ?? '#e5e7eb' }}>
            <FolderKanban className="h-4 w-4 text-white" />
          </div>
          <div className="min-w-0">
            <h3 className="text-body font-semibold text-text-primary truncate">{project.name}</h3>
            {project.departmentName && (
              <p className="text-2xs text-text-tertiary truncate">{project.departmentName}</p>
            )}
          </div>
        </div>
        {project.priority && (
          <Badge tone={priorityColors[project.priority]} variant="soft" dot className="shrink-0 capitalize">
            {project.priority.toLowerCase()}
          </Badge>
        )}
      </div>

      {project.description && (
        <p className="text-caption text-text-tertiary line-clamp-2 mb-3">{project.description}</p>
      )}

      <div className="flex items-center gap-2 mt-auto">
        <Badge tone={statusColors[project.status]} variant="soft" dot>
          {project.status === 'ACTIVE' ? 'Active' : 'Archived'}
        </Badge>
        {project.managerName && (
          <span className="text-2xs text-text-tertiary ml-auto">{project.managerName}</span>
        )}
      </div>
    </button>
  );
}
