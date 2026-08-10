import { useState } from 'react';
import {
  ArrowLeft,
  Send,
  Check,
  X,
  CheckCircle2,
  Archive,
  Trash2,
  MessageSquare,
  Paperclip,
  Plus,
  FileText,
  Clock,
  AlertCircle,
  Bell,
  XCircle,
  CheckCheck,
  Info,
  Pencil,
} from 'lucide-react';
import { Card, CardBody, CardHeader, CardTitle } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Textarea } from '../../components/ui/Textarea';
import { Timeline, type TimelineItem } from '../../components/ui/Timeline';
import { EmptyState } from '../../components/ui/EmptyState';
import { PageLoader } from '../../components/ui/PageLoader';
import { useToast } from '../../components/ui/Toast';
import { useWorkspaceId } from '../../hooks/useWorkspaceId';
import {
  useHandoverEntry,
  useHandoverComments,
  useHandoverAttachments,
  useHandoverTimeline,
  useSendHandover,
  useAcceptHandover,
  useRejectHandover,
  useCompleteHandover,
  useArchiveHandover,
  useDeleteHandoverEntry,
  useAddHandoverComment,
  useDeleteHandoverComment,
  useDeleteHandoverAttachment,
} from '../../services/handover-hooks';
const statusTone: Record<string, 'neutral' | 'info' | 'success' | 'danger' | 'warning'> = {
  DRAFT: 'neutral',
  PENDING: 'info',
  ACCEPTED: 'warning',
  REJECTED: 'danger',
  COMPLETED: 'success',
  ARCHIVED: 'neutral',
};

const priorityTone: Record<string, 'neutral' | 'success' | 'warning' | 'danger'> = {
  LOW: 'neutral',
  MEDIUM: 'success',
  HIGH: 'warning',
  URGENT: 'danger',
};

const eventTone: Record<string, TimelineItem['tone']> = {
  CREATED: 'accent',
  UPDATED: 'neutral',
  SENT: 'info',
  ACCEPTED: 'success',
  REJECTED: 'danger',
  COMPLETED: 'success',
  ARCHIVED: 'neutral',
  COMMENTED: 'accent',
  ATTACHMENT_ADDED: 'info',
  ATTACHMENT_REMOVED: 'neutral',
  REMINDER_SENT: 'warning',
};

const eventIcon: Record<string, React.ReactNode> = {
  CREATED: <Plus />,
  UPDATED: <Pencil />,
  SENT: <Send />,
  ACCEPTED: <Check />,
  REJECTED: <XCircle />,
  COMPLETED: <CheckCheck />,
  ARCHIVED: <Archive />,
  COMMENTED: <MessageSquare />,
  ATTACHMENT_ADDED: <Paperclip />,
  ATTACHMENT_REMOVED: <Trash2 />,
  REMINDER_SENT: <Bell />,
};

function formatDate(value?: string) {
  if (!value) return '—';
  return new Date(value).toLocaleString();
}

