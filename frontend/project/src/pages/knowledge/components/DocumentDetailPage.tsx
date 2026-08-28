import { useState, useEffect, useCallback, type ReactNode } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  Loader2,
  ArrowLeft,
  Download,
  Trash2,
  Archive,
  RotateCcw,
  CheckCircle,
  XCircle,
  Send,
  Edit2,
  ExternalLink,
  FileWarning,
  FileText,
  Eye,
  AlertTriangle,
} from 'lucide-react';
import { Card, CardBody, CardHeader, CardTitle } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Badge } from '../../../components/ui/Badge';
import type { Tone } from '../../../components/ui/Badge';
import {
  useDocumentDetail,
  useDocumentFile,
  useDeleteDocument,
  useArchiveDocument,
  useRestoreDocument,
  useSubmitForApproval,
  useApproveDocument,
  useRejectDocument,
} from '../../../services/document-hooks';
import { documentService } from '../../../services/document-service';
import { downloadAuthenticatedFile } from '../../../lib/file-download';
import { getFileIcon, formatFileSize, formatDate } from '../types/document-types';
import type { DocumentResponse } from '../types/document-types';

const statusTone: Record<DocumentResponse['status'], Tone> = {
  ACTIVE: 'success',
  ARCHIVED: 'warning',
  DELETED: 'danger',
};

const approvalTone: Record<string, Tone> = {
  pending: 'info',
  approved: 'success',
  rejected: 'danger',
};

const fileEmojiMap: Record<string, string> = {
  pdf: '\u{1F4C4}',
  docx: '\u{1F4DD}',
  xlsx: '\u{1F4CA}',
  pptx: '\u{1F4C8}',
  img: '\u{1F5BC}\uFE0F',
  zip: '\u{1F4E6}',
  other: '\u{1F4CE}',
};

function isImageMime(mimeType?: string): boolean {
  if (!mimeType) return false;
  return /^image\//.test(mimeType);
}

function isPdfMime(mimeType?: string, fileName?: string): boolean {
  if (mimeType) return mimeType.toLowerCase() === 'application/pdf';
  if (!fileName) return false;
  return fileName.toLowerCase().endsWith('.pdf');
}

function isTextMime(mimeType?: string): boolean {
  if (!mimeType) return false;
  return /^text\//.test(mimeType) || mimeType === 'application/json' || mimeType === 'application/xml';
}

function MetaItem({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div>
      <p className="text-caption font-medium text-text-secondary mb-0.5">{label}</p>
      <div className="text-body text-text-primary break-words">{value ?? '\u2014'}</div>
    </div>
  );
}

