import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { userService } from './user-service';
import {
  createTeam,
  updateTeam,
  archiveTeam,
  deleteTeamPermanently,
  restoreTeam,
  removeTeamMember,
  listWorkspaceTeams,
  type TeamSummary,
  type CreateTeamRequest,
  type UpdateTeamRequest,
} from './team-service';

export interface WorkspaceTeam extends TeamSummary {
  departmentId: string;
  departmentName?: string;
}

const teamKeys = {
  workspaceTeams: (wsId: string) => ['workspace', 'teams', wsId] as const,
};

export function useWorkspaceTeams(wsId: string | undefined) {
  return useQuery<WorkspaceTeam[]>({
    queryKey: teamKeys.workspaceTeams(wsId!),
    queryFn: async () => {
      const teams = await listWorkspaceTeams(wsId!);
      return teams.map((t) => ({ ...t, departmentId: t.departmentId }));
    },
    enabled: !!wsId,
  });
}

export function useWorkspaceUsers(wsId: string | undefined) {
  return useQuery({
    queryKey: ['workspace', 'users', wsId] as const,
    queryFn: () => userService(wsId!).list(),
    enabled: !!wsId,
  });
}

export function useCreateTeam(wsId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ departmentId, data }: { departmentId: string; data: CreateTeamRequest }) =>
      createTeam(wsId!, departmentId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: teamKeys.workspaceTeams(wsId!) });
    },
  });
}

export function useUpdateTeam(wsId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ departmentId, teamId, data }: { departmentId: string; teamId: string; data: UpdateTeamRequest }) =>
      updateTeam(wsId!, departmentId, teamId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: teamKeys.workspaceTeams(wsId!) });
    },
  });
}

export function useArchiveTeam(wsId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ departmentId, teamId }: { departmentId: string; teamId: string }) =>
      archiveTeam(wsId!, departmentId, teamId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: teamKeys.workspaceTeams(wsId!) });
    },
  });
}

export function useRestoreTeam(wsId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ departmentId, teamId }: { departmentId: string; teamId: string }) =>
      restoreTeam(wsId!, departmentId, teamId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: teamKeys.workspaceTeams(wsId!) });
    },
  });
}

export function useDeleteTeamPermanently(wsId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ departmentId, teamId }: { departmentId: string; teamId: string }) =>
      deleteTeamPermanently(wsId!, departmentId, teamId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: teamKeys.workspaceTeams(wsId!) });
    },
  });
}

export function useAssignMemberToTeam(wsId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, teamId }: { userId: string; teamId: string }) =>
      userService(wsId!).update(userId, { teamId }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['workspace', 'users', wsId!] });
      qc.invalidateQueries({ queryKey: teamKeys.workspaceTeams(wsId!) });
    },
  });
}

export function useRemoveMemberFromTeam(wsId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ departmentId, teamId, userId }: { departmentId: string; teamId: string; userId: string }) =>
      removeTeamMember(wsId!, departmentId, teamId, userId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['workspace', 'users', wsId!] });
      qc.invalidateQueries({ queryKey: teamKeys.workspaceTeams(wsId!) });
    },
  });
}