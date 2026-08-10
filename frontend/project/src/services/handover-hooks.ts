import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  handoverEntryService,
  handoverJournalService,
  handoverJournalAccessService,
  handoverAIService,
  type CreateHandoverEntryRequest,
  type UpdateHandoverEntryRequest,
  type HandoverAIGenerateRequest,
  type HandoverAIEditRequest,
  type HandoverEntryResponse,
  type HandoverJournalResponse,
  type HandoverCommentResponse,
  type HandoverAttachmentResponse,
  type HandoverTimelineEventResponse,
  type CreateHandoverCommentRequest,
  type CreateHandoverAttachmentRequest,
  type HandoverStatusUpdateRequest,
} from './handover-service';
import type { PageResponse } from '../types/api';

/* ---------- Query keys ---------- */

const handoverKeys = {
  entries: {
    all: (wsId: string) => ['handover', 'entries', wsId] as const,
    detail: (wsId: string, id: string) => ['handover', 'entries', wsId, id] as const,
    inbox: (wsId: string) => ['handover', 'inbox', wsId] as const,
    sent: (wsId: string) => ['handover', 'sent', wsId] as const,
    myEntries: (wsId: string) => ['handover', 'my-entries', wsId] as const,
    comments: (wsId: string, id: string) => ['handover', 'comments', wsId, id] as const,
    attachments: (wsId: string, id: string) => ['handover', 'attachments', wsId, id] as const,
    timeline: (wsId: string, id: string) => ['handover', 'timeline', wsId, id] as const,
  },
  journals: {
    all: (wsId: string, deptId: string, projId: string) =>
      ['handover', 'journals', wsId, deptId, projId] as const,
    detail: (wsId: string, deptId: string, projId: string, id: string) =>
      ['handover', 'journals', wsId, deptId, projId, id] as const,
    access: (wsId: string) => ['handover', 'journals', 'accessible', wsId] as const,
    accessDetail: (wsId: string, id: string) => ['handover', 'journals', 'accessible', wsId, id] as const,
  },
};

/* ========== Handover Entry Hooks ========== */

export function useHandoverEntries(
  wsId: string,
  params?: { status?: string; priority?: string; projectId?: string; page?: number; size?: number },
) {
  const svc = handoverEntryService(wsId);

  return useQuery<PageResponse<HandoverEntryResponse>>({
    queryKey: [...handoverKeys.entries.all(wsId), params],
    queryFn: () => svc.list(params),
    enabled: !!wsId,
  });
}

export function useHandoverEntry(wsId: string, entryId: string) {
  const svc = handoverEntryService(wsId);

  return useQuery<HandoverEntryResponse>({
    queryKey: handoverKeys.entries.detail(wsId, entryId),
    queryFn: () => svc.getById(entryId),
    enabled: !!wsId && !!entryId,
  });
}

export function useCreateHandoverEntry(wsId: string) {
  const qc = useQueryClient();
  const svc = handoverEntryService(wsId);

  return useMutation({
    mutationFn: (data: CreateHandoverEntryRequest) => svc.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: handoverKeys.entries.all(wsId) });
      qc.invalidateQueries({ queryKey: handoverKeys.entries.sent(wsId) });
    },
  });
}

export function useUpdateHandoverEntry(wsId: string, entryId: string) {
  const qc = useQueryClient();
  const svc = handoverEntryService(wsId);

  return useMutation({
    mutationFn: (data: UpdateHandoverEntryRequest) => svc.update(entryId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: handoverKeys.entries.all(wsId) });
      qc.invalidateQueries({ queryKey: handoverKeys.entries.detail(wsId, entryId) });
    },
  });
}

export function useDeleteHandoverEntry(wsId: string) {
  const qc = useQueryClient();
  const svc = handoverEntryService(wsId);

  return useMutation({
    mutationFn: (entryId: string) => svc.delete(entryId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: handoverKeys.entries.all(wsId) });
      qc.invalidateQueries({ queryKey: handoverKeys.entries.sent(wsId) });
    },
  });
}

/* ========== Inbox / Sent ========== */

export function useHandoverInbox(wsId: string | undefined, page?: number, size?: number) {
  const svc = handoverEntryService(wsId ?? '');

  return useQuery<PageResponse<HandoverEntryResponse>>({
    queryKey: handoverKeys.entries.inbox(wsId ?? ''),
    queryFn: () => svc.inbox(page, size),
    enabled: !!wsId,
  });
}

