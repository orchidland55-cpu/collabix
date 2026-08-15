import { useState, useEffect, useRef, useMemo } from 'react';
import { Upload, FileText, Archive, Trash2, RotateCcw, AlertTriangle } from 'lucide-react';
import { Modal } from '../../../components/ui/Modal';
import { Input } from '../../../components/ui/Input';
import { Textarea } from '../../../components/ui/Textarea';
import { Select } from '../../../components/ui/Select';
import { Button } from '../../../components/ui/Button';
import {
  useUploadDocument,
  useUpdateDocument,
  useDeleteDocument,
  useArchiveDocument,
  useRestoreDocument,
} from '../../../services/document-hooks';
import { useWorkspaceProjects } from '../../../services/project-hooks';
import type { DocumentResponse } from '../types/document-types';
import { formatFileSize } from '../types/document-types';
import { getUploadErrorMessage, logUploadError } from '../../../lib/upload-error';

/* ------------------------------------------------------------------ */
/*  UploadDocumentModal                                                */
/* ------------------------------------------------------------------ */

export interface UploadDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  wsId: string;
  deptId?: string;
  projId?: string;
}

export function UploadDocumentModal({ isOpen, onClose, wsId, deptId = '', projId = '' }: UploadDocumentModalProps) {
  const needsProjectSelection = !deptId || !projId;
  const uploadMutation = useUploadDocument(wsId, deptId || undefined, projId || undefined);
  const { data: projectsData, isLoading: projectsLoading } = useWorkspaceProjects(needsProjectSelection ? wsId : undefined);
  const projects = projectsData?.content ?? [];
  const [selectedProjectId, setSelectedProjectId] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('');
  const [tags, setTags] = useState('');
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const selectedProject = useMemo(
    () => projects.find((p) => p.id === selectedProjectId),
    [projects, selectedProjectId],
  );

  const effectiveDeptId = needsProjectSelection ? selectedProject?.departmentId : deptId;
  const effectiveProjId = needsProjectSelection ? selectedProject?.id : projId;

  useEffect(() => {
    if (isOpen) {
      setSelectedProjectId(projId || '');
      setFile(null);
      setTitle('');
      setDescription('');
      setCategory('');
      setTags('');
      setError(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  }, [isOpen, projId]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0] ?? null;
    setFile(selected);
    setError(null);
  };

  const handleSubmit = async () => {
    if (!file) {
      setError('Please select a file to upload.');
      return;
    }
    if (!effectiveDeptId || !effectiveProjId) {
      setError('Please select a project to upload this document to.');
      return;
    }
    setError(null);
    try {
      await uploadMutation.mutateAsync({
        file,
        title: title.trim() || undefined,
        description: description.trim() || undefined,
        category: category.trim() || undefined,
        tags: tags.trim() || undefined,
        departmentId: effectiveDeptId,
        projectId: effectiveProjId,
      });
      onClose();
    } catch (err) {
      logUploadError('project-document', err);
      setError(getUploadErrorMessage(err));
    }
  };

  const canSubmit = !!file && !!effectiveDeptId && !!effectiveProjId && !uploadMutation.isPending;

  return (
    <Modal
      open={isOpen}
      onClose={onClose}
      title="Upload Document"
      description="Select a file and provide optional metadata."
      size="md"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button onClick={handleSubmit} disabled={!canSubmit} leftIcon={<Upload className="h-4 w-4" />}>
            {uploadMutation.isPending ? 'Uploading...' : 'Upload'}
          </Button>
        </div>
      }
    >
      <div className="flex flex-col gap-4">
        {needsProjectSelection && (
          <Select
            label="Project *"
            value={selectedProjectId}
            onChange={(e) => {
              setSelectedProjectId(e.target.value);
              setError(null);
            }}
            options={[
              { value: '', label: projectsLoading ? 'Loading projects...' : 'Select a project...' },
              ...projects.map((p) => ({
                value: p.id,
                label: p.departmentName ? `${p.name} (${p.departmentName})` : p.name,
              })),
            ]}
          />
        )}
        <div>
          <label className="mb-1.5 block text-caption font-medium text-text-secondary">File *</label>
          <input
            ref={fileInputRef}
            type="file"
            onChange={handleFileChange}
            className="block w-full text-caption text-text-secondary file:mr-3 file:rounded-lg file:border-0 file:bg-accent-50 file:px-3 file:py-1.5 file:text-caption file:font-medium file:text-accent-700 dark:file:bg-accent-100/10 dark:file:text-accent-400"
          />
          {file && (
            <div className="mt-2 flex items-center gap-2 rounded-lg border border-border-subtle bg-surface-2 px-3 py-2">
              <FileText className="h-4 w-4 shrink-0 text-text-tertiary" />
              <div className="min-w-0 flex-1 truncate text-caption text-text-primary">{file.name}</div>
              <div className="shrink-0 text-2xs text-text-tertiary">{formatFileSize(file.size)}</div>
            </div>
          )}
        </div>
        <Input label="Title" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Optional document title" />
        <Textarea label="Description" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Optional description" rows={3} />
        <div className="grid grid-cols-2 gap-4">
          <Input label="Category" value={category} onChange={(e) => setCategory(e.target.value)} placeholder="e.g. CV" />
          <Input label="Tags" value={tags} onChange={(e) => setTags(e.target.value)} placeholder="Comma-separated" />
        </div>
        {needsProjectSelection && !projectsLoading && projects.length === 0 && (
          <p className="text-caption text-warning-600">No projects are available. Create a project before uploading documents.</p>
        )}
        {error && <p className="text-caption text-danger-600">{error}</p>}
      </div>
    </Modal>
  );
}

