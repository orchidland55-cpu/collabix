import { useState, useMemo, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ArrowLeft, AlertCircle } from 'lucide-react';
import { Tabs, type TabItem } from '../../components/ui/Tabs';
import { Badge } from '../../components/ui/Badge';
import { Skeleton } from '../../components/ui/Skeleton';
import { Card, CardBody } from '../../components/ui/Card';
import { EmptyState } from '../../components/ui/EmptyState';
import { useDepartmentDetail } from '../../services/department-hooks';
import { detectDeptType, type DeptType } from '../../lib/access';
import { DEPT_TABS } from './department-tabs';

/* HR workflows */
import { HrDashboardTab } from './hr/DashboardTab';
import { CandidatesTab } from './hr/CandidatesTab';
import { EmployeesTab } from './hr/EmployeesTab';
import { SkillsTab } from './hr/SkillsTab';
import { OnboardingTab } from './hr/OnboardingTab';
import { PerformanceReviewsTab } from './hr/PerformanceReviewsTab';
import { InterviewsTab } from './hr/InterviewsTab';
import { AttendanceTab } from './hr/AttendanceTab';
import { NotificationsTab } from './hr/NotificationsTab';
import { DocumentsTab as HrDocumentsTab } from './hr/DocumentsTab';
import { HrReportsTab } from './hr/ReportsTab';
import { HrAnalyticsTab } from './hr/AnalyticsTab';

/* Development workflows */
import { DevelopmentDashboardTab } from './development/DashboardTab';
import { DevelopmentProjectsTab } from './development/ProjectsTab';
import { DevelopmentSprintsTab } from './development/SprintsTab';
import { DevelopmentTasksTab } from './development/TasksTab';
import { DevelopmentAnalyticsTab } from './development/AnalyticsTab';

/* AI workflows */
import { AIDashboardTab } from './ai/DashboardTab';
import { ModelsTab } from './ai/ModelsTab';
import { AIAnalyticsTab } from './ai/AnalyticsTab';

/* Marketing workflows */
import { MarketingDashboardTab } from './marketing/DashboardTab';
import { MarketingCampaignsTab } from './marketing/CampaignsTab';
import { MarketingAnalyticsTab } from './marketing/AnalyticsTab';

/* Cybersecurity workflows */
import { CybersecurityDashboardTab } from './cybersecurity/DashboardTab';
import { SecurityAuditsTab } from './cybersecurity/SecurityAuditsTab';
import { CybersecurityAnalyticsTab } from './cybersecurity/AnalyticsTab';

/* Shared department components (same layout, department-scoped data) */
import { DeptOverview } from './common/DeptOverview';
import { DeptAnalytics } from './common/DeptAnalytics';
import { DeptDocuments } from './common/DeptDocuments';
import { DeptReports } from './common/DeptReports';
import { DeptActivity } from './common/DeptActivity';
import { DeptSettings } from './common/DeptSettings';

const tabItems: Record<DeptType, TabItem[]> = (Object.keys(DEPT_TABS) as DeptType[]).reduce(
  (acc, type) => {
    acc[type] = DEPT_TABS[type].map((t) => ({ id: t.id, label: t.label, icon: <t.icon /> }));
    return acc;
  },
  {} as Record<DeptType, TabItem[]>,
);

