import { useState, useCallback, useEffect, useMemo } from 'react';
import { useParams, useNavigate, useOutletContext } from 'react-router-dom';
import { ConversationHeader } from './ConversationHeader';
import { ConversationComposer } from './ConversationComposer';
import { ConversationMessages } from './ConversationMessages';
import { ConversationErrorCard } from './ConversationErrorCard';
import { ConversationEmptyState } from './ConversationEmptyStates';
import { ConversationLoadingThinking } from './ConversationLoading';
import { useConversationDetail, useDeleteConversation, useUpdateConversation } from '../../../services/conversation-hooks';
import { useMessages, useCreateMessage } from '../../../services/message-hooks';
import { useAuth } from '../../../lib/auth-context';
import { aiPath } from '../../../hooks/useEffectiveWorkspaceId';
import type { Message, ErrorType } from './ConversationTypes';

interface OutletContext {
  toggleContextPanel: () => void;
  contextPanelOpen: boolean;
  workspaceId: string;
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });
}

export function ConversationChatView() {
  const { conversationId } = useParams();
  const navigate = useNavigate();
  const { toggleContextPanel, contextPanelOpen, workspaceId } = useOutletContext<OutletContext>();
  const { user } = useAuth();

  const [favorite, setFavorite] = useState(false);
  const [error, setError] = useState<ErrorType | null>(null);

  const { data: conversation, isError: convError } = useConversationDetail(workspaceId, conversationId);
  const { data: messagesData, isLoading: messagesLoading, isError: messagesError } = useMessages(workspaceId, conversationId ?? '');
  const createMessage = useCreateMessage(workspaceId, conversationId ?? '');
  const updateConversation = useUpdateConversation(workspaceId, conversationId ?? '');
  const deleteConversation = useDeleteConversation(workspaceId);

  const messages: Message[] = useMemo(() => {
    const pages = messagesData?.pages ?? [];
    const flat = pages.flatMap((p) => p.content);
    return flat.map((m) => ({
      id: m.id,
      role: m.senderId === user?.id ? 'user' as const : 'user' as const,
      content: m.content,
      timestamp: formatTime(m.createdAt),
    }));
  }, [messagesData, user?.id]);

  useEffect(() => {
    setError(null);
    setFavorite(false);
  }, [conversationId]);

  const handleSend = useCallback(async (content: string) => {
    if (!conversationId || !content.trim()) return;
    setError(null);
    try {
      await createMessage.mutateAsync({ content: content.trim(), messageType: 'TEXT' });
    } catch {
      setError('connection_lost');
    }
  }, [conversationId, createMessage]);

  const handleRename = useCallback(async (newTitle: string) => {
    if (!conversationId) return;
    await updateConversation.mutateAsync({ name: newTitle });
  }, [conversationId, updateConversation]);

  const handleDelete = useCallback(async () => {
    if (!conversationId) return;
    await deleteConversation.mutateAsync(conversationId);
    navigate(aiPath('/app/ai/conversations', workspaceId));
  }, [conversationId, deleteConversation, navigate, workspaceId]);

  if (!conversationId || !workspaceId) {
    return <ConversationEmptyState variant="no-messages" onAction={() => navigate(aiPath('/app/ai/conversations', workspaceId))} />;
  }

  if (convError || messagesError) {
    return (
      <div className="p-6">
        <ConversationErrorCard type="unexpected" onRetry={() => window.location.reload()} onDismiss={() => navigate(aiPath('/app/ai/conversations', workspaceId))} />
      </div>
    );
  }

  const title = conversation?.name ?? 'Conversation';
  const hasMessages = messages.length > 0 || createMessage.isPending;

  return (
    <div className="flex h-full flex-col">
      <ConversationHeader
        title={title}
        updatedAt={conversation?.updatedAt ? formatTime(conversation.updatedAt) : 'Just now'}
        favorite={favorite}
        onRename={handleRename}
        onToggleFavorite={() => setFavorite(!favorite)}
        onDelete={handleDelete}
        onToggleContextPanel={toggleContextPanel}
        contextPanelOpen={contextPanelOpen}
      />

      {messagesLoading && messages.length === 0 ? (
        <ConversationLoadingThinking state="thinking" />
      ) : hasMessages ? (
        <ConversationMessages
          messages={messages}
          onCopy={(_id, content) => navigator.clipboard.writeText(content)}
          onRegenerate={() => undefined}
          onLike={() => undefined}
          onDislike={() => undefined}
          onBookmark={() => undefined}
          onContinueConversation={() => undefined}
          onFollowUpSelect={(_id, question) => handleSend(question)}
        />
      ) : (
        <ConversationEmptyState variant="no-messages" onAction={() => handleSend('Hello')} />
      )}

      {error && (
        <div className="px-4 sm:px-6 py-4">
          <ConversationErrorCard type={error} onRetry={() => setError(null)} onDismiss={() => setError(null)} />
        </div>
      )}

      <ConversationComposer
        onSend={handleSend}
        streaming={createMessage.isPending}
        suggestedPrompts={undefined}
      />
    </div>
  );
}
