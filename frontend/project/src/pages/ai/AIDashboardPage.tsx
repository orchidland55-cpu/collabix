import { useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import {
  BarChart3,
  ScrollText,
  BookOpen,
  FileText,
  MessageSquare,
  Sparkles,
  AlertCircle,
  RefreshCw,
  Clock,
  Bot,
} from 'lucide-react';
import { AIHeader } from '../../components/ai/AIHeader';
import { AISection } from '../../components/ai/AISection';
import { AIHero } from '../../components/ai/AIHero';
import { AIStatCard } from '../../components/ai/AIStatCard';
import { AIActionCard } from '../../components/ai/AIActionCard';
import { AIActivityCard, type AIActivityItem } from '../../components/ai/AIActivityCard';
import { AIConversationPreview } from '../../components/ai/AIConversationPreview';
import { AIReportPreview, type AIReportItem } from '../../components/ai/AIReportPreview';
import { AIPromptPreview } from '../../components/ai/AIPromptPreview';
import { Card, CardBody } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../lib/auth-context';
import { useDepartmentDashboard } from '../../services/department-hooks';
import { useWorkspaceDashboard } from '../../services/workspace-hooks';
import { useAIReportHistory } from '../../services/reporting-ai-hooks';
import { useConversationsList } from '../../services/conversation-hooks';
import { useAIPrompts } from '../../services/prompt-ai-hooks';
import { useAIPermissions } from '../../hooks/useAIPermissions';
import { aiPath, useEffectiveWorkspaceId } from '../../hooks/useEffectiveWorkspaceId';

function getGreeting(): string {
  const h = new Date().getHours();
  if (h < 12) return 'Good Morning';
  if (h < 17) return 'Good Afternoon';
  return 'Good Evening';
}

export function AIDashboardPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const workspaceId = useEffectiveWorkspaceId();
  const {
    isAdmin,
    isMember,
    canGenerateAnalytics,
    canGenerateReports,
    canGenerateHandover,
    canReadHandover,
    canUseKnowledgeAI,
    canReadReports,
    departmentId,
  } = useAIPermissions();

  const useWorkspaceScope = isAdmin || !departmentId;
  const {
    data: workspaceDashboard,
    isLoading: wsLoading,
    isError: wsError,
    refetch: refetchWs,
  } = useWorkspaceDashboard(useWorkspaceScope ? workspaceId : undefined);

  const {
    data: deptDashboard,
    isLoading: deptLoading,
    isError: deptError,
    refetch: refetchDept,
  } = useDepartmentDashboard(
    !useWorkspaceScope ? workspaceId : undefined,
    !useWorkspaceScope ? departmentId : undefined,
  );

  const { data: reportHistory } = useAIReportHistory(canReadReports ? workspaceId : undefined);
  const { data: conversationsPage } = useConversationsList(workspaceId);
  const { data: prompts } = useAIPrompts();

  const isLoading = useWorkspaceScope ? wsLoading : deptLoading;
  const isError = useWorkspaceScope ? wsError : deptError;
  const refetch = useWorkspaceScope ? refetchWs : refetchDept;

  const quickActions = [
    canGenerateAnalytics && {
      id: 'analytics',
      icon: <BarChart3 />,
      title: 'Analytics AI',
      description: 'Analyze dashboards and business metrics.',
      path: aiPath('/app/ai/analytics', workspaceId),
    },
    canGenerateReports && {
      id: 'reports',
      icon: <FileText />,
      title: 'Reporting AI',
      description: 'Generate professional executive reports.',
      path: aiPath('/app/ai/reports', workspaceId),
    },
    canUseKnowledgeAI && {
      id: 'knowledge',
      icon: <BookOpen />,
      title: 'Knowledge AI',
      description: 'Search and explain company knowledge.',
      path: aiPath('/app/ai/knowledge', workspaceId),
    },
    (canGenerateHandover || canReadHandover) && {
      id: 'handover',
      icon: <ScrollText />,
      title: 'Handover AI',
      description: 'Review handover journals and work continuity.',
      path: aiPath('/app/ai/handover', workspaceId),
    },
    {
      id: 'conversations',
      icon: <MessageSquare />,
      title: 'Conversations',
      description: 'Workspace messaging and collaboration.',
      path: aiPath('/app/ai/conversations', workspaceId),
    },
    canReadReports && {
      id: 'history',
      icon: <Clock />,
      title: 'History & Reports',
      description: 'View previous AI generations and reports.',
      path: aiPath('/app/ai/history', workspaceId),
    },
  ].filter(Boolean) as { id: string; icon: ReactNode; title: string; description: string; path: string }[];

  if (!workspaceId) {
    return (
      <div className="flex flex-col items-center justify-center py-20 animate-fade-in">
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-surface-2 text-text-tertiary">
          <Bot className="h-6 w-6" />
        </div>
        <h3 className="text-section font-semibold text-text-primary">No workspace selected</h3>
        <p className="mt-1 max-w-sm text-body text-text-tertiary text-center">
          Select a workspace from the top bar to use Collabix AI.
        </p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6 animate-fade-in">
        <div className="rounded-2xl border border-border-subtle bg-elevated p-8 sm:p-10">
          <div aria-hidden="true" className="h-4 w-32 rounded-md bg-surface-2 animate-shimmer mb-4" />
          <div aria-hidden="true" className="h-8 w-96 max-w-full rounded-md bg-surface-2 animate-shimmer mb-3" />
          <div aria-hidden="true" className="h-5 w-[500px] max-w-full rounded-md bg-surface-2 animate-shimmer mb-6" />
          <div className="flex gap-3">
            <div aria-hidden="true" className="h-11 w-44 rounded-lg bg-surface-2 animate-shimmer" />
            <div aria-hidden="true" className="h-11 w-48 rounded-lg bg-surface-2 animate-shimmer" />
          </div>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center py-20 animate-fade-in">
        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-danger-50 text-danger-500 dark:bg-danger-500/10">
          <AlertCircle className="h-6 w-6" />
        </div>
        <h3 className="text-section font-semibold text-text-primary">Unable to load AI overview</h3>
        <p className="mt-1 max-w-sm text-body text-text-tertiary text-center">
          We could not retrieve workspace data. Check your connection and try again.
        </p>
        <Button variant="secondary" className="mt-6" leftIcon={<RefreshCw className="h-4 w-4" />} onClick={() => refetch()}>
          Retry
        </Button>
      </div>
    );
  }

  const aiStats = useWorkspaceScope && workspaceDashboard
    ? [
        { id: 'projects', icon: <FileText />, label: 'Active Projects', value: String(workspaceDashboard.projectSummary.activeCount), description: `${workspaceDashboard.projectSummary.totalCount} total`, tone: 'accent' as const },
        { id: 'tasks', icon: <Sparkles />, label: 'Active Tasks', value: String(workspaceDashboard.taskSummary.activeTasks), description: `${workspaceDashboard.taskSummary.overdueTasks} overdue`, tone: 'success' as const },
        { id: 'members', icon: <MessageSquare />, label: 'Members', value: String(workspaceDashboard.memberSummary.totalCount), description: `${workspaceDashboard.memberSummary.activeCount} active`, tone: 'info' as const },
        { id: 'departments', icon: <BookOpen />, label: 'Departments', value: String(workspaceDashboard.departmentSummary.totalCount), description: 'In workspace', tone: 'warning' as const },
        { id: 'teams', icon: <BarChart3 />, label: 'Teams', value: String(workspaceDashboard.teamSummary.totalCount), description: `${workspaceDashboard.teamSummary.activeCount} active`, tone: 'neutral' as const },
        { id: 'reports', icon: <Clock />, label: 'AI Reports', value: String(reportHistory?.content?.length ?? 0), description: 'Generated', tone: 'neutral' as const },
      ]
    : deptDashboard
      ? [
          { id: 'projects', icon: <FileText />, label: 'Active Projects', value: String(deptDashboard.overview?.activeProjects ?? 0), description: deptDashboard.overview?.departmentName ?? 'Department', tone: 'accent' as const },
          { id: 'tasks', icon: <Sparkles />, label: 'Active Tasks', value: String(deptDashboard.taskSummary?.activeTasks ?? 0), description: `${deptDashboard.taskSummary?.overdueTasks ?? 0} overdue`, tone: 'success' as const },
          { id: 'members', icon: <MessageSquare />, label: 'Team Size', value: String(deptDashboard.overview?.totalMembers ?? 0), description: `${deptDashboard.overview?.activeMembers ?? 0} active`, tone: 'info' as const },
          { id: 'teams', icon: <BarChart3 />, label: 'Teams', value: String(deptDashboard.overview?.totalTeams ?? 0), description: 'Active teams', tone: 'neutral' as const },
          { id: 'reports', icon: <Clock />, label: 'AI Reports', value: String(reportHistory?.content?.length ?? 0), description: 'Generated', tone: 'neutral' as const },
        ]
      : [];

  const recentActivities: AIActivityItem[] = (
    useWorkspaceScope
      ? (workspaceDashboard?.recentActivities ?? [])
      : (deptDashboard?.departmentActivities ?? [])
  ).slice(0, 5).map((a) => ({
    id: a.id,
    icon: <MessageSquare />,
    title: a.description,
    description: ('projectName' in a && a.projectName) || ('actorName' in a && a.actorName) || 'Activity',
    timestamp: a.createdAt ? new Date(a.createdAt).toLocaleDateString() : 'recent',
  }));

  const reportItems: AIReportItem[] = (reportHistory?.content ?? []).slice(0, 5).map((r) => ({
    id: r.reportId,
    title: r.title,
    category: r.reportType,
    date: r.generationDate ? new Date(r.generationDate).toLocaleDateString() : '',
    description: r.executiveSummary?.slice(0, 100) ?? '',
  }));

  const conversationItems = (conversationsPage?.content ?? []).slice(0, 5).map((c) => ({
    id: c.id,
    title: c.name,
    preview: c.lastMessagePreview ?? 'No messages yet',
    timestamp: c.lastMessageAt ? new Date(c.lastMessageAt).toLocaleDateString() : '',
  }));

  const promptItems = (prompts ?? []).slice(0, 4).map((p) => ({
    id: p.id,
    title: p.name,
    category: p.category,
    description: p.description ?? p.code,
  }));

  const scopeLabel = isAdmin
    ? 'Workspace-wide overview'
    : isMember
      ? 'Your authorized department overview'
      : `${user?.departmentName ?? 'Department'} overview`;

  return (
    <div className="flex flex-col gap-8 animate-fade-in">
      <AIHeader />

      <AIHero
        greeting={`${getGreeting()}, ${user?.firstName ?? 'there'}.`}
        title="Collabix AI Overview"
        description={`${scopeLabel}. Use the modules below to analyze data, generate reports, and explore knowledge.`}
      />

      {isMember && (
        <Card>
          <CardBody className="py-3">
            <p className="text-caption text-text-secondary">
              Report generation is limited to admins and managers. You can read authorized reports and use Knowledge AI within your scope.
            </p>
          </CardBody>
        </Card>
      )}

      {quickActions.length > 0 && (
        <AISection title="AI Modules" description="Available tools for your role">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {quickActions.map((action) => (
              <AIActionCard
                key={action.id}
                icon={action.icon}
                title={action.title}
                description={action.description}
                onClick={() => navigate(action.path)}
              />
            ))}
          </div>
        </AISection>
      )}

      {aiStats.length > 0 && (
        <AISection title="Workspace Metrics" description="Real data from your workspace">
          <div className="grid gap-4 grid-cols-2 sm:grid-cols-3 lg:grid-cols-5">
            {aiStats.map((stat) => (
              <AIStatCard key={stat.id} icon={stat.icon} label={stat.label} value={stat.value} description={stat.description} tone={stat.tone} />
            ))}
          </div>
        </AISection>
      )}

      <div className="grid gap-8 lg:grid-cols-2">
        <AISection title="Recent Activity">
          <Card>
            <CardBody>
              {recentActivities.length === 0 ? (
                <p className="text-caption text-text-tertiary py-4 text-center">No recent activity in this scope.</p>
              ) : (
                <AIActivityCard items={recentActivities} />
              )}
            </CardBody>
          </Card>
        </AISection>

        <div className="flex flex-col gap-8">
          {canReadReports && (
            <AIReportPreview
              items={reportItems}
              onOpen={(id) => navigate(aiPath(`/app/ai/report/${id}`, workspaceId))}
              onViewAll={() => navigate(aiPath('/app/ai/history', workspaceId))}
            />
          )}
          <AIConversationPreview
            items={conversationItems}
            onOpen={(id) => navigate(aiPath(`/app/ai/conversations/${id}`, workspaceId))}
            onViewAll={() => navigate(aiPath('/app/ai/conversations', workspaceId))}
          />
        </div>
      </div>

      {promptItems.length > 0 && (
        <AIPromptPreview
          items={promptItems.map((p) => ({ id: p.id, title: p.title, category: p.category, description: p.description }))}
          onViewAll={() => navigate(aiPath('/app/ai/prompts', workspaceId))}
        />
      )}
    </div>
  );
}
