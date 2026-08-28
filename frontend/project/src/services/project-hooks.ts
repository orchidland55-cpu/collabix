import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  listProjects,
  listWorkspaceProjects,
  getProjectById,
  createProject,
  updateProject,
  deleteProject,
  restoreProject,
  hardDeleteProject,
  listArchivedProjects,
} from './project-service';
import { useWorkspaceDetail } from './workspace-hooks';
import { useAuth } from '../lib/auth-context';
import { hasPermission, isAdmin, isManager, isSuperAdmin } from '../lib/access';

export { useProjectDepartmentContext, getProjectQueryErrorState, getProjectEmptyDescription } from './project-department-context';
export type { ProjectDepartmentContext, ProjectQueryErrorState } from './project-department-context';
import type { CreateProjectRequest, UpdateProjectRequest, ProjectResponse } from '../pages/projects/projects-types';
import type { PageResponse } from '../types/api';

const projectKeys = {
  all: ['projects'] as const,
  paginated: (wsId: string, deptId: string, search?: string, page?: number) =>
    ['projects', 'paginated', wsId, deptId, search, page] as const,
  detail: (wsId: string, deptId: string, projectId: string) =>
    ['projects', 'detail', wsId, deptId, projectId] as const,
  archived: (wsId: string, deptId: string) => ['projects', 'archived', wsId, deptId] as const,
};

export function useProjectList(
  wsId: string | undefined,
  deptId: string | undefined,
  search?: string,
  page = 0,
) {
  return useQuery<PageResponse<ProjectResponse>>({
    queryKey: projectKeys.paginated(wsId!, deptId!, search, page),
    queryFn: () => listProjects(wsId!, deptId!, search, page),
    enabled: !!wsId && !!deptId,
  });
}

export function useWorkspaceProjects(wsId: string | undefined, search?: string, page = 0) {
  return useQuery<PageResponse<ProjectResponse>>({
    queryKey: ['projects', 'workspace', wsId, search, page] as const,
    queryFn: () => listWorkspaceProjects(wsId!, search, page),
    enabled: !!wsId,
  });
}

export function useProjectDetail(
  wsId: string | undefined,
  deptId: string | undefined,
  projectId: string | undefined,
) {
  return useQuery<ProjectResponse>({
    queryKey: projectKeys.detail(wsId!, deptId!, projectId!),
    queryFn: () => getProjectById(wsId!, deptId!, projectId!),
    enabled: !!wsId && !!deptId && !!projectId,
  });
}

export function useCreateProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      wsId,
      deptId,
      data,
    }: {
      wsId: string;
      deptId: string;
      data: CreateProjectRequest;
    }) => createProject(wsId, deptId, data),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: projectKeys.paginated(variables.wsId, variables.deptId) });
    },
  });
}

export function useUpdateProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      wsId,
      deptId,
      projectId,
      data,
    }: {
      wsId: string;
      deptId: string;
      projectId: string;
      data: UpdateProjectRequest;
    }) => updateProject(wsId, deptId, projectId, data),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: projectKeys.paginated(variables.wsId, variables.deptId) });
      qc.invalidateQueries({
        queryKey: projectKeys.detail(variables.wsId, variables.deptId, variables.projectId),
      });
    },
  });
}

export function useDeleteProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      wsId,
      deptId,
      projectId,
    }: {
      wsId: string;
      deptId: string;
      projectId: string;
    }) => deleteProject(wsId, deptId, projectId),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: projectKeys.paginated(variables.wsId, variables.deptId) });
      qc.invalidateQueries({ queryKey: projectKeys.archived(variables.wsId, variables.deptId) });
      qc.invalidateQueries({ queryKey: projectKeys.detail(variables.wsId, variables.deptId, variables.projectId) });
    },
  });
}

export function useRestoreProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      wsId,
      deptId,
      projectId,
    }: {
      wsId: string;
      deptId: string;
      projectId: string;
    }) => restoreProject(wsId, deptId, projectId),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: projectKeys.paginated(variables.wsId, variables.deptId) });
      qc.invalidateQueries({ queryKey: projectKeys.archived(variables.wsId, variables.deptId) });
      qc.invalidateQueries({
        queryKey: projectKeys.detail(variables.wsId, variables.deptId, variables.projectId),
      });
    },
  });
}

export function useHardDeleteProject() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      wsId,
      deptId,
      projectId,
    }: {
      wsId: string;
      deptId: string;
      projectId: string;
    }) => hardDeleteProject(wsId, deptId, projectId),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: projectKeys.paginated(variables.wsId, variables.deptId) });
      qc.invalidateQueries({ queryKey: projectKeys.archived(variables.wsId, variables.deptId) });
      qc.invalidateQueries({ queryKey: projectKeys.detail(variables.wsId, variables.deptId, variables.projectId) });
    },
  });
}

export function useArchivedProjects(wsId: string | undefined, deptId: string | undefined) {
  return useQuery<ProjectResponse[]>({
    queryKey: projectKeys.archived(wsId!, deptId!),
    queryFn: () => listArchivedProjects(wsId!, deptId!),
    enabled: !!wsId && !!deptId,
  });
}

export interface ProjectAccess {
  canCreate: boolean;
  canUpdate: boolean;
  canArchive: boolean;
  canRestore: boolean;
  canHardDelete: boolean;
  isLoading: boolean;
}

export function useProjectAccess(wsId: string | undefined): ProjectAccess {
  const { user } = useAuth();
  const { data: workspace, isLoading } = useWorkspaceDetail(wsId || undefined);

  const roles = user?.roles ?? [];
  const superAdmin = isSuperAdmin(roles);
  const globalAdmin = isAdmin(roles);
  const globalManager = isManager(roles);
  const wsRole = workspace?.myRole ?? null;
  const isWorkspaceManager = wsRole === 'OWNER' || wsRole === 'ADMIN';
  const isWorkspaceOwner = wsRole === 'OWNER';
  const canManageProjects = superAdmin || globalAdmin || isWorkspaceManager || globalManager;

  return {
    canCreate: !!user && hasPermission(user, 'PROJECT_CREATE') && canManageProjects,
    canUpdate: !!user && hasPermission(user, 'PROJECT_UPDATE') && canManageProjects,
    canArchive: !!user && hasPermission(user, 'PROJECT_DELETE') && canManageProjects,
    canRestore: !!user && hasPermission(user, 'PROJECT_UPDATE') && canManageProjects,
    canHardDelete: !!user && hasPermission(user, 'PROJECT_DELETE') && (superAdmin || isWorkspaceOwner),
    isLoading,
  };
}