export function HandoverDetailPage({ entryId, onBack }: { entryId: string; onBack: () => void }) {
  const wsId = useWorkspaceId();
  const { toast } = useToast();
  const [comment, setComment] = useState('');
  const [rejectOpen, setRejectOpen] = useState(false);

  const entryQuery = useHandoverEntry(wsId, entryId);
  const timelineQuery = useHandoverTimeline(wsId, entryId);

  const sendMutation = useSendHandover(wsId);
  const acceptMutation = useAcceptHandover(wsId);
  const completeMutation = useCompleteHandover(wsId);
  const archiveMutation = useArchiveHandover(wsId);
  const deleteMutation = useDeleteHandoverEntry(wsId);
  const addCommentMutation = useAddHandoverComment(wsId, entryId);
  const deleteAttachmentMutation = useDeleteHandoverAttachment(wsId, entryId);

  const handleAction = async (fn: () => Promise<unknown>, success: string) => {
    try {
      await fn();
      toast({ title: success, tone: 'success' });
    } catch (err) {
      toast({
        title: 'Action failed',
        description: (err as { message?: string })?.message ?? 'An unexpected error occurred.',
        tone: 'danger',
      });
    }
  };

  if (entryQuery.isLoading) return <PageLoader />;
  if (entryQuery.isError || !entryQuery.data) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-4">
        <AlertCircle className="h-12 w-12 text-danger-500" />
        <p className="text-body font-medium text-text-primary">Failed to load handover</p>
        <p className="text-caption text-text-tertiary">{(entryQuery.error as { message?: string })?.message ?? 'An unexpected error occurred.'}</p>
        <Button onClick={onBack} leftIcon={<ArrowLeft />}>Back to Handovers</Button>
      </div>
    );
  }

  const entry = entryQuery.data;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-3">
        <button
          type="button"
          onClick={onBack}
          className="flex h-9 w-9 items-center justify-center rounded-lg border border-border-subtle text-text-secondary hover:bg-surface-2 hover:text-text-primary transition-colors"
          aria-label="Back"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <div className="flex flex-wrap items-center justify-end gap-2">
          {entry.status === 'DRAFT' && (
            <>
              <Button
                size="sm"
                leftIcon={<Send />}
                loading={sendMutation.isPending}
                onClick={() => handleAction(() => sendMutation.mutateAsync({ entryId }), 'Handover sent')}
              >
                Send
              </Button>
              <Button
                size="sm"
                variant="outline"
                leftIcon={<Trash2 />}
                loading={deleteMutation.isPending}
                onClick={() => handleAction(() => deleteMutation.mutateAsync(entryId), 'Handover deleted')}
              >
                Delete
              </Button>
            </>
          )}
          {entry.status === 'PENDING' && (
            <>
              <Button
                size="sm"
                variant="success"
                leftIcon={<Check />}
                loading={acceptMutation.isPending}
                onClick={() => handleAction(() => acceptMutation.mutateAsync({ entryId }), 'Handover accepted')}
              >
                Accept
              </Button>
              <Button
                size="sm"
                variant="outline"
                leftIcon={<X />}
                onClick={() => setRejectOpen(true)}
              >
                Reject
              </Button>
            </>
          )}
          {entry.status === 'ACCEPTED' && (
            <Button
              size="sm"
              leftIcon={<CheckCircle2 />}
              loading={completeMutation.isPending}
              onClick={() => handleAction(() => completeMutation.mutateAsync({ entryId }), 'Handover completed')}
            >
              Mark Complete
            </Button>
          )}
          {entry.status !== 'ARCHIVED' && (
            <Button
              size="sm"
              variant="outline"
              leftIcon={<Archive />}
              loading={archiveMutation.isPending}
              onClick={() => handleAction(() => archiveMutation.mutateAsync({ entryId }), 'Handover archived')}
            >
              Archive
            </Button>
          )}
        </div>
      </div>

      {rejectOpen && (
        <RejectPanel
          entryId={entryId}
          onCancel={() => setRejectOpen(false)}
          onSubmitted={() => {
            setRejectOpen(false);
            toast({ title: 'Handover rejected', tone: 'success' });
          }}
        />
      )}

      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap items-center gap-2">
          <h1 className="text-page font-semibold text-text-primary">{entry.title}</h1>
          <Badge tone={statusTone[entry.status]} variant="soft">{entry.status}</Badge>
          <Badge tone={priorityTone[entry.priority]} variant="outline">{entry.priority}</Badge>
        </div>
        <p className="whitespace-pre-wrap text-body leading-relaxed text-text-secondary">{entry.content}</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetaBox label="Sender" value={`${entry.sender.firstName} ${entry.sender.lastName}`} sub={entry.sender.email} />
        <MetaBox label="Receiver" value={entry.receiver ? `${entry.receiver.firstName} ${entry.receiver.lastName}` : '—'} sub={entry.receiver?.email} />
        <MetaBox label="Due date" value={formatDate(entry.dueDate)} />
        <MetaBox label="Project" value={entry.projectId} sub={`Created ${formatDate(entry.createdAt)}`} />
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="flex flex-col gap-6 lg:col-span-2">
          <CommentsCard
            entryId={entryId}
            comment={comment}
            setComment={setComment}
            onAdd={() => {
              if (!comment.trim()) return;
              handleAction(
                () => addCommentMutation.mutateAsync({ content: comment.trim() }).then(() => setComment('')),
                'Comment added',
              );
            }}
            loading={addCommentMutation.isPending}
          />

          <AttachmentsCard entryId={entryId} onDelete={(id) => handleAction(() => deleteAttachmentMutation.mutateAsync(id), 'Attachment removed')} />
        </div>

        <Card>
          <CardHeader><CardTitle>Timeline</CardTitle></CardHeader>
          <CardBody>
            {timelineQuery.isLoading ? (
              <PageLoader />
            ) : !timelineQuery.data || timelineQuery.data.length === 0 ? (
              <EmptyState icon={<Clock />} title="No activity yet" />
            ) : (
              <Timeline
                items={timelineQuery.data.map((ev) => ({
                  id: ev.id,
                  tone: eventTone[ev.eventType] ?? 'neutral',
                  icon: eventIcon[ev.eventType],
                  title: ev.description ?? '',
                  timestamp: formatDate(ev.occurredAt),
                }))}
              />
            )}
          </CardBody>
        </Card>
      </div>
    </div>
  );
}

function MetaBox({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="rounded-lg border border-border-subtle bg-surface-2 p-4">
      <p className="text-2xs font-medium text-text-tertiary">{label}</p>
      <p className="mt-1 truncate text-body font-semibold text-text-primary" title={value}>{value}</p>
      {sub && <p className="mt-0.5 truncate text-caption text-text-tertiary" title={sub}>{sub}</p>}
    </div>
  );
}

