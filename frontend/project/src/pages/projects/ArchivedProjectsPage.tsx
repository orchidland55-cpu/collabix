import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Archive, AlertCircle, RotateCcw, ShieldBan } from 'lucide-react';
import { Card, CardBody } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Skeleton } from '../../components/ui/Skeleton';
import { EmptyState } from '../../components/ui/EmptyState';
import {
  useArchivedProjects,
  useProjectAccess,
  useProjectDepartmentContext,
  getProjectQueryErrorState,
} from '../../services/project-hooks';
import { RestoreProjectModal } from './modals/RestoreProjectModal';
import type { ProjectResponse } from './projects-types';

export function ArchivedProjectsPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const wsId = searchParams.get('ws') ?? '';
  const deptId = searchParams.get('dept') ?? '';
  const [restoreTarget, setRestoreTarget] = useState<ProjectResponse | null>(null);

  const {
    workspaceId: contextWsId,
    departmentId: contextDeptId,
    canSelectDepartment,
    isScopedUser,
    hasAssignedDepartment,
    isLoading: contextLoading,
  } = useProjectDepartmentContext();

  const effectiveWsId = wsId || contextWsId;
  const effectiveDeptId = canSelectDepartment ? deptId : contextDeptId;

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

  const { data: projects, isLoading, isError, error } = useArchivedProjects(
    effectiveWsId || undefined,
    effectiveDeptId || undefined,
  );
  const { canRestore } = useProjectAccess(effectiveWsId || undefined);

  if (contextLoading) {
    return (
      <div className="flex flex-col gap-6 animate-fade-in">
        <Skeleton className="h-7 w-48" />
        <Skeleton className="h-16 w-full rounded-lg" />
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
          />
        </CardBody>
      </Card>
    );
  }

  if (canSelectDepartment && (!effectiveWsId || !effectiveDeptId)) {
    return (
      <Card>
        <CardBody className="py-16">
          <EmptyState
            icon={<Archive className="h-6 w-6" />}
            title="Select a department"
            description="Choose a workspace and department from the projects page to view archived projects."
            action={<Button variant="outline" onClick={() => navigate('/app/projects')}>Go to Projects</Button>}
          />
        </CardBody>
      </Card>
    );
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6 animate-fade-in">
        <div className="flex flex-col gap-1.5">
          <Skeleton className="h-7 w-48" />
          <Skeleton className="h-4 w-64" />
        </div>
        <div className="flex flex-col gap-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-16 rounded-lg" />
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
            action={<Button variant="outline" onClick={() => navigate('/app/projects')}>Back to Projects</Button>}
          />
        </CardBody>
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-page font-semibold text-text-primary">Archived Projects</h1>
        <p className="text-body text-text-secondary">Restore archived projects to bring them back to active projects.</p>
      </div>

      {!projects || projects.length === 0 ? (
        <Card>
          <CardBody className="py-16">
            <EmptyState
              icon={<Archive className="h-6 w-6" />}
              title="No archived projects"
              description={isScopedUser ? 'No archived projects in your department.' : 'Archived projects will appear here.'}
            />
          </CardBody>
        </Card>
      ) : (
        <div className="flex flex-col gap-3">
          {projects.map((project) => (
            <div key={project.id} className="flex items-center justify-between gap-4 rounded-lg border border-border-subtle bg-surface p-4">
              <div className="min-w-0 flex-1">
                <p className="text-body font-semibold text-text-primary truncate">{project.name}</p>
                <div className="flex items-center gap-2 mt-1">
                  <Badge tone="neutral" variant="soft">Archived</Badge>
                  {project.departmentName && <span className="text-2xs text-text-tertiary">{project.departmentName}</span>}
                  {project.updatedAt && <span className="text-2xs text-text-tertiary">Archived {new Date(project.updatedAt).toLocaleDateString()}</span>}
                </div>
              </div>
              {canRestore && (
                <Button variant="outline" size="sm" leftIcon={<RotateCcw className="h-3.5 w-3.5" />} onClick={() => setRestoreTarget(project)}>
                  Restore
                </Button>
              )}
            </div>
          ))}
        </div>
      )}

      {restoreTarget && effectiveWsId && effectiveDeptId && (
        <RestoreProjectModal
          open={!!restoreTarget}
          onClose={() => setRestoreTarget(null)}
          wsId={effectiveWsId}
          deptId={effectiveDeptId}
          projectId={restoreTarget.id}
          projectName={restoreTarget.name}
        />
      )}
    </div>
  );
}
