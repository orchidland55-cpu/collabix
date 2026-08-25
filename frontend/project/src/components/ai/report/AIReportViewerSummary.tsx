import { Sparkles } from 'lucide-react';
import { MarkdownRenderer } from '../MarkdownRenderer';

interface AIReportViewerSummaryProps {
  summary: string;
}

export function AIReportViewerSummary({ summary }: AIReportViewerSummaryProps) {
  return (
    <div className="relative overflow-hidden rounded-2xl border border-border-subtle bg-gradient-to-br from-accent-500/[0.04] to-accent-600/[0.02] dark:from-accent-400/[0.06] dark:to-accent-500/[0.03] p-6 sm:p-8">
      <div className="pointer-events-none absolute -top-16 -right-16 h-40 w-40 rounded-full bg-accent-500/5 blur-3xl" />
      <div className="relative">
        <div className="flex items-center gap-2 mb-3">
          <Sparkles className="h-4 w-4 text-accent-600 dark:text-accent-400" />
          <p className="text-caption font-semibold text-accent-600 dark:text-accent-400">Executive Summary</p>
        </div>
        <MarkdownRenderer content={summary} />
      </div>
    </div>
  );
}
