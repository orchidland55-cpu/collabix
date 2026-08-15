import { useState } from 'react';
import { Search, Plus, Eye, Pencil, Loader2, Upload, Download, Trash2, FileText, Phone, Mail, MessageSquare, CalendarClock, UserPlus } from 'lucide-react';
import { Card, CardBody } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Textarea } from '../../../components/ui/Textarea';
import { Badge, type Tone } from '../../../components/ui/Badge';
import { IconButton } from '../../../components/ui/IconButton';
import { Modal } from '../../../components/ui/Modal';
import { EmptyState } from '../../../components/ui/EmptyState';
import { Pagination } from '../../../components/ui/Pagination';
import { useToast } from '../../../components/ui/Toast';
import { useCandidatesList, useCreateCandidate, useUpdateCandidate, useDeleteCandidate, useChangeCandidateStatus, useCandidateTimeline, useCandidateNotes, useCreateCandidateNote, useCandidateInterviews, useCandidateStats } from '../../../services/candidate-hooks';
import type { CandidateResponse, CreateCandidateRequest } from '../../../services/candidate-service';
import { useCandidateAttachments, useUploadCandidateAttachment, useDeleteCandidateAttachment, useCandidateAttachmentStats } from '../../../services/candidate-attachment-hooks';
import type { CandidateAttachmentResponse } from '../../../services/candidate-attachment-service';
import { candidateAttachmentService } from '../../../services/candidate-attachment-service';
import { DocumentViewerModal } from '../../../components/documents/DocumentViewerModal';
import { downloadAuthenticatedFile } from '../../../lib/file-download';
import { CANDIDATE_STATUSES, CANDIDATE_SOURCES, candidateStatusColor, candidateStatusLabel, candidateSourceLabel, NOTE_CATEGORIES, noteCategoryLabel, NOTE_PRIORITIES, notePriorityColor, NOTE_VISIBILITIES, noteVisibilityLabel, formatEnum, formatDateTime } from './hr-constants';

