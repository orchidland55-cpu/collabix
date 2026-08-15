import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { interviewService } from './interview-service';
import type { CreateInterviewRequest, UpdateInterviewRequest, UpdateInterviewNotesRequest, AddParticipantRequest, InterviewFeedbackRequest } from './interview-service';

const keys = {
  candidateInterviews: (wsId: string, deptId: string, candidateId: string) => ['interviews', wsId, deptId, candidateId] as const,
  detail: (wsId: string, deptId: string, candidateId: string, id: string) => ['interviews', wsId, deptId, candidateId, id] as const,
  participants: (wsId: string, deptId: string, candidateId: string, id: string) => ['interviews', 'participants', wsId, deptId, candidateId, id] as const,
  feedback: (wsId: string, deptId: string, candidateId: string, id: string) => ['interviews', 'feedback', wsId, deptId, candidateId, id] as const,
  today: (wsId: string, deptId: string) => ['interviews', 'today', wsId, deptId] as const,
  week: (wsId: string, deptId: string) => ['interviews', 'week', wsId, deptId] as const,
  upcoming: (wsId: string, deptId: string) => ['interviews', 'upcoming', wsId, deptId] as const,
  completed: (wsId: string, deptId: string) => ['interviews', 'completed', wsId, deptId] as const,
  stats: (wsId: string, deptId: string) => ['interviews', 'stats', wsId, deptId] as const,
};

export function useCandidateInterviewsList(wsId: string, deptId: string, candidateId: string | undefined) {
  return useQuery({ queryKey: keys.candidateInterviews(wsId, deptId, candidateId ?? ''), queryFn: () => interviewService.listForCandidate(wsId, deptId, candidateId!), enabled: !!wsId && !!deptId && !!candidateId });
}

export function useInterviewDetail(wsId: string, deptId: string, candidateId: string, interviewId: string | undefined) {
  return useQuery({ queryKey: keys.detail(wsId, deptId, candidateId, interviewId ?? ''), queryFn: () => interviewService.getById(wsId, deptId, candidateId, interviewId!), enabled: !!wsId && !!deptId && !!interviewId });
}

export function useInterviewsToday(wsId: string, deptId: string) {
  return useQuery({ queryKey: keys.today(wsId, deptId), queryFn: () => interviewService.today(wsId, deptId), enabled: !!wsId && !!deptId });
}

export function useInterviewsWeek(wsId: string, deptId: string) {
  return useQuery({ queryKey: keys.week(wsId, deptId), queryFn: () => interviewService.week(wsId, deptId), enabled: !!wsId && !!deptId });
}

export function useInterviewsUpcoming(wsId: string, deptId: string) {
  return useQuery({ queryKey: keys.upcoming(wsId, deptId), queryFn: () => interviewService.upcoming(wsId, deptId), enabled: !!wsId && !!deptId });
}

export function useInterviewsCompleted(wsId: string, deptId: string) {
  return useQuery({ queryKey: keys.completed(wsId, deptId), queryFn: () => interviewService.completed(wsId, deptId), enabled: !!wsId && !!deptId });
}

export function useInterviewStats(wsId: string, deptId: string) {
  return useQuery({ queryKey: keys.stats(wsId, deptId), queryFn: () => interviewService.stats(wsId, deptId), enabled: !!wsId && !!deptId });
}

export function useInterviewParticipants(wsId: string, deptId: string, candidateId: string, interviewId: string | undefined) {
  return useQuery({ queryKey: keys.participants(wsId, deptId, candidateId, interviewId ?? ''), queryFn: () => interviewService.listParticipants(wsId, deptId, candidateId, interviewId!), enabled: !!wsId && !!deptId && !!interviewId });
}

export function useInterviewFeedback(wsId: string, deptId: string, candidateId: string, interviewId: string | undefined) {
  return useQuery({ queryKey: keys.feedback(wsId, deptId, candidateId, interviewId ?? ''), queryFn: () => interviewService.listFeedback(wsId, deptId, candidateId, interviewId!), enabled: !!wsId && !!deptId && !!interviewId });
}

export function useCreateInterview(wsId: string, deptId: string, candidateId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateInterviewRequest) => interviewService.create(wsId, deptId, candidateId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.candidateInterviews(wsId, deptId, candidateId) });
      qc.invalidateQueries({ queryKey: keys.upcoming(wsId, deptId) });
    },
  });
}

export function useUpdateInterview(wsId: string, deptId: string, candidateId: string, interviewId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateInterviewRequest) => interviewService.update(wsId, deptId, candidateId, interviewId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.candidateInterviews(wsId, deptId, candidateId) });
      qc.invalidateQueries({ queryKey: keys.detail(wsId, deptId, candidateId, interviewId) });
      qc.invalidateQueries({ queryKey: keys.upcoming(wsId, deptId) });
    },
  });
}

export function useUpdateInterviewNotes(wsId: string, deptId: string, candidateId: string, interviewId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateInterviewNotesRequest) => interviewService.updateNotes(wsId, deptId, candidateId, interviewId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.detail(wsId, deptId, candidateId, interviewId) });
      qc.invalidateQueries({ queryKey: keys.candidateInterviews(wsId, deptId, candidateId) });
    },
  });
}

export function useCancelInterview(wsId: string, deptId: string, candidateId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (interviewId: string) => interviewService.cancel(wsId, deptId, candidateId, interviewId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.candidateInterviews(wsId, deptId, candidateId) });
      qc.invalidateQueries({ queryKey: keys.upcoming(wsId, deptId) });
    },
  });
}

export function useCompleteInterview(wsId: string, deptId: string, candidateId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (interviewId: string) => interviewService.complete(wsId, deptId, candidateId, interviewId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.candidateInterviews(wsId, deptId, candidateId) });
      qc.invalidateQueries({ queryKey: keys.upcoming(wsId, deptId) });
    },
  });
}

export function useDeleteInterview(wsId: string, deptId: string, candidateId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (interviewId: string) => interviewService.delete(wsId, deptId, candidateId, interviewId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.candidateInterviews(wsId, deptId, candidateId) });
      qc.invalidateQueries({ queryKey: keys.upcoming(wsId, deptId) });
    },
  });
}

export function useAddInterviewParticipant(wsId: string, deptId: string, candidateId: string, interviewId: string) {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (data: AddParticipantRequest) => interviewService.addParticipant(wsId, deptId, candidateId, interviewId, data), onSuccess: () => qc.invalidateQueries({ queryKey: keys.participants(wsId, deptId, candidateId, interviewId) }) });
}

export function useRemoveInterviewParticipant(wsId: string, deptId: string, candidateId: string, interviewId: string) {
  const qc = useQueryClient();
  return useMutation({ mutationFn: (participantId: string) => interviewService.removeParticipant(wsId, deptId, candidateId, interviewId, participantId), onSuccess: () => qc.invalidateQueries({ queryKey: keys.participants(wsId, deptId, candidateId, interviewId) }) });
}

export function useSubmitInterviewFeedback(wsId: string, deptId: string, candidateId: string, interviewId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: InterviewFeedbackRequest) => interviewService.submitFeedback(wsId, deptId, candidateId, interviewId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: keys.feedback(wsId, deptId, candidateId, interviewId) });
      qc.invalidateQueries({ queryKey: keys.candidateInterviews(wsId, deptId, candidateId) });
    },
  });
}
