import { useState } from 'react';
import { AIBusinessHeader } from './AIBusinessHeader';
import { AIBusinessContextPanel } from './AIBusinessContextPanel';
import { AIBusinessResultPanel } from './AIBusinessResultPanel';
import { AIBusinessFollowUp } from './AIBusinessFollowUp';
import { AIBusinessResources } from './AIBusinessResources';
import { AIBusinessEmptyState } from './AIBusinessEmptyState';
import { AIBusinessLoading } from './AIBusinessLoading';
import { AIBusinessErrorCard } from './AIBusinessErrorCard';
import { useAIAskQuestion } from '../../../services/knowledge-ai-hooks';
import type { KnowledgeAIResponse } from '../../../services/knowledge-ai-service';
import { useAIPermissions } from '../../../hooks/useAIPermissions';
import { useAIScopeSelectors, type AIScopeSelection } from '../../../hooks/useAIScopeSelectors';
import {
  knowledgeFollowUps,
  knowledgeResources,
} from './AIBusinessTypes';

export function AIKnowledgePage({
  workspaceId = '',
  departmentId = '',
}: {
  workspaceId?: string;
  departmentId?: string;
}) {
  const [hasResult, setHasResult] = useState(false);
  const [resultData, setResultData] = useState<KnowledgeAIResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const askMutation = useAIAskQuestion();
  const { canUseKnowledgeAI, isAdmin } = useAIPermissions();
  const scopeSelectors = useAIScopeSelectors(departmentId || undefined);

  async function handleSearch(_selection: AIScopeSelection, question: string) {
    setError(null);
    const effectiveQuestion = question.trim() || 'Summarize key knowledge documents in this workspace.';
    try {
      const deptId = _selection.departmentId || scopeSelectors.defaultDepartmentId || departmentId;
      const result = await askMutation.mutateAsync({
        workspaceId,
        departmentId: deptId,
        projectId: _selection.projectId,
        question: effectiveQuestion,
      });
      setResultData(result);
      setHasResult(true);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Knowledge search failed');
    }
  }

  if (!canUseKnowledgeAI) {
    return (
      <div className="flex flex-col gap-6">
        <AIBusinessHeader module="knowledge" title="Knowledge AI" description="Knowledge search is not available for your role." />
      </div>
    );
  }

  if (askMutation.isPending) return <AIBusinessLoading />;

  if (error) {
    return (
      <div className="flex flex-col gap-6">
        <AIBusinessHeader module="knowledge" title="Knowledge AI" description="Search, explain and explore your company knowledge base." />
        <AIBusinessErrorCard message={error} onRetry={() => setError(null)} onDismiss={() => setError(null)} />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <AIBusinessHeader module="knowledge" title="Knowledge AI" description="Search, explain and explore your company knowledge base." />

      <div className="flex flex-col lg:flex-row gap-5">
        <div className="w-full lg:w-72 shrink-0">
          <AIBusinessContextPanel
            scopeOptions={scopeSelectors.scopeOptions.filter((o) => o.value !== 'TEAM' && o.value !== 'PROJECT')}
            departments={scopeSelectors.departments}
            projects={[]}
            teams={[]}
            defaultScope={isAdmin ? 'WORKSPACE' : 'DEPARTMENT'}
            defaultDepartmentId={scopeSelectors.defaultDepartmentId}
            onAnalyze={handleSearch}
            analyzeLabel="Search Knowledge"
            inputPlaceholder="Ask about policies, procedures or documentation..."
            showScopeSelectors={scopeSelectors.scopeOptions.length > 0}
          />
        </div>
        <div className="flex-1 min-w-0 space-y-5">
          {hasResult && resultData ? (
            <>
              <AIBusinessResultPanel
                summary={resultData.answer}
                insights={resultData.sources.map((s) => `${s.title} (${s.type})`)}
                recommendations={resultData.suggestedRelatedDocuments}
                keyPoints={[
                  `Confidence: ${resultData.confidence}`,
                  `Sources: ${resultData.sources.length} documents found`,
                  `Execution: ${resultData.executionTime}ms`,
                  resultData.missingInformation || 'No missing information',
                ]}
              />
              <AIBusinessFollowUp actions={knowledgeFollowUps} />
              <AIBusinessResources resources={knowledgeResources} />
            </>
          ) : (
            <AIBusinessEmptyState module="knowledge" onAction={() => handleSearch({ scope: 'DEPARTMENT', departmentId: scopeSelectors.defaultDepartmentId }, '')} />
          )}
        </div>
      </div>
    </div>
  );
}
