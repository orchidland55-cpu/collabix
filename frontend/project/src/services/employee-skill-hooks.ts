import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { employeeSkillService } from './employee-skill-service';
import type { CreateEmployeeSkillRequest, UpdateEmployeeSkillRequest } from './employee-skill-service';

const keys = {
  all: (wsId: string, deptId: string, empId: string) => ['employee-skills', wsId, deptId, empId] as const,
  detail: (wsId: string, deptId: string, empId: string, skillId: string) => ['employee-skills', wsId, deptId, empId, skillId] as const,
  stats: (wsId: string, deptId: string) => ['employee-skills', 'stats', wsId, deptId] as const,
};

export function useEmployeeSkillsList(wsId: string, deptId: string, employeeId: string) {
  return useQuery({ queryKey: keys.all(wsId, deptId, employeeId), queryFn: () => employeeSkillService.list(wsId, deptId, employeeId), enabled: !!wsId && !!deptId && !!employeeId });
}

export function useEmployeeSkillStats(wsId: string, deptId: string) {
  return useQuery({ queryKey: keys.stats(wsId, deptId), queryFn: () => employeeSkillService.getStats(wsId, deptId), enabled: !!wsId && !!deptId });
}

export function useCreateEmployeeSkill(wsId: string, deptId: string, employeeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateEmployeeSkillRequest) => employeeSkillService.create(wsId, deptId, employeeId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.all(wsId, deptId, employeeId) });
      qc.invalidateQueries({ queryKey: keys.stats(wsId, deptId) });
    },
  });
}

export function useUpdateEmployeeSkill(wsId: string, deptId: string, employeeId: string, skillId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateEmployeeSkillRequest) => employeeSkillService.update(wsId, deptId, employeeId, skillId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.all(wsId, deptId, employeeId) });
      qc.invalidateQueries({ queryKey: keys.detail(wsId, deptId, employeeId, skillId) });
      qc.invalidateQueries({ queryKey: keys.stats(wsId, deptId) });
    },
  });
}

export function useDeleteEmployeeSkill(wsId: string, deptId: string, employeeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (skillId: string) => employeeSkillService.delete(wsId, deptId, employeeId, skillId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.all(wsId, deptId, employeeId) });
      qc.invalidateQueries({ queryKey: keys.stats(wsId, deptId) });
    },
  });
}

export function useVerifyEmployeeSkill(wsId: string, deptId: string, employeeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (skillId: string) => employeeSkillService.verify(wsId, deptId, employeeId, skillId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.all(wsId, deptId, employeeId) });
      qc.invalidateQueries({ queryKey: keys.stats(wsId, deptId) });
    },
  });
}

export function useUnverifyEmployeeSkill(wsId: string, deptId: string, employeeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (skillId: string) => employeeSkillService.unverify(wsId, deptId, employeeId, skillId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.all(wsId, deptId, employeeId) });
      qc.invalidateQueries({ queryKey: keys.stats(wsId, deptId) });
    },
  });
}
