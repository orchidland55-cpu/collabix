import { useEffectiveWorkspaceId } from '../hooks/useEffectiveWorkspaceId';
import { useAIPermissions } from '../hooks/useAIPermissions';
import { useAIScopeSelectors } from '../hooks/useAIScopeSelectors';
import { reportingAIService } from './reporting-ai-service';
import { analyticsAIService } from './analytics-ai-service';
import type { AIPromptResponse } from './prompt-ai-service';

export interface PromptExecutionRequest {
  promptId: string;
  promptCode: string;
  promptCategory: 'ANALYTICS' | 'REPORTS' | 'KNOWLEDGE' | 'HANDOVER' | 'GENERAL';
  workspaceId: string;
  departmentId?: string;
  projectId?: string;
  teamId?: string;
  scope?: 'WORKSPACE' | 'DEPARTMENT' | 'PROJECT' | 'TEAM';
  variables?: Record<string, string>;
  question?: string;
}

export interface PromptExecutionResult {
  success: boolean;
  resultType: 'report' | 'analytics' | 'knowledge' | 'handover' | 'conversation';
  resultId?: string;
  title?: string;
  content?: string;
  error?: string;
}

export function promptExecutionService() {
  return {
    async execute(request: PromptExecutionRequest): Promise<PromptExecutionResult> {
      const { promptCategory, workspaceId, departmentId, projectId, teamId, scope, variables, question } = request;

      try {
        switch (promptCategory) {
          case 'ANALYTICS': {
            const analyticsService = analyticsAIService();
            const result = await analyticsService.generate({
              workspaceId,
              departmentId,
              projectId,
              teamId,
              scope: scope || 'DEPARTMENT',
              startDate: variables?.startDate,
              endDate: variables?.endDate,
            });
            return {
              success: true,
              resultType: 'analytics',
              resultId: result.reportId,
              title: `Analytics Report`,
              content: result.executiveSummary,
            };
          }

          case 'REPORTS': {
            const reportService = reportingAIService();
            const result = await reportService.generate({
              workspaceId,
              departmentId,
              projectId,
              teamId,
              scope: scope || 'DEPARTMENT',
              title: variables?.title || question || 'Executive Report',
              reportType: 'EXECUTIVE',
              periodStart: variables?.periodStart,
              periodEnd: variables?.periodEnd,
            });
            return {
              success: true,
              resultType: 'report',
              resultId: result.reportId,
              title: result.title,
              content: result.finalReport || result.executiveSummary,
            };
          }

          case 'KNOWLEDGE': {
            // Knowledge AI uses a different endpoint - ask a question
            // For now, we'll trigger a knowledge search
            return {
              success: false,
              resultType: 'knowledge',
              error: 'Knowledge AI prompt execution not yet implemented. Use Knowledge AI module directly.',
            };
          }

          case 'HANDOVER': {
            // Handover AI requires date and shift
            return {
              success: false,
              resultType: 'handover',
              error: 'Handover AI prompt execution not yet implemented. Use Handover AI module directly.',
            };
          }

          default: {
            return {
              success: false,
              resultType: 'conversation',
              error: `Unknown prompt category: ${promptCategory}`,
            };
          }
        }
      } catch (error) {
        return {
          success: false,
          resultType: 'conversation',
          error: error instanceof Error ? error.message : 'Prompt execution failed',
        };
      }
    },

    getExecutionEndpoint(promptCategory: string): string {
      switch (promptCategory) {
        case 'ANALYTICS':
          return '/api/analytics/ai/generate';
        case 'REPORTS':
          return '/api/reports/ai/generate';
        case 'KNOWLEDGE':
          return '/api/knowledge/ai/ask';
        case 'HANDOVER':
          return '/api/handover/ai/generate';
        default:
          return '/api/ai/conversations';
      }
    },

    getRequiredVariables(promptCategory: string, promptCode: string): string[] {
      const baseVars = ['workspaceId'];
      switch (promptCategory) {
        case 'ANALYTICS':
          return [...baseVars, 'departmentId', 'startDate', 'endDate'];
        case 'REPORTS':
          return [...baseVars, 'departmentId', 'title', 'periodStart', 'periodEnd'];
        case 'HANDOVER':
          return [...baseVars, 'departmentId', 'projectId', 'date', 'shift'];
        case 'KNOWLEDGE':
          return [...baseVars, 'question'];
        default:
          return baseVars;
      }
    },
  };
}

export function usePromptExecution() {
  const workspaceId = useEffectiveWorkspaceId();
  const { departmentId: userDepartmentId, canGenerateReports, canGenerateAnalytics } = useAIPermissions();
  const scopeSelectors = useAIScopeSelectors(userDepartmentId || undefined);

  const executePrompt = async (
    prompt: AIPromptResponse,
    variables: Record<string, string>,
    question?: string
  ): Promise<PromptExecutionResult> => {
    if (!workspaceId) {
      return { success: false, resultType: 'conversation', error: 'No workspace selected' };
    }

    const scope = (variables.scope as 'WORKSPACE' | 'DEPARTMENT' | 'PROJECT' | 'TEAM' | undefined) || scopeSelectors.defaultScope;
    const departmentId = variables.departmentId || scopeSelectors.defaultDepartmentId || userDepartmentId;

    const request: PromptExecutionRequest = {
      promptId: prompt.id,
      promptCode: prompt.code,
      promptCategory: prompt.category,
      workspaceId,
      departmentId,
      projectId: variables.projectId,
      teamId: variables.teamId,
      scope,
      variables,
      question,
    };

    return promptExecutionService().execute(request);
  };

  return { executePrompt };
}