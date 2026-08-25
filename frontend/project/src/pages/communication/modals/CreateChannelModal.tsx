import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Hash, Lock, Users } from 'lucide-react';
import { Modal, type ModalProps } from '../../../components/ui/Modal';
import { Input } from '../../../components/ui/Input';
import { Button } from '../../../components/ui/Button';
import { useCreateConversation } from '../../../services/conversation-hooks';
import type { ConversationType } from '../../../types/communication';

type CreateChannelModalProps = Pick<ModalProps, 'open' | 'onClose'>;

export function CreateChannelModal({ open, onClose }: CreateChannelModalProps) {
  const [searchParams] = useSearchParams();
  const wsId = searchParams.get('ws') ?? '';
  const initialType = searchParams.get('type');
  const [name, setName] = useState('');
  const [topic, setTopic] = useState('');
  const [type, setType] = useState<ConversationType>(
    initialType === 'DIRECT' ? 'DIRECT' : 'WORKSPACE',
  );
  const [isPrivate, setIsPrivate] = useState(false);
  const createConversation = useCreateConversation(wsId);

  const handleSubmit = async () => {
    if (!name.trim()) return;
    try {
      await createConversation.mutateAsync({
        name: name.trim(),
        topic: topic.trim() || undefined,
        type,
        isPrivate,
      });
      setName('');
      setTopic('');
      onClose();
    } catch {
      // handled by mutation
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Create Channel">
      <div className="flex flex-col gap-4">
        <div>
          <label className="text-caption font-medium text-text-primary mb-1.5 block">Channel Type</label>
          <div className="grid grid-cols-3 gap-2">
            {(['WORKSPACE', 'DEPARTMENT', 'TEAM'] as ConversationType[]).map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setType(t)}
                className={`flex items-center justify-center gap-2 rounded-lg border px-3 py-2.5 text-body font-medium transition-colors ${
                  type === t
                    ? 'border-accent-500 bg-accent-50 text-accent-700 dark:bg-accent-100 dark:text-accent-200'
                    : 'border-border-subtle text-text-tertiary hover:text-text-primary hover:border-border-default'
                }`}
              >
                {t === 'WORKSPACE' ? <Users className="h-4 w-4" /> : t === 'DEPARTMENT' ? <Hash className="h-4 w-4" /> : <Lock className="h-4 w-4" />}
                {t.charAt(0) + t.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        </div>

        <Input
          label="Channel Name"
          placeholder="e.g. project-alpha"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />

        <Input
          label="Topic (optional)"
          placeholder="What's this channel about?"
          value={topic}
          onChange={(e) => setTopic(e.target.value)}
        />

        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={isPrivate}
            onChange={(e) => setIsPrivate(e.target.checked)}
            className="rounded border-border-subtle"
          />
          <span className="text-body text-text-primary">Make private</span>
          <span className="text-caption text-text-tertiary">Only invited members can see this channel</span>
        </label>

        <div className="flex justify-end gap-2 pt-2">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            disabled={!name.trim() || createConversation.isPending}
          >
            {createConversation.isPending ? 'Creating...' : 'Create Channel'}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
