import { useState, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Search,
  ChevronDown,
  Eye,
  Download,
  Printer,
  AlertCircle,
  CheckCircle2,
  Clock,
  Users,
  TrendingUp,
  FileText,
  Sparkles,
  Building2,
  CalendarDays,
} from 'lucide-react';
import { Card, CardBody, CardHeader, CardTitle } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Badge } from '../../../components/ui/Badge';
import { Dropdown, type DropdownItem } from '../../../components/ui/Dropdown';
import { IconButton } from '../../../components/ui/IconButton';
import { EmptyState } from '../../../components/ui/EmptyState';
import { PageLoader } from '../../../components/ui/PageLoader';
import { useToast } from '../../../components/ui/Toast';
import { useAuth } from '../../../lib/auth-context';
import { isAdmin } from '../../../lib/access';
import { useDepartmentsList } from '../../../services/admin-hooks';
import { useProjectList } from '../../../services/project-hooks';
import {
  useAccessibleHandoverJournals,
  useAccessibleHandoverJournal,
  useGenerateHandoverJournal,
} from '../../../services/handover-hooks';
import type { HandoverJournalResponse } from '../../../services/handover-service';

type ViewMode = 'dashboard' | 'review';

interface FiltersType {
  search?: string;
  shift?: string;
  date?: string;
}

const SHIFT_OPTIONS = ['MORNING', 'EVENING'] as const;

function dateOnly(iso?: string): string {
  return iso ? iso.substring(0, 10) : '';
}

function todayStr(): string {
  return new Date().toISOString().substring(0, 10);
}

function yesterdayStr(): string {
  return new Date(Date.now() - 86400000).toISOString().substring(0, 10);
}

function shiftLabel(shift?: string): string {
  switch (shift) {
    case 'MORNING': return 'Morning';
    case 'EVENING': return 'Evening';
    default: return 'Shift not set';
  }
}

