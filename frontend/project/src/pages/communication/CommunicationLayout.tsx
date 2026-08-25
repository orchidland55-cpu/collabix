import { useState } from 'react';
import { Outlet, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { MessageSquare, Hash, Bell, Search, FileText, Plus } from 'lucide-react';
import { Button } from '../../components/ui/Button';
import { cn } from '../../lib/cn';
import { CreateChannelModal } from './modals/CreateChannelModal';

const tabs = [
  { id: 'conversations', label: 'Channels', icon: Hash },
  { id: 'direct-messages', label: 'Direct Messages', icon: MessageSquare },
  { id: 'announcements', label: 'Announcements', icon: Bell },
  { id: 'search', label: 'Search', icon: Search },
  { id: 'files', label: 'Shared Files', icon: FileText },
];

function activeTabFromPathname(pathname: string): string {
  const sub = pathname.replace('/app/communication', '').split('/')[0];
  return tabs.some((t) => t.id === sub) ? sub : 'conversations';
}

export function CommunicationLayout() {
  const [searchParams] = useSearchParams();
  const location = useLocation();
  const wsId = searchParams.get('ws') ?? '';
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState(() => activeTabFromPathname(location.pathname));

  // Keep the tab highlight in sync with deep links and sidebar navigation.
  const urlTab = activeTabFromPathname(location.pathname);
  if (urlTab !== activeTab) {
    setActiveTab(urlTab);
  }

  // The create-channel "route" opens the dashboard with the modal on top,
  // so every Create Channel button across the module can keep linking to it.
  const isCreateChannel = location.pathname.endsWith('/create-channel');
  const closeCreateChannel = () =>
    navigate(`/app/communication/conversations${wsId ? `?ws=${wsId}` : ''}`);

  const handleTabChange = (tabId: string) => {
    setActiveTab(tabId);
    navigate(`/app/communication/${tabId}${wsId ? `?ws=${wsId}` : ''}`);
  };

  return (
    <div className="flex h-full flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-page font-semibold text-text-primary">Communication</h1>
          <p className="text-caption text-text-tertiary mt-0.5">
            Team conversations and announcements
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="primary" onClick={() => navigate(`/app/communication/create-channel?ws=${wsId}`)}>
            <Plus className="h-4 w-4" />
            Create Channel
          </Button>
        </div>
      </div>

      <div className="flex gap-3 border-b border-border-subtle pb-2">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => handleTabChange(tab.id)}
              className={cn(
                'flex items-center gap-2 rounded-lg px-3 py-2 text-body font-medium transition-colors',
                isActive
                  ? 'bg-accent-50 text-accent-700 dark:bg-accent-100 dark:text-accent-200'
                  : 'text-text-tertiary hover:text-text-primary hover:bg-surface-2',
              )}
            >
              <Icon className="h-4 w-4" />
              {tab.label}
            </button>
          );
        })}
      </div>

      <div className="flex-1">
        <Outlet />
      </div>

      <CreateChannelModal open={isCreateChannel} onClose={closeCreateChannel} />
    </div>
  );
}
