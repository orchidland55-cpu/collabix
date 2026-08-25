import { Sparkles, Lightbulb, Target, ListChecks, AlertTriangle } from 'lucide-react';
import { cn } from '../../../lib/cn';
import { MarkdownRenderer } from '../MarkdownRenderer';

interface AIBusinessResultPanelProps {
  summary: string;
  insights: string[];
  recommendations: string[];
  keyPoints: string[];
  className?: string;
}

export function AIBusinessResultPanel({ summary, insights, recommendations, keyPoints, className }: AIBusinessResultPanelProps) {
  return (
    <div className={cn('rounded-xl border border-border-subtle bg-elevated dark:bg-surface divide-y divide-border-subtle', className)}>
      <Section icon={<Sparkles />} title="Executive Summary" iconColor="text-accent-500">
        <MarkdownRenderer content={summary} className="text-body" />
      </Section>

      <Section icon={<Lightbulb />} title="Business Insights" iconColor="text-info-500">
        <ul className="space-y-2">
          {insights.map((item) => (
            <li key={item} className="flex items-start gap-2.5 text-body text-text-secondary">
              <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-accent-400" />
              <MarkdownRenderer content={item} className="inline" />
            </li>
          ))}
        </ul>
      </Section>

      <Section icon={<Target />} title="Recommendations" iconColor="text-success-500">
        <ul className="space-y-2">
          {recommendations.map((item) => (
            <li key={item} className="flex items-start gap-2.5 text-body text-text-secondary">
              <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-success-500" />
              <MarkdownRenderer content={item} className="inline" />
            </li>
          ))}
        </ul>
      </Section>

      <div className="px-5 py-4">
        <div className="flex items-center gap-2 mb-3">
          <ListChecks className="h-4 w-4 text-text-tertiary" />
          <p className="text-caption font-semibold text-text-primary">Key Points</p>
        </div>
        <div className="grid gap-2 sm:grid-cols-2">
          {keyPoints.map((pt) => (
            <div key={pt} className="flex items-center gap-2 rounded-lg bg-surface-2 px-3 py-2">
              <AlertTriangle className="h-3.5 w-3.5 text-accent-500 shrink-0" />
              <span className="text-caption text-text-secondary">{pt}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function Section({ icon, title, iconColor, children }: { icon: React.ReactNode; title: string; iconColor: string; children: React.ReactNode }) {
  return (
    <div className="px-5 py-5">
      <div className="flex items-center gap-2 mb-3">
        <span className={cn('h-4 w-4', iconColor)}>{icon}</span>
        <h3 className="text-caption font-semibold text-text-primary">{title}</h3>
      </div>
      {children}
    </div>
  );
}