export function HandoverJournalPage({
  workspaceId: propWorkspaceId = '',
}: {
  workspaceId?: string;
  departmentId?: string;
  projectId?: string;
}) {
  const [searchParams] = useSearchParams();
  const { user } = useAuth();
  const { toast } = useToast();

  const workspaceId = propWorkspaceId || (searchParams.get('ws') ?? '');
  const isAdminUser = isAdmin(user?.roles);

  const [viewMode, setViewMode] = useState<ViewMode>('dashboard');
  const [selectedJournal, setSelectedJournal] = useState<HandoverJournalResponse | null>(null);
  const [filters, setFilters] = useState<FiltersType>({});
  const [selectedDepartmentId, setSelectedDepartmentId] = useState<string | undefined>(undefined);
  const [selectedProjectId, setSelectedProjectId] = useState<string | undefined>(undefined);

  const departmentsQuery = useDepartmentsList();
  const departments = departmentsQuery.data ?? [];

  // Effective department scope: admins may pick any department (or all); managers/members are locked to their own.
  const effectiveDepartmentId = isAdminUser ? selectedDepartmentId : (user?.departmentId || undefined);
  const effectiveDepartmentName = isAdminUser
    ? departments.find((d) => d.id === selectedDepartmentId)?.name
    : user?.departmentName;

  const { data: projectsPage } = useProjectList(
    workspaceId || undefined,
    selectedDepartmentId || undefined,
  );
  const projects = projectsPage?.content ?? [];

  const { data: journalsPage, isLoading, isError, error } = useAccessibleHandoverJournals(
    workspaceId || undefined,
    {
      departmentId: effectiveDepartmentId,
      shift: filters.shift,
      date: filters.date,
      page: 0,
      size: 100,
    },
  );
  const journals = journalsPage?.content ?? [];

  const generateMutation = useGenerateHandoverJournal(workspaceId, selectedDepartmentId, selectedProjectId);

  const handleGenerate = async () => {
    if (!workspaceId || !selectedDepartmentId || !selectedProjectId) {
      toast({ title: 'Select a department and project', description: 'Pick a department and project to generate a journal.', tone: 'warning' });
      return;
    }
    try {
      await generateMutation.mutateAsync();
      toast({ title: 'Journal generated', description: 'Your handover journal has been generated from submitted entries.', tone: 'success' });
    } catch (err) {
      toast({
        title: 'Generation failed',
        description: (err as { message?: string })?.message ?? 'An unexpected error occurred.',
        tone: 'danger',
      });
    }
  };

  const filteredJournals = useMemo(() => {
    let result = journals;

    if (filters.search) {
      const q = filters.search.toLowerCase();
      result = result.filter(
        (j) =>
          (j.departmentsIncluded ?? '').toLowerCase().includes(q) ||
          (j.generatedBy ?? '').toLowerCase().includes(q) ||
          (j.shift ?? '').toLowerCase().includes(q),
      );
    }

    return result;
  }, [journals, filters.search]);

  const sections = useMemo(() => {
    const t = todayStr();
    const y = yesterdayStr();
    return {
      today: filteredJournals.filter((j) => dateOnly(j.journalDate) === t),
      previous: filteredJournals.filter((j) => dateOnly(j.journalDate) === y),
      history: filteredJournals.filter((j) => {
        const d = dateOnly(j.journalDate);
        return d !== t && d !== y;
      }),
    };
  }, [filteredJournals]);

  const stats = {
    total: journals.length,
    generated: journals.filter((j) => j.generationStatus === 'GENERATED').length,
    pending: journals.filter((j) => j.generationStatus === 'PENDING').length,
    failed: journals.filter((j) => j.generationStatus === 'FAILED').length,
  };

  if (isLoading) {
    return <PageLoader />;
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-4">
        <AlertCircle className="h-12 w-12 text-danger-500" />
        <p className="text-body font-medium text-text-primary">Failed to load handover journals</p>
        <p className="text-caption text-text-tertiary">{(error as Error)?.message ?? 'An unexpected error occurred'}</p>
        <Button onClick={() => window.location.reload()}>Retry</Button>
      </div>
    );
  }

  if (!isAdminUser && !user?.departmentId) {
    return (
      <EmptyScope
        title="No department assigned"
        description="You are not assigned to a department. Contact your workspace administrator to be assigned one before viewing handover journals."
      />
    );
  }

  if (!workspaceId) {
    return (
      <EmptyScope
        icon={<CalendarDays />}
        title="Select a workspace"
        description="No active workspace selected. Navigate from a workspace to see its handover journals."
      />
    );
  }

  if (viewMode === 'review' && selectedJournal) {
    return (
      <ReviewView
        workspaceId={workspaceId}
        journalId={selectedJournal.id}
        departmentName={selectedJournal.departmentsIncluded}
        onBack={() => {
          setViewMode('dashboard');
          setSelectedJournal(null);
        }}
      />
    );
  }

  const departmentItems: DropdownItem[] = [
    { label: 'All Departments', onClick: () => setSelectedDepartmentId(undefined) },
    ...(departments.length > 0 ? [{ divider: true as const }] : []),
    ...departments.map((d) => ({
      label: d.name,
      onClick: () => setSelectedDepartmentId(d.id),
    })),
  ];

  const projectItems: DropdownItem[] = [
    { label: 'Select project...', onClick: () => setSelectedProjectId(undefined) },
    ...(projects.length > 0 ? [{ divider: true as const }] : []),
    ...projects.map((p) => ({
      label: p.name,
      onClick: () => setSelectedProjectId(p.id),
    })),
  ];

  const shiftItems: DropdownItem[] = [
    { label: 'All Shifts', onClick: () => setFilters((f) => ({ ...f, shift: undefined })) },
    { divider: true },
    ...SHIFT_OPTIONS.map((s) => ({
      label: shiftLabel(s),
      onClick: () => setFilters((f) => ({ ...f, shift: s })),
    })),
  ];

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-page font-semibold text-text-primary">Handover Journal</h1>
          <p className="text-body text-text-secondary">
            Shift-to-shift handover summaries from submitted employee entries.
          </p>
        </div>
        {isAdminUser && (
          <Button leftIcon={<Sparkles />} loading={generateMutation.isPending} onClick={handleGenerate}>
            Generate Journal
          </Button>
        )}
      </div>

      {/* Department scope banner */}
      {isAdminUser ? (
        <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-center">
          <Dropdown
            trigger={
              <Button variant="outline">
                <Building2 className="h-4 w-4" />
                {selectedDepartmentId
                  ? departments.find((d) => d.id === selectedDepartmentId)?.name ?? 'Department'
                  : 'All Departments'}
                <ChevronDown className="h-3.5 w-3.5" />
              </Button>
            }
            items={departmentItems}
          />
          <Dropdown
            trigger={
              <Button variant="outline">
                <FileText className="h-4 w-4" />
                {selectedProjectId
                  ? projects.find((p) => p.id === selectedProjectId)?.name ?? 'Project'
                  : 'Project (for generation)'}
                <ChevronDown className="h-3.5 w-3.5" />
              </Button>
            }
            items={projectItems}
          />
        </div>
      ) : (
        <div className="flex items-center gap-2 rounded-lg border border-border-subtle bg-surface-2 px-3 py-2 w-fit">
          <Building2 className="h-4 w-4 text-text-tertiary" />
          <span className="text-caption text-text-secondary">
            Viewing <strong className="text-text-primary">{effectiveDepartmentName ?? 'your department'}</strong>
          </span>
        </div>
      )}

      {/* Analytics */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <AnalyticsWidget icon={<FileText />} label="Journals" value={stats.total} tone="info" />
        <AnalyticsWidget icon={<CheckCircle2 />} label="Generated" value={stats.generated} tone="success" />
        <AnalyticsWidget icon={<Clock />} label="Pending" value={stats.pending} tone="warning" />
        <AnalyticsWidget icon={<TrendingUp />} label="Total Entries" value={journals.reduce((s, j) => s + (j.entriesCount ?? 0), 0)} tone="accent" />
      </div>

      {/* Toolbar */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
        <div className="flex-1">
          <Input
            placeholder="Search reports..."
            leftIcon={<Search />}
            value={filters.search ?? ''}
            onChange={(e) => setFilters((f) => ({ ...f, search: e.target.value }))}
            containerClassName="w-full"
          />
        </div>
        <div className="flex gap-2">
          <Dropdown
            trigger={
              <Button variant="outline">
                Shift {filters.shift ? `: ${shiftLabel(filters.shift)}` : ''}
                <ChevronDown className="h-3.5 w-3.5" />
              </Button>
            }
            items={shiftItems}
          />
          <Input
            type="date"
            value={filters.date ?? ''}
            onChange={(e) => setFilters((f) => ({ ...f, date: e.target.value || undefined }))}
            className="w-44"
            aria-label="Filter by date"
          />
        </div>
      </div>

      {/* Sections */}
      {filteredJournals.length === 0 ? (
        <EmptyState
          icon={<FileText />}
          title="No journals found"
          description="No handover journals match your filters yet. Journals are generated from submitted daily entries."
        />
      ) : (
        <div className="flex flex-col gap-8">
          <JournalSection
            title="Today's Journal"
            journals={sections.today}
            onView={(j) => {
              setSelectedJournal(j);
              setViewMode('review');
            }}
          />
          <JournalSection
            title="Previous Shift Journal"
            journals={sections.previous}
            onView={(j) => {
              setSelectedJournal(j);
              setViewMode('review');
            }}
          />
          <JournalSection
            title="History"
            journals={sections.history}
            onView={(j) => {
              setSelectedJournal(j);
              setViewMode('review');
            }}
          />
        </div>
      )}
    </div>
  );
}