export function DepartmentDetailPage({ departmentId, onBack }: { departmentId: string; onBack: () => void }) {
  const [searchParams, setSearchParams] = useSearchParams();
  const wsId = searchParams.get('ws') ?? '';
  const tabParam = searchParams.get('tab');
  const [activeTab, setActiveTab] = useState(tabParam ?? 'dashboard');
  const { data: dept, isLoading, isError, error } = useDepartmentDetail(wsId || undefined, departmentId);

  const deptType = useMemo<DeptType>(() => (dept?.name ? detectDeptType(dept.name) : 'generic'), [dept?.name]);

  const tabs = tabItems[deptType] ?? tabItems.generic;

  useEffect(() => {
    if (tabParam && tabs.some((t) => t.id === tabParam)) {
      setActiveTab(tabParam);
    } else if (tabParam && tabs.length > 0) {
      setActiveTab('dashboard');
    }
  }, [tabParam, tabs]);

  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    const next = new URLSearchParams(searchParams);
    next.set('tab', tab);
    setSearchParams(next, { replace: true });
  };

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6 animate-fade-in">
        <div className="flex items-center gap-3">
          <Skeleton className="h-9 w-9 rounded-lg" />
          <div>
            <Skeleton className="h-6 w-48" />
            <Skeleton className="h-4 w-64 mt-1" />
          </div>
        </div>
        <Skeleton className="h-10 w-full rounded-lg" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <Card>
        <CardBody className="py-16">
          <EmptyState
            icon={<AlertCircle className="h-6 w-6" />}
            title="Failed to load department"
            description={error instanceof Error ? error.message : 'An error occurred.'}
          />
        </CardBody>
      </Card>
    );
  }

  const renderActive = () => {
    const shared: Record<string, React.ReactNode> = {
      documents: deptType === 'hr' ? <HrDocumentsTab wsId={wsId} deptId={departmentId} /> : <DeptDocuments wsId={wsId} deptId={departmentId} />,
      reports: deptType === 'hr' ? <HrReportsTab wsId={wsId} deptId={departmentId} /> : <DeptReports wsId={wsId} deptId={departmentId} />,
      activity: <DeptActivity wsId={wsId} deptId={departmentId} />,
      settings: <DeptSettings wsId={wsId} deptId={departmentId} onRemoved={onBack} />,
    };

    if (shared[activeTab]) return shared[activeTab];

    if (deptType === 'generic' && activeTab === 'dashboard') {
      return <DeptOverview wsId={wsId} deptId={departmentId} />;
    }

    if (deptType === 'generic' && activeTab === 'analytics') {
      return <DeptAnalytics wsId={wsId} />;
    }

    switch (deptType) {
      case 'hr':
        switch (activeTab) {
          case 'dashboard': return <HrDashboardTab wsId={wsId} deptId={departmentId} onNavigate={handleTabChange} />;
          case 'employees': return <EmployeesTab wsId={wsId} deptId={departmentId} />;
          case 'candidates': return <CandidatesTab wsId={wsId} deptId={departmentId} />;
          case 'interviews': return <InterviewsTab wsId={wsId} deptId={departmentId} />;
          case 'skills': return <SkillsTab wsId={wsId} deptId={departmentId} />;
          case 'onboarding': return <OnboardingTab wsId={wsId} deptId={departmentId} />;
          case 'reviews': return <PerformanceReviewsTab wsId={wsId} deptId={departmentId} />;
          case 'attendance': return <AttendanceTab wsId={wsId} deptId={departmentId} />;
          case 'notifications': return <NotificationsTab wsId={wsId} deptId={departmentId} />;
          case 'analytics': return <HrAnalyticsTab wsId={wsId} deptId={departmentId} />;
          default: return null;
        }
      case 'development':
        switch (activeTab) {
          case 'dashboard': return <DevelopmentDashboardTab wsId={wsId} deptId={departmentId} onNavigate={handleTabChange} />;
          case 'projects': return <DevelopmentProjectsTab wsId={wsId} deptId={departmentId} />;
          case 'sprints': return <DevelopmentSprintsTab wsId={wsId} deptId={departmentId} />;
          case 'tasks': return <DevelopmentTasksTab wsId={wsId} deptId={departmentId} />;
          case 'analytics': return <DevelopmentAnalyticsTab wsId={wsId} deptId={departmentId} />;
          default: return null;
        }
      case 'ai':
        switch (activeTab) {
          case 'dashboard': return <AIDashboardTab wsId={wsId} deptId={departmentId} onNavigate={handleTabChange} />;
          case 'models': return <ModelsTab wsId={wsId} deptId={departmentId} />;
          case 'analytics': return <AIAnalyticsTab wsId={wsId} deptId={departmentId} />;
          default: return null;
        }
      case 'marketing':
        switch (activeTab) {
          case 'dashboard': return <MarketingDashboardTab wsId={wsId} deptId={departmentId} onNavigate={handleTabChange} />;
          case 'campaigns': return <MarketingCampaignsTab wsId={wsId} deptId={departmentId} />;
          case 'analytics': return <MarketingAnalyticsTab wsId={wsId} deptId={departmentId} />;
          default: return null;
        }
      case 'cybersecurity':
        switch (activeTab) {
          case 'dashboard': return <CybersecurityDashboardTab wsId={wsId} deptId={departmentId} onNavigate={handleTabChange} />;
          case 'audits': return <SecurityAuditsTab wsId={wsId} deptId={departmentId} />;
          case 'analytics': return <CybersecurityAnalyticsTab wsId={wsId} deptId={departmentId} />;
          default: return null;
        }
      default:
        return null;
    }
  };

  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <div className="flex items-center gap-3">
        <button onClick={onBack} className="flex h-9 w-9 items-center justify-center rounded-lg text-text-secondary hover:bg-surface-2 transition-colors">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-page font-semibold text-text-primary">{dept?.name ?? 'Department'}</h1>
            <Badge tone={dept?.status === 'ACTIVE' ? 'success' : 'warning'} variant="soft">{dept?.status}</Badge>
          </div>
          <p className="text-caption text-text-tertiary mt-0.5">{dept?.description ?? 'Department workspace'}</p>
        </div>
      </div>

      <Tabs items={tabs} active={activeTab} onChange={handleTabChange} />

      {renderActive()}
    </div>
  );
}

export default DepartmentDetailPage;
