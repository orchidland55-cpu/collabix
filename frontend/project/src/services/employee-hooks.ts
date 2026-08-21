import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { employeeService } from './employee-service';
import type { CreateEmployeeRequest, UpdateEmployeeRequest } from './employee-service';

const keys = {
  all: (wsId: string, deptId: string) => ['employees', wsId, deptId] as const,
  list: (wsId: string, deptId: string, page: number, size: number) => ['employees', wsId, deptId, page, size] as const,
  select: (wsId: string, deptId: string) => ['employees', wsId, deptId, 'select'] as const,
  detail: (wsId: string, deptId: string, id: string) => ['employees', wsId, deptId, id] as const,
  timeline: (wsId: string, deptId: string, id: string) => ['employees', 'timeline', wsId, deptId, id] as const,
  stats: (wsId: string, deptId: string) => ['employees', 'stats', wsId, deptId] as const,
};

export function useEmployeesList(wsId: string, deptId: string, page = 0, size = 10) {
  return useQuery({
    queryKey: keys.list(wsId, deptId, page, size),
    queryFn: () => employeeService.list(wsId, deptId, { page, size }),
    enabled: !!wsId && !!deptId,
  });
}

/** Loads all department employees for dropdowns (reviews, onboarding, etc.). */
export function useDepartmentEmployeesForSelect(wsId: string, deptId: string) {
  return useQuery({
    queryKey: keys.select(wsId, deptId),
    queryFn: () => employeeService.list(wsId, deptId, { page: 0, size: 500 }),
    enabled: !!wsId && !!deptId,
    staleTime: 0,
    refetchOnMount: 'always',
  });
}

export function useEmployeeDetail(wsId: string, deptId: string, employeeId: string | undefined) {
  return useQuery({ queryKey: keys.detail(wsId, deptId, employeeId ?? ''), queryFn: () => employeeService.getById(wsId, deptId, employeeId!), enabled: !!wsId && !!deptId && !!employeeId });
}

export function useEmployeeTimeline(wsId: string, deptId: string, employeeId: string | undefined) {
  return useQuery({ queryKey: keys.timeline(wsId, deptId, employeeId ?? ''), queryFn: () => employeeService.getTimeline(wsId, deptId, employeeId!), enabled: !!wsId && !!deptId && !!employeeId });
}

export function useEmployeeStats(wsId: string, deptId: string) {
  return useQuery({ queryKey: keys.stats(wsId, deptId), queryFn: () => employeeService.getStats(wsId, deptId), enabled: !!wsId && !!deptId });
}

export function useCreateEmployee(wsId: string, deptId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateEmployeeRequest) => employeeService.create(wsId, deptId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.all(wsId, deptId) });
      qc.invalidateQueries({ queryKey: keys.select(wsId, deptId) });
    },
  });
}

export function useUpdateEmployee(wsId: string, deptId: string, employeeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateEmployeeRequest) => employeeService.update(wsId, deptId, employeeId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.all(wsId, deptId) });
      qc.invalidateQueries({ queryKey: keys.select(wsId, deptId) });
      qc.invalidateQueries({ queryKey: keys.detail(wsId, deptId, employeeId) });
    },
  });
}

export function useDeleteEmployee(wsId: string, deptId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => employeeService.delete(wsId, deptId, id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.all(wsId, deptId) });
      qc.invalidateQueries({ queryKey: keys.select(wsId, deptId) });
    },
  });
}
