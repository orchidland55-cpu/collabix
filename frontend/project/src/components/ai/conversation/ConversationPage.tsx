import { useNavigate } from 'react-router-dom';
import { useOutletContext } from 'react-router-dom';
import { ConversationComposer } from './ConversationComposer';
import { ConversationWelcome } from './ConversationWelcome';
import { useCreateConversation } from '../../../services/conversation-hooks';
import { useToast } from '../../ui/Toast';
import { aiPath } from '../../../hooks/useEffectiveWorkspaceId';

interface OutletContext {
  workspaceId: string;
}

export function ConversationPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const { workspaceId } = useOutletContext<OutletContext>();
  const createConversation = useCreateConversation(workspaceId);

  async function startConversation(initialMessage?: string) {
    if (!workspaceId) return;
    try {
      const conversation = await createConversation.mutateAsync({
        name: initialMessage?.slice(0, 60) || 'New conversation',
        type: 'WORKSPACE',
      });
      navigate(aiPath(`/app/ai/conversations/${conversation.id}`, workspaceId));
    } catch (err) {
      toast({
        title: 'Could not start conversation',
        description: err instanceof Error ? err.message : 'An unexpected error occurred.',
        tone: 'danger',
      });
    }
  }

  return (
    <div className="flex h-full flex-col">
      <div className="flex-1 overflow-y-auto">
        <ConversationWelcome
          onStartConversation={() => startConversation()}
          onPromptClick={(prompt) => startConversation(prompt)}
        />
      </div>
      <ConversationComposer
        onSend={(content) => startConversation(content)}
        suggestedPrompts={undefined}
      />
    </div>
  );
}
