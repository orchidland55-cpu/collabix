import { useState } from 'react';
import { Search, CheckCircle, Loader2, Plus, Trash2, Archive, XCircle } from 'lucide-react';
import { Card, CardBody } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Textarea } from '../../../components/ui/Textarea';
import { Badge, type Tone } from '../../../components/ui/Badge';
import { Progress } from '../../../components/ui/Progress';
import { IconButton } from '../../../components/ui/IconButton';
import { Modal } from '../../../components/ui/Modal';
import { EmptyState } from '../../../components/ui/EmptyState';
import { Pagination } from '../../../components/ui/Pagination';
import { useToast } from '../../../components/ui/Toast';
import { usePerformanceReviewsList, usePerformanceReviewStats, useCreatePerformanceReview, useSubmitPerformanceReview, useApprovePerformanceReview, useRejectPerformanceReview, useArchivePerformanceReview, useDeletePerformanceReview } from '../../../services/performance-review-hooks';
import type { CreatePerformanceReviewRequest } from '../../../services/performance-review-service';
import { useDepartmentEmployeesForSelect } from '../../../services/employee-hooks';
import { performanceLevelColor, reviewStatusColor, REVIEW_PERIODS, reviewPeriodLabel, formatEnum, isReviewEligibleEmployee, employmentStatusLabel } from './hr-constants';

const EMPTY_SCORES = {
  objectivesAchieved: 0, technicalSkills: 0, softSkills: 0, punctualityAttendance: 0,
  teamwork: 0, initiativeProblemSolving: 0, communication: 0, continuousLearningAdaptability: 0,
};