/* ------------------------------------------------------------------ */
/*  EditDocumentModal                                                  */
/* ------------------------------------------------------------------ */

export interface EditDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  wsId: string;
  deptId: string;
  projId: string;
  document: DocumentResponse;
}

export function EditDocumentModal({ isOpen, onClose, wsId, deptId, projId, document }: EditDocumentModalProps) {
  const updateMutation = useUpdateDocument(wsId, deptId, projId, document.id);
  const [title, setTitle] = useState(document.title);
  const [description, setDescription] = useState(document.description ?? '');
  const [category, setCategory] = useState(document.category ?? '');
  const [tags, setTags] = useState(document.tags ?? '');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setTitle(document.title);
      setDescription(document.description ?? '');
      setCategory(document.category ?? '');
      setTags(document.tags ?? '');
      setError(null);
    }
  }, [isOpen, document]);

  const handleSubmit = async () => {
    if (!title.trim()) {
      setError('Title is required.');
      return;
    }
    setError(null);
    try {
      await updateMutation.mutateAsync({
        title: title.trim() !== document.title ? title.trim() : undefined,
        description: description.trim() !== (document.description ?? '') ? description.trim() || undefined : undefined,
        category: category.trim() !== (document.category ?? '') ? category.trim() || undefined : undefined,
        tags: tags.trim() !== (document.tags ?? '') ? tags.trim() || undefined : undefined,
      });
      onClose();
    } catch {
      setError('Failed to update document. Please try again.');
    }
  };

  return (
    <Modal
      open={isOpen}
      onClose={onClose}
      title="Edit Document"
      description={`Update details for "${document.fileName}".`}
      size="md"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button onClick={handleSubmit} disabled={!title.trim() || updateMutation.isPending}>
            {updateMutation.isPending ? 'Saving...' : 'Save'}
          </Button>
        </div>
      }
    >
      <div className="flex flex-col gap-4">
        <Input label="Title *" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Document title" />
        <Textarea label="Description" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Optional description" rows={3} />
        <div className="grid grid-cols-2 gap-4">
          <Input label="Category" value={category} onChange={(e) => setCategory(e.target.value)} placeholder="e.g. Reports" />
          <Input label="Tags" value={tags} onChange={(e) => setTags(e.target.value)} placeholder="Comma-separated" />
        </div>
        {error && <p className="text-caption text-danger-600">{error}</p>}
      </div>
    </Modal>
  );
}

/* ------------------------------------------------------------------ */
/*  DeleteDocumentModal                                                */
/* ------------------------------------------------------------------ */

export interface DeleteDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  wsId: string;
  deptId: string;
  projId: string;
  document: DocumentResponse;
}

