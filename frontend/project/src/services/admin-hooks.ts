import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userService } from './user-service';
import { userHistoryService } from './user-history-service';
import { roleService } from './role-service';
import { permissionService } from './permission-service';
import { listDepartments } from './department-service';
import { teamService } from './team-service';
import { useWorkspaceId } from '../hooks/useWorkspaceId';
import { useWorkspacesList } from './workspace-hooks';
import type { UserSearchCriteria, UserHistorySearchCriteria, CreateUserRequest, UpdateUserRequest, AssignRolesRequest } from '../types';
import type { TeamSummary } from './team-service';

/* ---------- Query key helpers ---------- */

const adminKeys = {
  all: ['admin'] as const,
  users: (wsId: string) => ['admin', 'users', wsId] as const,
  userDetail: (wsId: string, id: string) => ['admin', 'users', wsId, id] as const,
  stats: (wsId: string) => ['admin', 'stats', wsId] as const,
  roles: () => ['admin', 'roles'] as const,
  roleDetail: (id: string) => ['admin', 'roles', id] as const,
  permissions: () => ['admin', 'permissions'] as const,
};

/* ---------- Workspace context ---------- */

function useEffectiveWorkspaceId(): string {
  const wsFromUrl = useWorkspaceId();
  const { data: workspaces } = useWorkspacesList();
  if (wsFromUrl) return wsFromUrl;
  if (workspaces && workspaces.length > 0) {
    return workspaces[0].id;
  }
  return '';
}

/* ---------- User hooks ---------- */

export function useUsersList() {
  const wsId = useEffectiveWorkspaceId();

  return useQuery({
    queryKey: adminKeys.users(wsId),
    queryFn: () => userService(wsId).list(),
    enabled: !!wsId,
  });
}

export function useUsersSearch(criteria: UserSearchCriteria, page: number, size: number, sort?: string) {
  const wsId = useEffectiveWorkspaceId();

  return useQuery({
    queryKey: [...adminKeys.users(wsId), { ...criteria, page, size, sort }],
    queryFn: () => userService(wsId).search(criteria, page, size, sort),
    enabled: !!wsId,
  });
}

export function useUserDetail(id: string | undefined) {
  const wsId = useEffectiveWorkspaceId();

  return useQuery({
    queryKey: adminKeys.userDetail(wsId, id ?? ''),
    queryFn: () => userService(wsId).getById(id!),
    enabled: !!id && !!wsId,
  });
}

export function useUserStatistics() {
  const wsId = useEffectiveWorkspaceId();

  return useQuery({
    queryKey: adminKeys.stats(wsId),
    queryFn: () => userService(wsId).statistics(),
    enabled: !!wsId,
  });
}

export function useCreateUser() {
  const qc = useQueryClient();
  const wsId = useEffectiveWorkspaceId();

  return useMutation({
    mutationFn: (data: CreateUserRequest) => {
      if (!wsId) throw new Error('No workspace selected');
      return userService(wsId).create(data);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.all });
    },
  });
}

export function useUpdateUser() {
  const qc = useQueryClient();
  const wsId = useEffectiveWorkspaceId();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserRequest }) => {
      if (!wsId) throw new Error('No workspace selected');
      return userService(wsId).update(id, data);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.all });
    },
  });
}

export function useDeleteUser() {
  const qc = useQueryClient();
  const wsId = useEffectiveWorkspaceId();

  return useMutation({
    mutationFn: (id: string) => {
      if (!wsId) throw new Error('No workspace selected');
      return userService(wsId).delete(id);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.all });
    },
  });
}

export function useDeleteUserPermanent() {
  const qc = useQueryClient();
  const wsId = useEffectiveWorkspaceId();

  return useMutation({
    mutationFn: (id: string) => {
      if (!wsId) throw new Error('No workspace selected');
      return userService(wsId).deletePermanent(id);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.all });
    },
  });
}

export function useActivateUser() {
  const qc = useQueryClient();
  const wsId = useEffectiveWorkspaceId();

  return useMutation({
    mutationFn: (id: string) => {
      if (!wsId) throw new Error('No workspace selected');
      return userService(wsId).activate(id);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.all });
    },
  });
}

export function useDeactivateUser() {
  const qc = useQueryClient();
  const wsId = useEffectiveWorkspaceId();

  return useMutation({
    mutationFn: (id: string) => {
      if (!wsId) throw new Error('No workspace selected');
      return userService(wsId).deactivate(id);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.all });
    },
  });
}

export function useSuspendUser() {
  const qc = useQueryClient();
  const wsId = useEffectiveWorkspaceId();

  return useMutation({
    mutationFn: (id: string) => {
      if (!wsId) throw new Error('No workspace selected');
      return userService(wsId).suspend(id);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.all });
    },
  });
}

export function useReactivateUser() {
  const qc = useQueryClient();
  const wsId = useEffectiveWorkspaceId();

  return useMutation({
    mutationFn: (id: string) => {
      if (!wsId) throw new Error('No workspace selected');
      return userService(wsId).reactivate(id);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.all });
    },
  });
}

export function useArchiveUser() {
  const qc = useQueryClient();
  const wsId = useEffectiveWorkspaceId();

  return useMutation({
    mutationFn: (id: string) => {
      if (!wsId) throw new Error('No workspace selected');
      return userService(wsId).archive(id);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.all });
    },
  });
}

export function useRestoreUser() {
  const qc = useQueryClient();
  const wsId = useEffectiveWorkspaceId();

  return useMutation({
    mutationFn: (id: string) => {
      if (!wsId) throw new Error('No workspace selected');
      return userService(wsId).restore(id);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.all });
    },
  });
}

export function useAssignRoles() {
  const qc = useQueryClient();
  const wsId = useEffectiveWorkspaceId();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: AssignRolesRequest }) => {
      if (!wsId) throw new Error('No workspace selected');
      return userService(wsId).assignRoles(id, data);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: adminKeys.all });
    },
  });
}

/* ---------- Role hooks ---------- */

export function useRolesList() {
  return useQuery({
    queryKey: adminKeys.roles(),
    queryFn: () => roleService.list(),
  });
}

export function useRoleDetail(id: string | undefined) {
  return useQuery({
    queryKey: adminKeys.roleDetail(id ?? ''),
    queryFn: () => roleService.getById(id!),
    enabled: !!id,
  });
}

/* ---------- User History hooks ---------- */

export function useUserHistory(criteria: UserHistorySearchCriteria, page: number, size: number) {
  const wsId = useEffectiveWorkspaceId();

  return useQuery({
    queryKey: [...adminKeys.all, 'history', wsId, { ...criteria, page, size }],
    queryFn: () => userHistoryService(wsId).list(criteria, page, size),
    enabled: !!wsId,
  });
}

/* ---------- Department hooks ---------- */

export function useDepartmentsList() {
  const wsId = useEffectiveWorkspaceId();

  return useQuery({
    queryKey: [...adminKeys.all, 'departments', wsId] as const,
    queryFn: () => listDepartments(wsId),
    enabled: !!wsId,
  });
}

/* ---------- Team hooks ---------- */

export function useTeamsByDepartment(wsId: string, deptId: string | undefined) {
  return useQuery<TeamSummary[]>({
    queryKey: ['teams', wsId, deptId],
    queryFn: () => teamService(wsId).listByDepartment(deptId!),
    enabled: !!wsId && !!deptId,
  });
}

/* ---------- Permission hooks ---------- */

export function usePermissionsList() {
  return useQuery({
    queryKey: adminKeys.permissions(),
    queryFn: () => permissionService.list(),
  });
}
