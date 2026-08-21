import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { CalendarClock, CheckCircle2, ClipboardList, FileEdit, Plus, Search, Send, Sunrise, Sunset } from 'lucide-react';
import { Card, CardBody } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Input } from '../../components/ui/Input';
import { Select } from '../../components/ui/Select';
import { Textarea } from '../../components/ui/Textarea';
import { EmptyState } from '../../components/ui/EmptyState';
import { PageLoader } from '../../components/ui/PageLoader';
import { useToast } from '../../components/ui/Toast';
import { useWorkspaceId } from '../../hooks/useWorkspaceId';
import { useProjectList } from '../../services/project-hooks';
import { useAuth } from '../../lib/auth-context';
import {
  useMyHandoverEntries,
  useCreateHandoverEntry,
  useSubmitHandover,
} from '../../services/handover-hooks';
import type { HandoverEntryResponse, HandoverShift } from '../../services/handover-service';
import { userService } from '../../services/user-service';

const SHIFTS: { value: HandoverShift; label: string }[] = [
  { value: 'MORNING', label: 'Morning' },
  { value: 'EVENING', label: 'Evening' },
];

const MOODS = [
  { value: 'FOCUSED', label: 'Focused' },
  { value: 'NORMAL', label: 'Normal' },
  { value: 'STRESSED', label: 'Stressed' },
  { value: 'OVERWHELMED', label: 'Overwhelmed' },
];

const STATUSES = ['DRAFT', 'SUBMITTED', 'PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED'];

const statusTone: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning'> = {
  DRAFT: 'neutral',
  SUBMITTED: 'success',
  PENDING: 'info',
  ACCEPTED: 'warning',
  REJECTED: 'danger',
  COMPLETED: 'success',
  ARCHIVED: 'neutral',
};

function todayInputValue() {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}

function formatDate(value?: string) {
  if (!value) return '—';
  return new Date(value).toLocaleString();
}

