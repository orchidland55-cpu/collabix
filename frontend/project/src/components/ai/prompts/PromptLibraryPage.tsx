import { useState, useMemo, useCallback } from 'react';
import { Star, Sparkles } from 'lucide-react';
import { PromptHeader } from './PromptHeader';
import { PromptSearch } from './PromptSearch';
import { PromptCategoryNav, type PromptCategoryNavProps } from './PromptCategoryNav';
import { PromptFeatured } from './PromptFeatured';
import { PromptGrid } from './PromptGrid';
import { PromptDetailDrawer } from './PromptDetailDrawer';
import { PromptRunModal } from './PromptRunModal';
import { PromptEmptyState } from './PromptEmptyState';
import { PromptLoading } from './PromptLoading';
import { PromptErrorCard } from './PromptErrorCard';
import type { Prompt, PromptCategoryId } from './PromptTypes';
import { useAIPrompts } from '../../../services/prompt-ai-hooks';
import type { AIPromptResponse } from '../../../services/prompt-ai-service';
import { usePromptExecution } from '../../../services/prompt-execution-service';
import { useToast } from '../../../components/ui/Toast';

const categoryMap: Record<AIPromptResponse['category'], PromptCategoryId> = {
  ANALYTICS: 'analytics',
  HANDOVER: 'handover',
  KNOWLEDGE: 'knowledge',
  REPORTS: 'reports',
  GENERAL: 'workspace',
};

function mapPrompt(p: AIPromptResponse): Prompt {
  // Determine required context based on category
  let requiredContext: string[] = [];
  switch (p.category) {
    case 'ANALYTICS':
      requiredContext = ['departmentId', 'period'];
      break;
    case 'REPORTS':
      requiredContext = ['departmentId', 'title', 'period'];
      break;
    case 'HANDOVER':
      requiredContext = ['departmentId', 'projectId', 'date', 'shift'];
      break;
    case 'KNOWLEDGE':
      requiredContext = ['question'];
      break;
    default:
      requiredContext = ['workspace'];
  }

  return {
    id: p.id,
    title: p.name,
    description: p.description ?? '',
    category: categoryMap[p.category] ?? 'workspace',
    tags: [p.code],
    businessObjective: p.description ?? p.name,
    useCases: [],
    requiredContext,
    expectedOutput: 'AI-generated output based on workspace context',
    executionTime: '',
    favorite: false,
    featured: p.active,
  };
}

