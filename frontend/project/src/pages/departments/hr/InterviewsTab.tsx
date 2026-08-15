import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Calendar, CalendarClock, CalendarPlus, CheckCircle2, Loader2, XCircle, X, Star, Users, Pencil, Trash2 } from 'lucide-react';
import { Badge } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';
import { IconButton } from '../../../components/ui/IconButton';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Textarea } from '../../../components/ui/Textarea';
import { Modal } from '../../../components/ui/Modal';
import { Tabs, type TabItem } from '../../../components/ui/Tabs';
import { EmptyState } from '../../../components/ui/EmptyState';
import { useToast } from '../../../components/ui/Toast';
import { Can } from '../../auth';
import { useCandidatesList } from '../../../services/candidate-hooks';
import { useInterviewsToday, useInterviewsWeek, useInterviewsUpcoming, useInterviewsCompleted, useInterviewStats } from '../../../services/interview-hooks';
import { interviewService } from '../../../services/interview-service';
import { InterviewDetailsDrawer } from './InterviewDetailsDrawer';
import type { InterviewResponse, InterviewFeedbackRequest, CreateInterviewRequest, UpdateInterviewRequest } from '../../../services/interview-service';
import { interviewTypeColor, interviewStatusColor, RECOMMENDATIONS, INTERVIEW_TYPES, formatEnum, formatDate, formatTime, toISOTimestamp, toDateInputValue, toTimeInputValue, todayDateInputValue, interviewScheduleSchema } from './hr-constants';

type ScheduleField = 'candidateId' | 'position' | 'scheduledDate' | 'startTime' | 'endTime';
type ScheduleFieldErrors = Partial<Record<ScheduleField, string>>;

const viewTabs: TabItem[] = [
  { id: 'upcoming', label: 'Upcoming' },
  { id: 'today', label: 'Today' },
  { id: 'week', label: 'This Week' },
  { id: 'completed', label: 'Completed' },
];

