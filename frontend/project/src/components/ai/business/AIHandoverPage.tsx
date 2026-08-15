import { useState } from 'react';
import { AIBusinessHeader } from './AIBusinessHeader';
import { AIBusinessContextPanel } from './AIBusinessContextPanel';
import { AIBusinessResultPanel } from './AIBusinessResultPanel';
import { AIBusinessFollowUp } from './AIBusinessFollowUp';
import { AIBusinessResources } from './AIBusinessResources';
import { AIBusinessEmptyState } from './AIBusinessEmptyState';
import { AIBusinessLoading } from './AIBusinessLoading';
import { AIBusinessErrorCard } from './AIBusinessErrorCard';
import { useAIGenerateHandover, useAccessibleHandoverJournals } from '../../../services/handover-hooks';
import type { HandoverAIResponse } from '../../../services/handover-service';
import { useAIPermissions } from '../../../hooks/useAIPermissions';
import { useAIScopeSelectors, type AIScopeSelection } from '../../../hooks/useAIScopeSelectors';
import {
  handoverFollowUps,
  handoverResources,
} from './AIBusinessTypes';

export function AIHandoverPage({
  workspaceId = '',
  departmentId = '',
  projectId = '',
}: {
  workspaceId?: string;
  departmentId?: string;
  projectId?: string;
}) {
  const [hasResult, setHasResult] = useState(false);
  const [resultData, setResultData] = useState<HandoverAIResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const generateMutation = useAIGenerateHandover(workspaceId, departmentId, projectId);
  const { canGenerateHandover, canReadHandover } = useAIPermissions();
  const { data: accessibleJournals, isLoading: journalsLoading } = useAccessibleHandoverJournals(
    canReadHandover && !canGenerateHandover ? workspaceId : undefined,
    { page: 0, size: 10 },
  );
  const scopeSelectors = useAIScopeSelectors(departmentId || undefined);

  async function handleAnalyze(selection: AIScopeSelection) {
    setError(null);
    try {
      const result = await generateMutation.mutateAsync({
        workspaceId,
        departmentId: selection.departmentId || departmentId,
        projectId: selection.projectId || projectId,
      });
      setResultData(result);
      setHasResult(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'AI generation failed');
    }
  }

  if (!canGenerateHandover) {
    if (!canReadHandover) {
      return (
        <div className="flex flex-col gap-6">
          <AIBusinessHeader module="handover" title="Handover AI" description="Handover information for your workspace." />
          <p className="text-caption text-text-tertiary text-center">You don&apos;t have permission to access handover information.</p>
        </div>
      );
    }

    const journals = accessibleJournals?.content ?? [];

    return (
      <div className="flex flex-col gap-6">
        <AIBusinessHeader
          module="handover"
          title="Handover AI"
          description="Read handover journals for projects you are authorized to access."
        />
        {journalsLoading && <AIBusinessLoading />}
        {!journalsLoading && journals.length === 0 && (
          <p className="text-caption text-text-tertiary text-center">No handover journals available in your authorized scope.</p>
        )}
        {!journalsLoading && journals.length > 0 && (
          <div className="grid gap-4">
            {journals.map((journal) => (
              <div key={journal.id} className="rounded-xl border border-border-subtle bg-surface p-4 space-y-2">
                <div className="flex items-center justify-between gap-2">
                  <p className="text-body font-medium text-text-primary">
                    {journal.journalDate}
                    {journal.shift ? ` · ${journal.shift}` : ''}
                  </p>
                  <span className="text-2xs text-text-tertiary">{journal.generationStatus}</span>
                </div>
                {journal.generatedSummary && (
                  <p className="text-caption text-text-secondary line-clamp-3">{journal.generatedSummary}</p>
                )}
                <div className="grid gap-1 text-2xs text-text-tertiary sm:grid-cols-3">
                  <span>Done: {journal.completedHandovers ?? 0}</span>
                  <span>Pending: {journal.pendingHandovers ?? 0}</span>
                  <span>Blocked: {journal.overdueHandovers ?? 0} overdue</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }

  if (generateMutation.isPending) return <AIBusinessLoading />;

  if (error) {
    return (
      <div className="flex flex-col gap-6">
        <AIBusinessHeader module="handover" title="Handover AI" description="Review handover journals, detect risks and ensure work continuity." />
        <AIBusinessErrorCard message={error} onRetry={() => setError(null)} onDismiss={() => setError(null)} />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <AIBusinessHeader module="handover" title="Handover AI" description="Review handover journals, detect risks and ensure work continuity." />

      <div className="flex flex-col lg:flex-row gap-5">
        <div className="w-full lg:w-72 shrink-0">
          <AIBusinessContextPanel
            scopeOptions={scopeSelectors.scopeOptions.filter((o) => o.value !== 'TEAM')}
            departments={scopeSelectors.departments}
            projects={scopeSelectors.projects}
            teams={[]}
            defaultScope={scopeSelectors.defaultScope === 'WORKSPACE' ? 'DEPARTMENT' : scopeSelectors.defaultScope}
            defaultDepartmentId={scopeSelectors.defaultDepartmentId}
            onAnalyze={(selection) => handleAnalyze(selection)}
            analyzeLabel="Review Handover"
            inputPlaceholder="Ask about handover details, risks or gaps..."
          />
        </div>
        <div className="flex-1 min-w-0 space-y-5">
          {hasResult && resultData ? (
            <>
              <AIBusinessResultPanel
                summary={resultData.executiveSummary}
                insights={[
                  resultData.completedWork ? `Completed: ${resultData.completedWork}` : 'No completed work recorded.',
                  resultData.pendingWork ? `Pending: ${resultData.pendingWork}` : 'No pending work recorded.',
                  resultData.criticalRisks ? `Risks: ${resultData.criticalRisks}` : 'No critical risks identified.',
                ]}
                recommendations={[resultData.recommendations]}
                keyPoints={[
                  `Priority Actions: ${resultData.priorityActions || 'None specified'}`,
                  `Work Continuity: ${resultData.workContinuity || 'Not specified'}`,
                  `Blocked Tasks: ${resultData.blockedTasks || 'None'}`,
                ]}
              />
              <AIBusinessFollowUp actions={handoverFollowUps} />
              <AIBusinessResources resources={handoverResources} />
            </>
          ) : (
            <AIBusinessEmptyState module="handover" onAction={() => handleAnalyze({ scope: 'DEPARTMENT', departmentId: scopeSelectors.defaultDepartmentId })} />
          )}
        </div>
      </div>
    </div>
  );
}
