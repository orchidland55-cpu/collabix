import { useMemo, useCallback } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { Menu, Sparkles } from 'lucide-react';
import { ConversationSidebar } from './ConversationSidebar';
import { ConversationContextPanel } from './ConversationContextPanel';
import { ConversationContextProvider, useConversationContext } from './ConversationContext';
import { useConversationsList } from '../../../services/conversation-hooks';
import { useEffectiveWorkspaceId, aiPath } from '../../../hooks/useEffectiveWorkspaceId';
import type { Conversation } from './ConversationTypes';
import type { ConversationResponse } from '../../../types/communication';

function mapConversation(c: ConversationResponse): Conversation {
  return {
    id: c.id,
    title: c.name,
    preview: c.lastMessagePreview ?? 'No messages yet',
    updatedAt: c.lastMessageAt
      ? new Date(c.lastMessageAt).toLocaleString()
      : new Date(c.updatedAt).toLocaleDateString(),
    pinned: false,
    favorite: false,
    unread: (c.unreadCount ?? 0) > 0,
  };
}

function ConversationLayoutInner() {
  const navigate = useNavigate();
  const wsId = useEffectiveWorkspaceId();
  const { sidebarOpen, setSidebarOpen, contextPanelOpen, toggleContextPanel } = useConversationContext();
  const { data: conversationsPage, isLoading } = useConversationsList(wsId);

  const conversations = useMemo(
    () => (conversationsPage?.content ?? []).map(mapConversation),
    [conversationsPage],
  );

  const handleNewConversation = useCallback(() => {
    navigate(aiPath('/app/ai/conversations', wsId));
    setSidebarOpen(false);
  }, [navigate, wsId, setSidebarOpen]);

  return (
    <div className="flex gap-0 xl:gap-5 h-full min-h-[480px]">
      <ConversationSidebar
        conversations={conversations}
        open={sidebarOpen}
        loading={isLoading}
        onClose={() => setSidebarOpen(false)}
        onNewConversation={handleNewConversation}
        onTogglePin={() => undefined}
        onToggleFavorite={() => undefined}
      />

      <div className="flex flex-1 min-w-0 flex-col rounded-xl border border-border-subtle bg-elevated dark:bg-surface overflow-hidden h-full">
        <div className="flex items-center gap-2 px-4 py-3 border-b border-border-subtle xl:hidden">
          <button
            type="button"
            onClick={() => setSidebarOpen(true)}
            aria-label="Open conversation sidebar"
            className="flex h-8 w-8 items-center justify-center rounded-lg text-text-secondary hover:bg-surface-2 transition-colors"
          >
            <Menu className="h-4 w-4" />
          </button>
          <Sparkles className="h-4 w-4 text-accent-600 dark:text-accent-400" />
          <p className="text-caption font-medium text-text-primary">Conversations</p>
        </div>
        <Outlet context={{ toggleContextPanel, contextPanelOpen, workspaceId: wsId }} />
      </div>

      <ConversationContextPanel open={contextPanelOpen} onClose={() => toggleContextPanel()} />
    </div>
  );
}

export function ConversationLayout() {
  return (
    <ConversationContextProvider>
      <ConversationLayoutInner />
    </ConversationContextProvider>
  );
}