export function DocumentDetailPage() {
  const { docId } = useParams<{ docId: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const workspaceId = searchParams.get('ws') ?? '';
  const departmentId = searchParams.get('dept') ?? '';
  const projectId = searchParams.get('proj') ?? '';

  const { data: document, isLoading: metaLoading, isError: metaError } = useDocumentDetail(
    workspaceId, departmentId, projectId, docId,
  );

  const shouldFetchFile = !!document && document.status === 'ACTIVE';
  const { data: fileData, isLoading: fileLoading, isError: fileError, error: fileErrorObj } = useDocumentFile(
    workspaceId, departmentId, projectId, docId, shouldFetchFile,
  );

  const deleteMutation = useDeleteDocument(workspaceId, departmentId, projectId);
  const archiveMutation = useArchiveDocument(workspaceId, departmentId, projectId);
  const restoreMutation = useRestoreDocument(workspaceId, departmentId, projectId);
  const submitApprovalMutation = useSubmitForApproval(workspaceId, departmentId, projectId);
  const approveMutation = useApproveDocument(workspaceId, departmentId, projectId);
  const rejectMutation = useRejectDocument(workspaceId, departmentId, projectId);

  const [actionError, setActionError] = useState<string | null>(null);
  const [objectUrl, setObjectUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!fileData?.blob || fileData.blob.size === 0) return;
    const url = URL.createObjectURL(fileData.blob);
    setObjectUrl(url);
    return () => {
      URL.revokeObjectURL(url);
      setObjectUrl(null);
    };
  }, [fileData]);

  const handleAction = useCallback((fn: () => Promise<unknown>) => {
    setActionError(null);
    fn().catch((err: unknown) => {
      const message = err instanceof Error ? err.message : 'Action failed';
      setActionError(message);
    });
  }, []);

  const handleDelete = useCallback(() => {
    if (!docId) return;
    handleAction(async () => {
      await deleteMutation.mutateAsync(docId);
      navigate('../documents');
    });
  }, [docId, handleAction, deleteMutation, navigate]);

  const handleArchive = useCallback(() => {
    if (!docId) return;
    handleAction(async () => { await archiveMutation.mutateAsync(docId); });
  }, [docId, handleAction, archiveMutation]);

  const handleRestore = useCallback(() => {
    if (!docId) return;
    handleAction(async () => { await restoreMutation.mutateAsync(docId); });
  }, [docId, handleAction, restoreMutation]);

  const handleSubmitApproval = useCallback(() => {
    if (!docId) return;
    handleAction(async () => { await submitApprovalMutation.mutateAsync(docId); });
  }, [docId, handleAction, submitApprovalMutation]);

  const handleApprove = useCallback(() => {
    if (!docId) return;
    handleAction(async () => { await approveMutation.mutateAsync(docId); });
  }, [docId, handleAction, approveMutation]);

  const handleReject = useCallback(() => {
    if (!docId) return;
    handleAction(async () => { await rejectMutation.mutateAsync(docId); });
  }, [docId, handleAction, rejectMutation]);

  const handleDownload = useCallback(() => {
    if (!docId || !document) return;
    const url = documentService.downloadPath(workspaceId, departmentId, projectId, docId);
    downloadAuthenticatedFile(url, document.fileName);
  }, [docId, document, workspaceId, departmentId, projectId]);

  const handleOpenInNewTab = useCallback(() => {
    if (!docId) return;
    const url = documentService.view(workspaceId, departmentId, projectId, docId);
    window.open(url, '_blank');
  }, [docId, workspaceId, departmentId, projectId]);

  const isMutating = deleteMutation.isPending
    || archiveMutation.isPending
    || restoreMutation.isPending
    || submitApprovalMutation.isPending
    || approveMutation.isPending
    || rejectMutation.isPending;

  if (metaLoading) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-3">
        <Loader2 className="h-8 w-8 animate-spin text-text-tertiary" />
        <p className="text-caption text-text-tertiary">Loading document...</p>
      </div>
    );
  }

  if (metaError || !document) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-4">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-danger-100 text-danger-600 dark:bg-danger-900 dark:text-danger-300">
          <FileWarning className="h-6 w-6" />
        </div>
        <p className="text-body font-medium text-danger-600">Document not found</p>
        <p className="text-caption text-text-tertiary">The document could not be found or you do not have access.</p>
        <Button variant="outline" onClick={() => navigate('../documents')}>
          <ArrowLeft className="h-4 w-4" />
          Back to Documents
        </Button>
      </div>
    );
  }

  const fileIcon = fileEmojiMap[getFileIcon(document.mimeType)] ?? fileEmojiMap.other;
  const isActive = document.status === 'ACTIVE';
  const isArchived = document.status === 'ARCHIVED';
  const approvalStatus = document.approvalStatus;
  const canSubmitForApproval = isActive && (!approvalStatus || approvalStatus === 'rejected');
  const canApproveOrReject = isActive && approvalStatus === 'pending';

  const pdf = isPdfMime(document.mimeType, document.fileName);
  const image = isImageMime(document.mimeType);
  const text = isTextMime(document.mimeType);
  const canPreview = pdf || image || text;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('../documents')}
          className="flex h-9 w-9 items-center justify-center rounded-lg border border-border-subtle text-text-secondary hover:bg-surface-2 hover:text-text-primary transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <div className="flex-1 min-w-0">
          <h1 className="text-page font-semibold text-text-primary truncate">{document.title}</h1>
          <p className="text-body text-text-secondary flex items-center gap-2">
            <span className="truncate">{document.fileName}</span>
            <span className="shrink-0 text-lg">{fileIcon}</span>
          </p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          {isActive && (
            <Button variant="outline" size="sm" leftIcon={<Download className="h-4 w-4" />} onClick={handleDownload}>
              Download
            </Button>
          )}
          {isActive && canPreview && (
            <Button variant="outline" size="sm" leftIcon={<ExternalLink className="h-4 w-4" />} onClick={handleOpenInNewTab}>
              Open
            </Button>
          )}
          <button
            onClick={() => navigate('../documents')}
            className="flex h-9 w-9 items-center justify-center rounded-lg border border-border-subtle text-text-secondary hover:bg-surface-2 hover:text-text-primary transition-colors"
          >
            <XCircle className="h-4 w-4" />
          </button>
        </div>
      </div>

      {actionError && (
        <Card className="border-danger-200 dark:border-danger-800">
          <CardBody className="flex items-start gap-3">
            <XCircle className="h-5 w-5 text-danger-600 shrink-0 mt-0.5" />
            <div>
              <p className="text-caption font-medium text-danger-700 dark:text-danger-200">Action failed</p>
              <p className="text-2xs text-danger-600 dark:text-danger-300">{actionError}</p>
            </div>
          </CardBody>
        </Card>
      )}

      <Card className="overflow-hidden">
        <div className="min-h-[400px] bg-surface-2">
          {fileLoading && (
            <div className="flex flex-col items-center justify-center py-20 gap-3">
              <Loader2 className="h-8 w-8 animate-spin text-text-tertiary" />
              <p className="text-caption text-text-tertiary">Loading document content...</p>
            </div>
          )}

          {fileError && !fileLoading && (
            <div className="flex flex-col items-center justify-center py-20 gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-danger-100 text-danger-600 dark:bg-danger-900 dark:text-danger-300">
                <AlertTriangle className="h-6 w-6" />
              </div>
              <p className="text-body font-medium text-text-primary">
                {(fileErrorObj as { status?: number })?.status === 403
                  ? "You don't have permission to view this document."
                  : (fileErrorObj as { status?: number })?.status === 404
                    ? 'Document not found.'
                    : 'Unable to load the document. Please check your connection and try again.'}
              </p>
              <Button variant="outline" size="sm" leftIcon={<Download className="h-4 w-4" />} onClick={handleDownload}>
                Download Instead
              </Button>
            </div>
          )}

          {!fileLoading && !fileError && objectUrl && pdf && (
            <iframe
              title={document.title}
              src={objectUrl}
              className="w-full h-[70vh] border-0"
            />
          )}

          {!fileLoading && !fileError && objectUrl && image && (
            <div className="flex items-center justify-center py-4">
              <img
                src={objectUrl}
                alt={document.title}
                className="max-w-full max-h-[70vh] object-contain rounded"
              />
            </div>
          )}

          {!fileLoading && !fileError && objectUrl && text && (
            <iframe
              title={document.title}
              src={objectUrl}
              className="w-full h-[70vh] border-0 bg-white"
            />
          )}

          {!fileLoading && !fileError && !canPreview && (
            <div className="flex flex-col items-center justify-center py-20 gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-surface text-text-tertiary">
                <FileText className="h-6 w-6" />
              </div>
              <p className="text-body font-medium text-text-primary">Preview not available</p>
              <p className="text-caption text-text-tertiary max-w-sm text-center">
                This file type cannot be previewed in the browser. Use the download button to open it.
              </p>
              <div className="flex items-center gap-2 text-caption text-text-tertiary">
                <Eye className="h-3.5 w-3.5" />
                <span>{document.fileName}</span>
              </div>
              <Button variant="outline" size="sm" leftIcon={<Download className="h-4 w-4" />} onClick={handleDownload}>
                Download Document
              </Button>
            </div>
          )}
        </div>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Actions</CardTitle>
        </CardHeader>
        <CardBody>
          <div className="flex flex-wrap gap-2">
            {isActive && (
              <Button variant="outline" size="sm" leftIcon={<Download className="h-4 w-4" />} onClick={handleDownload}>
                Download
              </Button>
            )}
            {isActive && (
              <Button variant="outline" size="sm" leftIcon={<Edit2 className="h-4 w-4" />} onClick={() => navigate(`../documents/${docId}/edit`)}>
                Edit
              </Button>
            )}
            {isActive && (
              <Button variant="outline" size="sm" leftIcon={<Archive className="h-4 w-4" />} onClick={handleArchive} disabled={isMutating}>
                Archive
              </Button>
            )}
            {isArchived && (
              <Button variant="outline" size="sm" leftIcon={<RotateCcw className="h-4 w-4" />} onClick={handleRestore} disabled={isMutating}>
                Restore
              </Button>
            )}
            {(isActive || isArchived) && (
              <Button variant="danger" size="sm" leftIcon={<Trash2 className="h-4 w-4" />} onClick={handleDelete} disabled={isMutating}>
                Delete
              </Button>
            )}
            {canSubmitForApproval && (
              <Button variant="primary" size="sm" leftIcon={<Send className="h-4 w-4" />} onClick={handleSubmitApproval} disabled={isMutating}>
                Submit for Approval
              </Button>
            )}
            {canApproveOrReject && (
              <>
                <Button variant="success" size="sm" leftIcon={<CheckCircle className="h-4 w-4" />} onClick={handleApprove} disabled={isMutating}>
                  Approve
                </Button>
                <Button variant="danger" size="sm" leftIcon={<XCircle className="h-4 w-4" />} onClick={handleReject} disabled={isMutating}>
                  Reject
                </Button>
              </>
            )}
          </div>
        </CardBody>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Details</CardTitle>
        </CardHeader>
        <CardBody>
          <div className="grid grid-cols-2 gap-x-8 gap-y-4 sm:grid-cols-3">
            <MetaItem
              label="Status"
              value={
                <Badge tone={statusTone[document.status]} variant="soft">
                  {document.status}
                </Badge>
              }
            />
            <MetaItem label="Version" value={`v${document.version}`} />
            <MetaItem label="Size" value={formatFileSize(document.fileSize)} />
            <MetaItem label="Type" value={document.mimeType} />
            {document.category && (
              <MetaItem label="Category" value={document.category} />
            )}
            {document.tags && (
              <MetaItem label="Tags" value={document.tags} />
            )}
            <MetaItem label="Created" value={formatDate(document.createdAt)} />
            <MetaItem label="Modified" value={formatDate(document.updatedAt)} />
            <MetaItem label="Views" value={String(document.viewCount)} />
            {approvalStatus && (
              <MetaItem
                label="Approval"
                value={
                  <Badge tone={approvalTone[approvalStatus] ?? 'neutral'} variant="soft">
                    {approvalStatus.charAt(0).toUpperCase() + approvalStatus.slice(1)}
                  </Badge>
                }
              />
            )}
            <MetaItem label="Created by" value={document.createdBy} />
            <MetaItem label="Updated by" value={document.updatedBy} />
          </div>
        </CardBody>
      </Card>

      {document.description && (
        <Card>
          <CardHeader>
            <CardTitle>Description</CardTitle>
          </CardHeader>
          <CardBody>
            <p className="text-body text-text-primary whitespace-pre-wrap">{document.description}</p>
          </CardBody>
        </Card>
      )}
    </div>
  );
}