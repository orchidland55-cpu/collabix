import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { AlertCircle, FileText } from 'lucide-react';
import { reportingAIService } from '../../../services/reporting-ai-service';
import { useWorkspaceId } from '../../../hooks/useWorkspaceId';
import { AIReportViewerHeader } from './AIReportViewerHeader';
import { AIReportViewerSummary } from './AIReportViewerSummary';
import { AIReportViewerInsights } from './AIReportViewerInsights';
import { AIReportViewerRecommendations } from './AIReportViewerRecommendations';
import { AIReportViewerCharts } from './AIReportViewerCharts';
import { AIReportViewerSources } from './AIReportViewerSources';
import { AIReportViewerActions } from './AIReportViewerActions';
import { AIReportViewerRelated } from './AIReportViewerRelated';
import { AIEmptyState } from '../AIEmptyState';
import { AILoadingHero } from '../AILoadingCard';

export function AIReportViewerPage() {
  const { reportId } = useParams<{ reportId: string }>();
  const wsId = useWorkspaceId();
  const [params] = useSearchParams();
  const workspaceId = wsId || params.get('ws') || '';

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['ai', 'report', workspaceId, reportId],
    queryFn: () => reportingAIService().getById(reportId!, workspaceId),
    enabled: !!workspaceId && !!reportId,
  });

  const [favorite, setFavorite] = useState(false);

  if (!workspaceId || !reportId) {
    return (
      <AIEmptyState
        icon={<FileText className="h-6 w-6" />}
        title="No report found"
        description="Select a workspace and open a report from history."
      />
    );
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6 animate-fade-in max-w-[1440px] mx-auto">
        <AILoadingHero />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="flex flex-col items-center justify-center py-20 animate-fade-in max-w-[1440px] mx-auto">
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-danger-50 text-danger-500 dark:bg-danger-500/10">
          <AlertCircle className="h-6 w-6" />
        </div>
        <h3 className="text-section font-semibold text-text-primary">Failed to load report</h3>
        <p className="mt-1 max-w-sm text-body text-text-tertiary text-center">
          This report may not exist or you may not have permission to view it.
        </p>
        <button
          type="button"
          onClick={() => refetch()}
          className="mt-5 rounded-lg bg-accent-600 px-4 py-2 text-body font-medium text-white hover:bg-accent-700 transition-colors"
        >
          Retry
        </button>
      </div>
    );
  }

  const insights = [data.majorHighlights, data.businessHealth, data.productivityReview].filter(Boolean) as string[];
  const recommendations = [data.recommendations, data.strategicPriorities].filter(Boolean) as string[];

  return (
    <div className="flex flex-col gap-8 animate-fade-in max-w-[1440px] mx-auto">
      <AIReportViewerHeader
        title={data.title}
        generatedDate={data.generationDate}
        workspace="Workspace"
        department={data.departmentId ?? 'Department'}
        category={data.reportType}
        status={data.approvalStatus}
        favorite={favorite}
        onToggleFavorite={() => setFavorite(!favorite)}
      />

      <AIReportViewerSummary summary={data.executiveSummary} />

      <div className="flex flex-col gap-6">
        <div>
          <h2 className="text-section font-semibold text-text-primary mb-4">Key Insights</h2>
          <AIReportViewerInsights insights={insights.length ? insights : ['No insights available.']} />
        </div>

        <div>
          <h2 className="text-section font-semibold text-text-primary mb-4">Recommendations</h2>
          <AIReportViewerRecommendations recommendations={recommendations.length ? recommendations : ['No recommendations available.']} />
        </div>

        <AIReportViewerCharts />
        <AIReportViewerSources sources={[]} />
        <AIReportViewerRelated reports={[]} />
      </div>

      <AIReportViewerActions favorite={favorite} onToggleFavorite={() => setFavorite(!favorite)} />
    </div>
  );
}