export function CandidatesTab({ wsId, deptId }: { wsId: string; deptId: string }) {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [sourceFilter, setSourceFilter] = useState('');
  const [page, setPage] = useState(0);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<CandidateResponse | null>(null);
  const [detail, setDetail] = useState<CandidateResponse | null>(null);
  const [form, setForm] = useState<CreateCandidateRequest>({
    firstName: '', lastName: '', email: '', position: '', phone: '', source: 'LINKEDIN',
  });

  const { toast } = useToast();

  const { data, isLoading, isError } = useCandidatesList(wsId, deptId, page);
  const { data: stats } = useCandidateStats(wsId, deptId);
  const createCandidate = useCreateCandidate(wsId, deptId);
  const updateCandidate = useUpdateCandidate(wsId, deptId, editing?.id ?? '');
  const deleteCandidate = useDeleteCandidate(wsId, deptId);
  const changeStatus = useChangeCandidateStatus(wsId, deptId);
  const { data: timeline } = useCandidateTimeline(wsId, deptId, detail?.id);
  const { data: notesData } = useCandidateNotes(wsId, deptId, detail?.id);
  const { data: interviewsData } = useCandidateInterviews(wsId, deptId, detail?.id);
  const { data: attachmentsData } = useCandidateAttachments(wsId, deptId, detail?.id);
  const { data: attachmentStats } = useCandidateAttachmentStats(wsId, deptId);
  const uploadAttachment = useUploadCandidateAttachment(wsId, deptId, detail?.id ?? '');
  const deleteAttachment = useDeleteCandidateAttachment(wsId, deptId, detail?.id ?? '');

  const candidates = data?.content ?? [];
  const totalPages = data?.page?.totalPages ?? 1;
  const notes = notesData?.content ?? [];
  const interviews = interviewsData?.content ?? [];
  const attachments = attachmentsData?.content ?? [];

  const [noteForm, setNoteForm] = useState({ title: '', category: 'GENERAL', priority: 'MEDIUM', content: '', visibility: 'DEPARTMENT' });
  const createNote = useCreateCandidateNote(wsId, deptId, detail?.id ?? '');
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [viewer, setViewer] = useState<{ attachment: CandidateAttachmentResponse } | null>(null);

  const filtered = candidates.filter((c) => {
    if (statusFilter && c.currentStatus !== statusFilter) return false;
    if (sourceFilter && c.source !== sourceFilter) return false;
    if (!search) return true;
    const q = search.toLowerCase();
    return c.firstName.toLowerCase().includes(q) || c.lastName.toLowerCase().includes(q) || c.email.toLowerCase().includes(q) || c.position.toLowerCase().includes(q);
  });

  const openCreate = () => {
    setEditing(null);
    setForm({ firstName: '', lastName: '', email: '', position: '', phone: '', source: 'LINKEDIN' });
    setShowForm(true);
  };

  const openEdit = (c: CandidateResponse) => {
    setEditing(c);
    setForm({ firstName: c.firstName, lastName: c.lastName, email: c.email, position: c.position, phone: c.phone ?? '', source: c.source ?? 'LINKEDIN' });
    setShowForm(true);
  };

  const handleSubmit = () => {
    const payload = { ...form, source: form.source || undefined };
    if (editing) {
      updateCandidate.mutate(payload, {
        onSuccess: () => { toast({ title: 'Candidate updated', tone: 'success' }); setShowForm(false); },
        onError: (err) => toast({ title: 'Failed to update candidate', description: err instanceof Error ? err.message : undefined, tone: 'danger' }),
      });
    } else {
      createCandidate.mutate(payload, {
        onSuccess: () => { toast({ title: 'Candidate created', tone: 'success' }); setShowForm(false); },
        onError: (err) => toast({ title: 'Failed to create candidate', description: err instanceof Error ? err.message : undefined, tone: 'danger' }),
      });
    }
  };

  const handleStatusChange = (c: CandidateResponse, newStatus: string) => {
    changeStatus.mutate({ id: c.id, data: { newStatus } }, {
      onSuccess: () => toast({ title: 'Status updated', tone: 'success' }),
      onError: (err) => toast({ title: 'Failed to update status', description: err instanceof Error ? err.message : undefined, tone: 'danger' }),
    });
  };

  const handleDelete = (c: CandidateResponse) => {
    if (!window.confirm(`Delete candidate "${c.firstName} ${c.lastName}"? This cannot be undone.`)) return;
    deleteCandidate.mutate(c.id, {
      onSuccess: () => toast({ title: 'Candidate deleted', tone: 'success' }),
      onError: () => toast({ title: 'Failed to delete candidate', tone: 'danger' }),
    });
  };

  const handleUpload = () => {
    if (!detail || !uploadFile) return;
    uploadAttachment.mutate({ file: uploadFile, attachmentType: 'CV' }, {
      onSuccess: () => { toast({ title: 'Resume uploaded', tone: 'success' }); setUploadFile(null); },
      onError: () => toast({ title: 'Failed to upload resume', tone: 'danger' }),
    });
  };

  const handleAddNote = () => {
    if (!detail) return;
    createNote.mutate(noteForm, {
      onSuccess: () => { toast({ title: 'Note added', tone: 'success' }); setNoteForm({ title: '', category: 'GENERAL', priority: 'MEDIUM', content: '', visibility: 'DEPARTMENT' }); },
      onError: () => toast({ title: 'Failed to add note', tone: 'danger' }),
    });
  };

  if (isLoading) {
    return <div className="flex items-center justify-center py-20"><Loader2 className="h-8 w-8 animate-spin text-text-tertiary" /></div>;
  }

  if (isError) {
    return <div className="flex flex-col items-center justify-center py-20 gap-3"><p className="text-body font-medium text-danger-600">Failed to load candidates</p><p className="text-caption text-text-tertiary">Please try again later.</p></div>;
  }

  return (
    <div className="flex flex-col gap-4">
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
            <span className="text-2xs text-text-tertiary">Total Candidates</span>
            <span className="text-section font-bold text-text-primary">{stats.totalCandidates}</span>
          </div>
          <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
            <span className="text-2xs text-text-tertiary">In Progress</span>
            <span className="text-section font-bold text-accent-600">{stats.inProgressCount}</span>
          </div>
          <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
            <span className="text-2xs text-text-tertiary">Hired</span>
            <span className="text-section font-bold text-success-600">{stats.hiredCount}</span>
          </div>
          <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
            <span className="text-2xs text-text-tertiary">Rejected</span>
            <span className="text-section font-bold text-danger-600">{stats.rejectedCount}</span>
          </div>
        </div>
      )}

      <div className="flex flex-wrap items-center gap-2">
        <Input
          placeholder="Search candidates..." leftIcon={<Search />}
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          containerClassName="flex-1 min-w-[200px]"
        />
        <Select
          containerClassName="w-44"
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          options={[{ value: '', label: 'All Statuses' }, ...CANDIDATE_STATUSES.map((s) => ({ value: s, label: candidateStatusLabel[s] }))]}
        />
        <Select
          containerClassName="w-44"
          value={sourceFilter}
          onChange={(e) => { setSourceFilter(e.target.value); setPage(0); }}
          options={[{ value: '', label: 'All Sources' }, ...CANDIDATE_SOURCES.map((s) => ({ value: s, label: candidateSourceLabel[s] }))]}
        />
        <Button leftIcon={<Plus />} onClick={openCreate}>Add Candidate</Button>
      </div>

      <Modal
        open={showForm}
        onClose={() => setShowForm(false)}
        title={editing ? 'Edit Candidate' : 'New Candidate'}
        footer={
          <>
            <Button variant="outline" onClick={() => setShowForm(false)}>Cancel</Button>
            <Button onClick={handleSubmit} disabled={!form.firstName || !form.lastName || !form.email || !form.position}>{editing ? 'Save' : 'Create'}</Button>
          </>
        }
      >
        <div className="grid grid-cols-2 gap-3">
          <Input label="First Name" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
          <Input label="Last Name" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
          <Input label="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <Input label="Phone" value={form.phone ?? ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          <div className="col-span-2">
            <Input label="Position" value={form.position} onChange={(e) => setForm({ ...form, position: e.target.value })} />
          </div>
          <Select
            label="Source"
            value={form.source ?? 'LINKEDIN'}
            onChange={(e) => setForm({ ...form, source: e.target.value })}
            options={CANDIDATE_SOURCES.map((s) => ({ value: s, label: candidateSourceLabel[s] }))}
          />
        </div>
      </Modal>

      {filtered.length === 0 ? (
        <Card>
          <CardBody className="py-16">
            <EmptyState icon={<UserPlus />} title="No candidates found" description="Add candidates to start building your pipeline." />
          </CardBody>
        </Card>
      ) : (
        <div className="space-y-2">
          {filtered.map((c) => (
            <div key={c.id} className="flex flex-wrap items-center gap-4 p-4 rounded-lg border border-border-subtle bg-surface hover:bg-surface-2 transition-colors">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-accent-100 text-accent-700 dark:bg-accent-900 dark:text-accent-300 text-body font-semibold">
                {c.firstName[0]}{c.lastName[0]}
              </div>
              <div className="flex-1 min-w-[180px]">
                <p className="text-body font-medium text-text-primary">{c.firstName} {c.lastName}</p>
                <p className="text-caption text-text-tertiary">{c.position} • {c.email}</p>
              </div>
              {c.source && (
                <Badge tone="neutral" variant="soft">{candidateSourceLabel[c.source] ?? formatEnum(c.source)}</Badge>
              )}
              <Badge tone={(candidateStatusColor[c.currentStatus] ?? 'neutral') as Tone} variant="soft" dot>{candidateStatusLabel[c.currentStatus] ?? c.currentStatus}</Badge>
              <div className="flex items-center gap-1">
                <Select
                  containerClassName="w-40"
                  value=""
                  onChange={(e) => e.target.value && handleStatusChange(c, e.target.value)}
                  options={[{ value: '', label: 'Change status' }, ...CANDIDATE_STATUSES.filter((s) => s !== c.currentStatus).map((s) => ({ value: s, label: candidateStatusLabel[s] }))]}
                />
                <IconButton label="View details" variant="ghost" size="sm" onClick={() => setDetail(c)}>
                  <Eye className="h-4 w-4" />
                </IconButton>
                <IconButton label="Edit" variant="ghost" size="sm" onClick={() => openEdit(c)}>
                  <Pencil className="h-4 w-4" />
                </IconButton>
                <IconButton label="Delete" variant="ghost" size="sm" className="text-danger-600" onClick={() => handleDelete(c)}>
                  <Trash2 className="h-4 w-4" />
                </IconButton>
              </div>
            </div>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex justify-center pt-2">
          <Pagination page={page + 1} totalPages={totalPages} onPageChange={(p) => setPage(p - 1)} />
        </div>
      )}

      <Modal
        open={!!detail}
        onClose={() => setDetail(null)}
        title={detail ? `${detail.firstName} ${detail.lastName}` : ''}
        description={detail ? `${detail.position} • ${candidateStatusLabel[detail.currentStatus] ?? detail.currentStatus}` : ''}
        size="xl"
        footer={<Button variant="outline" onClick={() => setDetail(null)}>Close</Button>}
      >
        {detail && (
          <div className="flex flex-col gap-4">
            <div className="grid grid-cols-2 gap-3">
              <Field icon={<Mail />} label="Email" value={detail.email} />
              <Field icon={<Phone />} label="Phone" value={detail.phone || '-'} />
              <Field icon={<UserPlus />} label="Source" value={candidateSourceLabel[detail.source] ?? formatEnum(detail.source)} />
              <Field icon={<CalendarClock />} label="Added" value={formatDateTime(detail.createdAt)} />
            </div>

            {attachmentStats && (
              <div className="flex items-center gap-2 flex-wrap">
                <Badge tone={attachmentStats.hasCv ? 'success' : 'neutral'} variant="soft">Resume: {attachmentStats.hasCv ? 'Uploaded' : 'Not uploaded'}</Badge>
                <Badge tone="neutral" variant="soft">{attachmentStats.totalAttachments} attachments</Badge>
              </div>
            )}

            <Card>
              <CardBody className="flex flex-col gap-3">
                <div className="flex items-center justify-between">
                  <p className="text-body font-semibold text-text-primary">Resume & Attachments</p>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <span className="text-caption text-text-secondary">Upload CV:</span>
                    <input type="file" className="text-2xs max-w-[180px]" onChange={(e) => setUploadFile(e.target.files?.[0] ?? null)} />
                    <Button size="sm" leftIcon={<Upload />} onClick={handleUpload} disabled={!uploadFile}>Upload</Button>
                  </label>
                </div>
                {attachments.length === 0 ? (
                  <p className="text-caption text-text-tertiary">No attachments uploaded.</p>
                ) : (
                  <div className="space-y-2">
                    {attachments.map((a) => (
                      <div key={a.id} className="flex items-center gap-3 p-3 rounded-lg border border-border-subtle cursor-pointer hover:bg-surface-2 transition-colors"
                        onClick={() => setViewer({ attachment: a })}>
                        <FileText className="h-4 w-4 text-text-tertiary" />
                        <div className="flex-1 min-w-0">
                          <p className="text-caption font-medium text-text-primary">{a.originalFileName}</p>
                          <p className="text-2xs text-text-tertiary">{formatEnum(a.attachmentType)} • v{a.fileVersion}</p>
                        </div>
                        <IconButton label="Download" variant="ghost" size="sm"
                          onClick={(e) => { e.stopPropagation(); downloadAuthenticatedFile(candidateAttachmentService.downloadUrl(wsId, deptId, detail.id, a.id), a.originalFileName).catch(() => toast({ title: 'Failed to download document', tone: 'danger' })); }}>
                          <Download className="h-4 w-4" />
                        </IconButton>
                        <IconButton label="Delete" variant="ghost" size="sm" className="text-danger-600"
                          onClick={(e) => { e.stopPropagation(); deleteAttachment.mutate(a.id, { onSuccess: () => toast({ title: 'Attachment deleted', tone: 'success' }) }); }}>
                          <Trash2 className="h-4 w-4" />
                        </IconButton>
                      </div>
                    ))}
                  </div>
                )}
              </CardBody>
            </Card>

            {viewer && (
              <DocumentViewerModal
                open
                onClose={() => setViewer(null)}
                title={viewer.attachment.originalFileName}
                fileName={viewer.attachment.originalFileName}
                mimeType={viewer.attachment.mimeType}
                url={candidateAttachmentService.downloadUrl(wsId, deptId, detail.id, viewer.attachment.id)}
              />
            )}

            <div className="grid lg:grid-cols-2 gap-4">
              <Card>
                <CardBody className="flex flex-col gap-3">
                  <p className="text-body font-semibold text-text-primary flex items-center gap-2"><MessageSquare className="h-4 w-4" /> Notes</p>
                  <div className="flex flex-col gap-3 border-b border-border-subtle pb-3">
                    <Input placeholder="Note title" value={noteForm.title} onChange={(e) => setNoteForm({ ...noteForm, title: e.target.value })} />
                    <div className="grid grid-cols-3 gap-2">
                      <Select value={noteForm.category} onChange={(e) => setNoteForm({ ...noteForm, category: e.target.value })}
                        options={NOTE_CATEGORIES.map((n) => ({ value: n, label: noteCategoryLabel[n] }))} />
                      <Select value={noteForm.priority} onChange={(e) => setNoteForm({ ...noteForm, priority: e.target.value })}
                        options={NOTE_PRIORITIES.map((p) => ({ value: p, label: formatEnum(p) }))} />
                      <Select value={noteForm.visibility} onChange={(e) => setNoteForm({ ...noteForm, visibility: e.target.value })}
                        options={NOTE_VISIBILITIES.map((v) => ({ value: v, label: noteVisibilityLabel[v] }))} />
                    </div>
                    <Textarea rows={3} placeholder="Note content..." value={noteForm.content} onChange={(e) => setNoteForm({ ...noteForm, content: e.target.value })} />
                    <div className="flex justify-end">
                      <Button size="sm" leftIcon={<Plus />} onClick={handleAddNote} disabled={!noteForm.title || !noteForm.content}>Add Note</Button>
                    </div>
                  </div>
                  {notes.length === 0 ? (
                    <p className="text-caption text-text-tertiary">No notes yet.</p>
                  ) : (
                    <div className="space-y-2">
                      {notes.map((n) => (
                        <div key={n.id} className="p-3 rounded-lg border border-border-subtle">
                          <div className="flex items-center justify-between gap-2">
                            <p className="text-caption font-medium text-text-primary">{n.title}</p>
                            <div className="flex items-center gap-1">
                              <Badge tone="neutral" variant="soft">{noteCategoryLabel[n.category] ?? formatEnum(n.category)}</Badge>
                              <Badge tone={(notePriorityColor[n.priority] ?? 'neutral') as Tone} variant="soft">{formatEnum(n.priority)}</Badge>
                            </div>
                          </div>
                          <p className="text-caption text-text-secondary mt-1">{n.content}</p>
                          <p className="text-2xs text-text-tertiary mt-1">{n.visibility} • {formatDateTime(n.createdAt)}</p>
                        </div>
                      ))}
                    </div>
                  )}
                </CardBody>
              </Card>

              <Card>
                <CardBody className="flex flex-col gap-3">
                  <p className="text-body font-semibold text-text-primary flex items-center gap-2"><CalendarClock className="h-4 w-4" /> Interviews</p>
                  {interviews.length === 0 ? (
                    <p className="text-caption text-text-tertiary">No interviews scheduled.</p>
                  ) : (
                    <div className="space-y-2">
                      {interviews.map((iv) => (
                        <div key={iv.id} className="flex items-center justify-between p-3 rounded-lg border border-border-subtle">
                          <div>
                            <p className="text-caption font-medium text-text-primary">{formatEnum(iv.type)}</p>
                            <p className="text-2xs text-text-tertiary">{iv.scheduledDate ?? '-'} {iv.startTime ?? ''}</p>
                          </div>
                          <Badge tone={iv.status === 'COMPLETED' ? 'success' : iv.status === 'CANCELLED' ? 'danger' : 'info'} variant="soft">{formatEnum(iv.status)}</Badge>
                        </div>
                      ))}
                    </div>
                  )}

                  <p className="text-body font-semibold text-text-primary mt-2 flex items-center gap-2"><CalendarClock className="h-4 w-4" /> Timeline</p>
                  {!timeline || timeline.length === 0 ? (
                    <p className="text-caption text-text-tertiary">No activity recorded.</p>
                  ) : (
                    <div className="flex flex-col gap-2">
                      {timeline.map((t) => (
                        <div key={t.id} className="flex items-start gap-3 p-2 rounded hover:bg-surface-2 transition-colors">
                          <span className="h-2 w-2 rounded-full bg-accent-500 mt-1.5 shrink-0" />
                          <div className="min-w-0">
                            <p className="text-caption font-medium text-text-primary">{t.title}</p>
                            {t.description && <p className="text-2xs text-text-tertiary">{t.description}</p>}
                            <p className="text-2xs text-text-tertiary">{t.actorName} • {formatDateTime(t.occurredAt)}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </CardBody>
              </Card>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}

function Field({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
      <span className="text-2xs text-text-tertiary flex items-center gap-1">{icon}{label}</span>
      <span className="text-caption text-text-primary break-words">{value}</span>
    </div>
  );
}
