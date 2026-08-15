import { apiClient } from '../lib/api';

export interface AIPromptResponse {
  id: string;
  code: string;
  name: string;
  category: 'ANALYTICS' | 'HANDOVER' | 'KNOWLEDGE' | 'GENERAL';
  promptTemplate: string;
  active: boolean;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export function promptAIService() {
  const base = '/ai/prompts';
  return {
    listActive: () => apiClient.get<AIPromptResponse[]>(base),
    getById: (id: string) => apiClient.get<AIPromptResponse>(`${base}/${id}`),
    getByCode: (code: string) => apiClient.get<AIPromptResponse>(`${base}/code/${encodeURIComponent(code)}`),
  };
}