function RejectPanel({
  entryId,
  onCancel,
  onSubmitted,
}: {
  entryId: string;
  onCancel: () => void;
  onSubmitted: () => void;
}) {
  const wsId = useWorkspaceId();
  const rejectMutation = useRejectHandover(wsId);
  const [reason, setReason] = useState('');
  const { toast } = useToast();

  return (
    <div className="rounded-xl border border-danger-200 dark:border-danger-800 bg-danger-50 dark:bg-danger-900/40 p-4">
      <div className="flex items-center gap-2 mb-3">
        <XCircle className="h-4 w-4 text-danger-600 dark:text-danger-300" />
        <p className="text-body font-medium text-danger-700 dark:text-danger-200">Reject handover</p>
      </div>
      <Textarea
        placeholder="Reason for rejection (optional)"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
      />
      <div className="mt-3 flex items-center justify-end gap-2">
        <Button size="sm" variant="outline" onClick={onCancel}>Cancel</Button>
        <Button
          size="sm"
          variant="danger"
          loading={rejectMutation.isPending}
          onClick={async () => {
            try {
              await rejectMutation.mutateAsync({ entryId, data: reason.trim() ? { reason: reason.trim() } : undefined });
              onSubmitted();
            } catch (err) {
              toast({
                title: 'Failed to reject handover',
                description: (err as { message?: string })?.message ?? 'An unexpected error occurred.',
                tone: 'danger',
              });
            }
          }}
        >
          Reject
        </Button>
      </div>
    </div>
  );
}

function CommentsCard({
  entryId,
  comment,
  setComment,
  onAdd,
  loading,
}: {
  entryId: string;
  comment: string;
  setComment: (v: string) => void;
  onAdd: () => void;
  loading: boolean;
}) {
  const wsId = useWorkspaceId();
  const commentsQuery = useHandoverComments(wsId, entryId);
  const deleteCommentMutation = useDeleteHandoverComment(wsId, entryId);
  const { toast } = useToast();

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2"><MessageSquare className="h-4 w-4" /> Comments</CardTitle>
      </CardHeader>
      <CardBody className="space-y-4">
        <div className="flex flex-col gap-2">
          <Textarea
            placeholder="Add a comment..."
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            className="min-h-[64px]"
          />
          <div className="flex justify-end">
            <Button size="sm" onClick={onAdd} loading={loading} disabled={!comment.trim()}>
              Add Comment
            </Button>
          </div>
        </div>

        {commentsQuery.isLoading ? (
          <PageLoader />
        ) : !commentsQuery.data || commentsQuery.data.length === 0 ? (
          <EmptyState icon={<MessageSquare />} title="No comments yet" description="Start the discussion about this handover." />
        ) : (
          <div className="flex flex-col gap-3">
            {commentsQuery.data.map((c) => (
              <div key={c.id} className="rounded-lg border border-border-subtle bg-surface-2 p-3">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-2">
                    <span className="h-5 w-5 rounded-full bg-accent-50 text-accent-700 text-2xs font-semibold inline-flex items-center justify-center">
                      {(c.author.firstName?.[0] ?? '?') + (c.author.lastName?.[0] ?? '')}
                    </span>
                    <span className="text-caption font-medium text-text-primary">
                      {c.author.firstName} {c.author.lastName}
                    </span>
                    <span className="text-caption text-text-tertiary">{formatDate(c.createdAt)}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => deleteCommentMutation.mutateAsync(c.id).catch(() => toast({ title: 'Failed to delete comment', tone: 'danger' }))}
                    className="text-text-tertiary hover:text-danger-500 transition-colors"
                    aria-label="Delete comment"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
                <p className="mt-2 text-body text-text-secondary">{c.content}</p>
              </div>
            ))}
          </div>
        )}
      </CardBody>
    </Card>
  );
}

function AttachmentsCard({ entryId, onDelete }: { entryId: string; onDelete: (id: string) => void }) {
  const wsId = useWorkspaceId();
  const attachmentsQuery = useHandoverAttachments(wsId, entryId);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2"><Paperclip className="h-4 w-4" /> Attachments</CardTitle>
      </CardHeader>
      <CardBody>
        {attachmentsQuery.isLoading ? (
          <PageLoader />
        ) : !attachmentsQuery.data || attachmentsQuery.data.length === 0 ? (
          <EmptyState icon={<Paperclip />} title="No attachments" description="Files shared on this handover will appear here." />
        ) : (
          <div className="flex flex-col gap-2">
            {attachmentsQuery.data.map((a) => (
              <div key={a.id} className="flex items-center justify-between gap-3 rounded-lg border border-border-subtle p-3">
                <div className="flex min-w-0 items-center gap-3">
                  <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-surface-2 text-text-tertiary">
                    <FileText className="h-4 w-4" />
                  </span>
                  <div className="min-w-0">
                    <p className="truncate text-body font-medium text-text-primary" title={a.fileName}>{a.fileName}</p>
                    <p className="text-caption text-text-tertiary">
                      {(a.fileSize ?? 0) > 0 ? `${(a.fileSize! / 1024).toFixed(1)} KB` : ''} · Uploaded {formatDate(a.createdAt)}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => onDelete(a.id)}
                  className="text-text-tertiary hover:text-danger-500 transition-colors"
                  aria-label="Delete attachment"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        )}
        <div className="mt-4 flex items-center gap-2 text-caption text-text-tertiary">
          <Info className="h-3.5 w-3.5" /> Attachment upload will be enabled with the file storage integration.
        </div>
      </CardBody>
    </Card>
  );
}
