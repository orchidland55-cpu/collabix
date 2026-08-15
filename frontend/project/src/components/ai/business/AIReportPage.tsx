import { useState } from 'react';
import { AIBusinessHeader } from './AIBusinessHeader';
import { AIBusinessContextPanel } from './AIBusinessContextPanel';
import { AIBusinessResultPanel } from './AIBusinessResultPanel';
import { AIBusinessFollowUp } from './AIBusinessFollowUp';
import { AIBusinessResources } from './AIBusinessResources';
import { AIBusinessEmptyState } from './AIBusinessEmptyState';
import { AIBusinessLoading } from './AIBusinessLoading';
import { AIBusinessErrorCard } from './AIBusinessErrorCard';
import { useAIPermissions } from '../../../hooks/useAIPermissions';
import { useAIGenerateReport } from '../../../services/reporting-ai-hooks';
import type { ReportingResponse } from '../../../services/reporting-ai-service';
import { useAIScopeSelectors, type AIScopeSelection } from '../../../hooks/useAIScopeSelectors';
import {
  reportFollowUps,
  reportResources,
} from './AIBusinessTypes';

export function AIReportPage({
  workspaceId = '',
  departmentId = '',
}: {
  workspaceId?: string;
  departmentId?: string;
}) {
  const [hasResult, setHasResult] = useState(false);
  const [resultData, setResultData] = useState<ReportingResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const generateMutation = useAIGenerateReport();
  const { canGenerateReports } = useAIPermissions();
  const scopeSelectors = useAIScopeSelectors(departmentId || undefined);

  async function handleGenerate(selection: AIScopeSelection, question: string) {
    setError(null);
    try {
      const title = question.trim() || 'Executive Report';
      const result = await generateMutation.mutateAsync({
        workspaceId,
        departmentId: selection.departmentId,
        projectId: selection.projectId,
        teamId: selection.teamId,
        scope: selection.scope,
        title,
        reportType: 'EXECUTIVE',
      });
      setResultData(result);
      setHasResult(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Report generation failed');
    }
  }

  if (!canGenerateReports) {
    return (
      <div className="flex flex-col gap-6">
        <AIBusinessHeader module="reports" title="Report AI" description="Read executive reports for your department." />
        <AIBusinessEmptyState module="reports" onAction={() => undefined} />
        <p className="text-caption text-text-tertiary text-center">Report generation is available to workspace admins and managers.</p>
      </div>
    );
  }

  if (generateMutation.isPending) return <AIBusinessLoading />;

  if (error) {
    return (
      <div className="flex flex-col gap-6">
        <AIBusinessHeader module="reports" title="Report AI" description="Generate professional executive reports and management summaries." />
        <AIBusinessErrorCard message={error} onRetry={() => setError(null)} onDismiss={() => setError(null)} />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <AIBusinessHeader module="reports" title="Report AI" description="Generate professional executive reports and management summaries." />

      <div className="flex flex-col lg:flex-row gap-5">
        <div className="w-full lg:w-72 shrink-0">
          <AIBusinessContextPanel
            scopeOptions={scopeSelectors.scopeOptions}
            departments={scopeSelectors.departments}
            projects={scopeSelectors.projects}
            teams={scopeSelectors.teams}
            defaultScope={scopeSelectors.defaultScope}
            defaultDepartmentId={scopeSelectors.defaultDepartmentId}
            onAnalyze={handleGenerate}
            analyzeLabel="Generate Report"
            inputPlaceholder="Describe the report you need..."
          />
        </div>
        <div className="flex-1 min-w-0 space-y-5">
          {hasResult && resultData ? (
            <>
              <AIBusinessResultPanel
                summary={resultData.executiveSummary}
                insights={[
                  resultData.majorHighlights || 'No highlights available.',
                  resultData.businessHealth || 'No health data available.',
                  resultData.productivityReview || 'No productivity review available.',
                ]}
                recommendations={[resultData.recommendations || 'No recommendations available.']}
                keyPoints={[
                  `Report: ${resultData.title}`,
                  `Version: ${resultData.reportVersion}`,
                  `Status: ${resultData.approvalStatus}`,
                  `Execution: ${resultData.executionTime}ms`,
                ]}
              />
              <AIBusinessFollowUp actions={reportFollowUps} />
              <AIBusinessResources resources={reportResources} />
            </>
          ) : (
            <AIBusinessEmptyState module="reports" onAction={() => handleGenerate({ scope: scopeSelectors.defaultScope, departmentId: scopeSelectors.defaultDepartmentId }, '')} />
          )}
        </div>
      </div>
    </div>
  );
}
