import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { departmentService, listDepartments, getDepartmentById, createDepartment, updateDepartment, archiveDepartment, restoreDepartment, deleteDepartmentPermanently, type DepartmentDashboardResponse, type DepartmentSummary, type DepartmentResponse, type CreateDepartmentRequest, type UpdateDepartmentRequest } from './department-service';

const departmentKeys = {
  all: ['departments'] as const,
  list: (workspaceId: string, includeArchived: boolean) => ['departments', 'list', workspaceId, includeArchived] as const,
  detail: (workspaceId: string, departmentId: string) => ['departments', 'detail', workspaceId, departmentId] as const,
};

export function useDepartmentList(workspaceId: string | undefined, includeArchived = false) {
  return useQuery<DepartmentSummary[]>({
    queryKey: departmentKeys.list(workspaceId!, includeArchived),
    queryFn: () => listDepartments(workspaceId!, includeArchived),
    enabled: !!workspaceId,
  });
}

export function useDepartmentDetail(workspaceId: string | undefined, departmentId: string | undefined) {
  return useQuery<DepartmentResponse>({
    queryKey: departmentKeys.detail(workspaceId!, departmentId!),
    queryFn: () => getDepartmentById(workspaceId!, departmentId!),
    enabled: !!workspaceId && !!departmentId,
  });
}

export function useCreateDepartment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ workspaceId, data }: { workspaceId: string; data: CreateDepartmentRequest }) =>
      createDepartment(workspaceId, data),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: departmentKeys.list(variables.workspaceId, false) });
      qc.invalidateQueries({ queryKey: departmentKeys.list(variables.workspaceId, true) });
    },
  });
}

export function useUpdateDepartment(workspaceId: string | undefined, departmentId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateDepartmentRequest) => updateDepartment(workspaceId!, departmentId!, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: departmentKeys.detail(workspaceId!, departmentId!) });
      qc.invalidateQueries({ queryKey: departmentKeys.list(workspaceId!, false) });
      qc.invalidateQueries({ queryKey: departmentKeys.list(workspaceId!, true) });
    },
  });
}

export function useArchiveDepartment(workspaceId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ departmentId }: { departmentId: string }) => archiveDepartment(workspaceId!, departmentId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: departmentKeys.list(workspaceId!, false) });
      qc.invalidateQueries({ queryKey: departmentKeys.list(workspaceId!, true) });
      qc.invalidateQueries({ queryKey: departmentKeys.all });
    },
  });
}

export function useRestoreDepartment(workspaceId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ departmentId }: { departmentId: string }) => restoreDepartment(workspaceId!, departmentId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: departmentKeys.list(workspaceId!, false) });
      qc.invalidateQueries({ queryKey: departmentKeys.list(workspaceId!, true) });
      qc.invalidateQueries({ queryKey: departmentKeys.all });
    },
  });
}

export function useDeleteDepartmentPermanently(workspaceId: string | undefined) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ departmentId }: { departmentId: string }) => deleteDepartmentPermanently(workspaceId!, departmentId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: departmentKeys.list(workspaceId!, false) });
      qc.invalidateQueries({ queryKey: departmentKeys.list(workspaceId!, true) });
      qc.invalidateQueries({ queryKey: departmentKeys.all });
    },
  });
}

/* ============================================================
   Dashboard
============================================================ */
export function useDepartmentDashboard(
  workspaceId: string | undefined,
  departmentId: string | undefined,
) {
  return useQuery<DepartmentDashboardResponse>({
    queryKey: ['department', 'dashboard', workspaceId, departmentId],
    queryFn: async () => {
      const svc = departmentService(workspaceId!, departmentId!);
      const res = await svc.getDashboard();
      return res;
    },
    enabled: !!workspaceId && !!departmentId,
  });
}

/* ============================================================
   Sprint stats (Development)
============================================================ */
export function useSprintStats(
  workspaceId: string | undefined,
  departmentId: string | undefined,
) {
  return useQuery({
    queryKey: ['department', 'sprints', workspaceId, departmentId],
    queryFn: async () => {
      const svc = departmentService(workspaceId!, departmentId!);
      const res = await svc.getSprintStats();
      return res;
    },
    enabled: !!workspaceId && !!departmentId,
  });
}

/* ============================================================
   Campaign stats (Marketing)
============================================================ */
export function useCampaignStats(
  workspaceId: string | undefined,
  departmentId: string | undefined,
) {
  return useQuery({
    queryKey: ['department', 'campaigns', workspaceId, departmentId],
    queryFn: async () => {
      const svc = departmentService(workspaceId!, departmentId!);
      const res = await svc.getCampaignStats();
      return res;
    },
    enabled: !!workspaceId && !!departmentId,
  });
}

/* ============================================================
   Audit stats (Cybersecurity)
============================================================ */
export function useAuditStats(
  workspaceId: string | undefined,
  departmentId: string | undefined,
) {
  return useQuery({
    queryKey: ['department', 'audits', workspaceId, departmentId],
    queryFn: async () => {
      const svc = departmentService(workspaceId!, departmentId!);
      const res = await svc.getAuditStats();
      return res;
    },
    enabled: !!workspaceId && !!departmentId,
  });
}

/* ============================================================
   AI Model stats
============================================================ */
export function useModelStats(
  workspaceId: string | undefined,
  departmentId: string | undefined,
) {
  return useQuery({
    queryKey: ['department', 'models', workspaceId, departmentId],
    queryFn: async () => {
      const svc = departmentService(workspaceId!, departmentId!);
      const res = await svc.getModelStats();
      return res;
    },
    enabled: !!workspaceId && !!departmentId,
  });
}

/* ============================================================
   Workspace Analytics
============================================================ */
export function useWorkspaceAnalytics(
  workspaceId: string | undefined,
) {
  return useQuery({
    queryKey: ['workspace', 'analytics', workspaceId],
    queryFn: async () => {
      const svc = departmentService(workspaceId!, '');
      const res = await svc.getAnalytics();
      return res;
    },
    enabled: !!workspaceId,
  });
}
