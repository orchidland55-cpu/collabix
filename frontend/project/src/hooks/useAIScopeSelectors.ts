import { useMemo } from 'react';
import { useDepartmentList } from '../services/department-hooks';
import { useAIPermissions } from './useAIPermissions';
import { useEffectiveWorkspaceId } from './useEffectiveWorkspaceId';
import { listProjects } from '../services/project-service';
import { listTeamsByDepartment } from '../services/team-service';
import { useQuery } from '@tanstack/react-query';

export type AIScopeType = 'WORKSPACE' | 'DEPARTMENT' | 'PROJECT' | 'TEAM';

export interface AIScopeSelection {
  scope: AIScopeType;
  departmentId?: string;
  projectId?: string;
  teamId?: string;
}

export function useAIScopeSelectors(selectedDepartmentId?: string) {
  const workspaceId = useEffectiveWorkspaceId();
  const { isAdmin, isManager, departmentId: userDepartmentId } = useAIPermissions();
  const { data: departments = [] } = useDepartmentList(workspaceId || undefined);

  const visibleDepartments = useMemo(() => {
    if (isAdmin) return departments;
    if (isManager && userDepartmentId) {
      return departments.filter((d) => d.id === userDepartmentId);
    }
    if (userDepartmentId) {
      return departments.filter((d) => d.id === userDepartmentId);
    }
    return departments;
  }, [departments, isAdmin, isManager, userDepartmentId]);

  const scopeOptions = useMemo(() => {
    if (isAdmin) {
      return [
        { value: 'WORKSPACE', label: 'Entire workspace' },
        { value: 'DEPARTMENT', label: 'Department' },
        { value: 'PROJECT', label: 'Project' },
        { value: 'TEAM', label: 'Team' },
      ];
    }
    if (isManager) {
      return [
        { value: 'DEPARTMENT', label: 'Department' },
        { value: 'PROJECT', label: 'Project' },
        { value: 'TEAM', label: 'Team' },
      ];
    }
    return [];
  }, [isAdmin, isManager]);

  const effectiveDepartmentId = selectedDepartmentId || userDepartmentId || visibleDepartments[0]?.id;

  const projectsQuery = useQuery({
    queryKey: ['ai-scope', 'projects', workspaceId, effectiveDepartmentId],
    queryFn: () => listProjects(workspaceId, effectiveDepartmentId!),
    enabled: !!workspaceId && !!effectiveDepartmentId,
  });

  const teamsQuery = useQuery({
    queryKey: ['ai-scope', 'teams', workspaceId, effectiveDepartmentId],
    queryFn: () => listTeamsByDepartment(workspaceId, effectiveDepartmentId!),
    enabled: !!workspaceId && !!effectiveDepartmentId,
  });

  return {
    scopeOptions,
    departments: visibleDepartments,
    projects: projectsQuery.data?.content ?? [],
    teams: teamsQuery.data ?? [],
    defaultDepartmentId: effectiveDepartmentId,
    defaultScope: (isAdmin ? 'WORKSPACE' : 'DEPARTMENT') as AIScopeType,
  };
}
