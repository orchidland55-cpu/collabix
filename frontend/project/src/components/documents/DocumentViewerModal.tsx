import { useEffect, useState } from 'react';
import { Loader2, FileWarning, Download, FileText, Eye } from 'lucide-react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { api } from '../../lib/api';

interface DocumentViewerModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  fileName: string;
  mimeType?: string;
  url: string;
}

type LoadState = 'loading' | 'ready' | 'error';

function isImageMime(mimeType?: string): boolean {
  if (!mimeType) return false;
  return /^image\//.test(mimeType);
}

function isPdfMime(mimeType?: string, fileName?: string): boolean {
  if (mimeType) return mimeType.toLowerCase() === 'application/pdf';
  if (!fileName) return false;
  return fileName.toLowerCase().endsWith('.pdf');
}

export function DocumentViewerModal({ open, onClose, title, fileName, mimeType, url }: DocumentViewerModalProps) {
  const [state, setState] = useState<LoadState>('loading');
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [effectiveType, setEffectiveType] = useState<string>(mimeType ?? '');

  useEffect(() => {
    if (!open) return;
    let revokeUrl: string | null = null;
    let cancelled = false;

    setState('loading');
    setObjectUrl(null);
    setEffectiveType(mimeType ?? '');

    api
      .get<Blob>(url, { responseType: 'blob' })
      .then((response) => {
        if (cancelled) return;
        const blob = response.data as Blob;
        if (!blob || blob.size === 0) {
          setState('error');
          return;
        }
        if (blob.type) setEffectiveType(blob.type);
        const objectUrl = URL.createObjectURL(blob);
        revokeUrl = objectUrl;
        setObjectUrl(objectUrl);
        setState('ready');
      })
      .catch(() => {
        if (!cancelled) setState('error');
      });

    return () => {
      cancelled = true;
      if (revokeUrl) URL.revokeObjectURL(revokeUrl);
    };
  }, [open, url, mimeType]);

  const pdf = state === 'ready' && objectUrl && isPdfMime(effectiveType, fileName);
  const image = state === 'ready' && objectUrl && isImageMime(effectiveType);

  const handleDownload = () => {
    if (!objectUrl) return;
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = fileName || 'document';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      description={fileName}
      size="xl"
      className="max-w-4xl"
      footer={
        state === 'ready' ? (
          <Button variant="outline" leftIcon={<Download className="h-4 w-4" />} onClick={handleDownload}>
            Download
          </Button>
        ) : undefined
      }
    >
      <div className="min-h-[400px]">
        {state === 'loading' && (
          <div className="flex flex-col items-center justify-center py-20 gap-3">
            <Loader2 className="h-8 w-8 animate-spin text-text-tertiary" />
            <p className="text-caption text-text-tertiary">Loading document...</p>
          </div>
        )}

        {state === 'error' && (
          <div className="flex flex-col items-center justify-center py-20 gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-danger-100 text-danger-600 dark:bg-danger-900 dark:text-danger-300">
              <FileWarning className="h-6 w-6" />
            </div>
            <p className="text-body font-medium text-text-primary">Could not load this document</p>
            <p className="text-caption text-text-tertiary max-w-sm text-center">
              The document could not be retrieved. It may have been deleted or you may not have access to it.
            </p>
          </div>
        )}

        {state === 'ready' && pdf && objectUrl && (
          <iframe
            title={title}
            src={objectUrl}
            className="w-full h-[70vh] rounded-lg border border-border-subtle bg-surface"
          />
        )}

        {state === 'ready' && image && objectUrl && (
          <div className="flex items-center justify-center py-6 bg-surface rounded-lg border border-border-subtle">
            <img src={objectUrl} alt={title} className="max-w-full max-h-[70vh] object-contain" />
          </div>
        )}

        {state === 'ready' && !pdf && !image && (
          <div className="flex flex-col items-center justify-center py-20 gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-surface-2 text-text-tertiary">
              <FileText className="h-6 w-6" />
            </div>
            <p className="text-body font-medium text-text-primary">Preview not available</p>
            <p className="text-caption text-text-tertiary max-w-sm text-center">
              This file type cannot be previewed in the browser. Use the download button to open it.
            </p>
            <div className="flex items-center gap-2 text-caption text-text-tertiary">
              <Eye className="h-3.5 w-3.5" />
              <span>{fileName}</span>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
}
