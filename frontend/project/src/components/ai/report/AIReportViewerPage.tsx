import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { AlertCircle, FileText, Award, AlertTriangle, Lightbulb, Target, ListChecks, TrendingUp } from 'lucide-react';
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
import { MarkdownRenderer } from '../MarkdownRenderer';
import type { Insight, Recommendation } from './AIReportViewerTypes';
import { AIEmptyState } from '../AIEmptyState';
import { AILoadingHero } from '../AILoadingCard';

interface ReportSection {
  id: string;
  icon: React.ReactNode;
  title: string;
  content: string;
  priority?: 'high' | 'medium' | 'low';
}

function ReportSectionCard({ section }: { section: ReportSection }) {
  if (!section.content || section.content === 'Not available.' || section.content === 'No data available.') return null;
  return (
    <div className="rounded-xl border border-border-subtle bg-elevated dark:bg-surface p-6 hover:shadow-cx-sm hover:-translate-y-0.5 transition-all duration-150">
      <div className="flex items-start gap-4">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-accent-50 text-accent-600 dark:bg-accent-100/10 dark:text-accent-300">
          {section.icon}
        </span>
        <div className="flex-1 min-w-0">
          <h3 className="text-body font-semibold text-text-primary">{section.title}</h3>
          <MarkdownRenderer content={section.content} className="mt-2 text-body text-text-secondary" />
        </div>
      </div>
    </div>
  );
}

function ReportDetailSection({ title, icon, content, children }: { title: string; icon: React.ReactNode; content?: string; children?: React.ReactNode }) {
  if (!content && !children) return null;
  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 mb-4">
        <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent-50 text-accent-600 dark:bg-accent-100/10 dark:text-accent-300">
          {icon}
        </span>
        <h2 className="text-section font-semibold text-text-primary">{title}</h2>
      </div>
      {content && <MarkdownRenderer content={content} />}
      {children}
    </div>
  );
}

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

  // Build structured sections
  const sections: ReportSection[] = [
    { id: 'major-highlights', icon: <TrendingUp className="h-5 w-5" />, title: 'Major Highlights', content: data.majorHighlights || 'No major highlights available.', priority: 'high' },
    { id: 'business-health', icon: <Lightbulb className="h-5 w-5" />, title: 'Business Health Assessment', content: data.businessHealth || 'Business health assessment not available.', priority: 'medium' },
    { id: 'productivity-review', icon: <TrendingUp className="h-5 w-5" />, title: 'Productivity Review', content: data.productivityReview || 'Productivity review not available.', priority: 'medium' },
    { id: 'achievements', icon: <Award className="h-5 w-5" />, title: 'Achievements', content: data.achievements || 'No achievements recorded.', priority: 'high' },
    { id: 'challenges', icon: <AlertTriangle className="h-5 w-5" />, title: 'Challenges', content: data.challenges || 'No challenges recorded.', priority: 'medium' },
    { id: 'strategic-priorities', icon: <Target className="h-5 w-5" />, title: 'Strategic Priorities', content: data.strategicPriorities || 'No strategic priorities defined.', priority: 'high' },
    { id: 'next-actions', icon: <ListChecks className="h-5 w-5" />, title: 'Next Actions', content: data.nextActions || 'No next actions specified.', priority: 'high' },
  ];

  const allInsights: Insight[] = [
    { id: 'major-highlights', icon: 'trending-up', title: 'Major Highlights', description: data.majorHighlights || 'No major highlights available.', priority: 'high' },
    { id: 'business-health', icon: 'bar-chart', title: 'Business Health', description: data.businessHealth || 'Business health assessment not available.', priority: 'medium' },
    { id: 'productivity-review', icon: 'users', title: 'Productivity Review', description: data.productivityReview || 'Productivity review not available.', priority: 'low' },
    { id: 'achievements', icon: 'check-circle', title: 'Achievements', description: data.achievements || 'No achievements recorded.', priority: 'high' },
    { id: 'challenges', icon: 'alert-triangle', title: 'Challenges', description: data.challenges || 'No challenges recorded.', priority: 'medium' },
  ];
  const insights = allInsights.filter((i) => i.description && i.description !== 'Not available.' && i.description !== 'No data available.');

  const allRecommendations: Recommendation[] = [
    { id: 'recommendations', title: 'Recommendations', description: data.recommendations || 'No recommendations available.', businessImpact: 'High', priority: 'high', suggestedAction: data.nextActions || '' },
    { id: 'strategic-priorities', title: 'Strategic Priorities', description: data.strategicPriorities || 'No strategic priorities defined.', businessImpact: 'High', priority: 'medium', suggestedAction: '' },
  ];
  const recommendations = allRecommendations.filter((r) => r.description && r.description !== 'Not available.' && r.description !== 'No data available.');

  return (
    <div className="flex flex-col gap-8 animate-fade-in max-w-[1440px] mx-auto">
      <AIReportViewerHeader
        title={data.title}
        generatedDate={data.generationDate}
        workspace="Workspace"
        department={data.departmentId ?? 'Department'}
        category={data.reportType}
        status={data.approvalStatus === 'APPROVED' ? 'completed' : 'draft'}
        favorite={favorite}
        onToggleFavorite={() => setFavorite(!favorite)}
      />

      <AIReportViewerSummary summary={data.executiveSummary} />

      <div className="flex flex-col gap-6">
        <ReportDetailSection
          title="Key Insights"
          icon={<TrendingUp className="h-5 w-5" />}
        >
          <AIReportViewerInsights insights={insights} />
        </ReportDetailSection>

        <ReportDetailSection
          title="Recommendations"
          icon={<Target className="h-5 w-5" />}
        >
          <AIReportViewerRecommendations recommendations={recommendations} />
        </ReportDetailSection>

        <ReportDetailSection
          title="Critical Risks & Issues"
          icon={<AlertTriangle className="h-5 w-5" />}
          content={data.criticalRisks}
        />

        <div className="grid gap-4 md:grid-cols-2">
          <ReportSectionCard section={sections.find(s => s.id === 'major-highlights')!} />
          <ReportSectionCard section={sections.find(s => s.id === 'business-health')!} />
          <ReportSectionCard section={sections.find(s => s.id === 'productivity-review')!} />
          <ReportSectionCard section={sections.find(s => s.id === 'achievements')!} />
          <ReportSectionCard section={sections.find(s => s.id === 'challenges')!} />
          <ReportSectionCard section={sections.find(s => s.id === 'strategic-priorities')!} />
          <ReportSectionCard section={sections.find(s => s.id === 'next-actions')!} />
        </div>

        {data.finalReport && data.finalReport !== data.executiveSummary && (
          <ReportDetailSection
            title="Full Report"
            icon={<FileText className="h-5 w-5" />}
            content={data.finalReport}
          />
        )}

        <AIReportViewerCharts />
        <AIReportViewerSources sources={[]} />
        <AIReportViewerRelated reports={[]} />
      </div>

      <AIReportViewerActions favorite={favorite} onToggleFavorite={() => setFavorite(!favorite)} reportData={data} />
    </div>
  );
}
