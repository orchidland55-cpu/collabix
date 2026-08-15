import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useProjectDepartmentContext } from './project-department-context';

export interface TaskDepartmentContext {
  workspaceId: string;
  departmentId: string | undefined;
  departmentName: string | undefined;
  projectId: string | undefined;
  /** Admin / workspace admin may choose department and project */
  canSelectDepartment: boolean;
  canSelectProject: boolean;
  /** Manager or member scoped to a single department */
  isScopedUser: boolean;
  hasAssignedDepartment: boolean;
  isLoading: boolean;
}

/**
 * Resolves workspace + department + project context for Tasks screens.
 *
 * Admin: manual department and project selection via URL.
 * Manager/Member: department from profile; project selected within that department.
 */
export function useTaskDepartmentContext(): TaskDepartmentContext {
  const [searchParams] = useSearchParams();
  const urlProjectId = searchParams.get('proj') ?? '';
  const base = useProjectDepartmentContext();

  return useMemo(() => {
    const canSelectProject = base.canSelectDepartment;

    let projectId: string | undefined;
    if (canSelectProject) {
      projectId = urlProjectId || undefined;
    } else if (base.isScopedUser) {
      projectId = urlProjectId || undefined;
    }

    return {
      workspaceId: base.workspaceId,
      departmentId: base.departmentId,
      departmentName: base.departmentName,
      projectId,
      canSelectDepartment: base.canSelectDepartment,
      canSelectProject,
      isScopedUser: base.isScopedUser,
      hasAssignedDepartment: base.hasAssignedDepartment,
      isLoading: base.isLoading,
    };
  }, [
    base.workspaceId,
    base.departmentId,
    base.departmentName,
    base.canSelectDepartment,
    base.isScopedUser,
    base.hasAssignedDepartment,
    base.isLoading,
    urlProjectId,
  ]);
}

export function getTaskEmptyDescription(
  scopedUser: boolean,
  isMemberUser: boolean,
  hasDepartment: boolean,
  hasSearch: boolean,
  hasStatusFilter?: boolean,
): string {
  if (!hasDepartment) {
    return scopedUser
      ? 'Your department context is loading.'
      : 'Select a workspace and department to open the task board.';
  }
  if (hasSearch || hasStatusFilter) {
    return 'No tasks match your current filters.';
  }
  if (isMemberUser) {
    return 'No tasks are assigned to you in this department.';
  }
  if (scopedUser) {
    return 'No tasks found for your department.';
  }
  return 'No tasks yet. Create your first task to get started.';
}

export interface TaskQueryErrorState {
  title: string;
  description: string;
  isAccessDenied: boolean;
}

export function getTaskQueryErrorState(
  error: unknown,
  scopedUser: boolean,
): TaskQueryErrorState {
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
      description: scopedUser
        ? 'You do not have permission to view these tasks.'
        : 'You do not have permission to view tasks in this department.',
      isAccessDenied: true,
    };
  }

  if (status === 404) {
    return {
      title: 'Tasks not found',
      description: 'The requested tasks or project could not be found.',
      isAccessDenied: false,
    };
  }

  return {
    title: 'Failed to load tasks',
    description: message,
    isAccessDenied: false,
  };
}