export function PromptLibraryPage() {
  const [activeCategory, setActiveCategory] = useState<PromptCategoryId | 'all'>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedPrompt, setSelectedPrompt] = useState<Prompt | null>(null);
  const [runPrompt, setRunPrompt] = useState<Prompt | null>(null);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [searches, setSearches] = useState<string[]>([]);
  const [executingPrompt, setExecutingPrompt] = useState<Prompt | null>(null);
  const [executionVariables, setExecutionVariables] = useState<Record<string, string>>({});
  const [executionQuestion, setExecutionQuestion] = useState('');

  const { data: apiPrompts, isLoading, isError, refetch } = useAIPrompts();
  const prompts = useMemo(() => (apiPrompts ?? []).map(mapPrompt), [apiPrompts]);
  const { executePrompt } = usePromptExecution();
  const { toast } = useToast();

  const filtered = useMemo(() => {
    let items = [...prompts];
    if (activeCategory === 'favorites') {
      items = items.filter((p) => favorites.has(p.id));
    } else if (activeCategory !== 'all') {
      items = items.filter((p) => p.category === activeCategory);
    }
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      items = items.filter(
        (p) =>
          p.title.toLowerCase().includes(q) ||
          p.description.toLowerCase().includes(q) ||
          p.tags.some((t) => t.toLowerCase().includes(q)),
      );
    }
    return items;
  }, [activeCategory, searchQuery, favorites, prompts]);

  const featured = useMemo(() => prompts.filter((p) => p.featured), [prompts]);
  const favoritePrompts = useMemo(() => prompts.filter((p) => favorites.has(p.id)), [prompts, favorites]);

  function handleToggleFavorite(id: string) {
    setFavorites((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function handleSearch(query: string) {
    setSearchQuery(query);
    if (query.trim() && !searches.includes(query.trim())) {
      setSearches((prev) => [query.trim(), ...prev].slice(0, 5));
    }
  }

  const handleCategoryChange: PromptCategoryNavProps['onChange'] = (cat) => {
    setActiveCategory(cat);
    setSearchQuery('');
  };

  const handleRunPrompt = useCallback(async (prompt: Prompt) => {
    // Find the original API prompt to get the category
    const apiPrompt = apiPrompts?.find((p) => p.id === prompt.id);
    if (!apiPrompt) {
      toast({ title: 'Prompt not found', tone: 'error' });
      return;
    }

    // For prompts that need variables, open the run modal
    if (prompt.requiredContext.length > 0) {
      setRunPrompt(prompt);
      setExecutingPrompt(prompt);
      setExecutionVariables({});
      setExecutionQuestion('');
      return;
    }

    // For prompts with no required context, execute directly
    await executePromptAction(apiPrompt, {}, '');
  }, [apiPrompts, executePrompt]);

  const executePromptAction = useCallback(async (
    apiPrompt: AIPromptResponse,
    variables: Record<string, string>,
    question: string
  ) => {
    // Convert AIPromptResponse to Prompt type for executing state
    const executingPromptData: Prompt = {
      id: apiPrompt.id,
      title: apiPrompt.name,
      description: apiPrompt.description ?? '',
      category: categoryMap[apiPrompt.category] ?? 'workspace',
      tags: [apiPrompt.code],
      businessObjective: apiPrompt.description ?? apiPrompt.name,
      useCases: [],
      requiredContext: [],
      expectedOutput: 'AI-generated output based on workspace context',
      executionTime: '',
      favorite: false,
      featured: apiPrompt.active,
    };
    setExecutingPrompt(executingPromptData);
    try {
      const result = await executePrompt(apiPrompt, variables, question);
      if (result.success) {
        toast({ title: 'Prompt executed successfully', tone: 'success' });
        // Navigate to the result if there's a resultId
        if (result.resultId) {
          // Could navigate to report viewer or show result modal
          console.log('Execution result:', result);
        }
      } else {
        toast({ title: result.error || 'Prompt execution failed', tone: 'error' });
      }
    } catch (error) {
      toast({ title: 'Failed to execute prompt', tone: 'error' });
    } finally {
      setExecutingPrompt(null);
      setRunPrompt(null);
    }
  }, [executePrompt]);

  const handleRunModalSubmit = useCallback(async (prompt: Prompt, variables: Record<string, string>, question: string) => {
    const apiPrompt = apiPrompts?.find((p) => p.id === prompt.id);
    if (!apiPrompt) return;
    await executePromptAction(apiPrompt, variables, question);
  }, [apiPrompts, executePromptAction]);

  if (isLoading) return <PromptLoading />;

  if (isError) {
    return (
      <div className="flex flex-col gap-6">
        <PromptHeader searches={searches} onSearch={() => {}} />
        <PromptErrorCard message="Unable to load prompt templates." onRetry={() => refetch()} onDismiss={() => refetch()} />
      </div>
    );
  }

  const showFeatured = activeCategory === 'all' && !searchQuery && featured.length > 0;
  const showFavorites = activeCategory === 'all' && !searchQuery && favoritePrompts.length > 0;

  return (
    <div className="flex flex-col gap-8 animate-fade-in">
      <PromptHeader searches={searches} onSearch={handleSearch} />

      <PromptSearch
        query={searchQuery}
        onQueryChange={handleSearch}
        recentSearches={searches}
        popularPrompts={featured.map((p) => p.title)}
        onClearSearch={() => setSearchQuery('')}
      />

      <PromptCategoryNav active={activeCategory} onChange={handleCategoryChange} />

      {showFeatured && (
        <PromptFeatured prompts={featured} onPreview={setSelectedPrompt} onRun={handleRunPrompt} onToggleFavorite={handleToggleFavorite} favorites={favorites} />
      )}

      {showFavorites && (
        <section className="flex flex-col gap-4">
          <div className="flex items-center gap-2">
            <Star className="h-4 w-4 text-text-tertiary" />
            <h2 className="text-section font-semibold text-text-primary">Favorites</h2>
          </div>
          <PromptGrid
            prompts={favoritePrompts}
            onPreview={setSelectedPrompt}
            onRun={handleRunPrompt}
            onToggleFavorite={handleToggleFavorite}
            favorites={favorites}
          />
        </section>
      )}

      {(activeCategory !== 'all' || searchQuery) && filtered.length === 0 ? (
        <PromptEmptyState
          variant={searchQuery ? 'no-results' : prompts.length === 0 ? 'no-category' : 'no-category'}
          searchQuery={searchQuery}
          category={activeCategory !== 'all' ? activeCategory : undefined}
          onClearSearch={() => setSearchQuery('')}
        />
      ) : (
        <section className="flex flex-col gap-4">
          {(activeCategory !== 'all' || searchQuery) && (
            <div className="flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-text-tertiary" />
              <h2 className="text-section font-semibold text-text-primary">
                {searchQuery ? `Results for "${searchQuery}"` : 'All Prompts'}
              </h2>
              <span className="text-caption text-text-tertiary">{filtered.length}</span>
            </div>
          )}
          {(activeCategory !== 'all' || searchQuery || prompts.length > 0) && (
            <PromptGrid
              prompts={filtered.length > 0 ? filtered : prompts}
              onPreview={setSelectedPrompt}
              onRun={handleRunPrompt}
              onToggleFavorite={handleToggleFavorite}
              favorites={favorites}
            />
          )}
        </section>
      )}

      {selectedPrompt && (
        <PromptDetailDrawer
          prompt={selectedPrompt}
          isFavorite={favorites.has(selectedPrompt.id)}
          onClose={() => setSelectedPrompt(null)}
          onToggleFavorite={() => handleToggleFavorite(selectedPrompt.id)}
          onRun={() => { handleRunPrompt(selectedPrompt); setSelectedPrompt(null); }}
        />
      )}

      {runPrompt && (
        <PromptRunModal
          prompt={runPrompt}
          onClose={() => { setRunPrompt(null); setExecutingPrompt(null); }}
          onRun={async (variables: Record<string, string>, question: string) => {
            await handleRunModalSubmit(runPrompt, variables, question);
          }}
          variables={executionVariables}
          onVariablesChange={setExecutionVariables}
          question={executionQuestion}
          onQuestionChange={setExecutionQuestion}
          isExecuting={!!executingPrompt}
        />
      )}
    </div>
  );
}