/* ---------- Section + Card ---------- */

function JournalSection({
  title,
  journals,
  onView,
}: {
  title: string;
  journals: HandoverJournalResponse[];
  onView: (j: HandoverJournalResponse) => void;
}) {
  if (journals.length === 0) return null;
  return (
    <div className="flex flex-col gap-3">
      <SectionHeader title={title} count={journals.length} />
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {journals.map((j) => (
          <JournalCard key={j.id} journal={j} onView={() => onView(j)} />
        ))}
      </div>
    </div>
  );
}

function SectionHeader({ title, count }: { title: string; count: number }) {
  return (
    <div className="flex items-center gap-2">
      <h2 className="text-section font-semibold text-text-primary">{title}</h2>
      <Badge tone="neutral" variant="soft">{count}</Badge>
    </div>
  );
}

function JournalCard({ journal, onView }: { journal: HandoverJournalResponse; onView: () => void }) {
  const { toast } = useToast();
  const statusTone = journal.generationStatus === 'GENERATED' ? 'success' : journal.generationStatus === 'FAILED' ? 'danger' : 'warning';
  const statusLabel = journal.generationStatus === 'GENERATED' ? 'Generated' : journal.generationStatus === 'FAILED' ? 'Failed' : 'Pending';
  const shiftEmoji = journal.shift === 'EVENING' ? '🌆' : '🌅';

  const actionItems: DropdownItem[] = [
    { label: 'View Report', icon: <Eye className="h-4 w-4" />, onClick: onView },
    { label: 'Download PDF', icon: <Download className="h-4 w-4" />, onClick: () => toast({ title: 'Coming soon', tone: 'info' }) },
    { label: 'Print', icon: <Printer className="h-4 w-4" />, onClick: () => toast({ title: 'Coming soon', tone: 'info' }) },
  ];

  return (
    <Card className="hover:border-border-default transition-colors">
      <CardBody className="space-y-4">
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-2">
            <span className="text-2xl">{shiftEmoji}</span>
            <div>
              <p className="text-body font-semibold text-text-primary">Handover Journal</p>
              <p className="text-caption text-text-secondary">
                {journal.departmentsIncluded ?? 'Department'} • {journal.projectId?.substring(0, 8) ?? ''}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Badge tone={statusTone} variant="soft">{statusLabel}</Badge>
            <Dropdown trigger={<IconButton label="Actions" variant="ghost">⋯</IconButton>} items={actionItems} align="right" />
          </div>
        </div>

        <div className="space-y-3 py-3 border-t border-b border-border-subtle">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-caption text-text-secondary">
            <span className="flex items-center gap-1.5"><CalendarDays className="h-3.5 w-3.5" /> {journal.journalDate ?? '—'}</span>
            <span className="flex items-center gap-1.5"><Clock className="h-3.5 w-3.5" /> Generated {journal.generationDate ?? '—'}</span>
            <span className="flex items-center gap-1.5">Shift: {shiftLabel(journal.shift)}</span>
            <span className="flex items-center gap-1.5">
              <Users className="h-3.5 w-3.5" /> Entries {journal.entriesCount ?? 0}
              {journal.journalVersion ? ` • v${journal.journalVersion}` : ''}
            </span>
          </div>
          {journal.generatedBy && (
            <span className="flex items-center gap-1 text-caption text-text-secondary">
              <Sparkles className="h-3.5 w-3.5 text-accent-600" /> Generated by {journal.generatedBy}
            </span>
          )}
          {journal.generatedSummary && (
            <p className="line-clamp-3 text-caption text-text-secondary leading-relaxed">
              {journal.generatedSummary}
            </p>
          )}
        </div>

        <Button fullWidth onClick={onView} variant="outline">View Full Report</Button>
      </CardBody>
    </Card>
  );
}