export function useHandoverSent(wsId: string | undefined, page?: number, size?: number) {
  const svc = handoverEntryService(wsId ?? '');

  return useQuery<PageResponse<HandoverEntryResponse>>({
    queryKey: handoverKeys.entries.sent(wsId ?? ''),
    queryFn: () => svc.sent(page, size),
    enabled: !!wsId,
  });
}

export function useMyHandoverEntries(
  wsId: string | undefined,
  params?: { status?: string; shift?: string; entryDate?: string; search?: string; page?: number; size?: number },
) {
  const svc = handoverEntryService(wsId ?? '');

  return useQuery<PageResponse<HandoverEntryResponse>>({
    queryKey: [...handoverKeys.entries.myEntries(wsId ?? ''), params],
    queryFn: () => svc.myEntries(params),
    enabled: !!wsId,
  });
}

/* ========== Status transition mutations ========== */

function useEntryAction(wsId: string, action: 'send' | 'submit' | 'accept' | 'reject' | 'complete' | 'archive') {
  const qc = useQueryClient();
  const svc = handoverEntryService(wsId);

  return useMutation({
    mutationFn: ({ entryId, data }: { entryId: string; data?: HandoverStatusUpdateRequest }) =>
      svc[action](entryId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: handoverKeys.entries.all(wsId) });
      qc.invalidateQueries({ queryKey: handoverKeys.entries.inbox(wsId) });
      qc.invalidateQueries({ queryKey: handoverKeys.entries.sent(wsId) });
      qc.invalidateQueries({ queryKey: handoverKeys.entries.myEntries(wsId) });
    },
  });
}

export function useSendHandover(wsId?: string) {
  return useEntryAction(wsId ?? '', 'send');
}

export function useSubmitHandover(wsId?: string) {
  return useEntryAction(wsId ?? '', 'submit');
}

export function useAcceptHandover(wsId?: string) {
  return useEntryAction(wsId ?? '', 'accept');
}

export function useRejectHandover(wsId?: string) {
  return useEntryAction(wsId ?? '', 'reject');
}

export function useCompleteHandover(wsId?: string) {
  return useEntryAction(wsId ?? '', 'complete');
}

export function useArchiveHandover(wsId?: string) {
  return useEntryAction(wsId ?? '', 'archive');
}

/* ========== Comments ========== */

export function useHandoverComments(wsId: string | undefined, entryId: string | undefined) {
  const svc = handoverEntryService(wsId ?? '');

  return useQuery<HandoverCommentResponse[]>({
    queryKey: handoverKeys.entries.comments(wsId ?? '', entryId ?? ''),
    queryFn: () => svc.comments(entryId!),
    enabled: !!wsId && !!entryId,
  });
}

export function useAddHandoverComment(wsId?: string, entryId?: string) {
  const qc = useQueryClient();
  const svc = handoverEntryService(wsId ?? '');

  return useMutation({
    mutationFn: (data: CreateHandoverCommentRequest) => {
      if (!entryId) throw new Error('entryId is required');
      return svc.addComment(entryId, data);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: handoverKeys.entries.comments(wsId ?? '', entryId ?? '') });
      qc.invalidateQueries({ queryKey: handoverKeys.entries.timeline(wsId ?? '', entryId ?? '') });
    },
  });
}

export function useDeleteHandoverComment(wsId?: string, entryId?: string) {
  const qc = useQueryClient();
  const svc = handoverEntryService(wsId ?? '');

  return useMutation({
    mutationFn: (commentId: string) => {
      if (!entryId) throw new Error('entryId is required');
      return svc.deleteComment(entryId, commentId);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: handoverKeys.entries.comments(wsId ?? '', entryId ?? '') });
    },
  });
}

/* ========== Attachments ========== */

export function useHandoverAttachments(wsId: string | undefined, entryId: string | undefined) {
  const svc = handoverEntryService(wsId ?? '');

  return useQuery<HandoverAttachmentResponse[]>({
    queryKey: handoverKeys.entries.attachments(wsId ?? '', entryId ?? ''),
    queryFn: () => svc.attachments(entryId!),
    enabled: !!wsId && !!entryId,
  });
}

