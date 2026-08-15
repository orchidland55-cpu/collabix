import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { employeeDocumentService } from './employee-document-service';

const keys = {
  employeeDocs: (wsId: string, deptId: string, employeeId: string) => ['employee-documents', wsId, deptId, employeeId] as const,
  detail: (wsId: string, deptId: string, employeeId: string, id: string) => ['employee-documents', wsId, deptId, employeeId, id] as const,
  stats: (wsId: string, deptId: string, employeeId: string) => ['employee-documents', 'stats', wsId, deptId, employeeId] as const,
  expiring: (wsId: string, deptId: string) => ['employee-documents', 'expiring', wsId, deptId] as const,
};

export function useEmployeeDocuments(wsId: string, deptId: string, employeeId: string | undefined) {
  return useQuery({ queryKey: keys.employeeDocs(wsId, deptId, employeeId ?? ''), queryFn: () => employeeDocumentService.list(wsId, deptId, employeeId!), enabled: !!wsId && !!deptId && !!employeeId });
}

export function useEmployeeDocumentDetail(wsId: string, deptId: string, employeeId: string, documentId: string | undefined) {
  return useQuery({ queryKey: keys.detail(wsId, deptId, employeeId, documentId ?? ''), queryFn: () => employeeDocumentService.getById(wsId, deptId, employeeId, documentId!), enabled: !!wsId && !!deptId && !!documentId });
}

export function useEmployeeDocumentStats(wsId: string, deptId: string, employeeId: string | undefined) {
  return useQuery({ queryKey: keys.stats(wsId, deptId, employeeId ?? ''), queryFn: () => employeeDocumentService.stats(wsId, deptId, employeeId!), enabled: !!wsId && !!deptId && !!employeeId });
}

export function useExpiringDocuments(wsId: string, deptId: string, withinDays = 30) {
  return useQuery({ queryKey: [...keys.expiring(wsId, deptId), withinDays], queryFn: () => employeeDocumentService.expiring(wsId, deptId, withinDays), enabled: !!wsId && !!deptId });
}

export function useUploadEmployeeDocument(wsId: string, deptId: string, employeeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: Parameters<typeof employeeDocumentService.upload>[3]) => employeeDocumentService.upload(wsId, deptId, employeeId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.employeeDocs(wsId, deptId, employeeId) });
      qc.invalidateQueries({ queryKey: keys.stats(wsId, deptId, employeeId) });
      qc.invalidateQueries({ queryKey: keys.expiring(wsId, deptId) });
    },
  });
}

export function useReplaceEmployeeDocument(wsId: string, deptId: string, employeeId: string, documentId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: Parameters<typeof employeeDocumentService.replace>[4]) => employeeDocumentService.replace(wsId, deptId, employeeId, documentId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.employeeDocs(wsId, deptId, employeeId) }),
  });
}

export function useVerifyEmployeeDocument(wsId: string, deptId: string, employeeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (documentId: string) => employeeDocumentService.verify(wsId, deptId, employeeId, documentId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.employeeDocs(wsId, deptId, employeeId) });
      qc.invalidateQueries({ queryKey: keys.stats(wsId, deptId, employeeId) });
    },
  });
}

export function useUnverifyEmployeeDocument(wsId: string, deptId: string, employeeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (documentId: string) => employeeDocumentService.unverify(wsId, deptId, employeeId, documentId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.employeeDocs(wsId, deptId, employeeId) });
      qc.invalidateQueries({ queryKey: keys.stats(wsId, deptId, employeeId) });
    },
  });
}

export function useDeleteEmployeeDocument(wsId: string, deptId: string, employeeId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (documentId: string) => employeeDocumentService.delete(wsId, deptId, employeeId, documentId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.employeeDocs(wsId, deptId, employeeId) });
      qc.invalidateQueries({ queryKey: keys.stats(wsId, deptId, employeeId) });
    },
  });
}
