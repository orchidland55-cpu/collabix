import { useState, useEffect, useRef } from 'react';
import { FileText, Plus, X, Loader2, Check, Download, ShieldCheck, CalendarClock, Upload } from 'lucide-react';
import { Card, CardBody } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Textarea } from '../../../components/ui/Textarea';
import { Select } from '../../../components/ui/Select';
import { Badge } from '../../../components/ui/Badge';
import { IconButton } from '../../../components/ui/IconButton';
import { EmptyState } from '../../../components/ui/EmptyState';
import { Modal } from '../../../components/ui/Modal';
import { useToast } from '../../../components/ui/Toast';
import { Can } from '../../auth';
import { useEmployeesList } from '../../../services/employee-hooks';
import { useEmployeeDocuments, useEmployeeDocumentStats, useExpiringDocuments, useUploadEmployeeDocument, useVerifyEmployeeDocument, useUnverifyEmployeeDocument, useDeleteEmployeeDocument } from '../../../services/employee-document-hooks';
import type { EmployeeDocumentType } from '../../../services/employee-document-service';
import { employeeDocumentService } from '../../../services/employee-document-service';
import { DocumentViewerModal } from '../../../components/documents/DocumentViewerModal';
import { downloadAuthenticatedFile } from '../../../lib/file-download';
import { getUploadErrorMessage, logUploadError } from '../../../lib/upload-error';
import { EMPLOYEE_DOCUMENT_TYPES, employeeDocumentTypeLabel } from './hr-constants';