export function useAddHandoverAttachment(wsId?: string, entryId?: string) {
  const qc = useQueryClient();
  const svc = handoverEntryService(wsId ?? '');

  return useMutation({
    mutationFn: (data: CreateHandoverAttachmentRequest) => {
      if (!entryId) throw new Error('entryId is required');
      return svc.addAttachment(entryId, data);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: handoverKeys.entries.attachments(wsId ?? '', entryId ?? '') });
      qc.invalidateQueries({ queryKey: handoverKeys.entries.timeline(wsId ?? '', entryId ?? '') });
    },
  });
}

export function useDeleteHandoverAttachment(wsId?: string, entryId?: string) {
  const qc = useQueryClient();
  const svc = handoverEntryService(wsId ?? '');

  return useMutation({
    mutationFn: (attachmentId: string) => {
      if (!entryId) throw new Error('entryId is required');
      return svc.deleteAttachment(entryId, attachmentId);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: handoverKeys.entries.attachments(wsId ?? '', entryId ?? '') });
      qc.invalidateQueries({ queryKey: handoverKeys.entries.timeline(wsId ?? '', entryId ?? '') });
    },
  });
}

/* ========== Timeline ========== */

export function useHandoverTimeline(wsId: string | undefined, entryId: string | undefined) {
  const svc = handoverEntryService(wsId ?? '');

  return useQuery<HandoverTimelineEventResponse[]>({
    queryKey: handoverKeys.entries.timeline(wsId ?? '', entryId ?? ''),
    queryFn: () => svc.timeline(entryId!),
    enabled: !!wsId && !!entryId,
  });
}

/* ========== Handover Journal Hooks ========== */

export function useHandoverJournals(
  wsId: string,
  deptId?: string,
  projId?: string,
  page?: number,
  size?: number,
) {
  const svc = handoverJournalService(wsId ?? '', deptId ?? '', projId ?? '');

  return useQuery<PageResponse<HandoverJournalResponse>>({
    queryKey: handoverKeys.journals.all(wsId, deptId ?? '', projId ?? ''),
    queryFn: () => svc.list(page, size),
    enabled: !!wsId && !!deptId && !!projId,
  });
}

export function useHandoverJournal(
  wsId: string,
  deptId?: string,
  projId?: string,
  journalId?: string,
) {
  const svc = handoverJournalService(wsId ?? '', deptId ?? '', projId ?? '');

  return useQuery<HandoverJournalResponse>({
    queryKey: handoverKeys.journals.detail(wsId, deptId ?? '', projId ?? '', journalId ?? ''),
    queryFn: () => svc.getById(journalId!),
    enabled: !!wsId && !!deptId && !!projId && !!journalId,
  });
}

export function useGenerateHandoverJournal(wsId: string, deptId?: string, projId?: string) {
  const qc = useQueryClient();
  const svc = handoverJournalService(wsId ?? '', deptId ?? '', projId ?? '');

  return useMutation({
    mutationFn: () => {
      if (!wsId || !deptId || !projId) throw new Error('workspaceId, departmentId, and projectId are required');
      return svc.generate();
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: handoverKeys.journals.all(wsId, deptId ?? '', projId ?? '') });
    },
  });
}

export function useRegenerateHandoverJournal(
  wsId: string,
  deptId?: string,
  projId?: string,
  journalId?: string,
) {
  const qc = useQueryClient();
  const svc = handoverJournalService(wsId ?? '', deptId ?? '', projId ?? '');

  return useMutation({
    mutationFn: () => {
      if (!journalId || !wsId || !deptId || !projId) throw new Error('journalId, workspaceId, departmentId, and projectId are required');
      return svc.regenerate(journalId);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: handoverKeys.journals.all(wsId, deptId ?? '', projId ?? '') });
      qc.invalidateQueries({ queryKey: handoverKeys.journals.detail(wsId, deptId ?? '', projId ?? '', journalId ?? '') });
    },
  });
}

export function useDeleteHandoverJournal(wsId: string, deptId?: string, projId?: string) {
  const qc = useQueryClient();
  const svc = handoverJournalService(wsId ?? '', deptId ?? '', projId ?? '');

  return useMutation({
    mutationFn: (journalId: string) => svc.delete(journalId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: handoverKeys.journals.all(wsId, deptId ?? '', projId ?? '') });
    },
  });
}