/* ---------- Review (report) view ---------- */

function ReviewView({
  workspaceId,
  journalId,
  departmentName,
  onBack,
}: {
  workspaceId: string;
  journalId: string;
  departmentName?: string;
  onBack: () => void;
}) {
  const { toast } = useToast();
  const { data: journal, isLoading } = useAccessibleHandoverJournal(workspaceId, journalId);

  const shiftDisplay: Record<string, string> = {
    MORNING: 'Morning (6 AM - 2 PM)',
    EVENING: 'Evening (2 PM - 10 PM)',
  };

  if (isLoading) {
    return <PageLoader />;
  }

  if (!journal) {
    return (
      <Card className="mb-6">
        <CardBody>
          <div className="flex flex-col items-center justify-center py-10 gap-3">
            <FileText className="h-8 w-8 text-text-tertiary" />
            <p className="text-body text-text-secondary">Journal details not available</p>
          </div>
          <Button variant="outline" onClick={onBack}>Back</Button>
        </CardBody>
      </Card>
    );
  }

  const actionItems: DropdownItem[] = [
    { label: 'Print', icon: <Printer className="h-4 w-4" />, onClick: () => toast({ title: 'Coming soon', tone: 'info' }) },
    { label: 'Export PDF', icon: <Download className="h-4 w-4" />, onClick: () => toast({ title: 'Coming soon', tone: 'info' }) },
  ];

  return (
    <div className="max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <button
          onClick={onBack}
          className="flex h-9 w-9 items-center justify-center rounded-lg border border-border-subtle text-text-secondary hover:bg-surface-2 hover:text-text-primary transition-colors"
          aria-label="Back"
        >
          <ChevronDown className="h-4 w-4 rotate-90" />
        </button>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => toast({ title: 'Coming soon', tone: 'info' })}><Printer className="h-4 w-4" /> Print</Button>
          <Dropdown trigger={<IconButton label="Actions" variant="ghost">⋯</IconButton>} items={actionItems} align="right" />
        </div>
      </div>

      <Card className="mb-6 print:border-0 print:shadow-none">
        <CardHeader>
          <CardTitle>Handover Report</CardTitle>
        </CardHeader>
        <CardBody>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
            <ReportInfoBox label="Department" value={departmentName ?? journal.departmentsIncluded ?? '—'} />
            <ReportInfoBox label="Project" value={journal.projectId ?? '—'} />
            <ReportInfoBox label="Shift" value={shiftDisplay[journal.shift ?? ''] ?? shiftLabel(journal.shift)} />
            <ReportInfoBox label="Date" value={journal.journalDate ?? '—'} />
            <ReportInfoBox label="Generated" value={journal.generationDate ?? '—'} />
            <ReportInfoBox label="Entries" value={String(journal.entriesCount ?? 0)} />
            <ReportInfoBox label="Version" value={journal.journalVersion ? `v${journal.journalVersion}` : 'v1'} />
            <ReportInfoBox label="Generated by" value={journal.generatedBy ?? '—'} />
            <ReportInfoBox label="Status" value={journal.generationStatus} />
          </div>
        </CardBody>
      </Card>

      <ReportBlock title="Executive Summary" content={journal.generatedSummary} />
      <ReportBlock title="Completed Work" content={journal.mainDoneWork} />
      <ReportBlock title="Remaining Work" content={journal.mainRemainingWork} />
      <ReportBlock title="Blockers" content={journal.blockers} danger />
      <ReportBlock title="Difficulties" content={journal.difficulties} />
      <ReportBlock title="Recommendations" content={journal.recommendations} />

      <div className="text-center py-6 text-caption text-text-tertiary border-t border-border-subtle">
        <p>This report was automatically generated by Collabix</p>
        <p className="mt-1">Report ID: {journalId}</p>
      </div>
    </div>
  );
}

