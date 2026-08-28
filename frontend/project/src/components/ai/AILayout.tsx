import { useMemo, useState, useEffect } from 'react';
import { Outlet, useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import { Sparkles, BarChart3, ScrollText, BookOpen, FileText, MessageSquare, BookMarked, Clock, X } from 'lucide-react';
import { cn } from '../../lib/cn';
import { useAIPermissions } from '../../hooks/useAIPermissions';
import { aiPath, useEffectiveWorkspaceId } from '../../hooks/useEffectiveWorkspaceId';

interface NavItem {
  id: string;
  label: string;
  icon: typeof Sparkles;
  path: string;
  visible: boolean;
}

export function AILayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const workspaceId = useEffectiveWorkspaceId();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const {
    canViewAnalytics,
    canViewReports,
    canGenerateHandover,
    canReadHandover,
    canUseKnowledgeAI,
    canReadReports,
  } = useAIPermissions();

  const aiNavItems: NavItem[] = useMemo(() => [
    { id: 'ai', label: 'AI Overview', icon: Sparkles, path: '/app/ai', visible: true },
    { id: 'ai-conversations', label: 'Conversations', icon: MessageSquare, path: '/app/ai/conversations', visible: true },
    { id: 'ai-prompts', label: 'Prompt Library', icon: BookMarked, path: '/app/ai/prompts', visible: true },
    { id: 'ai-history', label: 'History & Reports', icon: Clock, path: '/app/ai/history', visible: canReadReports },
    { id: 'ai-analytics', label: 'Analytics AI', icon: BarChart3, path: '/app/ai/analytics', visible: canViewAnalytics },
    { id: 'ai-handover', label: 'Handover AI', icon: ScrollText, path: '/app/ai/handover', visible: canGenerateHandover || canReadHandover },
    { id: 'ai-knowledge', label: 'Knowledge AI', icon: BookOpen, path: '/app/ai/knowledge', visible: canUseKnowledgeAI },
    { id: 'ai-reports', label: 'Reporting AI', icon: FileText, path: '/app/ai/reports', visible: canViewReports },
  ], [canViewAnalytics, canViewReports, canGenerateHandover, canReadHandover, canUseKnowledgeAI, canReadReports]);

  const visibleItems = aiNavItems.filter((item) => item.visible);

  const isActive = (path: string) => {
    if (path === '/app/ai') return location.pathname === '/app/ai';
    return location.pathname.startsWith(path);
  };

  function navigateTo(path: string) {
    navigate(aiPath(path, workspaceId));
    setSidebarOpen(false);
  }

  useEffect(() => {
    if (workspaceId && !searchParams.get('ws')) {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        next.set('ws', workspaceId);
        return next;
      }, { replace: true });
    }
  }, [workspaceId, searchParams, setSearchParams]);

  return (
    <div className="flex gap-0 lg:gap-6">
      <button
        type="button"
        onClick={() => setSidebarOpen(true)}
        aria-label="Open AI navigation"
        aria-expanded={sidebarOpen}
        aria-haspopup="dialog"
        className="lg:hidden fixed bottom-6 right-6 z-50 flex h-12 w-12 items-center justify-center rounded-full bg-accent-600 text-white shadow-cx-lg hover:bg-accent-700 transition-colors"
      >
        <Sparkles className="h-5 w-5" />
      </button>

      {sidebarOpen && (
        <div className="fixed inset-0 z-40 bg-text-primary/40 dark:bg-black/60 backdrop-blur-sm animate-fade-in lg:hidden" aria-hidden="true" onClick={() => setSidebarOpen(false)} />
      )}

      <div
        role={sidebarOpen ? 'dialog' : undefined}
        aria-modal={sidebarOpen ? 'true' : undefined}
        aria-label="AI navigation"
        className={cn(
          'fixed inset-y-0 left-0 z-50 w-64 shrink-0 bg-elevated border-r border-border-subtle overflow-y-auto transition-transform duration-300 lg:sticky lg:top-0 lg:z-0 lg:block lg:h-[calc(100vh-7rem)] lg:border lg:border-border-subtle lg:rounded-xl lg:bg-surface',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0',
        )}
      >
        <div className="flex items-center justify-between px-4 py-4 lg:hidden">
          <div className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-accent-600 dark:text-accent-400" />
            <p className="text-section font-semibold text-text-primary">Collabix AI</p>
          </div>
          <button
            type="button"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close AI navigation"
            className="flex h-8 w-8 items-center justify-center rounded-lg text-text-tertiary hover:bg-surface-2 hover:text-text-primary transition-colors"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="p-3 hidden lg:block">
          <div className="flex items-center gap-2 px-2 py-3">
            <Sparkles className="h-5 w-5 text-accent-600 dark:text-accent-400" />
            <p className="text-section font-semibold text-text-primary">Collabix AI</p>
          </div>
        </div>

        <nav aria-label="AI sections" className="px-2 pb-4">
          {visibleItems.map((item) => {
            const Icon = item.icon;
            const active = isActive(item.path);
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => navigateTo(item.path)}
                className={cn(
                  'flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-body transition-colors text-left',
                  active
                    ? 'bg-accent-600/10 text-accent-700 dark:bg-accent-100/15 dark:text-accent-200 font-medium'
                    : 'text-text-secondary hover:bg-surface-2 hover:text-text-primary',
                )}
              >
                <Icon className={cn('h-4 w-4 shrink-0', active && 'text-accent-600 dark:text-accent-300')} />
                <span className="flex-1">{item.label}</span>
              </button>
            );
          })}
        </nav>
      </div>

      <div className="flex-1 min-w-0 flex flex-col gap-6">
        <Outlet />
      </div>
    </div>
  );
}
