import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../lib/auth-context';
import { useWorkspaceId } from '../hooks/useWorkspaceId';
import { useWorkspaceDetail, useWorkspacesList } from './workspace-hooks';
import { isAdmin, isManager, isMember, isSuperAdmin } from '../lib/access';

export interface ProjectDepartmentContext {
  workspaceId: string;
  departmentId: string | undefined;
  departmentName: string | undefined;
  /** Admin may choose department via selector */
  canSelectDepartment: boolean;
  /** Scoped to a single department (Manager/Member) */
  isScopedUser: boolean;
  hasAssignedDepartment: boolean;
  isLoading: boolean;
}

/**
 * Resolves workspace + department context for Projects screens.
 *
 * Admin (global or workspace): manual department selection via URL/selector.
 * Manager/Member: department comes from authenticated user profile.
 */
export function useProjectDepartmentContext(): ProjectDepartmentContext {
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const urlWorkspaceId = useWorkspaceId();
  const urlDepartmentId = searchParams.get('dept') ?? '';

  const { data: workspaces, isLoading: workspacesLoading } = useWorkspacesList();
  const workspaceId = urlWorkspaceId || workspaces?.[0]?.id || '';
  const { data: workspace, isLoading: workspaceLoading } = useWorkspaceDetail(workspaceId || undefined);

  return useMemo(() => {
    const roles = user?.roles ?? [];
    const isGlobalAdmin = isSuperAdmin(roles) || isAdmin(roles);
    const wsRole = workspace?.myRole ?? null;
    const isWorkspaceAdmin = wsRole === 'OWNER' || wsRole === 'ADMIN';
    const canSelectDepartment = isGlobalAdmin || isWorkspaceAdmin;
    const isScopedUser = !canSelectDepartment && (isManager(roles) || isMember(roles));
    const hasAssignedDepartment = !!user?.departmentId;

    let departmentId: string | undefined;
    let departmentName: string | undefined;

    if (canSelectDepartment) {
      departmentId = urlDepartmentId || undefined;
      departmentName = undefined;
    } else if (isScopedUser) {
      departmentId = user?.departmentId;
      departmentName = user?.departmentName;
    }

    return {
      workspaceId,
      departmentId,
      departmentName,
      canSelectDepartment,
      isScopedUser,
      hasAssignedDepartment,
      isLoading: workspacesLoading || (!!workspaceId && workspaceLoading),
    };
  }, [
    user?.roles,
    user?.departmentId,
    user?.departmentName,
    workspace?.myRole,
    workspaceId,
    urlDepartmentId,
    workspacesLoading,
    workspaceLoading,
  ]);
}

export interface ProjectQueryErrorState {
  title: string;
  description: string;
  isAccessDenied: boolean;
}

export function getProjectQueryErrorState(
  error: unknown,
  scopedUser: boolean,
): ProjectQueryErrorState {
  const status = typeof error === 'object' && error !== null && 'status' in error
    ? (error as { status: number | null }).status
    : null;
  const message = typeof error === 'object' && error !== null && 'message' in error
    ? String((error as { message: string }).message)
    : error instanceof Error
      ? error.message
      : 'An unexpected error occurred.';

  if (status === 403) {
    return {
      title: 'Access denied',
      description: 'You do not have permission to view this project or department.',
      isAccessDenied: true,
    };
  }

  if (status === 404) {
    return {
      title: 'Not found',
      description: 'The project you are looking for does not exist or is no longer available.',
      isAccessDenied: false,
    };
  }

  return {
    title: 'Failed to load projects',
    description: message,
    isAccessDenied: false,
  };
}

export function getProjectEmptyDescription(
  scopedUser: boolean,
  isMemberUser: boolean,
  hasSearch: boolean,
): string {
  if (hasSearch) {
    return 'No projects match your search.';
  }
  if (isMemberUser) {
    return 'No projects are currently available to you.';
  }
  if (scopedUser) {
    return 'No projects found for your department.';
  }
  return 'No projects yet. Create your first project to get started.';
}
