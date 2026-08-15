import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { aiPath, useEffectiveWorkspaceId } from '../../../hooks/useEffectiveWorkspaceId';
import {
  Search,
  Plus,
  MessageSquare,
  Pin,
  Star,
  StarOff,
  X,
  Sparkles,
  ChevronDown,
  BookMarked,
} from 'lucide-react';
import { cn } from '../../../lib/cn';
import { IconButton } from '../../ui/IconButton';
import { type Conversation } from './ConversationTypes';

interface ConversationSidebarProps {
  conversations: Conversation[];
  open: boolean;
  loading?: boolean;
  onClose: () => void;
  onNewConversation: () => void;
  onTogglePin: (id: string) => void;
  onToggleFavorite: (id: string) => void;
}

export function ConversationSidebar({
  conversations,
  open,
  loading = false,
  onClose,
  onNewConversation,
  onTogglePin,
  onToggleFavorite,
}: ConversationSidebarProps) {
  const navigate = useNavigate();
  const workspaceId = useEffectiveWorkspaceId();
  const { conversationId } = useParams();
  const [searchQuery, setSearchQuery] = useState('');

  const grouped = {
    pinned: conversations.filter((c) => c.pinned),
    favorites: conversations.filter((c) => c.favorite && !c.pinned),
    recent: conversations.filter((c) => !c.pinned && !c.favorite),
  };

  const query = searchQuery.toLowerCase();
  const filtered: Record<string, Conversation[]> = query
    ? {
        pinned: grouped.pinned.filter(match),
        favorites: grouped.favorites.filter(match),
        recent: grouped.recent.filter(match),
      }
    : grouped;

  function match(c: Conversation) {
    return c.title.toLowerCase().includes(query) || c.preview.toLowerCase().includes(query);
  }

  function handleSelect(id: string) {
    const params = new URLSearchParams(window.location.search);
    const ws = params.get('ws');
    const qs = ws ? `?ws=${ws}` : '';
    navigate(`/app/ai/conversations/${id}${qs}`);
    onClose();
  }

  function renderSection(label: string, items: Conversation[]) {
    if (items.length === 0) return null;
    return (
      <div>
        <div className="flex items-center gap-1 px-3 py-1.5">
          <p className="text-2xs font-semibold uppercase tracking-wider text-text-tertiary">{label}</p>
          <span className="text-2xs text-text-tertiary">{items.length}</span>
        </div>
        {items.map((conv) => (
          <button
            key={conv.id}
            type="button"
            onClick={() => handleSelect(conv.id)}
            className={cn(
              'group flex w-full items-start gap-3 rounded-lg px-3 py-2.5 text-left transition-colors',
              conv.id === conversationId
                ? 'bg-accent-600/10 text-accent-700 dark:bg-accent-100/15 dark:text-accent-200'
                : 'text-text-secondary hover:bg-surface-2 hover:text-text-primary',
            )}
          >
            <span
              className={cn(
                'flex h-8 w-8 shrink-0 items-center justify-center rounded-lg',
                conv.id === conversationId
                  ? 'bg-accent-600/15 text-accent-600 dark:bg-accent-100/15 dark:text-accent-200'
                  : 'bg-surface-2 text-text-tertiary group-hover:bg-border-subtle',
              )}
            >
              <MessageSquare className="h-4 w-4" />
            </span>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <p className="truncate text-body font-medium">{conv.title}</p>
                {conv.unread && <span className="h-2 w-2 shrink-0 rounded-full bg-accent-500" />}
              </div>
              <p className="truncate text-caption text-text-tertiary">{conv.preview}</p>
              <p className="mt-0.5 text-2xs text-text-tertiary">{conv.updatedAt}</p>
            </div>
            <div className="flex shrink-0 flex-col gap-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); onTogglePin(conv.id); }}
                aria-label={conv.pinned ? 'Unpin conversation' : 'Pin conversation'}
                className="flex h-6 w-6 items-center justify-center rounded text-text-tertiary hover:text-text-primary hover:bg-surface-2"
              >
                <Pin className={cn('h-3 w-3', conv.pinned && 'fill-current text-accent-500')} />
              </button>
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); onToggleFavorite(conv.id); }}
                aria-label={conv.favorite ? 'Remove from favorites' : 'Add to favorites'}
                className="flex h-6 w-6 items-center justify-center rounded text-text-tertiary hover:text-text-primary hover:bg-surface-2"
              >
                {conv.favorite
                  ? <Star className="h-3 w-3 fill-accent-500 text-accent-500" />
                  : <StarOff className="h-3 w-3" />
                }
              </button>
            </div>
          </button>
        ))}
      </div>
    );
  }

  const totalFiltered = (Object.values(filtered) as Conversation[][]).reduce((a, b) => a + b.length, 0);

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 z-40 bg-text-primary/40 dark:bg-black/60 backdrop-blur-sm animate-fade-in xl:hidden"
          aria-hidden="true"
          onClick={onClose}
        />
      )}
      <div
        role={open ? 'dialog' : undefined}
        aria-modal={open ? 'true' : undefined}
        aria-label="Conversation history"
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-80 flex-col bg-elevated border-r border-border-subtle transition-transform duration-300 xl:sticky xl:top-0 xl:z-0 xl:h-[calc(100vh-7rem)] xl:border xl:border-border-subtle xl:rounded-xl xl:bg-surface',
          open ? 'translate-x-0' : '-translate-x-full xl:translate-x-0',
        )}
      >
        <div className="flex items-center justify-between px-4 py-4 border-b border-border-subtle">
          <div className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 text-accent-600" />
            <p className="text-section font-semibold text-text-primary">Conversations</p>
          </div>
          <div className="flex items-center gap-1">
            <IconButton size="sm" label="New conversation" onClick={onNewConversation}>
              <Plus className="h-4 w-4" />
            </IconButton>
            <button
              type="button"
              onClick={onClose}
              aria-label="Close conversation sidebar"
              className="flex h-8 w-8 items-center justify-center rounded-lg text-text-tertiary hover:bg-surface-2 hover:text-text-primary transition-colors xl:hidden"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        <div className="px-3 py-3">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-tertiary" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search conversations..."
              aria-label="Search conversations"
              className="w-full rounded-lg border border-border-subtle bg-surface py-2 pl-9 pr-3 text-body text-text-primary placeholder:text-text-tertiary focus:outline-none focus:ring-2 focus:ring-accent-500 focus:border-transparent transition-colors"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                aria-label="Clear search"
                className="absolute right-3 top-1/2 -translate-y-1/2 text-text-tertiary hover:text-text-primary"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            )}
          </div>
        </div>

        <nav aria-label="Conversation list" className="flex-1 overflow-y-auto px-2 pb-4 space-y-1">
          {searchQuery && totalFiltered === 0 && (
            <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
              <Search className="h-8 w-8 text-text-tertiary mb-3" />
              <p className="text-body font-medium text-text-secondary">No conversations found</p>
              <p className="text-caption text-text-tertiary mt-1">
                Try a different search term.
              </p>
            </div>
          )}
          {loading && conversations.length === 0 && (
            <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
              <p className="text-caption text-text-tertiary">Loading conversations...</p>
            </div>
          )}
          {!searchQuery && !loading && conversations.length === 0 && (
            <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
              <MessageSquare className="h-8 w-8 text-text-tertiary mb-3" />
              <p className="text-body font-medium text-text-secondary">No conversations yet</p>
              <p className="text-caption text-text-tertiary mt-1">
                Start a workspace conversation to collaborate with your team.
              </p>
            </div>
          )}
          {renderSection('Pinned', filtered.pinned)}
          {renderSection('Favorites', filtered.favorites)}
          {renderSection('Recent', filtered.recent)}
        </nav>

        <div className="border-t border-border-subtle px-3 py-3">
          <button
            type="button"
            onClick={() => navigate(aiPath('/app/ai/prompts', workspaceId))}
            className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-body text-text-secondary hover:bg-surface-2 hover:text-text-primary transition-colors"
          >
            <BookMarked className="h-4 w-4" />
            <span>Browse Prompt Library</span>
            <ChevronDown className="h-3.5 w-3.5 ml-auto text-text-tertiary" />
          </button>
        </div>
      </div>
    </>
  );
}