function ReportBlock({ title, content, danger }: { title: string; content?: string; danger?: boolean }) {
  if (!content) return null;
  return (
    <Card className={`mb-6 print:border-0 print:shadow-none ${danger ? 'border-danger-200 dark:border-danger-800' : ''}`}>
      <CardHeader><CardTitle>{title}</CardTitle></CardHeader>
      <CardBody>
        <p className={`text-body leading-relaxed whitespace-pre-wrap ${danger ? 'text-danger-700 dark:text-danger-200' : 'text-text-secondary'}`}>
          {content}
        </p>
      </CardBody>
    </Card>
  );
}

function ReportInfoBox({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-2xs text-text-tertiary font-medium mb-1">{label}</p>
      <p className="text-body font-semibold text-text-primary break-all">{value}</p>
    </div>
  );
}

function AnalyticsWidget({ icon, label, value, tone }: { icon: React.ReactNode; label: string; value: string | number; tone: string }) {
  const bgColor: Record<string, string> = {
    accent: 'bg-accent-50 dark:bg-accent-100',
    success: 'bg-success-50 dark:bg-success-100',
    warning: 'bg-warning-50 dark:bg-warning-100',
    info: 'bg-info-50 dark:bg-info-100',
    danger: 'bg-danger-50 dark:bg-danger-100',
  };
  const textColor: Record<string, string> = {
    accent: 'text-accent-700 dark:text-accent-200',
    success: 'text-success-700 dark:text-success-200',
    warning: 'text-warning-700 dark:text-warning-200',
    info: 'text-info-700 dark:text-info-200',
    danger: 'text-danger-700 dark:text-danger-200',
  };
  return (
    <div className={`rounded-lg p-3 border border-border-subtle ${bgColor[tone]} ${textColor[tone]}`}>
      <div className="flex items-center gap-2 mb-2">
        <span className="[&>svg]:h-4 [&>svg]:w-4">{icon}</span>
        <p className="text-2xs font-medium opacity-75">{label}</p>
      </div>
      <p className="text-display font-bold">{value}</p>
    </div>
  );
}

function EmptyScope({ icon, title, description }: { icon?: React.ReactNode; title: string; description: string }) {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1.5">
        <h1 className="text-page font-semibold text-text-primary">Handover Journal</h1>
        <p className="text-body text-text-secondary">
          Automatically generated work summaries from employee handover entries.
        </p>
      </div>
      <Card>
        <CardBody>
          <EmptyState icon={icon ?? <FileText />} title={title} description={description} />
        </CardBody>
      </Card>
    </div>
  );
}