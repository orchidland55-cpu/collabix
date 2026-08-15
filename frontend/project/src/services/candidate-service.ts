import { apiClient } from '../lib/api';
import type { PageResponse } from '../types/api';

export interface CandidateResponse {
  id: string;
  departmentId: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  position: string;
  source: string;
  currentStatus: string;
  recruiterId?: string;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCandidateRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  position: string;
  source?: string;
}

export interface UpdateCandidateRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  position?: string;
  source?: string;
}

export interface CandidateStatusChangeRequest {
  newStatus: string;
  reason?: string;
}

export interface CandidateTimelineEntry {
  id: string;
  eventType: string;
  title: string;
  description?: string;
  occurredAt: string;
  actorId: string;
  actorName: string;
}

export interface CandidateStatistics {
  totalCandidates: number;
  hiredCount: number;
  rejectedCount: number;
  inProgressCount: number;
  candidatesPerStatus: Record<string, number>;
}

export interface RecruiterNoteResponse {
  id: string;
  candidateId: string;
  title: string;
  category: string;
  priority: string;
  content: string;
  visibility: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRecruiterNoteRequest {
  title: string;
  category: string;
  priority?: string;
  content: string;
  visibility: string;
}

export interface InterviewResponse {
  id: string;
  candidateId: string;
  type: string;
  status: string;
  title?: string;
  position?: string;
  description?: string;
  scheduledDate?: string;
  startTime?: string;
  endTime?: string;
  location?: string;
  meetingLink?: string;
  createdAt: string;
}

export interface CreateInterviewRequest {
  type: string;
  title?: string;
  position?: string;
  description?: string;
  scheduledDate?: string;
  startTime?: string;
  endTime?: string;
  location?: string;
  meetingLink?: string;
}

function base(wsId: string, deptId: string) {
  return `/workspaces/${wsId}/departments/${deptId}/candidates`;
}

function notesBase(wsId: string, deptId: string, candidateId: string) {
  return `/workspaces/${wsId}/departments/${deptId}/candidates/${candidateId}/notes`;
}

function interviewsBase(wsId: string, deptId: string, candidateId: string) {
  return `/workspaces/${wsId}/departments/${deptId}/candidates/${candidateId}/interviews`;
}

export const candidateService = {
  list: (wsId: string, deptId: string, params?: { page?: number; size?: number; keyword?: string; status?: string }) =>
    apiClient.get<PageResponse<CandidateResponse>>(`${base(wsId, deptId)}`, { params }),

  getById: (wsId: string, deptId: string, candidateId: string) =>
    apiClient.get<CandidateResponse>(`${base(wsId, deptId)}/${candidateId}`),

  create: (wsId: string, deptId: string, data: CreateCandidateRequest) =>
    apiClient.post<CandidateResponse>(`${base(wsId, deptId)}`, data),

  update: (wsId: string, deptId: string, candidateId: string, data: UpdateCandidateRequest) =>
    apiClient.put<CandidateResponse>(`${base(wsId, deptId)}/${candidateId}`, data),

  delete: (wsId: string, deptId: string, candidateId: string) =>
    apiClient.delete<void>(`${base(wsId, deptId)}/${candidateId}`),

  changeStatus: (wsId: string, deptId: string, candidateId: string, data: CandidateStatusChangeRequest) =>
    apiClient.put<CandidateResponse>(`${base(wsId, deptId)}/${candidateId}/status`, data),

  getTimeline: (wsId: string, deptId: string, candidateId: string) =>
    apiClient.get<CandidateTimelineEntry[]>(`${base(wsId, deptId)}/${candidateId}/timeline`),

  getStats: (wsId: string, deptId: string) =>
    apiClient.get<CandidateStatistics>(`${base(wsId, deptId)}/stats`),

  getNotes: (wsId: string, deptId: string, candidateId: string) =>
    apiClient.get<PageResponse<RecruiterNoteResponse>>(`${notesBase(wsId, deptId, candidateId)}`),

  createNote: (wsId: string, deptId: string, candidateId: string, data: CreateRecruiterNoteRequest) =>
    apiClient.post<RecruiterNoteResponse>(`${notesBase(wsId, deptId, candidateId)}`, data),

  deleteNote: (wsId: string, deptId: string, candidateId: string, noteId: string) =>
    apiClient.delete<void>(`${notesBase(wsId, deptId, candidateId)}/${noteId}`),

  getInterviews: (wsId: string, deptId: string, candidateId: string) =>
    apiClient.get<PageResponse<InterviewResponse>>(`${interviewsBase(wsId, deptId, candidateId)}`),

  createInterview: (wsId: string, deptId: string, candidateId: string, data: CreateInterviewRequest) =>
    apiClient.post<InterviewResponse>(`${interviewsBase(wsId, deptId, candidateId)}`, data),
};
