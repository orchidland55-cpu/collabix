import { AlertCircle, ArrowRight } from 'lucide-react';
import { Badge } from '../../ui/Badge';
import { MarkdownRenderer } from '../MarkdownRenderer';
import { type Recommendation } from './AIReportViewerTypes';

interface AIReportViewerRecommendationsProps {
  recommendations: Recommendation[];
}

const priorityConfig = {
  critical: { label: 'Critical', tone: 'danger' as const },
  high: { label: 'High', tone: 'warning' as const },
  medium: { label: 'Medium', tone: 'info' as const },
  low: { label: 'Low', tone: 'neutral' as const },
};

export function AIReportViewerRecommendations({ recommendations }: AIReportViewerRecommendationsProps) {
  if (recommendations.length === 0) return null;
  return (
    <div className="space-y-3">
      {recommendations.map((rec) => {
        const pri = priorityConfig[rec.priority];
        return (
          <div
            key={rec.id}
            className="rounded-xl border border-border-subtle bg-elevated dark:bg-surface p-5 hover:shadow-cx-sm transition-all duration-150"
          >
            <div className="flex items-start gap-3">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-danger-50 text-danger-500 dark:bg-danger-500/10">
                <AlertCircle className="h-4 w-4" />
              </span>
              <div className="flex-1 min-w-0 space-y-2">
                <div className="flex items-center gap-2">
                  <p className="text-body font-semibold text-text-primary">{rec.title}</p>
                  <Badge variant="soft" tone={pri.tone}>{pri.label}</Badge>
                </div>
                <MarkdownRenderer content={rec.description} className="text-caption" />
                <div className="flex flex-col gap-1 pt-1 text-caption">
                  <p className="text-text-tertiary">
                    <span className="font-medium text-text-primary">Business Impact:</span> {rec.businessImpact}
                  </p>
                  <p className="flex items-center gap-1.5 text-accent-600 dark:text-accent-400">
                    <ArrowRight className="h-3.5 w-3.5" />
                    {rec.suggestedAction}
                  </p>
                </div>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
