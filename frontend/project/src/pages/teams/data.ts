import { useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useWorkspaceId } from '../../hooks/useWorkspaceId';
import { useDepartmentList } from '../../services/department-hooks';
import { useWorkspaceTeams, useWorkspaceUsers } from '../../services/team-hooks';
import type { WorkspaceTeam } from '../../services/team-hooks';
import { UserStatus } from '../../types';
import type { Team } from './types';
import { statusMap } from './types';

function formatCreatedAt(value: string | undefined | null): string {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleDateString();
}

function mapWorkspaceTeamToTeam(t: WorkspaceTeam): Team {
  return {
    id: t.id,
    name: t.name,
    description: t.description ?? '',
    department: t.departmentName ?? '',
    departmentId: t.departmentId,
    manager: t.managerName ?? 'Unassigned',
    managerId: t.managerId ?? undefined,
    memberCount: t.memberCount ?? 0,
    status: statusMap[t.status] ?? 'active',
    createdAt: formatCreatedAt(t.createdAt),
    members: [],
  };
}

export interface ManagerOption {
  id: string;
  name: string;
}

export function useTeamsData() {
  const wsId = useWorkspaceId();
  const qc = useQueryClient();
  const { data: deptList } = useDepartmentList(wsId);
  const teamsQuery = useWorkspaceTeams(wsId);
  const usersQuery = useWorkspaceUsers(wsId);

  const refetch = () => {
    qc.invalidateQueries({ queryKey: ['workspace', 'teams', wsId] });
  };

  const departments = useMemo(
    () => (deptList ?? []).map((d) => d.name),
    [deptList],
  );

  const teams: Team[] = useMemo(
    () => (teamsQuery.data ?? []).map(mapWorkspaceTeamToTeam),
    [teamsQuery.data],
  );

  const managers: ManagerOption[] = useMemo(
    () =>
      (usersQuery.data ?? [])
        .filter((u) => u.status === UserStatus.ACTIVE)
        .map((u) => ({ id: u.id, name: `${u.firstName} ${u.lastName}`.trim() }))
        .sort((a, b) => a.name.localeCompare(b.name)),
    [usersQuery.data],
  );

  const isLoading = teamsQuery.isLoading;
  const isError = teamsQuery.isError;

  return { teams, departments, managers, isLoading, isError, refetch };
}