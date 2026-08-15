import { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { Clock, Star } from 'lucide-react';
import { HistoryHeader } from './HistoryHeader';
import { HistorySearch } from './HistorySearch';
import { HistoryFilters } from './HistoryFilters';
import { HistoryTimeline } from './HistoryTimeline';
import { HistoryDetailDrawer } from './HistoryDetailDrawer';
import { HistoryEmptyState } from './HistoryEmptyState';
import { HistoryLoading } from './HistoryLoading';
import { HistoryErrorCard } from './HistoryErrorCard';
import { type HistoryItem, type ActivityCategory } from './HistoryTypes';
import { type ReportingResponse } from '../../../services/reporting-ai-service';
import { useAIReportHistory } from '../../../services/reporting-ai-hooks';
import { aiPath } from '../../../hooks/useEffectiveWorkspaceId';

function mapReportToHistoryItem(report: ReportingResponse): HistoryItem {
  const date = report.generationDate ? new Date(report.generationDate) : new Date();
  const now = new Date();
  const diffDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));
  let label = 'Today';
  if (diffDays === 1) label = 'Yesterday';
  else if (diffDays <= 7) label = `${diffDays} days ago`;
  else if (diffDays <= 30) label = 'Last week';
  else label = 'Last month';

  return {
    id: report.reportId,
    type: 'executive-summary',
    title: report.title,
    description: report.executiveSummary?.slice(0, 120) ?? 'Executive report',
    category: 'reports',
    workspace: 'Workspace',
    department: 'Department',
    date: label,
    time: date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    favorite: false,
    status: report.approvalStatus === 'APPROVED' ? 'completed' : 'draft',
  };
}

export function HistoryPage({
  workspaceId,
}: {
  workspaceId?: string;
}) {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState<ActivityCategory | 'all'>('all');
  const [timeFilter, setTimeFilter] = useState('all-time');
  const [selectedItem, setSelectedItem] = useState<HistoryItem | null>(null);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [searches, setSearches] = useState<string[]>([]);

  const { data: reportHistory, isLoading, isError } = useAIReportHistory(workspaceId);

  const allItems = useMemo(() => {
    const items: HistoryItem[] = [];
    if (reportHistory?.content) {
      items.push(...reportHistory.content.map(mapReportToHistoryItem));
    }
    return items;
  }, [reportHistory]);

  const items = useMemo(() => {
    let list = [...allItems];

    if (categoryFilter === 'favorites') {
      list = list.filter((i) => favorites.has(i.id));
    } else if (categoryFilter !== 'all') {
      list = list.filter((i) => i.category === categoryFilter);
    }

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      list = list.filter(
        (i) =>
          i.title.toLowerCase().includes(q) ||
          i.description.toLowerCase().includes(q) ||
          i.workspace.toLowerCase().includes(q),
      );
    }

    return list;
  }, [allItems, categoryFilter, searchQuery, favorites]);

  const grouped = useMemo(() => {
    const groups: Record<string, HistoryItem[]> = {};
    const dateOrder = ['Today', 'Yesterday', '3 days ago', '4 days ago', '5 days ago', '6 days ago', 'Last week', 'Last month'];

    for (const item of items) {
      if (!groups[item.date]) groups[item.date] = [];
      groups[item.date].push(item);
    }

    return dateOrder.filter((d) => groups[d]).map((d) => ({ label: d, items: groups[d] }));
  }, [items]);

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

  const favoriteItems = useMemo(() => allItems.filter((i) => favorites.has(i.id)), [allItems, favorites]);

  if (isLoading) return <HistoryLoading />;

  if (isError || error) {
    return (
      <div className="flex flex-col gap-6">
        <HistoryHeader />
        <HistoryErrorCard message={error ?? 'Failed to load history'} onRetry={() => setError(null)} onDismiss={() => setError(null)} />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-8 animate-fade-in">
      <HistoryHeader />

      <HistorySearch
        query={searchQuery}
        onQueryChange={handleSearch}
        onClearSearch={() => setSearchQuery('')}
        recentSearches={searches}
      />

      <HistoryFilters
        categoryActive={categoryFilter}
        timeActive={timeFilter}
        onCategoryChange={setCategoryFilter}
        onTimeChange={setTimeFilter}
      />

      {categoryFilter === 'favorites' && favoriteItems.length > 0 && (
        <section className="flex flex-col gap-4">
          <div className="flex items-center gap-2">
            <Star className="h-4 w-4 text-text-tertiary" />
            <h2 className="text-section font-semibold text-text-primary">Favorites</h2>
          </div>
          <HistoryTimeline
            groups={[{ label: 'Favorites', items: favoriteItems }]}
            onItemClick={setSelectedItem}
            onToggleFavorite={handleToggleFavorite}
            favorites={favorites}
          />
        </section>
      )}

      {(categoryFilter !== 'favorites' || !favoriteItems.length) && (
        <>
          {grouped.length === 0 ? (
            <HistoryEmptyState
              variant={searchQuery ? 'no-results' : categoryFilter !== 'all' ? 'no-category' : 'no-history'}
              searchQuery={searchQuery}
              onClearSearch={() => setSearchQuery('')}
              onStartAction={() => workspaceId && navigate(aiPath('/app/ai/reports', workspaceId))}
            />
          ) : (
            <div className="flex flex-col gap-4">
              {searchQuery && (
                <div className="flex items-center gap-2">
                  <Clock className="h-4 w-4 text-text-tertiary" />
                  <p className="text-caption text-text-secondary">
                    {items.length} result{items.length !== 1 ? 's' : ''} for &ldquo;{searchQuery}&rdquo;
                  </p>
                </div>
              )}
              <HistoryTimeline
                groups={grouped}
                onItemClick={setSelectedItem}
                onToggleFavorite={handleToggleFavorite}
                favorites={favorites}
              />
            </div>
          )}
        </>
      )}

      {selectedItem && (
        <HistoryDetailDrawer
          item={selectedItem}
          isFavorite={favorites.has(selectedItem.id)}
          onClose={() => setSelectedItem(null)}
          onToggleFavorite={() => handleToggleFavorite(selectedItem.id)}
          onReopen={() => {
            if (workspaceId && selectedItem) {
              navigate(aiPath(`/app/ai/report/${selectedItem.id}`, workspaceId));
            }
          }}
          onCopy={() => {}}
          onDelete={() => {}}
        />
      )}
    </div>
  );
}
