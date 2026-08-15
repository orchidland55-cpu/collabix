import { Star, Play } from 'lucide-react';
import { cn } from '../../lib/cn';
import { Badge } from '../ui/Badge';

export interface AIPromptItem {
  id: string;
  title: string;
  category: string;
  description: string;
}

export interface AIPromptPreviewProps {
  items: AIPromptItem[];
  onRun?: (id: string) => void;
  onViewAll?: () => void;
  className?: string;
}

export function AIPromptPreview({ items, onRun, onViewAll, className }: AIPromptPreviewProps) {
  return (
    <div className={cn('rounded-xl border border-border-subtle bg-elevated', className)}>
      <div className="flex items-center justify-between px-5 py-4 border-b border-border-subtle">
        <div>
          <p className="text-section font-semibold text-text-primary">Prompt Library</p>
          <p className="mt-0.5 text-caption text-text-tertiary">Active AI prompt templates</p>
        </div>
        {onViewAll && items.length > 0 && (
          <button type="button" onClick={onViewAll} className="text-caption font-medium text-accent-600 hover:text-accent-700 dark:text-accent-300">
            View all
          </button>
        )}
      </div>
      <div className="px-3 py-2">
        {items.length === 0 ? (
          <div className="flex flex-col items-center py-8 text-center">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-surface-2 text-text-tertiary mb-3">
              <Star className="h-5 w-5" />
            </div>
            <p className="text-body font-medium text-text-primary">No saved prompts</p>
            <p className="text-caption text-text-tertiary mt-1">Favorite a prompt to access it quickly.</p>
          </div>
        ) : (
          items.map((item) => (
            <div
              key={item.id}
              className="flex items-start gap-3 rounded-lg px-2 py-2.5 group hover:bg-surface-2 transition-colors"
            >
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-warning-50 text-warning-600 dark:bg-warning-100 dark:text-warning-500">
                <Star className="h-4 w-4 fill-warning-500" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <p className="text-body font-medium text-text-primary truncate">{item.title}</p>
                  <Badge tone="accent" variant="soft" className="shrink-0">{item.category}</Badge>
                </div>
                <p className="text-caption text-text-tertiary truncate">{item.description}</p>
              </div>
              <button
                type="button"
                onClick={() => onRun?.(item.id)}
                aria-label={`Run prompt: ${item.title}`}
                className="shrink-0 flex h-7 w-7 items-center justify-center rounded-md text-text-tertiary opacity-0 group-hover:opacity-100 hover:bg-surface-2 hover:text-accent-600 transition-all"
              >
                <Play className="h-3.5 w-3.5" />
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