export function DocumentsTab({ wsId, deptId }: { wsId: string; deptId: string }) {
  const [selectedEmp, setSelectedEmp] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [type, setType] = useState<EmployeeDocumentType>('RESUME');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [expirationDate, setExpirationDate] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [viewer, setViewer] = useState<{ doc: { id: string; originalFileName: string; mimeType: string; title?: string } } | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { toast } = useToast();
  const { data: empData } = useEmployeesList(wsId, deptId, 0, 100);
  const { data: docsData, isLoading } = useEmployeeDocuments(wsId, deptId, selectedEmp ?? undefined);
  const { data: stats } = useEmployeeDocumentStats(wsId, deptId, selectedEmp ?? undefined);
  const { data: expiringData } = useExpiringDocuments(wsId, deptId);
  const uploadDoc = useUploadEmployeeDocument(wsId, deptId, selectedEmp ?? '');
  const verifyDoc = useVerifyEmployeeDocument(wsId, deptId, selectedEmp ?? '');
  const unverifyDoc = useUnverifyEmployeeDocument(wsId, deptId, selectedEmp ?? '');
  const deleteDoc = useDeleteEmployeeDocument(wsId, deptId, selectedEmp ?? '');

  const employees = empData?.content ?? [];
  const docs = docsData?.content ?? [];
  const expiringDocs = expiringData ?? [];
  const empName = (id?: string) => {
    if (!id) return 'Employee';
    const e = employees.find((x) => x.id === id);
    return e ? `${e.firstName} ${e.lastName}` : 'Employee';
  };

  useEffect(() => {
    if (!showForm) return;
    setType('RESUME');
    setTitle('');
    setDescription('');
    setExpirationDate('');
    setFile(null);
    setUploadError(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }, [showForm]);

  const resetUploadForm = () => {
    setShowForm(false);
    setType('RESUME');
    setTitle('');
    setDescription('');
    setExpirationDate('');
    setFile(null);
    setUploadError(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleUpload = () => {
    if (!selectedEmp || !file) {
      setUploadError('Please select a file to upload.');
      return;
    }
    setUploadError(null);
    uploadDoc.mutate(
      {
        file,
        documentType: type,
        title: title.trim() || undefined,
        description: description.trim() || undefined,
        expirationDate: expirationDate.trim() || undefined,
      },
      {
        onSuccess: () => {
          toast({ title: 'Document uploaded', tone: 'success' });
          resetUploadForm();
        },
        onError: (error) => {
          logUploadError('employee-document', error);
          setUploadError(getUploadErrorMessage(error));
        },
      },
    );
  };

  const handleDownload = (docId: string, fileName: string) => {
    if (!selectedEmp) return;
    downloadAuthenticatedFile(employeeDocumentService.downloadUrl(wsId, deptId, selectedEmp, docId), fileName)
      .catch(() => toast({ title: 'Failed to download document', tone: 'danger' }));
  };

  const viewerUrl = (docId: string) =>
    selectedEmp ? employeeDocumentService.downloadUrl(wsId, deptId, selectedEmp, docId) : '';

  return (
    <div className="flex flex-col gap-4">
      {expiringDocs.length > 0 && (
        <Card>
          <CardBody className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
              <CalendarClock className="h-4 w-4 text-warning-600" />
              <span className="text-body font-semibold text-text-primary">Documents expiring soon</span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
              {expiringDocs.map((d) => (
                <div key={d.id} className="flex items-start justify-between gap-2 p-3 rounded-lg border border-border-subtle">
                  <div className="min-w-0">
                    <p className="text-caption font-medium text-text-primary">{d.title || d.originalFileName}</p>
                    <p className="text-2xs text-text-tertiary">{empName(d.employeeId)} • {employeeDocumentTypeLabel[d.documentType] ?? d.documentType}</p>
                  </div>
                  <Badge tone="warning" variant="soft">{d.expirationDate ?? '-'}</Badge>
                </div>
              ))}
            </div>
          </CardBody>
        </Card>
      )}

      <div className="flex items-center gap-2">
        <select value={selectedEmp ?? ''} onChange={(e) => { setSelectedEmp(e.target.value || null); setShowForm(false); }}
          className="cx-input h-10 px-3 max-w-xs">
          <option value="">Select an employee...</option>
          {employees.map((e) => (
            <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>
          ))}
        </select>
        {selectedEmp && (
          <Can permission="EMPLOYEE_DOCUMENT_UPLOAD">
            <Button leftIcon={<Plus />} size="sm" onClick={() => setShowForm(true)}>Upload Document</Button>
          </Can>
        )}
      </div>

      {!selectedEmp && (
        <EmptyState icon={<FileText />} title="Select an employee" description="Choose an employee to view and manage their documents." />
      )}

      {selectedEmp && stats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
            <span className="text-2xs text-text-tertiary">Total Documents</span>
            <span className="text-section font-bold text-text-primary">{stats.totalDocuments}</span>
          </div>
          <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
            <span className="text-2xs text-text-tertiary">Verified</span>
            <span className="text-section font-bold text-success-600">{stats.verifiedCount}</span>
          </div>
          <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
            <span className="text-2xs text-text-tertiary">Unverified</span>
            <span className="text-section font-bold text-warning-600">{stats.unverifiedCount}</span>
          </div>
          <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
            <span className="text-2xs text-text-tertiary">Expiring</span>
            <span className="text-section font-bold text-danger-600">{stats.expiringCount}</span>
          </div>
        </div>
      )}

      <Modal
        open={showForm && !!selectedEmp}
        onClose={resetUploadForm}
        title="Upload Document"
        description={selectedEmp ? `Upload a document for ${empName(selectedEmp)}.` : undefined}
        size="md"
        footer={
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={resetUploadForm}>Cancel</Button>
            <Button
              onClick={handleUpload}
              disabled={!file || uploadDoc.isPending}
              leftIcon={uploadDoc.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Upload className="h-4 w-4" />}
            >
              {uploadDoc.isPending ? 'Uploading...' : 'Upload'}
            </Button>
          </div>
        }
      >
        <div className="flex flex-col gap-4">
          <div>
            <label className="mb-1.5 block text-caption font-medium text-text-secondary">File *</label>
            <input
              ref={fileInputRef}
              type="file"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.gif,.txt,.rtf"
              onChange={(e) => {
                setFile(e.target.files?.[0] ?? null);
                setUploadError(null);
              }}
              className="block w-full text-caption text-text-secondary file:mr-3 file:rounded-lg file:border-0 file:bg-accent-50 file:px-3 file:py-1.5 file:text-caption file:font-medium file:text-accent-700 dark:file:bg-accent-100/10 dark:file:text-accent-400"
            />
            {file && (
              <div className="mt-2 flex items-center gap-2 rounded-lg border border-border-subtle bg-surface-2 px-3 py-2">
                <FileText className="h-4 w-4 shrink-0 text-text-tertiary" />
                <div className="min-w-0 flex-1 truncate text-caption text-text-primary">{file.name}</div>
                <div className="shrink-0 text-2xs text-text-tertiary">{formatBytes(file.size)}</div>
              </div>
            )}
          </div>
          <Input label="Title" placeholder="Optional document title" value={title} onChange={(e) => setTitle(e.target.value)} />
          <Textarea label="Description" placeholder="Optional description" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Select
              label="Category"
              value={type}
              onChange={(e) => setType(e.target.value as EmployeeDocumentType)}
              options={EMPLOYEE_DOCUMENT_TYPES.map((t) => ({ value: t, label: employeeDocumentTypeLabel[t] ?? t }))}
            />
            <Input
              label="Expiration date"
              placeholder="YYYY-MM-DD (optional)"
              value={expirationDate}
              onChange={(e) => setExpirationDate(e.target.value)}
            />
          </div>
          {uploadError && <p className="text-caption text-danger-600">{uploadError}</p>}
        </div>
      </Modal>

      {selectedEmp && !showForm && (
        isLoading ? (
          <div className="flex items-center justify-center py-16"><Loader2 className="h-8 w-8 animate-spin text-text-tertiary" /></div>
        ) : docs.length === 0 ? (
          <EmptyState icon={<FileText />} title="No documents" description="Upload documents to manage employee records." />
        ) : (
          <div className="space-y-2">
            {docs.map((d) => (
              <div key={d.id}
                className="flex items-center gap-4 p-4 rounded-lg border border-border-subtle bg-surface hover:bg-surface-2 transition-colors cursor-pointer"
                onClick={() => setViewer({ doc: d })}>
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-surface-2 text-text-tertiary">
                  <FileText className="h-5 w-5" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-body font-medium text-text-primary">{d.title || d.originalFileName}</p>
                  <p className="text-caption text-text-tertiary">
                    {employeeDocumentTypeLabel[d.documentType] ?? d.documentType} • v{d.fileVersion} • {formatBytes(d.fileSize)}
                    {d.expirationDate && ` • Exp: ${d.expirationDate}`}
                  </p>
                  {d.description && <p className="text-2xs text-text-tertiary truncate mt-0.5">{d.description}</p>}
                </div>
                {d.verified ? (
                  <Badge tone="success" variant="soft"><ShieldCheck className="h-3.5 w-3.5 mr-1" /> Verified</Badge>
                ) : (
                  <Badge tone="warning" variant="soft">Unverified</Badge>
                )}
                <div className="flex items-center gap-1">
                  <IconButton label="Download" variant="ghost" size="sm"
                    onClick={(e) => { e.stopPropagation(); handleDownload(d.id, d.originalFileName); }}>
                    <Download className="h-4 w-4" />
                  </IconButton>
                  <Can permission="EMPLOYEE_DOCUMENT_VERIFY">
                    {d.verified ? (
                      <IconButton label="Unverify" variant="ghost" size="sm" onClick={(e) => { e.stopPropagation(); unverifyDoc.mutate(d.id); }}>
                        <Check className="h-4 w-4" />
                      </IconButton>
                    ) : (
                      <IconButton label="Verify" variant="ghost" size="sm" className="text-success-600" onClick={(e) => { e.stopPropagation(); verifyDoc.mutate(d.id); }}>
                        <ShieldCheck className="h-4 w-4" />
                      </IconButton>
                    )}
                  </Can>
                  <Can permission="EMPLOYEE_DOCUMENT_DELETE">
                    <IconButton label="Delete" variant="ghost" size="sm" className="text-danger-600"
                      onClick={(e) => { e.stopPropagation(); deleteDoc.mutate(d.id, { onSuccess: () => toast({ title: 'Document deleted', tone: 'success' }) }); }}>
                      <X className="h-4 w-4" />
                    </IconButton>
                  </Can>
                </div>
              </div>
            ))}
          </div>
        )
      )}

      {viewer && selectedEmp && (
        <DocumentViewerModal
          open
          onClose={() => setViewer(null)}
          title={viewer.doc.title || viewer.doc.originalFileName}
          fileName={viewer.doc.originalFileName}
          mimeType={viewer.doc.mimeType}
          url={viewerUrl(viewer.doc.id)}
        />
      )}
    </div>
  );
}

function formatBytes(bytes: number): string {
  if (!bytes) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`;
}
