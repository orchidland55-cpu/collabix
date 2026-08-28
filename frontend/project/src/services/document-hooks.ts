import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { documentService } from './document-service';
import type { CreateDocumentRequest, UpdateDocumentRequest } from '../pages/knowledge/types/document-types';
import { api } from '../lib/api';

const docKeys = {
  all: (wsId: string, deptId: string, projId: string) =>
    ['documents', wsId, deptId, projId] as const,
  detail: (wsId: string, deptId: string, projId: string, docId: string) =>
    ['documents', wsId, deptId, projId, docId] as const,
  workspace: (wsId: string) =>
    ['documents', 'workspace', wsId] as const,
};

export function useDocumentsList(wsId: string, deptId: string, projId: string) {
  return useQuery({
    queryKey: docKeys.all(wsId, deptId, projId),
    queryFn: () => documentService.list(wsId, deptId, projId),
    enabled: !!wsId && !!deptId && !!projId,
  });
}

export function useWorkspaceDocuments(wsId: string) {
  return useQuery({
    queryKey: docKeys.workspace(wsId),
    queryFn: () => documentService.listByWorkspace(wsId),
    enabled: !!wsId,
  });
}

export function useDocumentDetail(wsId: string, deptId: string, projId: string, docId: string | undefined) {
  return useQuery({
    queryKey: docKeys.detail(wsId, deptId, projId, docId ?? ''),
    queryFn: () => documentService.getById(wsId, deptId, projId, docId!),
    enabled: !!wsId && !!deptId && !!projId && !!docId,
  });
}

export function useCreateDocument(wsId: string, deptId: string, projId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateDocumentRequest) =>
      documentService.create(wsId, deptId, projId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: docKeys.all(wsId, deptId, projId) });
      qc.invalidateQueries({ queryKey: docKeys.workspace(wsId) });
    },
  });
}

export function useUploadDocument(wsId: string, deptId?: string, projId?: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ file, taskId, title, description, category, tags, departmentId, projectId }: {
      file: File;
      taskId?: string;
      title?: string;
      description?: string;
      category?: string;
      tags?: string;
      departmentId?: string;
      projectId?: string;
    }) => {
      const effectiveDeptId = departmentId ?? deptId ?? '';
      const effectiveProjId = projectId ?? projId ?? '';
      return documentService.upload(wsId, effectiveDeptId, effectiveProjId, file, taskId, title, description, category, tags);
    },
    onSuccess: (_data, variables) => {
      const effectiveDeptId = variables.departmentId ?? deptId ?? '';
      const effectiveProjId = variables.projectId ?? projId ?? '';
      if (effectiveDeptId && effectiveProjId) {
        qc.invalidateQueries({ queryKey: docKeys.all(wsId, effectiveDeptId, effectiveProjId) });
      }
      qc.invalidateQueries({ queryKey: docKeys.workspace(wsId) });
    },
  });
}

export function useUpdateDocument(wsId: string, deptId: string, projId: string, docId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateDocumentRequest) =>
      documentService.update(wsId, deptId, projId, docId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: docKeys.all(wsId, deptId, projId) });
      qc.invalidateQueries({ queryKey: docKeys.detail(wsId, deptId, projId, docId) });
    },
  });
}

export function useDeleteDocument(wsId: string, deptId: string, projId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (docId: string) =>
      documentService.delete(wsId, deptId, projId, docId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: docKeys.all(wsId, deptId, projId) });
      qc.invalidateQueries({ queryKey: docKeys.workspace(wsId) });
    },
  });
}

export function useArchiveDocument(wsId: string, deptId: string, projId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (docId: string) =>
      documentService.archive(wsId, deptId, projId, docId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: docKeys.all(wsId, deptId, projId) });
      qc.invalidateQueries({ queryKey: docKeys.workspace(wsId) });
    },
  });
}

export function useRestoreDocument(wsId: string, deptId: string, projId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (docId: string) =>
      documentService.restore(wsId, deptId, projId, docId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: docKeys.all(wsId, deptId, projId) });
      qc.invalidateQueries({ queryKey: docKeys.workspace(wsId) });
    },
  });
}

export function useSubmitForApproval(wsId: string, deptId: string, projId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (docId: string) =>
      documentService.submitForApproval(wsId, deptId, projId, docId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: docKeys.all(wsId, deptId, projId) });
      qc.invalidateQueries({ queryKey: docKeys.workspace(wsId) });
    },
  });
}

export function useApproveDocument(wsId: string, deptId: string, projId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (docId: string) =>
      documentService.approve(wsId, deptId, projId, docId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: docKeys.all(wsId, deptId, projId) });
      qc.invalidateQueries({ queryKey: docKeys.workspace(wsId) });
    },
  });
}

export function useRejectDocument(wsId: string, deptId: string, projId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (docId: string) =>
      documentService.reject(wsId, deptId, projId, docId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: docKeys.all(wsId, deptId, projId) });
      qc.invalidateQueries({ queryKey: docKeys.workspace(wsId) });
    },
  });
}

export function useDocumentFile(wsId: string, deptId: string, projId: string, docId: string | undefined, enabled = true) {
  return useQuery({
    queryKey: ['document-file', wsId, deptId, projId, docId ?? ''],
    queryFn: async () => {
      const apiUrl = documentService.viewPath(wsId, deptId, projId, docId!);
      const fullUrl = documentService.view(wsId, deptId, projId, docId!);
      const response = await api.get<Blob>(apiUrl, { responseType: 'blob' });
      return { blob: response.data as Blob, url: fullUrl };
    },
    enabled: !!wsId && !!deptId && !!projId && !!docId && enabled,
    staleTime: Infinity,
    gcTime: 5 * 60 * 1000,
  });
}
