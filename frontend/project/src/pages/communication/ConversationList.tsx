import { useState, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Hash, Users, Lock, Plus, Search, MoreHorizontal } from 'lucide-react';
import { Card, CardBody } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Badge } from '../../components/ui/Badge';
import { IconButton } from '../../components/ui/IconButton';
import { EmptyState } from '../../components/ui/EmptyState';
import { Tabs, type TabItem } from '../../components/ui/Tabs';
import { Dropdown } from '../../components/ui/Dropdown';
import { useConversationsList, useConversationsByType } from '../../services/conversation-hooks';
import type { ConversationResponse } from '../../types/communication';

const tabs: TabItem[] = [
  { id: 'all', label: 'All Channels' },
  { id: 'WORKSPACE', label: 'Workspace' },
  { id: 'DEPARTMENT', label: 'Department' },
  { id: 'TEAM', label: 'Team' },
];

export function ConversationList() {
  const [searchParams] = useSearchParams();
  const wsId = searchParams.get('ws') ?? '';
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('all');
  const [search, setSearch] = useState('');

  const { data: allData } = useConversationsList(wsId);
  const { data: workspaceData } = useConversationsByType(wsId, 'WORKSPACE');
  const { data: deptData } = useConversationsByType(wsId, 'DEPARTMENT');
  const { data: teamData } = useConversationsByType(wsId, 'TEAM');

  const conversations = useMemo(() => {
    let items: ConversationResponse[] = [];
    switch (activeTab) {
      case 'all':
        items = allData?.content ?? [];
        break;
      case 'WORKSPACE':
        items = workspaceData?.content ?? [];
        break;
      case 'DEPARTMENT':
        items = deptData?.content ?? [];
        break;
      case 'TEAM':
        items = teamData?.content ?? [];
        break;
    }
    if (search) {
      items = items.filter((c) =>
        c.name.toLowerCase().includes(search.toLowerCase()),
      );
    }
    return items;
  }, [activeTab, allData, workspaceData, deptData, teamData, search]);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-4">
        <div className="flex-1 max-w-md">
          <Input
            placeholder="Search channels..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            leftIcon={<Search className="h-4 w-4" />}
          />
        </div>
        <Button variant="primary" onClick={() => navigate(`/app/communication/create-channel?ws=${wsId}`)}>
          <Plus className="h-4 w-4" />
          New Channel
        </Button>
      </div>

      <Tabs tabs={tabs} activeTab={activeTab} onTabChange={setActiveTab} />

      {conversations.length === 0 ? (
        <Card>
          <CardBody>
            <EmptyState
              icon={<Hash className="h-8 w-8" />}
              title="No channels found"
              description={search ? 'Try a different search term.' : 'Create a new channel to get started.'}
              action={
                <Button variant="primary" onClick={() => navigate(`/app/communication/create-channel?ws=${wsId}`)}>
                  <Plus className="h-4 w-4" />
                  Create Channel
                </Button>
              }
            />
          </CardBody>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {conversations.map((conv) => (
            <button
              key={conv.id}
              type="button"
              onClick={() => navigate(`/app/communication/chat/${conv.id}?ws=${wsId}`)}
              className="flex flex-col gap-3 rounded-xl border border-border-subtle bg-elevated p-4 text-left hover:border-border-default hover:shadow-cx-sm transition-all"
            >
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2.5">
                  <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-accent-50 text-accent-600 dark:bg-accent-100 dark:text-accent-300">
                    {conv.isPrivate ? <Lock className="h-4 w-4" /> : <Hash className="h-4 w-4" />}
                  </span>
                  <div>
                    <p className="text-body font-semibold text-text-primary">{conv.name}</p>
                    <p className="text-caption text-text-tertiary capitalize">{conv.type.toLowerCase()}</p>
                  </div>
                </div>
                <Dropdown
                  trigger={<IconButton icon={<MoreHorizontal className="h-4 w-4" />} ariaLabel="Options" />}
                  items={[
                    { id: 'open', label: 'Open Channel', onClick: () => navigate(`/app/communication/chat/${conv.id}?ws=${wsId}`) },
                    { id: 'members', label: 'Open Chat & Members', onClick: () => navigate(`/app/communication/chat/${conv.id}?ws=${wsId}`) },
                  ]}
                />
              </div>
              {conv.lastMessagePreview && (
                <p className="text-caption text-text-tertiary line-clamp-2">{conv.lastMessagePreview}</p>
              )}
              <div className="flex items-center justify-between mt-auto">
                <div className="flex items-center gap-1.5 text-caption text-text-tertiary">
                  <Users className="h-3.5 w-3.5" />
                  {conv.memberCount}
                </div>
                {conv.unreadCount > 0 && (
                  <Badge variant="primary" size="sm">{conv.unreadCount}</Badge>
                )}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
