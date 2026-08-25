import { useEffect, useRef, useState } from 'react';
import { X, Play, Sparkles, Loader2 } from 'lucide-react';
import { Badge } from '../../ui/Badge';
import { Button } from '../../ui/Button';
import { promptCategories, type Prompt } from './PromptTypes';

interface PromptRunModalProps {
  prompt: Prompt;
  onClose: () => void;
  onRun: (variables: Record<string, string>, question: string) => void;
  variables: Record<string, string>;
  onVariablesChange: (variables: Record<string, string>) => void;
  question: string;
  onQuestionChange: (question: string) => void;
  isExecuting: boolean;
}

export function PromptRunModal({ prompt, onClose, onRun, variables, onVariablesChange, question, onQuestionChange, isExecuting }: PromptRunModalProps) {
  const ref = useRef<HTMLDivElement>(null);
  const category = promptCategories.find((c) => c.id === prompt.category);
  const [localVariables, setLocalVariables] = useState<Record<string, string>>(variables);
  const [localQuestion, setLocalQuestion] = useState(question);

  useEffect(() => {
    setLocalVariables(variables);
  }, [variables]);

  useEffect(() => {
    setLocalQuestion(question);
  }, [question]);

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  useEffect(() => {
    ref.current?.focus();
  }, []);

  const handleVariableChange = (key: string, value: string) => {
    const newVariables = { ...localVariables, [key]: value };
    setLocalVariables(newVariables);
    onVariablesChange(newVariables);
  };

  const handleSubmit = () => {
    onRun(localVariables, localQuestion);
  };

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-text-primary/40 dark:bg-black/60 backdrop-blur-sm animate-fade-in p-4" onClick={onClose}>
      <div
        ref={ref}
        role="dialog"
        aria-modal="true"
        aria-label={`Run ${prompt.title}`}
        tabIndex={-1}
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-lg rounded-2xl border border-border-subtle bg-elevated dark:bg-surface shadow-cx-xl animate-scale-in"
      >
        <div className="flex items-center justify-between px-6 py-4 border-b border-border-subtle">
          <div className="flex items-center gap-3">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-accent-50 text-accent-600 dark:bg-accent-100/10 dark:text-accent-300">
              <Sparkles className="h-4 w-4" />
            </span>
            <div>
              <h2 className="text-body font-semibold text-text-primary">{prompt.title}</h2>
              <Badge variant="soft" tone="accent" className="mt-0.5">{category?.label || prompt.category}</Badge>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="flex h-8 w-8 items-center justify-center rounded-lg text-text-tertiary hover:bg-surface-2 hover:text-text-primary transition-colors"
            disabled={isExecuting}
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="px-6 py-5 space-y-5">
          <div>
            <p className="text-caption text-text-tertiary mb-1">Description</p>
            <p className="text-body text-text-secondary leading-relaxed">{prompt.description}</p>
          </div>

          {prompt.requiredContext.length > 0 && (
            <div>
              <p className="text-caption font-medium text-text-primary mb-3">Required Context</p>
              <div className="space-y-3">
                {prompt.requiredContext.map((ctx) => (
                  <div key={ctx}>
                    <label className="text-2xs font-medium text-text-tertiary block mb-1">{ctx}</label>
                    <input
                      type="text"
                      placeholder={`Enter ${ctx.toLowerCase()}...`}
                      value={localVariables[ctx] || ''}
                      onChange={(e) => handleVariableChange(ctx, e.target.value)}
                      className="w-full rounded-lg border border-border-subtle bg-surface px-3 py-2 text-caption text-text-primary placeholder:text-text-tertiary focus:outline-none focus:ring-2 focus:ring-accent-500 focus:border-transparent transition-colors"
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          {prompt.requiredContext.includes('question') || prompt.requiredContext.includes('title') ? (
            <div>
              <p className="text-caption font-medium text-text-primary mb-3">Your Question / Request</p>
              <textarea
                value={localQuestion}
                onChange={(e) => setLocalQuestion(e.target.value)}
                placeholder="Describe what you need..."
                rows={3}
                className="w-full rounded-lg border border-border-subtle bg-surface px-3 py-2 text-caption text-text-primary placeholder:text-text-tertiary focus:outline-none focus:ring-2 focus:ring-accent-500 focus:border-transparent transition-colors resize-none"
              />
            </div>
          ) : null}

          <div className="rounded-lg bg-surface-2 p-3">
            <div className="flex items-start gap-2">
              <Sparkles className="h-4 w-4 text-accent-500 mt-0.5 shrink-0" />
              <p className="text-caption text-text-tertiary">
                Collabix AI will use your workspace context and the provided inputs to generate the output. Results are processed securely.
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-border-subtle">
          <Button variant="ghost" onClick={onClose} disabled={isExecuting}>Cancel</Button>
          <Button leftIcon={isExecuting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Play />} onClick={handleSubmit} disabled={isExecuting}>
            {isExecuting ? 'Executing...' : 'Generate'}
          </Button>
        </div>
      </div>
    </div>
  );
}
