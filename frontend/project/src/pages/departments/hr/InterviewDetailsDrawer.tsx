import { useEffect, useRef, useState } from 'react';
import {
  X,
  Mail,
  Phone,
  Briefcase,
  CalendarClock,
  Users,
  Star,
  FileText,
  Download,
  Eye,
  Loader2,
  MapPin,
  Link2,
  StickyNote,
  Save,
  Pencil,
} from 'lucide-react';
import { cn } from '../../../lib/cn';
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
} from '../../../components/ui/Card';
import { Badge, type Tone } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';
import { IconButton } from '../../../components/ui/IconButton';
import { Textarea } from '../../../components/ui/Textarea';
import { Select } from '../../../components/ui/Select';
import { useToast } from '../../../components/ui/Toast';
import {
  useInterviewDetail,
  useUpdateInterviewNotes,
  useAddInterviewParticipant,
  useRemoveInterviewParticipant,
} from '../../../services/interview-hooks';
import { useCandidateDetail } from '../../../services/candidate-hooks';
import { useCandidateAttachments } from '../../../services/candidate-attachment-hooks';
import { useUsersList } from '../../../services/admin-hooks';
import { candidateAttachmentService } from '../../../services/candidate-attachment-service';
import { DocumentViewerModal } from '../../../components/documents/DocumentViewerModal';
import { downloadAuthenticatedFile } from '../../../lib/file-download';
import type { InterviewResponse } from '../../../services/interview-service';
import type { CandidateAttachmentResponse } from '../../../services/candidate-attachment-service';
import {
  interviewTypeColor,
  interviewStatusColor,
  candidateStatusColor,
  candidateStatusLabel,
  candidateSourceLabel,
  recommendationColor,
  formatEnum,
  formatDate,
  formatTime,
} from './hr-constants';

interface Props {
  wsId: string;
  deptId: string;
  interview: InterviewResponse;
  open: boolean;
  onClose: () => void;
  onEdit?: (interview: InterviewResponse) => void;
}

interface TimingStatus {
  label: string;
  tone: Tone;
}

function timingStatus(iv: InterviewResponse): TimingStatus {
  if (iv.status === 'COMPLETED') return { label: 'Completed', tone: 'success' };
  if (iv.status === 'CANCELLED') return { label: 'Cancelled', tone: 'danger' };
  if (iv.status === 'NO_SHOW') return { label: 'No Show', tone: 'danger' };
  const now = Date.now();
  const start = iv.startTime ? new Date(iv.startTime).getTime() : null;
  const end = iv.endTime ? new Date(iv.endTime).getTime() : null;
  if (start && end && now >= start && now <= end)
    return { label: 'In Progress', tone: 'warning' };
  if (end && now > end) return { label: 'Completed', tone: 'success' };
  return { label: 'Upcoming', tone: 'info' };
}

function InfoField({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value?: string;
}) {
  return (
    <div className='flex flex-col gap-1 p-3 rounded-lg border border-border-subtle'>
      <span className='text-2xs text-text-tertiary flex items-center gap-1'>
        {icon}
        {label}
      </span>
      <span className='text-caption text-text-primary break-words'>
        {value || '-'}
      </span>
    </div>
  );
}

