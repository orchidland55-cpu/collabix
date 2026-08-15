import { apiClient } from '../lib/api';
import type { PageResponse } from '../types/api';

export interface ReportingGenerateRequest {
  workspaceId: string;
  departmentId?: string;
  projectId?: string;
  teamId?: string;
  scope?: 'WORKSPACE' | 'DEPARTMENT' | 'PROJECT' | 'TEAM';
  title: string;
  reportType: 'EXECUTIVE' | 'WEEKLY' | 'MONTHLY' | 'DAILY' | 'DEPARTMENT' | 'WORKSPACE' | 'PROJECT' | 'CUSTOM';
  periodStart?: string;
  periodEnd?: string;
}

export interface ReportingEditRequest {
  workspaceId: string;
  departmentId: string;
  projectId?: string;
  title?: string;
  executiveSummary?: string;
  majorHighlights?: string;
  businessHealth?: string;
  productivityReview?: string;
  criticalRisks?: string;
  achievements?: string;
  challenges?: string;
  recommendations?: string;
  strategicPriorities?: string;
  nextActions?: string;
  finalReport?: string;
}

export interface ReportingResponse {
  reportId: string;
  workspaceId: string;
  departmentId: string;
  projectId?: string;
  title: string;
  reportType: string;
  periodStart?: string;
  periodEnd?: string;
  reportVersion: number;
  executiveSummary: string;
  majorHighlights: string;
  businessHealth: string;
  productivityReview: string;
  criticalRisks: string;
  achievements: string;
  challenges: string;
  recommendations: string;
  strategicPriorities: string;
  nextActions: string;
  finalReport: string;
  generationStatus: string;
  approvalStatus: string;
  generationDate: string;
  generatedBy: string;
  executionTime: number;
  createdAt: string;
  updatedAt: string;
}

export function reportingAIService() {
  const base = '/reports/ai';
  return {
    generate: (data: ReportingGenerateRequest) =>
      apiClient.post<ReportingResponse>(`${base}/generate`, data),

    regenerate: (reportId: string, data: ReportingGenerateRequest) =>
      apiClient.post<ReportingResponse>(`${base}/regenerate/${reportId}`, data),

    edit: (reportId: string, data: ReportingEditRequest) =>
      apiClient.put<ReportingResponse>(`${base}/${reportId}`, data),

    approve: (reportId: string) =>
      apiClient.post<ReportingResponse>(`${base}/${reportId}/approve`),

    reject: (reportId: string) =>
      apiClient.post<ReportingResponse>(`${base}/${reportId}/reject`),

    getHistory: (workspaceId: string, page?: number, size?: number) => {
      const params: Record<string, unknown> = { workspaceId };
      if (page != null) params.page = page;
      if (size != null) params.size = size;
      return apiClient.get<PageResponse<ReportingResponse>>(`${base}/history`, { params });
    },

    getById: (reportId: string, workspaceId: string) =>
      apiClient.get<ReportingResponse>(`${base}/${reportId}`, { params: { workspaceId } }),
  };
}
