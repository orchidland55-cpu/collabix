import { useState } from 'react';
import { Copy, RefreshCw, ThumbsUp, ThumbsDown, Share2, Bookmark, MessageSquare, Check } from 'lucide-react';
import { cn } from '../../../lib/cn';
import { IconButton } from '../../ui/IconButton';

interface ConversationResponseActionsProps {
  onCopy: () => void;
  onRegenerate: () => void;
  onLike: () => void;
  onDislike: () => void;
  onBookmark: () => void;
  onContinueConversation: () => void;
  liked?: boolean;
  disliked?: boolean;
  bookmarked?: boolean;
  isLastMessage?: boolean;
}

export function ConversationResponseActions({
  onCopy,
  onRegenerate,
  onLike,
  onDislike,
  onBookmark,
  onContinueConversation,
  liked,
  disliked,
  bookmarked,
  isLastMessage,
}: ConversationResponseActionsProps) {
  const [copied, setCopied] = useState(false);

  function handleCopy() {
    onCopy();
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div className="mt-3 flex items-center gap-1">
      <IconButton size="sm" label={copied ? 'Copied' : 'Copy response'} onClick={handleCopy}>
        {copied ? <Check className="h-3.5 w-3.5 text-success-500" /> : <Copy className="h-3.5 w-3.5" />}
      </IconButton>
      <IconButton size="sm" label="Regenerate response" onClick={onRegenerate}>
        <RefreshCw className="h-3.5 w-3.5" />
      </IconButton>
      <IconButton
        size="sm"
        label={liked ? 'Remove like' : 'Like response'}
        onClick={onLike}
      >
        <ThumbsUp className={cn('h-3.5 w-3.5', liked && 'text-accent-500 fill-accent-500')} />
      </IconButton>
      <IconButton
        size="sm"
        label={disliked ? 'Remove dislike' : 'Dislike response'}
        onClick={onDislike}
      >
        <ThumbsDown className={cn('h-3.5 w-3.5', disliked && 'text-danger-500 fill-danger-500')} />
      </IconButton>
      <IconButton
        size="sm"
        label={bookmarked ? 'Remove bookmark' : 'Bookmark response'}
        onClick={onBookmark}
      >
        <Bookmark className={cn('h-3.5 w-3.5', bookmarked && 'fill-accent-500 text-accent-500')} />
      </IconButton>
      <IconButton size="sm" label="Share" disabled onClick={() => {}}>
        <Share2 className="h-3.5 w-3.5" />
      </IconButton>
      {isLastMessage && (
        <IconButton size="sm" label="Continue conversation" onClick={onContinueConversation}>
          <MessageSquare className="h-3.5 w-3.5" />
        </IconButton>
      )}
    </div>
  );
}
