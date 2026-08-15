import { apiClient } from '../lib/api';

export interface AnalyticsAIGenerateRequest {
  workspaceId: string;
  departmentId?: string;
  projectId?: string;
  teamId?: string;
  scope?: 'WORKSPACE' | 'DEPARTMENT' | 'PROJECT' | 'TEAM';
  startDate?: string;
  endDate?: string;
}

export interface AnalyticsAIEditRequest {
  workspaceId: string;
  departmentId: string;
  projectId?: string;
  executiveSummary?: string;
  kpiHighlights?: string;
  trendsSummary?: string;
  riskAssessment?: string;
  recommendations?: string;
  detailedReport?: string;
}

export interface AnalyticsAIResponse {
  reportId: string;
  workspaceId: string;
  departmentId: string;
  projectId?: string;
  reportDate: string;
  timeRangeStart?: string;
  timeRangeEnd?: string;
  executiveSummary: string;
  kpiHighlights: string;
  trendsSummary: string;
  riskAssessment: string;
  recommendations: string;
  detailedReport: string;
  generationStatus: 'PENDING' | 'GENERATED' | 'FAILED';
  generationDate: string;
  generatedBy: string;
  executionTime: number;
  createdAt: string;
  updatedAt: string;
}

export function analyticsAIService() {
  const base = '/analytics/ai';
  return {
    generate: (data: AnalyticsAIGenerateRequest) =>
      apiClient.post<AnalyticsAIResponse>(`${base}/generate`, data),

    regenerate: (reportId: string, data: AnalyticsAIGenerateRequest) =>
      apiClient.post<AnalyticsAIResponse>(`${base}/regenerate/${reportId}`, data),

    edit: (reportId: string, data: AnalyticsAIEditRequest) =>
      apiClient.put<AnalyticsAIResponse>(`${base}/${reportId}`, data),

    approve: (reportId: string) =>
      apiClient.post<AnalyticsAIResponse>(`${base}/${reportId}/approve`),

    reject: (reportId: string) =>
      apiClient.post<AnalyticsAIResponse>(`${base}/${reportId}/reject`),
  };
}
