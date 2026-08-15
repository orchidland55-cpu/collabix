import { useState } from 'react';
import { AIBusinessHeader } from './AIBusinessHeader';
import { AIBusinessContextPanel } from './AIBusinessContextPanel';
import { AIBusinessResultPanel } from './AIBusinessResultPanel';
import { AIBusinessFollowUp } from './AIBusinessFollowUp';
import { AIBusinessResources } from './AIBusinessResources';
import { AIBusinessEmptyState } from './AIBusinessEmptyState';
import { AIBusinessLoading } from './AIBusinessLoading';
import { AIBusinessErrorCard } from './AIBusinessErrorCard';
import { useAIGenerateAnalytics } from '../../../services/analytics-ai-hooks';
import type { AnalyticsAIResponse } from '../../../services/analytics-ai-service';
import { useAIPermissions } from '../../../hooks/useAIPermissions';
import { useAIScopeSelectors, type AIScopeSelection } from '../../../hooks/useAIScopeSelectors';
import {
  analyticsFollowUps,
  analyticsResources,
} from './AIBusinessTypes';

export function AIAnalyticsPage({
  workspaceId = '',
  departmentId = '',
}: {
  workspaceId?: string;
  departmentId?: string;
}) {
  const [hasResult, setHasResult] = useState(false);
  const [resultData, setResultData] = useState<AnalyticsAIResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const generateMutation = useAIGenerateAnalytics();
  const { canGenerateAnalytics } = useAIPermissions();
  const scopeSelectors = useAIScopeSelectors(departmentId || undefined);

  async function handleAnalyze(selection: AIScopeSelection, question: string) {
    setError(null);
    try {
      const result = await generateMutation.mutateAsync({
        workspaceId,
        departmentId: selection.departmentId,
        projectId: selection.projectId,
        teamId: selection.teamId,
        scope: selection.scope,
      });
      setResultData(result);
      setHasResult(true);
      if (question && !result.executiveSummary?.includes(question)) {
        // Question is advisory; backend uses collected metrics as primary context.
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'AI generation failed');
    }
  }

  if (!canGenerateAnalytics) {
    return (
      <div className="flex flex-col gap-6">
        <AIBusinessHeader module="analytics" title="Analytics AI" description="View analytics insights for your authorized scope." />
        <p className="text-caption text-text-tertiary text-center">Analytics generation is available to workspace admins and managers.</p>
      </div>
    );
  }

  if (generateMutation.isPending) return <AIBusinessLoading />;

  if (error) {
    return (
      <div className="flex flex-col gap-6">
        <AIBusinessHeader module="analytics" title="Analytics AI" description="Analyze business metrics, detect trends and generate performance insights." />
        <AIBusinessErrorCard message={error} onRetry={() => setError(null)} onDismiss={() => setError(null)} />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <AIBusinessHeader module="analytics" title="Analytics AI" description="Analyze business metrics, detect trends and generate performance insights." />

      <div className="flex flex-col lg:flex-row gap-5">
        <div className="w-full lg:w-72 shrink-0">
          <AIBusinessContextPanel
            scopeOptions={scopeSelectors.scopeOptions}
            departments={scopeSelectors.departments}
            projects={scopeSelectors.projects}
            teams={scopeSelectors.teams}
            defaultScope={scopeSelectors.defaultScope}
            defaultDepartmentId={scopeSelectors.defaultDepartmentId}
            onAnalyze={handleAnalyze}
            analyzeLabel="Analyze"
            inputPlaceholder="Ask about KPIs, trends or performance..."
          />
        </div>
        <div className="flex-1 min-w-0 space-y-5">
          {hasResult && resultData ? (
            <>
              <AIBusinessResultPanel
                summary={resultData.executiveSummary}
                insights={[
                  resultData.kpiHighlights || 'No KPI highlights available.',
                  resultData.trendsSummary || 'No trends available.',
                  resultData.riskAssessment || 'No risks identified.',
                ]}
                recommendations={[resultData.recommendations]}
                keyPoints={[
                  `Report: ${resultData.reportId}`,
                  `Status: ${resultData.generationStatus}`,
                  `Execution: ${resultData.executionTime}ms`,
                ]}
              />
              <AIBusinessFollowUp actions={analyticsFollowUps} />
              <AIBusinessResources resources={analyticsResources} />
            </>
          ) : (
            <AIBusinessEmptyState module="analytics" onAction={() => handleAnalyze({ scope: scopeSelectors.defaultScope, departmentId: scopeSelectors.defaultDepartmentId }, '')} />
          )}
        </div>
      </div>
    </div>
  );
}