/* ========== Accessible (department-scoped) Journal Hooks ========== */

export interface AccessibleJournalParams {
  departmentId?: string;
  projectId?: string;
  shift?: string;
  date?: string;
  page?: number;
  size?: number;
}

export function useAccessibleHandoverJournals(wsId: string | undefined, params?: AccessibleJournalParams) {
  const svc = handoverJournalAccessService(wsId ?? '');

  return useQuery<PageResponse<HandoverJournalResponse>>({
    queryKey: [...handoverKeys.journals.access(wsId ?? ''), params] as const,
    queryFn: () => svc.list(params),
    enabled: !!wsId,
  });
}

export function useAccessibleHandoverJournal(wsId: string | undefined, journalId: string | undefined) {
  const svc = handoverJournalAccessService(wsId ?? '');

  return useQuery<HandoverJournalResponse>({
    queryKey: handoverKeys.journals.accessDetail(wsId ?? '', journalId ?? ''),
    queryFn: () => svc.getById(journalId!),
    enabled: !!wsId && !!journalId,
  });
}

/* ========== AI Handover Hooks ========== */

function invalidateJournals(qc: ReturnType<typeof useQueryClient>, wsId?: string, deptId?: string, projId?: string, journalId?: string) {
  if (wsId) {
    qc.invalidateQueries({ queryKey: handoverKeys.journals.access(wsId) });
    if (journalId) {
      qc.invalidateQueries({ queryKey: handoverKeys.journals.accessDetail(wsId, journalId) });
    }
  }
  if (wsId && deptId && projId) {
    qc.invalidateQueries({ queryKey: handoverKeys.journals.all(wsId, deptId, projId) });
    if (journalId) {
      qc.invalidateQueries({ queryKey: handoverKeys.journals.detail(wsId, deptId, projId, journalId) });
    }
  } else if (wsId) {
    qc.invalidateQueries({ queryKey: ['handover', 'journals'] });
  }
}

export function useAIGenerateHandover(wsId?: string, deptId?: string, projId?: string) {
  const qc = useQueryClient();
  const svc = handoverAIService();

  return useMutation({
    mutationFn: (data: HandoverAIGenerateRequest) => svc.generate(data),
    onSuccess: () => {
      invalidateJournals(qc, wsId, deptId, projId);
    },
  });
}

export function useAIRegenerateHandover(wsId?: string, deptId?: string, projId?: string) {
  const qc = useQueryClient();
  const svc = handoverAIService();

  return useMutation({
    mutationFn: ({ journalId, data }: { journalId: string; data: HandoverAIGenerateRequest }) =>
      svc.regenerate(journalId, data),
    onSuccess: (_result, { journalId }) => {
      invalidateJournals(qc, wsId, deptId, projId, journalId);
    },
  });
}

export function useAIEditHandover(wsId?: string, deptId?: string, projId?: string) {
  const qc = useQueryClient();
  const svc = handoverAIService();

  return useMutation({
    mutationFn: ({ journalId, data }: { journalId: string; data: HandoverAIEditRequest }) =>
      svc.edit(journalId, data),
    onSuccess: (_result, { journalId }) => {
      invalidateJournals(qc, wsId, deptId, projId, journalId);
    },
  });
}

export function useAIApproveHandover(wsId?: string, deptId?: string, projId?: string) {
  const qc = useQueryClient();
  const svc = handoverAIService();

  return useMutation({
    mutationFn: ({ journalId, data }: { journalId: string; data: HandoverAIGenerateRequest }) =>
      svc.approve(journalId, data),
    onSuccess: (_result, { journalId }) => {
      invalidateJournals(qc, wsId, deptId, projId, journalId);
    },
  });
}

export function useAIRejectHandover(wsId?: string, deptId?: string, projId?: string) {
  const qc = useQueryClient();
  const svc = handoverAIService();

  return useMutation({
    mutationFn: ({ journalId, data }: { journalId: string; data: HandoverAIGenerateRequest }) =>
      svc.reject(journalId, data),
    onSuccess: (_result, { journalId }) => {
      invalidateJournals(qc, wsId, deptId, projId, journalId);
    },
  });
}