export function HandoverEntriesPage() {
  const wsId = useWorkspaceId();
  const { toast } = useToast();

  const [showForm, setShowForm] = useState(false);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [shiftFilter, setShiftFilter] = useState('');

  const entriesQuery = useMyHandoverEntries(wsId, {
    status: statusFilter || undefined,
    shift: shiftFilter || undefined,
    search: search || undefined,
    size: 50,
  });

  const entries = entriesQuery.data?.content ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-page font-semibold text-text-primary">Handover Entries</h1>
          <p className="text-body text-text-secondary">
            Log your daily work report so your team's handover journal can be generated automatically.
          </p>
        </div>
        <Button onClick={() => setShowForm(true)} leftIcon={<Plus />}>New Entry</Button>
      </div>

      {showForm && (
        <EntryForm
          workspaceId={wsId}
          onClose={() => setShowForm(false)}
          onSaved={() => setShowForm(false)}
        />
      )}

      <Card>
        <CardBody className="flex flex-col gap-4">
          <div className="flex flex-wrap items-end gap-3">
            <div className="min-w-[220px] flex-1">
              <Input
                label="Search"
                placeholder="Search your entries..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                leftIcon={<Search className="h-4 w-4" />}
              />
            </div>
            <div className="w-44">
              <Select
                label="Status"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                options={[
                  { value: '', label: 'All statuses' },
                  ...STATUSES.map((s) => ({ value: s, label: s })),
                ]}
              />
            </div>
            <div className="w-44">
              <Select
                label="Shift"
                value={shiftFilter}
                onChange={(e) => setShiftFilter(e.target.value)}
                options={[
                  { value: '', label: 'All shifts' },
                  ...SHIFTS.map((s) => ({ value: s.value, label: s.label })),
                ]}
              />
            </div>
          </div>

          {entriesQuery.isLoading ? (
            <PageLoader />
          ) : entries.length === 0 ? (
            <EmptyState
              icon={<ClipboardList />}
              title="No entries found"
              description="Log your first daily work entry to start building your handover journal."
              action={<Button onClick={() => setShowForm(true)} leftIcon={<Plus />}>New Entry</Button>}
            />
          ) : (
            <div className="flex flex-col gap-3">
              {entries.map((entry) => (
                <EntryRow key={entry.id} entry={entry} onEdit={() => setShowForm(true)} />
              ))}
            </div>
          )}
        </CardBody>
      </Card>
    </div>
  );

  function EntryRow({ entry, onEdit }: { entry: HandoverEntryResponse; onEdit: () => void }) {
    const submitMutation = useSubmitHandover(wsId);
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async () => {
      setSubmitting(true);
      try {
        await submitMutation.mutateAsync({ entryId: entry.id });
        toast({ title: 'Entry submitted', description: 'Your report is now part of the handover journal.', tone: 'success' });
      } catch (err) {
        toast({
          title: 'Submit failed',
          description: (err as { message?: string })?.message ?? 'An unexpected error occurred.',
          tone: 'danger',
        });
      } finally {
        setSubmitting(false);
      }
    };

    const editable = entry.status === 'DRAFT' || entry.status === 'REJECTED';

    return (
      <div className="flex flex-col gap-3 rounded-xl border border-border-subtle p-4 transition-colors hover:border-border-default">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex flex-wrap items-center gap-2">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-accent-50 text-accent-600">
              {entry.shift === 'EVENING' ? <Sunset className="h-4 w-4" /> : <Sunrise className="h-4 w-4" />}
            </span>
            <div>
              <p className="text-body font-semibold text-text-primary">
                {entry.entryDate ? `Report · ${entry.entryDate}` : entry.title || 'Handover entry'}
                {entry.shift ? ` · ${entry.shift.charAt(0) + entry.shift.slice(1).toLowerCase()}` : ''}
              </p>
              <p className="text-caption text-text-secondary">
                {entry.completedTasks ? `${entry.completedTasks.split('\n').filter(Boolean).length} completed · ` : ''}
                Submitted {formatDate(entry.submittedAt)} · Created {formatDate(entry.createdAt)}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Badge tone={statusTone[entry.status]} variant="soft">{entry.status}</Badge>
            {editable && (
              <>
                <Button size="sm" variant="outline" leftIcon={<FileEdit />} onClick={onEdit}>
                  Edit
                </Button>
                <Button size="sm" leftIcon={<Send />} loading={submitting} onClick={handleSubmit}>
                  Submit
                </Button>
              </>
            )}
            {entry.status === 'SUBMITTED' && (
              <Button size="sm" variant="outline" disabled leftIcon={<CheckCircle2 />}>
                In Journal
              </Button>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 gap-3 text-caption text-text-secondary sm:grid-cols-2">
          {entry.completedTasks && (
            <div>
              <p className="font-medium text-text-primary">Completed</p>
              <p className="whitespace-pre-line">{entry.completedTasks}</p>
            </div>
          )}
          {entry.currentProgress && (
            <div>
              <p className="font-medium text-text-primary">Current progress</p>
              <p className="whitespace-pre-line">{entry.currentProgress}</p>
            </div>
          )}
          {entry.pendingTasks && (
            <div>
              <p className="font-medium text-text-primary">Pending</p>
              <p className="whitespace-pre-line">{entry.pendingTasks}</p>
            </div>
          )}
          {entry.blockers && (
            <div>
              <p className="font-medium text-text-primary">Blockers</p>
              <p className="whitespace-pre-line">{entry.blockers}</p>
            </div>
          )}
        </div>
      </div>
    );
  }
}

function EntryForm({ workspaceId, onClose, onSaved }: { workspaceId: string; onClose: () => void; onSaved: () => void }) {
  const { toast } = useToast();
  const { user } = useAuth();
  const userDepartmentId = user?.departmentId ?? '';
  const { data: projectsPage } = useProjectList(workspaceId, userDepartmentId || undefined);
  const projects = projectsPage?.content ?? [];

  const createMutation = useCreateHandoverEntry(workspaceId);
  const submitMutation = useSubmitHandover(workspaceId);

  const { data: members } = useQuery({
    queryKey: ['handover', 'members', workspaceId],
    queryFn: () => userService(workspaceId).list(),
    enabled: !!workspaceId,
  });

  const [form, setForm] = useState({
    entryDate: todayInputValue(),
    shift: 'MORNING' as HandoverShift,
    projectId: '',
    completedTasks: '',
    currentProgress: '',
    pendingTasks: '',
    blockers: '',
    importantNotes: '',
    estimatedRemainingWork: '',
    mood: '',
    priority: 'MEDIUM',
  });
  const [mention, setMention] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState<'draft' | 'submit' | null>(null);

  const setField = (key: keyof typeof form, value: string) => {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: '' }));
  };

  const validate = () => {
    const next: Record<string, string> = {};
    if (!userDepartmentId) next.departmentId = 'Your department could not be determined';
    if (!form.projectId) next.projectId = 'Select a project';
    if (!form.entryDate) next.entryDate = 'Select a date';
    if (!form.shift) next.shift = 'Select a shift';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const buildPayload = () => ({
    departmentId: userDepartmentId,
    projectId: form.projectId,
    shift: form.shift,
    entryDate: form.entryDate || undefined,
    completedTasks: form.completedTasks.trim() || undefined,
    currentProgress: form.currentProgress.trim() || undefined,
    pendingTasks: form.pendingTasks.trim() || undefined,
    blockers: form.blockers.trim() || undefined,
    importantNotes: [mention, form.importantNotes.trim()].filter(Boolean).join('\n') || undefined,
    estimatedRemainingWork: form.estimatedRemainingWork.trim() || undefined,
    mood: form.mood || undefined,
    priority: form.priority as 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT',
  });

  const run = async (mode: 'draft' | 'submit') => {
    if (!validate()) return;
    setBusy(mode);
    try {
      const created = await createMutation.mutateAsync(buildPayload());
      if (mode === 'submit') {
        await submitMutation.mutateAsync({ entryId: created.id });
      }
      toast({
        title: mode === 'submit' ? 'Entry submitted' : 'Draft saved',
        description:
          mode === 'submit'
            ? 'Your report is now part of the handover journal.'
            : 'Your draft has been saved. You can submit it later.',
        tone: 'success',
      });
      onSaved();
    } catch (err) {
      toast({
        title: mode === 'submit' ? 'Submit failed' : 'Save failed',
        description: (err as { message?: string })?.message ?? 'An unexpected error occurred.',
        tone: 'danger',
      });
    } finally {
      setBusy(null);
    }
  };

  return (
    <Card>
      <CardBody className="flex flex-col gap-5">
        <div className="flex flex-col gap-1.5">
          <h2 className="text-heading font-semibold text-text-primary">Daily work report</h2>
          <p className="text-caption text-text-secondary">
            Fill in your shift report. Submitting it feeds the AI-generated handover journal.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {!userDepartmentId ? (
            <div className="flex items-end">
              <p className="text-caption text-danger-600">Your department could not be determined. Contact support.</p>
            </div>
          ) : (
            <Input
              label="Department"
              value={userDepartmentId}
              disabled
            />
          )}
          <Select
            label="Project"
            value={form.projectId}
            onChange={(e) => setField('projectId', e.target.value)}
            invalid={!!errors.projectId}
            errorText={errors.projectId}
            disabled={!userDepartmentId}
            options={[
              { value: '', label: userDepartmentId ? 'Select project' : 'No department available' },
              ...projects.map((p) => ({ value: p.id, label: p.name })),
            ]}
          />
          <Select
            label="Shift"
            value={form.shift}
            onChange={(e) => setField('shift', e.target.value)}
            invalid={!!errors.shift}
            errorText={errors.shift}
            options={SHIFTS.map((s) => ({ value: s.value, label: s.label }))}
          />
          <Input
            label="Report date"
            type="date"
            value={form.entryDate}
            onChange={(e) => setField('entryDate', e.target.value)}
            invalid={!!errors.entryDate}
            errorText={errors.entryDate}
            leftIcon={<CalendarClock className="h-4 w-4" />}
          />
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Textarea
            label="Tasks completed"
            placeholder="What did you finish this shift?"
            value={form.completedTasks}
            onChange={(e) => setField('completedTasks', e.target.value)}
          />
          <Textarea
            label="Current progress"
            placeholder="Where is the work in progress right now?"
            value={form.currentProgress}
            onChange={(e) => setField('currentProgress', e.target.value)}
          />
          <Textarea
            label="Pending tasks"
            placeholder="What still needs to be done?"
            value={form.pendingTasks}
            onChange={(e) => setField('pendingTasks', e.target.value)}
          />
          <Textarea
            label="Blockers"
            placeholder="Any obstacles or risks?"
            value={form.blockers}
            onChange={(e) => setField('blockers', e.target.value)}
          />
          <Textarea
            label="Important notes"
            placeholder="Anything the next shift should know?"
            value={form.importantNotes}
            onChange={(e) => setField('importantNotes', e.target.value)}
          />
          <Textarea
            label="Estimated remaining work"
            placeholder="How much work is left, roughly?"
            value={form.estimatedRemainingWork}
            onChange={(e) => setField('estimatedRemainingWork', e.target.value)}
          />
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Select
            label="Mention teammate (optional)"
            value={mention}
            onChange={(e) => setMention(e.target.value)}
            options={[
              { value: '', label: 'None' },
              ...(members ?? [])
                .filter((m) => m.status === 'ACTIVE')
                .map((m) => ({ value: `@${m.firstName} ${m.lastName}`, label: `${m.firstName} ${m.lastName}` })),
            ]}
          />
          <Select
            label="Mood / workload"
            value={form.mood}
            onChange={(e) => setField('mood', e.target.value)}
            options={[
              { value: '', label: 'Select mood' },
              ...MOODS.map((m) => ({ value: m.value, label: m.label })),
            ]}
          />
          <Select
            label="Priority"
            value={form.priority}
            onChange={(e) => setField('priority', e.target.value)}
            options={['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((p) => ({ value: p, label: p }))}
          />
        </div>

        <div className="flex flex-wrap items-center justify-end gap-2 border-t border-border-subtle pt-4">
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button
            variant="secondary"
            leftIcon={<ClipboardList />}
            loading={busy === 'draft'}
            disabled={busy === 'submit'}
            onClick={() => run('draft')}
          >
            Save Draft
          </Button>
          <Button
            leftIcon={<Send />}
            loading={busy === 'submit'}
            disabled={busy === 'draft'}
            onClick={() => run('submit')}
          >
            Submit Entry
          </Button>
        </div>
      </CardBody>
    </Card>
  );
}