export function DeleteDocumentModal({ isOpen, onClose, wsId, deptId, projId, document }: DeleteDocumentModalProps) {
  const deleteMutation = useDeleteDocument(wsId, deptId, projId);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) setError(null);
  }, [isOpen]);

  const handleDelete = async () => {
    setError(null);
    try {
      await deleteMutation.mutateAsync(document.id);
      onClose();
    } catch {
      setError('Failed to delete document. Please try again.');
    }
  };

  return (
    <Modal
      open={isOpen}
      onClose={onClose}
      size="sm"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button variant="danger" onClick={handleDelete} disabled={deleteMutation.isPending} leftIcon={<Trash2 className="h-4 w-4" />}>
            {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
          </Button>
        </div>
      }
    >
      <div className="flex flex-col items-center gap-3 text-center py-2">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-danger-50 text-danger-600 dark:bg-danger-100 dark:text-danger-500">
          <AlertTriangle className="h-6 w-6" />
        </span>
        <h3 className="text-page font-semibold text-text-primary">Delete "{document.fileName}"?</h3>
        <p className="text-body text-text-secondary max-w-sm">
          This action cannot be undone. The document will be permanently removed from the system.
        </p>
      </div>
      {error && <p className="mt-3 text-center text-caption text-danger-600">{error}</p>}
    </Modal>
  );
}

/* ------------------------------------------------------------------ */
/*  ArchiveDocumentModal                                               */
/* ------------------------------------------------------------------ */

export interface ArchiveDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  wsId: string;
  deptId: string;
  projId: string;
  document: DocumentResponse;
}

export function ArchiveDocumentModal({ isOpen, onClose, wsId, deptId, projId, document }: ArchiveDocumentModalProps) {
  const archiveMutation = useArchiveDocument(wsId, deptId, projId);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) setError(null);
  }, [isOpen]);

  const handleArchive = async () => {
    setError(null);
    try {
      await archiveMutation.mutateAsync(document.id);
      onClose();
    } catch {
      setError('Failed to archive document. Please try again.');
    }
  };

  return (
    <Modal
      open={isOpen}
      onClose={onClose}
      size="sm"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button variant="danger" onClick={handleArchive} disabled={archiveMutation.isPending} leftIcon={<Archive className="h-4 w-4" />}>
            {archiveMutation.isPending ? 'Archiving...' : 'Archive'}
          </Button>
        </div>
      }
    >
      <div className="flex flex-col items-center gap-3 text-center py-2">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-warning-50 text-warning-600 dark:bg-warning-100 dark:text-warning-500">
          <Archive className="h-6 w-6" />
        </span>
        <h3 className="text-page font-semibold text-text-primary">Archive "{document.fileName}"?</h3>
        <p className="text-body text-text-secondary max-w-sm">
          The document will be archived and hidden from active views. You can restore it later.
        </p>
      </div>
      {error && <p className="mt-3 text-center text-caption text-danger-600">{error}</p>}
    </Modal>
  );
}

/* ------------------------------------------------------------------ */
/*  RestoreDocumentModal                                               */
/* ------------------------------------------------------------------ */

export interface RestoreDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  wsId: string;
  deptId: string;
  projId: string;
  document: DocumentResponse;
}

export function RestoreDocumentModal({ isOpen, onClose, wsId, deptId, projId, document }: RestoreDocumentModalProps) {
  const restoreMutation = useRestoreDocument(wsId, deptId, projId);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) setError(null);
  }, [isOpen]);

  const handleRestore = async () => {
    setError(null);
    try {
      await restoreMutation.mutateAsync(document.id);
      onClose();
    } catch {
      setError('Failed to restore document. Please try again.');
    }
  };

  return (
    <Modal
      open={isOpen}
      onClose={onClose}
      size="sm"
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button variant="primary" onClick={handleRestore} disabled={restoreMutation.isPending} leftIcon={<RotateCcw className="h-4 w-4" />}>
            {restoreMutation.isPending ? 'Restoring...' : 'Restore'}
          </Button>
        </div>
      }
    >
      <div className="flex flex-col items-center gap-3 text-center py-2">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-success-50 text-success-600 dark:bg-success-100 dark:text-success-500">
          <RotateCcw className="h-6 w-6" />
        </span>
        <h3 className="text-page font-semibold text-text-primary">Restore "{document.fileName}"?</h3>
        <p className="text-body text-text-secondary max-w-sm">
          This document will be restored and will appear in active document lists again.
        </p>
      </div>
      {error && <p className="mt-3 text-center text-caption text-danger-600">{error}</p>}
    </Modal>
  );
}
