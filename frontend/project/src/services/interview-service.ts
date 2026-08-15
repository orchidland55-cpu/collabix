import { apiClient } from '../lib/api';
import type { PageResponse } from '../types/api';

export type InterviewType = 'HR' | 'TECHNICAL' | 'MANAGERIAL' | 'FINAL' | 'CUSTOM';
export type InterviewStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'RESCHEDULED' | 'NO_SHOW';
export type Recommendation = 'STRONG_HIRE' | 'HIRE' | 'NEUTRAL' | 'NO_HIRE' | 'STRONG_NO_HIRE';

export interface InterviewParticipantResponse {
  id: string;
  interviewId: string;
  userId: string;
  userFirstName: string;
  userLastName: string;
  userEmail: string;
  role: string;
}

export interface InterviewFeedbackResponse {
  id: string;
  interviewId: string;
  rating?: number;
  recommendation: Recommendation;
  notes?: string;
  submittedBy: string;
  submittedAt: string;
}

export interface InterviewResponse {
  id: string;
  candidateId: string;
  type: InterviewType;
  status: InterviewStatus;
  title?: string;
  position?: string;
  description?: string;
  scheduledDate?: string;
  startTime?: string;
  endTime?: string;
  location?: string;
  meetingLink?: string;
  notes?: string;
  archived: boolean;
  participants: InterviewParticipantResponse[];
  feedbacks: InterviewFeedbackResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateInterviewRequest {
  type: InterviewType;
  title?: string;
  position?: string;
  description?: string;
  scheduledDate?: string;
  startTime?: string;
  endTime?: string;
  location?: string;
  meetingLink?: string;
}

export interface UpdateInterviewRequest {
  type?: InterviewType;
  title?: string;
  position?: string;
  description?: string;
  scheduledDate?: string;
  startTime?: string;
  endTime?: string;
  location?: string;
  meetingLink?: string;
}

export interface AddParticipantRequest {
  userId: string;
  role?: string;
}

export interface UpdateInterviewNotesRequest {
  notes?: string;
}

export interface InterviewFeedbackRequest {
  rating?: number;
  recommendation: Recommendation;
  notes?: string;
}

export interface InterviewStatistics {
  interviewsToday: number;
  upcomingInterviews: number;
  completedInterviews: number;
  cancelledInterviews: number;
  averageRating: number;
  candidatesWaitingForInterview: number;
}

function candidateBase(wsId: string, deptId: string, candidateId: string) {
  return `/workspaces/${wsId}/departments/${deptId}/candidates/${candidateId}/interviews`;
}

function deptBase(wsId: string, deptId: string) {
  return `/workspaces/${wsId}/departments/${deptId}/interviews`;
}

export const interviewService = {
  /* --- candidate-scoped --- */
  listForCandidate: (wsId: string, deptId: string, candidateId: string) =>
    apiClient.get<PageResponse<InterviewResponse>>(`${candidateBase(wsId, deptId, candidateId)}`),

  getById: (wsId: string, deptId: string, candidateId: string, interviewId: string) =>
    apiClient.get<InterviewResponse>(`${candidateBase(wsId, deptId, candidateId)}/${interviewId}`),

  create: (wsId: string, deptId: string, candidateId: string, data: CreateInterviewRequest) =>
    apiClient.post<InterviewResponse>(`${candidateBase(wsId, deptId, candidateId)}`, data),

  update: (wsId: string, deptId: string, candidateId: string, interviewId: string, data: UpdateInterviewRequest) =>
    apiClient.put<InterviewResponse>(`${candidateBase(wsId, deptId, candidateId)}/${interviewId}`, data),

  updateNotes: (wsId: string, deptId: string, candidateId: string, interviewId: string, data: UpdateInterviewNotesRequest) =>
    apiClient.put<InterviewResponse>(`${candidateBase(wsId, deptId, candidateId)}/${interviewId}/notes`, data),

  cancel: (wsId: string, deptId: string, candidateId: string, interviewId: string) =>
    apiClient.post<InterviewResponse>(`${candidateBase(wsId, deptId, candidateId)}/${interviewId}/cancel`),

  complete: (wsId: string, deptId: string, candidateId: string, interviewId: string) =>
    apiClient.post<InterviewResponse>(`${candidateBase(wsId, deptId, candidateId)}/${interviewId}/complete`),

  delete: (wsId: string, deptId: string, candidateId: string, interviewId: string) =>
    apiClient.delete<void>(`${candidateBase(wsId, deptId, candidateId)}/${interviewId}`),

  addParticipant: (wsId: string, deptId: string, candidateId: string, interviewId: string, data: AddParticipantRequest) =>
    apiClient.post<InterviewParticipantResponse>(`${candidateBase(wsId, deptId, candidateId)}/${interviewId}/participants`, data),

  removeParticipant: (wsId: string, deptId: string, candidateId: string, interviewId: string, participantId: string) =>
    apiClient.delete<void>(`${candidateBase(wsId, deptId, candidateId)}/${interviewId}/participants/${participantId}`),

  listParticipants: (wsId: string, deptId: string, candidateId: string, interviewId: string) =>
    apiClient.get<InterviewParticipantResponse[]>(`${candidateBase(wsId, deptId, candidateId)}/${interviewId}/participants`),

  submitFeedback: (wsId: string, deptId: string, candidateId: string, interviewId: string, data: InterviewFeedbackRequest) =>
    apiClient.post<InterviewFeedbackResponse>(`${candidateBase(wsId, deptId, candidateId)}/${interviewId}/feedback`, data),

  listFeedback: (wsId: string, deptId: string, candidateId: string, interviewId: string) =>
    apiClient.get<InterviewFeedbackResponse[]>(`${candidateBase(wsId, deptId, candidateId)}/${interviewId}/feedback`),

  /* --- department-scoped calendar --- */
  today: (wsId: string, deptId: string) =>
    apiClient.get<InterviewResponse[]>(`${deptBase(wsId, deptId)}/today`),

  week: (wsId: string, deptId: string) =>
    apiClient.get<InterviewResponse[]>(`${deptBase(wsId, deptId)}/week`),

  upcoming: (wsId: string, deptId: string) =>
    apiClient.get<InterviewResponse[]>(`${deptBase(wsId, deptId)}/upcoming`),

  completed: (wsId: string, deptId: string) =>
    apiClient.get<InterviewResponse[]>(`${deptBase(wsId, deptId)}/Completed`),

  stats: (wsId: string, deptId: string) =>
    apiClient.get<InterviewStatistics>(`${deptBase(wsId, deptId)}/stats`),
};
