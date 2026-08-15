import { useState, useRef, useEffect } from 'react';
import { Edit3, Heart, Trash2, Share2, MoreVertical, Check, X, Download, Info } from 'lucide-react';
import { cn } from '../../../lib/cn';
import { IconButton } from '../../ui/IconButton';

interface ConversationHeaderProps {
  title: string;
  updatedAt: string;
  favorite: boolean;
  onRename: (title: string) => void;
  onToggleFavorite: () => void;
  onDelete: () => void;
  onToggleContextPanel: () => void;
  contextPanelOpen: boolean;
}

export function ConversationHeader({
  title,
  updatedAt,
  favorite,
  onRename,
  onToggleFavorite,
  onDelete,
  onToggleContextPanel,
  contextPanelOpen,
}: ConversationHeaderProps) {
  const [editing, setEditing] = useState(false);
  const [editValue, setEditValue] = useState(title);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (editing && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [editing]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setSettingsOpen(false);
      }
    }
    if (settingsOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [settingsOpen]);

  function handleSave() {
    if (editValue.trim() && editValue.trim() !== title) {
      onRename(editValue.trim());
    }
    setEditing(false);
  }

  function handleCancel() {
    setEditValue(title);
    setEditing(false);
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter') handleSave();
    if (e.key === 'Escape') handleCancel();
  }

  return (
    <div className="flex items-center justify-between gap-4 px-6 py-4 border-b border-border-subtle bg-elevated dark:bg-surface rounded-t-xl">
      <div className="flex items-center gap-3 min-w-0 flex-1">
        <div className="flex-1 min-w-0">
          {editing ? (
            <div className="flex items-center gap-2">
              <input
                ref={inputRef}
                type="text"
                value={editValue}
                onChange={(e) => setEditValue(e.target.value)}
                onKeyDown={handleKeyDown}
                aria-label="Conversation title"
                className="flex-1 rounded-lg border border-accent-500 bg-surface px-3 py-1.5 text-body font-medium text-text-primary focus:outline-none focus:ring-2 focus:ring-accent-500"
              />
              <IconButton size="sm" label="Save" onClick={handleSave}>
                <Check className="h-4 w-4" />
              </IconButton>
              <IconButton size="sm" label="Cancel" onClick={handleCancel}>
                <X className="h-4 w-4" />
              </IconButton>
            </div>
          ) : (
            <h2 className="text-section font-semibold text-text-primary truncate">{title}</h2>
          )}
          <p className="text-2xs text-text-tertiary mt-0.5">Last updated {updatedAt}</p>
        </div>
      </div>

      <div className="flex items-center gap-1">
        <IconButton
          size="sm"
          label="Rename conversation"
          onClick={() => { setEditing(true); setEditValue(title); }}
        >
          <Edit3 className="h-4 w-4" />
        </IconButton>
        <IconButton
          size="sm"
          label={favorite ? 'Remove from favorites' : 'Add to favorites'}
          onClick={onToggleFavorite}
        >
          <Heart className={cn('h-4 w-4', favorite && 'fill-danger-500 text-danger-500')} />
        </IconButton>
        <IconButton
          size="sm"
          label="Context panel"
          onClick={onToggleContextPanel}
        >
          <Info className={cn('h-4 w-4', contextPanelOpen && 'text-accent-600')} />
        </IconButton>

        <div className="relative" ref={menuRef}>
          <IconButton
            size="sm"
            label="Conversation settings"
            onClick={() => setSettingsOpen(!settingsOpen)}
          >
            <MoreVertical className="h-4 w-4" />
          </IconButton>
          {settingsOpen && (
            <div
              role="menu"
              aria-label="Conversation actions"
              className="absolute right-0 top-full mt-1 z-50 w-48 rounded-xl border border-border-subtle bg-elevated shadow-cx-lg py-1 animate-fade-in"
            >
              <button
                type="button"
                role="menuitem"
                onClick={() => { setEditing(true); setEditValue(title); setSettingsOpen(false); }}
                className="flex w-full items-center gap-2.5 px-4 py-2 text-body text-text-secondary hover:bg-surface-2 hover:text-text-primary transition-colors"
              >
                <Edit3 className="h-4 w-4" />
                Rename
              </button>
              <button
                type="button"
                role="menuitem"
                onClick={() => { onToggleFavorite(); setSettingsOpen(false); }}
                className="flex w-full items-center gap-2.5 px-4 py-2 text-body text-text-secondary hover:bg-surface-2 hover:text-text-primary transition-colors"
              >
                <Heart className={cn('h-4 w-4', favorite && 'fill-danger-500 text-danger-500')} />
                {favorite ? 'Remove from Favorites' : 'Add to Favorites'}
              </button>
              <button
                type="button"
                role="menuitem"
                disabled
                title="Sharing is not available yet"
                className="flex w-full items-center gap-2.5 px-4 py-2 text-body text-text-tertiary cursor-not-allowed opacity-60"
              >
                <Share2 className="h-4 w-4" />
                Share
              </button>
              <button
                type="button"
                role="menuitem"
                disabled
                title="Export is not available yet"
                className="flex w-full items-center gap-2.5 px-4 py-2 text-body text-text-tertiary cursor-not-allowed opacity-60"
              >
                <Download className="h-4 w-4" />
                Export
              </button>
              <div className="my-1 border-t border-border-subtle" />
              <button
                type="button"
                role="menuitem"
                onClick={() => { onDelete(); setSettingsOpen(false); }}
                className="flex w-full items-center gap-2.5 px-4 py-2 text-body text-danger-500 hover:bg-danger-50 dark:hover:bg-danger-500/10 transition-colors"
              >
                <Trash2 className="h-4 w-4" />
                Delete Conversation
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