export function InterviewDetailsDrawer({
  wsId,
  deptId,
  interview,
  open,
  onClose,
  onEdit,
}: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const { toast } = useToast();

  const { data: detail } = useInterviewDetail(
    wsId,
    deptId,
    interview.candidateId,
    interview.id,
  );
  const {
    data: candidate,
    isLoading: candidateLoading,
    isError: candidateError,
  } = useCandidateDetail(wsId, deptId, interview.candidateId);
  const {
    data: attachmentsData,
    isLoading: attachmentsLoading,
    isError: attachmentsError,
  } = useCandidateAttachments(wsId, deptId, interview.candidateId);
  const { data: usersData } = useUsersList();

  const iv = detail ?? interview;
  const attachments = attachmentsData?.content ?? [];

  const [participantUserId, setParticipantUserId] = useState('');
  const addParticipant = useAddInterviewParticipant(
    wsId,
    deptId,
    interview.candidateId,
    interview.id,
  );
  const removeParticipant = useRemoveInterviewParticipant(
    wsId,
    deptId,
    interview.candidateId,
    interview.id,
  );

  const [notes, setNotes] = useState(iv.notes ?? '');
  const [dirty, setDirty] = useState(false);
  const saveNotes = useUpdateInterviewNotes(
    wsId,
    deptId,
    interview.candidateId,
    interview.id,
  );

  const [viewer, setViewer] = useState<{
    attachment: CandidateAttachmentResponse;
  } | null>(null);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  useEffect(() => {
    setNotes(iv.notes ?? '');
    setDirty(false);
  }, [iv.notes]);

  useEffect(() => {
    if (open) ref.current?.focus();
  }, [open]);

  const handleSaveNotes = () => {
    saveNotes.mutate(
      { notes },
      {
        onSuccess: () => {
          setDirty(false);
          toast({ title: 'Interview notes saved', tone: 'success' });
        },
        onError: () => toast({ title: 'Failed to save notes', tone: 'danger' }),
      },
    );
  };

  const timing = timingStatus(iv);

  const resume = attachments.find((a) => a.attachmentType === 'CV');

  return (
    <>
      {open && (
        <div
          className='fixed inset-0 z-40 bg-text-primary/30 dark:bg-black/50 backdrop-blur-sm animate-fade-in'
          onClick={onClose}
        />
      )}
      <div
        ref={ref}
        role='dialog'
        aria-modal='true'
        aria-label='Interview details'
        tabIndex={-1}
        className={cn(
          'fixed right-0 top-0 z-50 h-full w-full max-w-2xl border-l border-border-subtle bg-elevated shadow-cx-xl transition-transform duration-300 flex flex-col',
          open ? 'translate-x-0' : 'translate-x-full',
        )}
      >
        <div className='flex items-center justify-between gap-3 px-5 py-4 border-b border-border-subtle'>
          <div className='min-w-0'>
            <p className='text-section font-semibold text-text-primary truncate'>
              Interview â€”{' '}
              {candidate
                ? `${candidate.firstName} ${candidate.lastName}`
                : formatEnum(iv.type)}
            </p>
            <div className='flex items-center gap-2 mt-1'>
              <Badge
                tone={(interviewTypeColor[iv.type] ?? 'neutral') as Tone}
                variant='soft'
              >
                {formatEnum(iv.type)}
              </Badge>
              <Badge
                tone={(interviewStatusColor[iv.status] ?? 'neutral') as Tone}
                variant='soft'
              >
                {formatEnum(iv.status)}
              </Badge>
              <Badge tone={timing.tone} variant='soft' dot>
                {timing.label}
              </Badge>
            </div>
          </div>
          <div className='flex items-center gap-1 shrink-0'>
            {onEdit &&
              (iv.status === 'SCHEDULED' || iv.status === 'RESCHEDULED') && (
                <Button
                  size='sm'
                  variant='outline'
                  leftIcon={<Pencil className='h-4 w-4' />}
                  onClick={() => onEdit(iv)}
                >
                  Edit
                </Button>
              )}
            <IconButton
              label='Close'
              variant='ghost'
              size='sm'
              onClick={onClose}
            >
              <X className='h-4 w-4' />
            </IconButton>
          </div>
        </div>

        <div className='flex-1 overflow-y-auto px-5 py-5 space-y-4'>
          <Card>
            <CardHeader>
              <CardTitle className='flex items-center gap-2'>
                <Users className='h-4 w-4' /> Candidate
              </CardTitle>
            </CardHeader>
            <CardBody>
              {candidateLoading ? (
                <div className='flex items-center justify-center py-10'>
                  <Loader2 className='h-6 w-6 animate-spin text-text-tertiary' />
                </div>
              ) : candidateError || !candidate ? (
                <p className='text-caption text-danger-600'>
                  Could not load candidate information.
                </p>
              ) : (
                <>
                  <div className='flex items-center gap-3 mb-4'>
                    <div className='flex h-11 w-11 items-center justify-center rounded-full bg-accent-100 text-accent-700 dark:bg-accent-900 dark:text-accent-300 text-body font-semibold'>
                      {candidate.firstName[0]}
                      {candidate.lastName[0]}
                    </div>
                    <div className='min-w-0'>
                      <p className='text-body font-semibold text-text-primary'>
                        {candidate.firstName} {candidate.lastName}
                      </p>
                      <p className='text-caption text-text-tertiary'>
                        {candidate.position}
                      </p>
                    </div>
                    <div className='ml-auto flex flex-col items-end gap-1'>
                      <Badge
                        tone={
                          (candidateStatusColor[candidate.currentStatus] ??
                            'neutral') as Tone
                        }
                        variant='soft'
                        dot
                      >
                        {candidateStatusLabel[candidate.currentStatus] ??
                          formatEnum(candidate.currentStatus)}
                      </Badge>
                      {candidate.source && (
                        <Badge tone='neutral' variant='soft'>
                          {candidateSourceLabel[candidate.source] ??
                            formatEnum(candidate.source)}
                        </Badge>
                      )}
                    </div>
                  </div>
                  <div className='grid grid-cols-2 gap-3'>
                    <InfoField
                      icon={<Mail className='h-3.5 w-3.5' />}
                      label='Email'
                      value={candidate.email}
                    />
                    <InfoField
                      icon={<Phone className='h-3.5 w-3.5' />}
                      label='Phone'
                      value={candidate.phone}
                    />
                    <InfoField
                      icon={<Briefcase className='h-3.5 w-3.5' />}
                      label='Position'
                      value={candidate.position}
                    />
                    <InfoField
                      icon={<Users className='h-3.5 w-3.5' />}
                      label='Interview Type'
                      value={formatEnum(iv.type)}
                    />
                    <InfoField
                      icon={<BadgeToneIcon />}
                      label='Candidate Status'
                      value={
                        candidateStatusLabel[candidate.currentStatus] ??
                        formatEnum(candidate.currentStatus)
                      }
                    />
                    <InfoField
                      icon={<Link2 className='h-3.5 w-3.5' />}
                      label='Source'
                      value={
                        candidate.source
                          ? (candidateSourceLabel[candidate.source] ??
                            formatEnum(candidate.source))
                          : ''
                      }
                    />
                  </div>
                </>
              )}
            </CardBody>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className='flex items-center gap-2'>
                <FileText className='h-4 w-4' /> Resume / Documents
              </CardTitle>
            </CardHeader>
            <CardBody>
              {attachmentsLoading ? (
                <div className='flex items-center justify-center py-10'>
                  <Loader2 className='h-6 w-6 animate-spin text-text-tertiary' />
                </div>
              ) : attachmentsError ? (
                <p className='text-caption text-danger-600'>
                  Could not load documents.
                </p>
              ) : attachments.length === 0 ? (
                <p className='text-caption text-text-tertiary'>
                  No resume or documents uploaded for this candidate.
                </p>
              ) : (
                <div className='space-y-2'>
                  {attachments.map((a) => (
                    <div
                      key={a.id}
                      className={cn(
                        'flex items-center gap-3 p-3 rounded-lg border cursor-pointer hover:bg-surface-2 transition-colors',
                        resume && a.id === resume.id
                          ? 'border-accent-300 dark:border-accent-700 bg-accent-50/50 dark:bg-accent-900/20'
                          : 'border-border-subtle',
                      )}
                    >
                      <FileText className='h-4 w-4 text-text-tertiary shrink-0' />
                      <div className='flex-1 min-w-0'>
                        <p className='text-caption font-medium text-text-primary truncate'>
                          {a.originalFileName}
                          {resume && a.id === resume.id && (
                            <Badge
                              tone='accent'
                              variant='soft'
                              className='ml-2'
                            >
                              Resume
                            </Badge>
                          )}
                        </p>
                        <p className='text-2xs text-text-tertiary'>
                          {formatEnum(a.attachmentType)} â€¢ v{a.fileVersion}
                        </p>
                      </div>
                      <IconButton
                        label='View'
                        variant='ghost'
                        size='sm'
                        onClick={() => setViewer({ attachment: a })}
                      >
                        <Eye className='h-4 w-4' />
                      </IconButton>
                      <IconButton
                        label='Download'
                        variant='ghost'
                        size='sm'
                        onClick={() =>
                          downloadAuthenticatedFile(
                            candidateAttachmentService.downloadUrl(
                              wsId,
                              deptId,
                              interview.candidateId,
                              a.id,
                            ),
                            a.originalFileName,
                          ).catch(() =>
                            toast({
                              title: 'Failed to download document',
                              tone: 'danger',
                            }),
                          )
                        }
                      >
                        <Download className='h-4 w-4' />
                      </IconButton>
                    </div>
                  ))}
                </div>
              )}
            </CardBody>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className='flex items-center gap-2'>
                <CalendarClock className='h-4 w-4' /> Interview
              </CardTitle>
            </CardHeader>
            <CardBody>
              <div className='grid grid-cols-2 gap-3'>
                <InfoField
                  icon={<Users className='h-3.5 w-3.5' />}
                  label='Type'
                  value={formatEnum(iv.type)}
                />
                <InfoField
                  icon={<Briefcase className='h-3.5 w-3.5' />}
                  label='Position'
                  value={iv.position}
                />
                <InfoField
                  icon={<CalendarClock className='h-3.5 w-3.5' />}
                  label='Date'
                  value={formatDate(iv.scheduledDate)}
                />
                <InfoField
                  icon={<CalendarClock className='h-3.5 w-3.5' />}
                  label='Time'
                  value={
                    iv.startTime
                      ? `${formatTime(iv.startTime)}${iv.endTime ? ` - ${formatTime(iv.endTime)}` : ''}`
                      : ''
                  }
                />
                <InfoField
                  icon={<MapPin className='h-3.5 w-3.5' />}
                  label='Location'
                  value={iv.location}
                />
                <InfoField
                  icon={<Link2 className='h-3.5 w-3.5' />}
                  label='Meeting Link'
                  value={iv.meetingLink}
                />
                {iv.title && (
                  <InfoField
                    icon={<StickyNote className='h-3.5 w-3.5' />}
                    label='Title'
                    value={iv.title}
                  />
                )}
                <InfoField
                  icon={<StickyNote className='h-3.5 w-3.5' />}
                  label='Status'
                  value={formatEnum(iv.status)}
                />
              </div>
              {iv.description && (
                <p className='text-caption text-text-secondary mt-3 whitespace-pre-wrap'>
                  {iv.description}
                </p>
              )}
            </CardBody>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className='flex items-center gap-2'>
                <StickyNote className='h-4 w-4' /> Interview Notes
              </CardTitle>
            </CardHeader>
            <CardBody className='flex flex-col gap-3'>
              <Textarea
                rows={6}
                placeholder='Write your interview notes...'
                value={notes}
                onChange={(e) => {
                  setNotes(e.target.value);
                  setDirty(true);
                }}
              />
              <div className='flex items-center justify-between gap-2'>
                <p className='text-2xs text-text-tertiary'>
                  {dirty
                    ? 'Unsaved changes'
                    : iv.notes
                      ? 'Notes saved'
                      : 'No notes saved yet'}
                </p>
                <Button
                  size='sm'
                  leftIcon={<Save className='h-4 w-4' />}
                  onClick={handleSaveNotes}
                  disabled={!dirty}
                  loading={saveNotes.isPending}
                >
                  Save Notes
                </Button>
              </div>
            </CardBody>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className='flex items-center gap-2'>
                <Users className='h-4 w-4' /> Participants
              </CardTitle>
            </CardHeader>
            <CardBody className='flex flex-col gap-3'>
              <div className='flex items-center gap-2'>
                <Select
                  containerClassName='flex-1'
                  value={participantUserId}
                  onChange={(e) => setParticipantUserId(e.target.value)}
                  options={[
                    { value: '', label: 'Select user to add...' },
                    ...(usersData ?? []).map((u) => ({
                      value: u.id,
                      label: `${u.firstName} ${u.lastName}${u.email ? ` (${u.email})` : ''}`,
                    })),
                  ]}
                />
                <Button
                  size='sm'
                  disabled={!participantUserId}
                  onClick={() => {
                    addParticipant.mutate(
                      { userId: participantUserId, role: 'INTERVIEWER' },
                      {
                        onSuccess: () => {
                          toast({
                            title: 'Participant added',
                            tone: 'success',
                          });
                          setParticipantUserId('');
                        },
                        onError: () =>
                          toast({
                            title: 'Failed to add participant',
                            tone: 'danger',
                          }),
                      },
                    );
                  }}
                >
                  Add
                </Button>
              </div>
              {iv.participants?.length === 0 ? (
                <p className='text-caption text-text-tertiary'>
                  No participants added.
                </p>
              ) : (
                <div className='space-y-2'>
                  {iv.participants?.map((p) => (
                    <div
                      key={p.id}
                      className='flex items-center justify-between p-3 rounded-lg border border-border-subtle'
                    >
                      <div>
                        <p className='text-caption font-medium text-text-primary'>
                          {p.userFirstName} {p.userLastName}
                        </p>
                        <p className='text-2xs text-text-tertiary'>
                          {p.userEmail}
                        </p>
                      </div>
                      <div className='flex items-center gap-2'>
                        <Badge tone='neutral' variant='soft'>
                          {formatEnum(p.role)}
                        </Badge>
                        <IconButton
                          label='Remove participant'
                          variant='ghost'
                          size='sm'
                          className='text-danger-600'
                          onClick={() =>
                            removeParticipant.mutate(p.id, {
                              onSuccess: () =>
                                toast({
                                  title: 'Participant removed',
                                  tone: 'success',
                                }),
                            })
                          }
                        >
                          <X className='h-4 w-4' />
                        </IconButton>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardBody>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className='flex items-center gap-2'>
                <Star className='h-4 w-4' /> Feedback
              </CardTitle>
            </CardHeader>
            <CardBody>
              {iv.feedbacks?.length === 0 ? (
                <p className='text-caption text-text-tertiary'>
                  No feedback submitted yet.
                </p>
              ) : (
                <div className='space-y-2'>
                  {iv.feedbacks?.map((f) => (
                    <div
                      key={f.id}
                      className='p-3 rounded-lg border border-border-subtle'
                    >
                      <div className='flex items-center justify-between'>
                        <Badge
                          tone={
                            (recommendationColor[f.recommendation] ??
                              'neutral') as Tone
                          }
                          variant='soft'
                        >
                          {formatEnum(f.recommendation)}
                        </Badge>
                        {f.rating != null && (
                          <span className='text-caption font-medium text-text-primary'>
                            {f.rating}/5
                          </span>
                        )}
                      </div>
                      {f.notes && (
                        <p className='text-caption text-text-secondary mt-2'>
                          {f.notes}
                        </p>
                      )}
                      <p className='text-2xs text-text-tertiary mt-1'>
                        Submitted {f.submittedAt}
                      </p>
                    </div>
                  ))}
                </div>
              )}
            </CardBody>
          </Card>
        </div>

        {viewer && (
          <DocumentViewerModal
            open
            onClose={() => setViewer(null)}
            title={viewer.attachment.originalFileName}
            fileName={viewer.attachment.originalFileName}
            mimeType={viewer.attachment.mimeType}
            url={candidateAttachmentService.downloadUrl(
              wsId,
              deptId,
              interview.candidateId,
              viewer.attachment.id,
            )}
          />
        )}
      </div>
    </>
  );
}

function BadgeToneIcon() {
  return <span className='h-3.5 w-3.5 rounded-full bg-accent-500' />;
}