export function PerformanceReviewsTab({ wsId, deptId }: { wsId: string; deptId: string }) {
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [showCreate, setShowCreate] = useState(false);
  const [rejectTarget, setRejectTarget] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [form, setForm] = useState<CreatePerformanceReviewRequest>({
    employeeId: '', reviewerId: '', reviewPeriod: 'QUARTERLY', reviewDate: '', ...EMPTY_SCORES,
  });

  const { toast } = useToast();

  const { data, isLoading, isError } = usePerformanceReviewsList(wsId, deptId, page);
  const { data: stats } = usePerformanceReviewStats(wsId, deptId);
  const {
    data: empData,
    isLoading: employeesLoading,
    refetch: refetchEmployees,
  } = useDepartmentEmployeesForSelect(wsId, deptId);
  const createReview = useCreatePerformanceReview(wsId, deptId);
  const submitReview = useSubmitPerformanceReview(wsId, deptId);
  const approveReview = useApprovePerformanceReview(wsId, deptId);
  const rejectReview = useRejectPerformanceReview(wsId, deptId);
  const archiveReview = useArchivePerformanceReview(wsId, deptId);
  const deleteReview = useDeletePerformanceReview(wsId, deptId);

  const reviews = data?.content ?? [];
  const totalPages = data?.page?.totalPages ?? 1;
  const employees = empData?.content ?? [];
  const reviewEmployees = employees.filter((e) => isReviewEligibleEmployee(e.employmentStatus));
  const employeeOptions = reviewEmployees.map((e) => ({
    value: e.id,
    label: `${e.firstName} ${e.lastName}${e.employmentStatus ? ` — ${employmentStatusLabel[e.employmentStatus] ?? formatEnum(e.employmentStatus)}` : ''}`,
  }));

  const filtered = reviews.filter((r) => {
    if (!search) return true;
    const q = search.toLowerCase();
    return r.employeeName?.toLowerCase().includes(q) || r.reviewerName?.toLowerCase().includes(q) || r.reviewPeriod?.toLowerCase().includes(q);
  });

  const openCreate = () => {
    setForm({ employeeId: '', reviewerId: '', reviewPeriod: 'QUARTERLY', reviewDate: '', ...EMPTY_SCORES });
    setShowCreate(true);
    void refetchEmployees();
  };

  const handleCreate = () => {
    const payload: CreatePerformanceReviewRequest = {
      ...form,
      employeeId: form.employeeId,
      reviewerId: form.reviewerId,
      teamId: form.teamId || undefined,
    };
    createReview.mutate(payload, {
      onSuccess: () => { toast({ title: 'Review created', tone: 'success' }); setShowCreate(false); },
      onError: (err) => {
        const msg = err instanceof Error ? err.message : 'Failed to create review';
        toast({ title: 'Failed to create review', description: msg, tone: 'danger' });
      },
    });
  };

  const handleReject = () => {
    if (!rejectTarget) return;
    rejectReview.mutate({ id: rejectTarget, reason: rejectReason }, {
      onSuccess: () => { toast({ title: 'Review rejected', tone: 'success' }); setRejectTarget(null); setRejectReason(''); },
      onError: () => toast({ title: 'Failed to reject review', tone: 'danger' }),
    });
  };

  const handleDelete = (id: string) => {
    if (!window.confirm('Delete this performance review? This cannot be undone.')) return;
    deleteReview.mutate(id, {
      onSuccess: () => toast({ title: 'Review deleted', tone: 'success' }),
      onError: () => toast({ title: 'Failed to delete review', tone: 'danger' }),
    });
  };

  if (isLoading) {
    return <div className="flex items-center justify-center py-20"><Loader2 className="h-8 w-8 animate-spin text-text-tertiary" /></div>;
  }

  if (isError) {
    return <div className="flex flex-col items-center justify-center py-20 gap-3"><p className="text-body font-medium text-danger-600">Failed to load performance reviews</p><p className="text-caption text-text-tertiary">Please try again later.</p></div>;
  }

  return (
    <div className="flex flex-col gap-4">
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
            <span className="text-2xs text-text-tertiary">Total Reviews</span>
            <span className="text-section font-bold text-text-primary">{stats.totalReviews}</span>
          </div>
          <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
            <span className="text-2xs text-text-tertiary">Avg Department Score</span>
            <span className="text-section font-bold text-accent-600">{stats.averageDepartmentScore?.toFixed(1) ?? '-'}</span>
          </div>
          <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
            <span className="text-2xs text-text-tertiary">Outstanding</span>
            <span className="text-section font-bold text-success-600">{stats.outstandingEmployees}</span>
          </div>
          <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
            <span className="text-2xs text-text-tertiary">Needs Improvement</span>
            <span className="text-section font-bold text-danger-600">{stats.needsImprovementEmployees}</span>
          </div>
        </div>
      )}

      <div className="flex items-center justify-between gap-4">
        <Input placeholder="Search reviews..." leftIcon={<Search />} value={search} onChange={(e) => { setSearch(e.target.value); setPage(0); }} containerClassName="max-w-sm" />
        <Button leftIcon={<Plus />} onClick={openCreate}>New Review</Button>
      </div>

      {filtered.length === 0 ? (
        <EmptyState icon={<Search />} title="No performance reviews found" description="Create reviews to evaluate employee performance." />
      ) : (
        <div className="space-y-3">
          {filtered.map((r) => (
            <Card key={r.id}>
              <CardBody className="flex flex-col gap-3">
                <div className="flex items-start justify-between">
                  <div>
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="text-body font-semibold text-text-primary">{r.employeeName}</p>
                      <Badge tone={(performanceLevelColor[r.performanceLevel] ?? 'neutral') as Tone} variant="soft">{formatEnum(r.performanceLevel)}</Badge>
                    </div>
                    <p className="text-caption text-text-tertiary">Reviewer: {r.reviewerName}</p>
                  </div>
                  <div className="text-right flex flex-col items-end gap-1">
                    <Badge tone={(reviewStatusColor[r.status] ?? 'neutral') as Tone} variant="soft" dot>{formatEnum(r.status)}</Badge>
                    <span className="text-2xs text-text-tertiary">{reviewPeriodLabel[r.reviewPeriod] ?? formatEnum(r.reviewPeriod)}</span>
                  </div>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <Criterion label="Objectives" value={r.objectivesAchieved} max={20} />
                  <Criterion label="Technical" value={r.technicalSkills} max={20} />
                  <Criterion label="Soft Skills" value={r.softSkills} max={20} />
                  <Criterion label="Punctuality" value={r.punctualityAttendance} max={20} />
                  <Criterion label="Teamwork" value={r.teamwork} max={20} />
                  <Criterion label="Initiative" value={r.initiativeProblemSolving} max={20} />
                  <Criterion label="Communication" value={r.communication} max={20} />
                  <Criterion label="Adaptability" value={r.continuousLearningAdaptability} max={20} />
                </div>

                <div className="flex flex-wrap items-center gap-4 pt-2 border-t border-border-subtle">
                  <div>
                    <span className="text-2xs text-text-tertiary">Total Score</span>
                    <p className="text-body font-bold text-text-primary">{r.totalScore}/{r.maxScore}</p>
                  </div>
                  <div className="flex-1 min-w-[160px]">
                    <div className="flex items-center gap-2">
                      <Progress value={r.percentage ?? 0} size="sm" tone={(r.percentage ?? 0) >= 80 ? 'success' : (r.percentage ?? 0) >= 60 ? 'warning' : 'danger'} />
                      <span className="text-caption font-medium text-text-primary">{(r.percentage ?? 0).toFixed(0)}%</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-1">
                    {r.status === 'DRAFT' && (
                      <Button size="sm" variant="outline" onClick={() => submitReview.mutate(r.id, { onSuccess: () => toast({ title: 'Review submitted', tone: 'success' }) })}>
                        Submit
                      </Button>
                    )}
                    {r.status === 'SUBMITTED' && (
                      <>
                        <Button size="sm" variant="outline" className="text-success-600" onClick={() => approveReview.mutate(r.id, { onSuccess: () => toast({ title: 'Review approved', tone: 'success' }) })}>
                          <CheckCircle className="h-4 w-4 mr-1" /> Approve
                        </Button>
                        <Button size="sm" variant="outline" className="text-danger-600" onClick={() => setRejectTarget(r.id)}>
                          <XCircle className="h-4 w-4 mr-1" /> Reject
                        </Button>
                      </>
                    )}
                    {r.status !== 'ARCHIVED' && (
                      <IconButton label="Archive" variant="ghost" size="sm" onClick={() => archiveReview.mutate(r.id, { onSuccess: () => toast({ title: 'Review archived', tone: 'success' }) })}>
                        <Archive className="h-4 w-4" />
                      </IconButton>
                    )}
                    <IconButton label="Delete" variant="ghost" size="sm" className="text-danger-600" onClick={() => handleDelete(r.id)}>
                      <Trash2 className="h-4 w-4" />
                    </IconButton>
                  </div>
                </div>
              </CardBody>
            </Card>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex justify-center pt-2">
          <Pagination page={page + 1} totalPages={totalPages} onPageChange={(p) => setPage(p - 1)} />
        </div>
      )}

      <Modal
        open={showCreate}
        onClose={() => setShowCreate(false)}
        title="New Performance Review"
        footer={
          <>
            <Button variant="outline" onClick={() => setShowCreate(false)}>Cancel</Button>
            <Button onClick={handleCreate} disabled={!form.employeeId || !form.reviewerId || !form.reviewDate}>Create</Button>
          </>
        }
      >
        <div className="flex flex-col gap-3">
          <div className="grid grid-cols-2 gap-3">
            <Select label="Employee" value={form.employeeId} onChange={(e) => setForm({ ...form, employeeId: e.target.value })}
              disabled={employeesLoading}
              options={[
                { value: '', label: employeesLoading ? 'Loading employees...' : 'Select employee...' },
                ...employeeOptions,
              ]}
              helperText={
                !employeesLoading && reviewEmployees.length === 0
                  ? 'No eligible employees. Terminated, resigned or retired employees are excluded — open the Employees tab, edit the employee and set their status back to Active.'
                  : undefined
              }
            />
            <Select label="Reviewer" value={form.reviewerId} onChange={(e) => setForm({ ...form, reviewerId: e.target.value })}
              disabled={employeesLoading}
              options={[
                { value: '', label: employeesLoading ? 'Loading employees...' : 'Select reviewer...' },
                ...employeeOptions.filter((o) => o.value !== form.employeeId),
              ]}
              helperText="Reviewer must be an existing employee in this department."
            />
            <Select label="Period" value={form.reviewPeriod} onChange={(e) => setForm({ ...form, reviewPeriod: e.target.value })}
              options={REVIEW_PERIODS.map((p) => ({ value: p, label: reviewPeriodLabel[p] }))} />
            <Input label="Review Date" type="date" value={form.reviewDate} onChange={(e) => setForm({ ...form, reviewDate: e.target.value })} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <ScoreInput label="Objectives Achieved" value={form.objectivesAchieved} onChange={(v) => setForm({ ...form, objectivesAchieved: v })} />
            <ScoreInput label="Technical Skills" value={form.technicalSkills} onChange={(v) => setForm({ ...form, technicalSkills: v })} />
            <ScoreInput label="Soft Skills" value={form.softSkills} onChange={(v) => setForm({ ...form, softSkills: v })} />
            <ScoreInput label="Punctuality & Attendance" value={form.punctualityAttendance} onChange={(v) => setForm({ ...form, punctualityAttendance: v })} />
            <ScoreInput label="Teamwork" value={form.teamwork} onChange={(v) => setForm({ ...form, teamwork: v })} />
            <ScoreInput label="Initiative & Problem Solving" value={form.initiativeProblemSolving} onChange={(v) => setForm({ ...form, initiativeProblemSolving: v })} />
            <ScoreInput label="Communication" value={form.communication} onChange={(v) => setForm({ ...form, communication: v })} />
            <ScoreInput label="Continuous Learning" value={form.continuousLearningAdaptability} onChange={(v) => setForm({ ...form, continuousLearningAdaptability: v })} />
          </div>
          <Textarea label="General Comment" rows={3} value={form.generalComment ?? ''} onChange={(e) => setForm({ ...form, generalComment: e.target.value })} />
          <div className="grid grid-cols-2 gap-3">
            <Input label="Strengths" value={form.strengths ?? ''} onChange={(e) => setForm({ ...form, strengths: e.target.value })} />
            <Input label="Areas for Improvement" value={form.areasForImprovement ?? ''} onChange={(e) => setForm({ ...form, areasForImprovement: e.target.value })} />
          </div>
        </div>
      </Modal>

      <Modal
        open={!!rejectTarget}
        onClose={() => { setRejectTarget(null); setRejectReason(''); }}
        title="Reject Review"
        footer={
          <>
            <Button variant="outline" onClick={() => { setRejectTarget(null); setRejectReason(''); }}>Cancel</Button>
            <Button variant="danger" onClick={handleReject} disabled={!rejectReason}>Reject</Button>
          </>
        }
      >
        <Textarea label="Rejection Reason" rows={3} placeholder="Explain why this review is rejected..." value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} />
      </Modal>
    </div>
  );
}

function ScoreInput({ label, value, onChange }: { label: string; value: number; onChange: (v: number) => void }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-2xs text-text-secondary">{label} (0-20)</span>
      <Input type="number" min={0} max={20} value={value} onChange={(e) => onChange(Math.max(0, Math.min(20, Number(e.target.value) || 0)))} />
    </div>
  );
}

function Criterion({ label, value, max }: { label: string; value?: number; max: number }) {
  const pct = value != null ? (value / max) * 100 : 0;
  return (
    <div className="flex flex-col gap-1 p-2 rounded border border-border-subtle">
      <div className="flex items-center justify-between">
        <span className="text-2xs text-text-tertiary">{label}</span>
        <span className="text-2xs font-medium text-text-primary">{value ?? '-'}/{max}</span>
      </div>
      <Progress value={pct} size="sm" tone={pct >= 80 ? 'success' : pct >= 60 ? 'warning' : 'danger'} />
    </div>
  );
}