export function InterviewsTab({ wsId, deptId }: { wsId: string; deptId: string }) {
  const [view, setView] = useState('upcoming');
  const [detail, setDetail] = useState<InterviewResponse | null>(null);
  const [feedbackTarget, setFeedbackTarget] = useState<InterviewResponse | null>(null);
  const [feedbackForm, setFeedbackForm] = useState<InterviewFeedbackRequest>({ recommendation: 'HIRE' });
  const [scheduleTarget, setScheduleTarget] = useState<InterviewResponse | null | 'new'>(null);
  const [scheduleForm, setScheduleForm] = useState({
    candidateId: '', type: 'EMPLOYEE', scheduledDate: '', startTime: '', endTime: '',
    position: '',
    location: '', meetingLink: '', title: '', description: '',
  });
  const [scheduleErrors, setScheduleErrors] = useState<ScheduleFieldErrors>({});

  const { toast } = useToast();
  const qc = useQueryClient();

  const { data: stats, isLoading: statsLoading } = useInterviewStats(wsId, deptId);
  const { data: upcoming, isLoading: upcomingLoading } = useInterviewsUpcoming(wsId, deptId);
  const { data: today, isLoading: todayLoading } = useInterviewsToday(wsId, deptId);
  const { data: week, isLoading: weekLoading } = useInterviewsWeek(wsId, deptId);
  const { data: completed, isLoading: completedLoading } = useInterviewsCompleted(wsId, deptId);
  const { data: candidatesData } = useCandidatesList(wsId, deptId);

  const loading = statsLoading || upcomingLoading || todayLoading || weekLoading || completedLoading;

  const candidates = candidatesData?.content ?? [];
  const candidateName = (id?: string) => {
    if (!id) return 'Candidate';
    const c = candidates.find((x) => x.id === id);
    return c ? `${c.firstName} ${c.lastName}` : 'Candidate';
  };

  const invalidateLists = () => {
    qc.invalidateQueries({ queryKey: ['interviews', 'today', wsId, deptId] });
    qc.invalidateQueries({ queryKey: ['interviews', 'week', wsId, deptId] });
    qc.invalidateQueries({ queryKey: ['interviews', 'upcoming', wsId, deptId] });
    qc.invalidateQueries({ queryKey: ['interviews', 'completed', wsId, deptId] });
    qc.invalidateQueries({ queryKey: ['interviews', 'stats', wsId, deptId] });
  };

  const cancelInterview = useMutation({
    mutationFn: ({ candidateId, interviewId }: { candidateId: string; interviewId: string }) =>
      interviewService.cancel(wsId, deptId, candidateId, interviewId),
    onSuccess: () => { toast({ title: 'Interview cancelled', tone: 'success' }); setDetail(null); invalidateLists(); },
    onError: () => toast({ title: 'Failed to cancel interview', tone: 'danger' }),
  });

  const completeInterview = useMutation({
    mutationFn: ({ candidateId, interviewId }: { candidateId: string; interviewId: string }) =>
      interviewService.complete(wsId, deptId, candidateId, interviewId),
    onSuccess: () => { toast({ title: 'Interview marked completed', tone: 'success' }); setDetail(null); invalidateLists(); },
    onError: () => toast({ title: 'Failed to complete interview', tone: 'danger' }),
  });

  const submitFeedback = useMutation({
    mutationFn: ({ candidateId, interviewId, data }: { candidateId: string; interviewId: string; data: InterviewFeedbackRequest }) =>
      interviewService.submitFeedback(wsId, deptId, candidateId, interviewId, data),
    onSuccess: () => {
      toast({ title: 'Feedback submitted', tone: 'success' });
      setFeedbackTarget(null);
      setFeedbackForm({ recommendation: 'HIRE' });
      invalidateLists();
    },
    onError: () => toast({ title: 'Failed to submit feedback', tone: 'danger' }),
  });

  const invalidateCandidateInterviews = (candidateId: string) => {
    qc.invalidateQueries({ queryKey: ['candidates', 'interviews', wsId, deptId, candidateId] });
  };

  const createInterview = useMutation({
    mutationFn: ({ candidateId, data }: { candidateId: string; data: CreateInterviewRequest }) =>
      interviewService.create(wsId, deptId, candidateId, data),
    onSuccess: (_data, variables) => {
      toast({ title: 'Interview scheduled', tone: 'success' });
      setScheduleTarget(null);
      invalidateLists();
      invalidateCandidateInterviews(variables.candidateId);
    },
    onError: () => toast({ title: 'Failed to schedule interview', tone: 'danger' }),
  });

  const updateInterview = useMutation({
    mutationFn: ({ candidateId, interviewId, data }: { candidateId: string; interviewId: string; data: UpdateInterviewRequest }) =>
      interviewService.update(wsId, deptId, candidateId, interviewId, data),
    onSuccess: (_data, variables) => {
      toast({ title: 'Interview updated', tone: 'success' });
      setScheduleTarget(null);
      setDetail(null);
      invalidateLists();
      invalidateCandidateInterviews(variables.candidateId);
    },
    onError: () => toast({ title: 'Failed to update interview', tone: 'danger' }),
  });

  const deleteInterview = useMutation({
    mutationFn: ({ candidateId, interviewId }: { candidateId: string; interviewId: string }) =>
      interviewService.delete(wsId, deptId, candidateId, interviewId),
    onSuccess: () => { toast({ title: 'Interview deleted', tone: 'success' }); invalidateLists(); },
    onError: () => toast({ title: 'Failed to delete interview', tone: 'danger' }),
  });

  const effectivePosition = () => scheduleForm.position.trim();

  const clearFieldError = (field: ScheduleField) =>
    setScheduleErrors((prev) => (prev[field] ? { ...prev, [field]: undefined } : prev));

  const handleScheduleSubmit = () => {
    if (!scheduleTarget) return;
    const result = interviewScheduleSchema.safeParse(scheduleForm);
    if (!result.success) {
      const fieldErrors = result.error.flatten().fieldErrors;
      setScheduleErrors({
        candidateId: fieldErrors.candidateId?.[0],
        position: fieldErrors.position?.[0],
        scheduledDate: fieldErrors.scheduledDate?.[0],
        startTime: fieldErrors.startTime?.[0],
        endTime: fieldErrors.endTime?.[0],
      });
      return;
    }
    setScheduleErrors({});
    const payload: CreateInterviewRequest = {
      type: scheduleForm.type as never,
      position: effectivePosition(),
      title: scheduleForm.title.trim() || undefined,
      description: scheduleForm.description.trim() || undefined,
      scheduledDate: toISOTimestamp(scheduleForm.scheduledDate),
      startTime: toISOTimestamp(scheduleForm.scheduledDate, scheduleForm.startTime),
      endTime: toISOTimestamp(scheduleForm.scheduledDate, scheduleForm.endTime),
      location: scheduleForm.location.trim() || undefined,
      meetingLink: scheduleForm.meetingLink.trim() || undefined,
    };
    if (scheduleTarget === 'new') {
      if (!scheduleForm.candidateId) return;
      createInterview.mutate({ candidateId: scheduleForm.candidateId, data: payload });
    } else {
      updateInterview.mutate({ candidateId: scheduleTarget.candidateId, interviewId: scheduleTarget.id, data: payload });
    }
  };

  const openSchedule = () => {
    setScheduleForm({ candidateId: '', type: 'EMPLOYEE', scheduledDate: '', startTime: '', endTime: '', position: '', location: '', meetingLink: '', title: '', description: '' });
    setScheduleErrors({});
    setScheduleTarget('new');
  };

  const openEdit = (iv: InterviewResponse) => {
    setScheduleForm({
      candidateId: iv.candidateId,
      type: iv.type,
      scheduledDate: toDateInputValue(iv.scheduledDate),
      startTime: toTimeInputValue(iv.startTime),
      endTime: toTimeInputValue(iv.endTime),
      position: iv.position ?? '',
      location: iv.location ?? '',
      meetingLink: iv.meetingLink ?? '',
      title: iv.title ?? '',
      description: iv.description ?? '',
    });
    setScheduleErrors({});
    setScheduleTarget(iv);
  };

  const handleDeleteInterview = (iv: InterviewResponse) => {
    if (!window.confirm('Delete this interview? This cannot be undone.')) return;
    deleteInterview.mutate({ candidateId: iv.candidateId, interviewId: iv.id });
  };

  const handleCancel = (iv: InterviewResponse) => {
    cancelInterview.mutate({ candidateId: iv.candidateId, interviewId: iv.id });
  };

  const handleFeedbackSubmit = () => {
    if (!feedbackTarget) return;
    submitFeedback.mutate({
      candidateId: feedbackTarget.candidateId,
      interviewId: feedbackTarget.id,
      data: feedbackForm,
    });
  };

  if (loading) {
    return <div className="flex items-center justify-center py-20"><Loader2 className="h-8 w-8 animate-spin text-text-tertiary" /></div>;
  }

  const list = view === 'today' ? (today ?? []) : view === 'week' ? (week ?? []) : view === 'completed' ? (completed ?? []) : (upcoming ?? []);

  const editCandidateId = scheduleTarget && scheduleTarget !== 'new' ? scheduleTarget.candidateId : undefined;

  return (
    <div className="flex flex-col gap-4">
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
          <Stat label="Today" value={stats.interviewsToday} tone="accent" />
          <Stat label="Upcoming" value={stats.upcomingInterviews} tone="info" />
          <Stat label="Completed" value={stats.completedInterviews} tone="success" />
          <Stat label="Cancelled" value={stats.cancelledInterviews} tone="danger" />
          <Stat label="Avg Rating" value={stats.averageRating ? stats.averageRating.toFixed(1) : '-'} tone="warning" />
        </div>
      )}

      <div className="flex items-center justify-between gap-3">
        <Tabs items={viewTabs} active={view} onChange={setView} />
        <Can permission="INTERVIEW_CREATE">
          <Button leftIcon={<CalendarPlus />} onClick={openSchedule}>Schedule Interview</Button>
        </Can>
      </div>

      {list.length === 0 ? (
        <EmptyState icon={<Calendar />} title={`No interviews ${view === 'completed' ? 'completed' : view === 'today' ? 'today' : view === 'week' ? 'this week' : 'upcoming'}`} description="Interviews scheduled with candidates will appear here." />
      ) : (
        <div className="space-y-2">
          {list.map((iv) => (
            <div key={iv.id} className="flex items-center gap-4 p-4 rounded-lg border border-border-subtle bg-surface hover:bg-surface-2 transition-colors">
              <div className={`flex h-10 w-10 items-center justify-center rounded-full ${iv.status === 'CANCELLED' ? 'bg-danger-100 text-danger-700 dark:bg-danger-900 dark:text-danger-300' : iv.status === 'COMPLETED' ? 'bg-success-100 text-success-700 dark:bg-success-900 dark:text-success-300' : 'bg-accent-100 text-accent-700 dark:bg-accent-900 dark:text-accent-300'}`}>
                {iv.status === 'CANCELLED' ? <XCircle className="h-5 w-5" /> : iv.status === 'COMPLETED' ? <CheckCircle2 className="h-5 w-5" /> : <CalendarClock className="h-5 w-5" />}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-body font-medium text-text-primary">{candidateName(iv.candidateId)}</p>
                <p className="text-caption text-text-tertiary">
                  {iv.type} • {formatDate(iv.scheduledDate)} {iv.startTime ? `${formatTime(iv.startTime)}${iv.endTime ? ` - ${formatTime(iv.endTime)}` : ''}` : ''}
                </p>
                {iv.description && <p className="text-2xs text-text-tertiary mt-0.5">{iv.description}</p>}
              </div>
              <div className="flex items-center gap-1">
                <Badge tone={(interviewTypeColor[iv.type] ?? 'neutral') as never} variant="soft">{formatEnum(iv.type)}</Badge>
                <Badge tone={(interviewStatusColor[iv.status] ?? 'neutral') as never} variant="soft">{formatEnum(iv.status)}</Badge>
              </div>
              <div className="flex items-center gap-1">
                <IconButton label="View details" variant="ghost" size="sm" onClick={() => setDetail(iv)}>
                  <Users className="h-4 w-4" />
                </IconButton>
                {(iv.status === 'SCHEDULED' || iv.status === 'RESCHEDULED') && (
                  <Can permission="INTERVIEW_UPDATE">
                    <IconButton label="Edit interview" variant="ghost" size="sm" onClick={() => openEdit(iv)}>
                      <Pencil className="h-4 w-4" />
                    </IconButton>
                  </Can>
                )}
                {iv.status === 'CANCELLED' && (
                  <Can permission="INTERVIEW_DELETE">
                    <IconButton label="Delete interview" variant="ghost" size="sm" className="text-danger-600" onClick={() => handleDeleteInterview(iv)}>
                      <Trash2 className="h-4 w-4" />
                    </IconButton>
                  </Can>
                )}
                {iv.status === 'SCHEDULED' && (
                  <Can permission="INTERVIEW_CANCEL">
                    <IconButton label="Cancel interview" variant="ghost" size="sm" className="text-danger-600" onClick={() => handleCancel(iv)}>
                      <X className="h-4 w-4" />
                    </IconButton>
                  </Can>
                )}
                {(iv.status === 'SCHEDULED' || iv.status === 'RESCHEDULED') && (
                  <Can permission="INTERVIEW_UPDATE">
                    <IconButton label="Mark completed" variant="ghost" size="sm" className="text-success-600" onClick={() => completeInterview.mutate({ candidateId: iv.candidateId, interviewId: iv.id })}>
                      <CheckCircle2 className="h-4 w-4" />
                    </IconButton>
                  </Can>
                )}
                {iv.status === 'COMPLETED' && (
                  <Can permission="INTERVIEW_CREATE">
                    <IconButton label="Submit feedback" variant="ghost" size="sm" className="text-success-600" onClick={() => { setFeedbackForm({ recommendation: 'HIRE' }); setFeedbackTarget(iv); }}>
                      <Star className="h-4 w-4" />
                    </IconButton>
                  </Can>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {detail && (
        <InterviewDetailsDrawer
          key={detail.id}
          wsId={wsId}
          deptId={deptId}
          interview={detail}
          open
          onClose={() => setDetail(null)}
          onEdit={(iv) => { openEdit(iv); setDetail(null); }}
        />
      )}

      <Modal
        open={!!scheduleTarget}
        onClose={() => { setScheduleTarget(null); setScheduleErrors({}); }}
        title={scheduleTarget === 'new' ? 'Schedule Interview' : scheduleTarget ? `Edit Interview — ${candidateName(scheduleTarget.candidateId)}` : ''}
        description={scheduleTarget === 'new' ? 'Create a new interview for a candidate.' : ''}
        footer={
          <>
            <Button variant="outline" onClick={() => setScheduleTarget(null)}>Cancel</Button>
            <Button onClick={handleScheduleSubmit}>
              {scheduleTarget === 'new' ? 'Schedule' : 'Save'}
            </Button>
          </>
        }
      >
        <div className="grid grid-cols-2 gap-3">
          {scheduleTarget === 'new' ? (
            <Select label="Candidate *" value={scheduleForm.candidateId} invalid={!!scheduleErrors.candidateId} errorText={scheduleErrors.candidateId}
              onChange={(e) => {
                const c = candidates.find((x) => x.id === e.target.value);
                setScheduleForm((prev) => ({
                  ...prev,
                  candidateId: e.target.value,
                  position: prev.position ? prev.position : (c?.position ?? ''),
                }));
                clearFieldError('candidateId');
              }}
              options={[{ value: '', label: 'Select candidate...' }, ...candidates.filter((c) => c.currentStatus !== 'HIRED' && c.currentStatus !== 'REJECTED' && c.currentStatus !== 'WITHDRAWN').map((c) => ({ value: c.id, label: `${c.firstName} ${c.lastName} — ${c.position}` }))]} />
          ) : (
            <div className="col-span-2">
              <Input label="Candidate" value={candidateName(editCandidateId)} disabled />
            </div>
          )}
          <Select label="Type *" value={scheduleForm.type} onChange={(e) => setScheduleForm({ ...scheduleForm, type: e.target.value })}
            options={INTERVIEW_TYPES.map((t) => ({ value: t, label: formatEnum(t) }))} />
          <div className="col-span-2">
            <Input label="Position *" placeholder="Enter position..." value={scheduleForm.position} invalid={!!scheduleErrors.position} errorText={scheduleErrors.position}
              onChange={(e) => { setScheduleForm({ ...scheduleForm, position: e.target.value }); clearFieldError('position'); }} />
          </div>
          <Input label="Date *" type="date" min={todayDateInputValue()} value={scheduleForm.scheduledDate} invalid={!!scheduleErrors.scheduledDate} errorText={scheduleErrors.scheduledDate}
            onChange={(e) => { setScheduleForm({ ...scheduleForm, scheduledDate: e.target.value }); clearFieldError('scheduledDate'); }} />
          <Input label="Start Time *" type="time" value={scheduleForm.startTime} invalid={!!scheduleErrors.startTime} errorText={scheduleErrors.startTime}
            onChange={(e) => { setScheduleForm({ ...scheduleForm, startTime: e.target.value }); clearFieldError('startTime'); }} />
          <Input label="End Time *" type="time" value={scheduleForm.endTime} invalid={!!scheduleErrors.endTime} errorText={scheduleErrors.endTime}
            onChange={(e) => { setScheduleForm({ ...scheduleForm, endTime: e.target.value }); clearFieldError('endTime'); }} />
          <div className="col-span-2">
            <Input label="Location" placeholder="e.g. Meeting Room 2 / Zoom" value={scheduleForm.location} onChange={(e) => setScheduleForm({ ...scheduleForm, location: e.target.value })} />
          </div>
          <div className="col-span-2">
            <Input label="Meeting Link" placeholder="https://..." value={scheduleForm.meetingLink} onChange={(e) => setScheduleForm({ ...scheduleForm, meetingLink: e.target.value })} />
          </div>
          <div className="col-span-2">
            <Input label="Title" placeholder="Optional title" value={scheduleForm.title} onChange={(e) => setScheduleForm({ ...scheduleForm, title: e.target.value })} />
          </div>
          <div className="col-span-2">
            <Textarea label="Description" rows={3} value={scheduleForm.description} onChange={(e) => setScheduleForm({ ...scheduleForm, description: e.target.value })} />
          </div>
        </div>
      </Modal>

      <Modal
        open={!!feedbackTarget}
        onClose={() => { setFeedbackTarget(null); setFeedbackForm({ recommendation: 'HIRE' }); }}
        title="Submit Interview Feedback"
        description={feedbackTarget ? `${candidateName(feedbackTarget.candidateId)} — ${formatEnum(feedbackTarget.type)}` : ''}
        footer={
          <>
            <Button variant="outline" onClick={() => { setFeedbackTarget(null); setFeedbackForm({ recommendation: 'HIRE' }); }}>Cancel</Button>
            <Button onClick={handleFeedbackSubmit}>Submit</Button>
          </>
        }
      >
        <div className="flex flex-col gap-4">
          <Select label="Recommendation"
            value={feedbackForm.recommendation}
            onChange={(e) => setFeedbackForm({ ...feedbackForm, recommendation: e.target.value as never })}
            options={RECOMMENDATIONS.map((r) => ({ value: r, label: formatEnum(r) }))}
          />
          <div className="flex flex-col gap-1">
            <span className="text-caption font-medium text-text-secondary">Rating (0-5)</span>
            <div className="flex items-center gap-3">
              <Input
                type="number" min={0} max={5}
                value={feedbackForm.rating != null ? String(feedbackForm.rating) : ''}
                placeholder="Optional"
                onChange={(e) => {
                  const v = Number(e.target.value);
                  setFeedbackForm({ ...feedbackForm, rating: e.target.value === '' ? undefined : Math.max(0, Math.min(5, v)) });
                }}
              />
              <span className="text-2xs text-text-tertiary">stars</span>
            </div>
          </div>
          <Textarea label="Notes" rows={4} placeholder="Candidate performance, strengths, concerns..."
            value={feedbackForm.notes ?? ''} onChange={(e) => setFeedbackForm({ ...feedbackForm, notes: e.target.value })} />
        </div>
      </Modal>
    </div>
  );
}

function Stat({ label, value, tone }: { label: string; value: number | string; tone: string }) {
  const toneClass: Record<string, string> = {
    accent: 'text-accent-600',
    info: 'text-info-600',
    success: 'text-success-600',
    danger: 'text-danger-600',
    warning: 'text-warning-600',
  };
  return (
    <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
      <span className="text-2xs text-text-tertiary">{label}</span>
      <span className={`text-section font-bold ${toneClass[tone] ?? 'text-text-primary'}`}>{value}</span>
    </div>
  );
}
