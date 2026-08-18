import { useState, useMemo } from 'react';
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

const categoryMap: Record<AIPromptResponse['category'], PromptCategoryId> = {
  ANALYTICS: 'analytics',
  HANDOVER: 'handover',
  KNOWLEDGE: 'knowledge',
  GENERAL: 'workspace',
};

function mapPrompt(p: AIPromptResponse): Prompt {
  return {
    id: p.id,
    title: p.name,
    description: p.description ?? '',
    category: categoryMap[p.category] ?? 'workspace',
    tags: [p.code],
    businessObjective: p.description ?? p.name,
    useCases: [],
    requiredContext: [],
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

  const { data: apiPrompts, isLoading, isError, refetch } = useAIPrompts();
  const prompts = useMemo(() => (apiPrompts ?? []).map(mapPrompt), [apiPrompts]);

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
        <PromptFeatured prompts={featured} onPreview={setSelectedPrompt} onRun={setRunPrompt} onToggleFavorite={handleToggleFavorite} favorites={favorites} />
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
            onRun={setRunPrompt}
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
              onRun={setRunPrompt}
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
          onRun={() => { setRunPrompt(selectedPrompt); setSelectedPrompt(null); }}
        />
      )}

      {runPrompt && (
        <PromptRunModal
          prompt={runPrompt}
          onClose={() => setRunPrompt(null)}
          onRun={() => setRunPrompt(null)}
        />
      )}
    </div>
  );
}
