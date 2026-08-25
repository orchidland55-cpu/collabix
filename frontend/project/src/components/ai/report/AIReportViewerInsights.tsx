import { TrendingUp, AlertTriangle, CheckCircle, BarChart3, Users } from 'lucide-react';
import { cn } from '../../../lib/cn';
import { Badge } from '../../ui/Badge';
import { MarkdownRenderer } from '../MarkdownRenderer';
import { type Insight } from './AIReportViewerTypes';

interface AIReportViewerInsightsProps {
  insights: Insight[];
}

const iconMap: Record<string, React.ReactNode> = {
  'trending-up': <TrendingUp className="h-5 w-5" />,
  'alert-triangle': <AlertTriangle className="h-5 w-5" />,
  'check-circle': <CheckCircle className="h-5 w-5" />,
  'bar-chart': <BarChart3 className="h-5 w-5" />,
  users: <Users className="h-5 w-5" />,
};

const priorityTone = { high: 'danger' as const, medium: 'warning' as const, low: 'info' as const };

const iconColors: Record<string, string> = {
  'trending-up': 'text-success-500',
  'alert-triangle': 'text-danger-500',
  'check-circle': 'text-success-500',
  'bar-chart': 'text-info-500',
  users: 'text-accent-500',
};

const bgColors: Record<string, string> = {
  'trending-up': 'bg-success-50 dark:bg-success-500/10',
  'alert-triangle': 'bg-danger-50 dark:bg-danger-500/10',
  'check-circle': 'bg-success-50 dark:bg-success-500/10',
  'bar-chart': 'bg-info-50 dark:bg-info-500/10',
  users: 'bg-accent-50 dark:bg-accent-100/10',
};

export function AIReportViewerInsights({ insights }: AIReportViewerInsightsProps) {
  if (insights.length === 0) return null;
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {insights.map((insight) => (
        <div
          key={insight.id}
          className="rounded-xl border border-border-subtle bg-elevated dark:bg-surface p-5 hover:shadow-cx-sm hover:-translate-y-0.5 transition-all duration-150"
        >
          <div className="flex items-start gap-3">
            <span className={cn('flex h-10 w-10 shrink-0 items-center justify-center rounded-xl', bgColors[insight.icon] || 'bg-surface-2', iconColors[insight.icon] || 'text-text-tertiary')}>
              {iconMap[insight.icon] || <BarChart3 className="h-5 w-5" />}
            </span>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2 mb-1">
                <p className="text-body font-semibold text-text-primary">{insight.title}</p>
                <Badge variant="soft" tone={priorityTone[insight.priority]} className="text-2xs">{insight.priority}</Badge>
              </div>
              <MarkdownRenderer content={insight.description} className="text-caption" />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
