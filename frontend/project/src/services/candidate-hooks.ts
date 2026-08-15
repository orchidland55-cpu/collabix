import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { candidateService } from './candidate-service';
import type { CreateCandidateRequest, UpdateCandidateRequest, CandidateStatusChangeRequest, CreateRecruiterNoteRequest, CreateInterviewRequest } from './candidate-service';

const keys = {
  all: (wsId: string, deptId: string) => ['candidates', wsId, deptId] as const,
  detail: (wsId: string, deptId: string, id: string) => ['candidates', wsId, deptId, id] as const,
  timeline: (wsId: string, deptId: string, id: string) => ['candidates', 'timeline', wsId, deptId, id] as const,
  stats: (wsId: string, deptId: string) => ['candidates', 'stats', wsId, deptId] as const,
  notes: (wsId: string, deptId: string, id: string) => ['candidates', 'notes', wsId, deptId, id] as const,
  interviews: (wsId: string, deptId: string, id: string) => ['candidates', 'interviews', wsId, deptId, id] as const,
};

export function useCandidatesList(wsId: string, deptId: string, page = 0, size = 10) {
  return useQuery({ queryKey: [...keys.all(wsId, deptId), page], queryFn: () => candidateService.list(wsId, deptId, { page, size }), enabled: !!wsId && !!deptId });
}

export function useCandidateDetail(wsId: string, deptId: string, candidateId: string | undefined) {
  return useQuery({ queryKey: keys.detail(wsId, deptId, candidateId ?? ''), queryFn: () => candidateService.getById(wsId, deptId, candidateId!), enabled: !!wsId && !!deptId && !!candidateId });
}

export function useCandidateStats(wsId: string, deptId: string) {
  return useQuery({ queryKey: keys.stats(wsId, deptId), queryFn: () => candidateService.getStats(wsId, deptId), enabled: !!wsId && !!deptId });
}

export function useCandidateTimeline(wsId: string, deptId: string, candidateId: string | undefined) {
  return useQuery({ queryKey: keys.timeline(wsId, deptId, candidateId ?? ''), queryFn: () => candidateService.getTimeline(wsId, deptId, candidateId!), enabled: !!wsId && !!deptId && !!candidateId });
}

export function useCandidateNotes(wsId: string, deptId: string, candidateId: string | undefined) {
  return useQuery({ queryKey: keys.notes(wsId, deptId, candidateId ?? ''), queryFn: () => candidateService.getNotes(wsId, deptId, candidateId!), enabled: !!wsId && !!deptId && !!candidateId });
}

export function useCandidateInterviews(wsId: string, deptId: string, candidateId: string | undefined) {
  return useQuery({ queryKey: keys.interviews(wsId, deptId, candidateId ?? ''), queryFn: () => candidateService.getInterviews(wsId, deptId, candidateId!), enabled: !!wsId && !!deptId && !!candidateId });
}

export function useCreateCandidate(wsId: string, deptId: string) {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (data: CreateCandidateRequest) => candidateService.create(wsId, deptId, data), onSuccess: () => { qc.invalidateQueries({ queryKey: keys.all(wsId, deptId) }); qc.invalidateQueries({ queryKey: keys.stats(wsId, deptId) }); } });
}

export function useUpdateCandidate(wsId: string, deptId: string, candidateId: string) {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (data: UpdateCandidateRequest) => candidateService.update(wsId, deptId, candidateId, data), onSuccess: () => { qc.invalidateQueries({ queryKey: keys.all(wsId, deptId) }); qc.invalidateQueries({ queryKey: keys.detail(wsId, deptId, candidateId) }); } });
}

export function useDeleteCandidate(wsId: string, deptId: string) {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (id: string) => candidateService.delete(wsId, deptId, id), onSuccess: () => qc.invalidateQueries({ queryKey: keys.all(wsId, deptId) }) });
}

export function useChangeCandidateStatus(wsId: string, deptId: string) {
  const qc = useQueryClient();
  return useMutation({ mutationFn: ({ id, data }: { id: string; data: CandidateStatusChangeRequest }) => candidateService.changeStatus(wsId, deptId, id, data), onSuccess: () => { qc.invalidateQueries({ queryKey: keys.all(wsId, deptId) }); qc.invalidateQueries({ queryKey: keys.stats(wsId, deptId) }); } });
}

export function useCreateCandidateNote(wsId: string, deptId: string, candidateId: string) {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (data: CreateRecruiterNoteRequest) => candidateService.createNote(wsId, deptId, candidateId, data), onSuccess: () => qc.invalidateQueries({ queryKey: keys.notes(wsId, deptId, candidateId) }) });
}

export function useCreateInterview(wsId: string, deptId: string, candidateId: string) {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (data: CreateInterviewRequest) => candidateService.createInterview(wsId, deptId, candidateId, data), onSuccess: () => qc.invalidateQueries({ queryKey: keys.interviews(wsId, deptId, candidateId) }) });
}
