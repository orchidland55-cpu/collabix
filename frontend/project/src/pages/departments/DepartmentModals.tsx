import { useState } from 'react';
import { AlertTriangle, Archive, RotateCcw, Trash2 } from 'lucide-react';
import { Modal } from '../../components/ui/Modal';
import { Button } from '../../components/ui/Button';
import { useWorkspaceId } from '../../hooks/useWorkspaceId';
import { useToast } from '../../components/ui/Toast';
import {
  useArchiveDepartment,
  useRestoreDepartment,
  useDeleteDepartmentPermanently,
} from '../../services/department-hooks';

export interface DepartmentRef {
  id: string;
  name: string;
  status: string;
}

export type DepartmentModalState =
  | { kind: 'archive'; dept: DepartmentRef }
  | { kind: 'restore'; dept: DepartmentRef }
  | { kind: 'delete'; dept: DepartmentRef }
  | null;

export function DepartmentModal({
  state,
  onClose,
  onSuccess,
}: {
  state: DepartmentModalState;
  onClose: () => void;
  onSuccess?: () => void;
}) {
  if (!state) return null;
  switch (state.kind) {
    case 'archive':
      return <ArchiveDepartmentModal key={state.dept.id} dept={state.dept} onClose={onClose} onSuccess={onSuccess} />;
    case 'restore':
      return <RestoreDepartmentModal key={state.dept.id} dept={state.dept} onClose={onClose} onSuccess={onSuccess} />;
    case 'delete':
      return <DeleteDepartmentPermanentlyModal key={state.dept.id} dept={state.dept} onClose={onClose} onSuccess={onSuccess} />;
  }
}

function ArchiveDepartmentModal({
  dept,
  onClose,
  onSuccess,
}: {
  dept: DepartmentRef;
  onClose: () => void;
  onSuccess?: () => void;
}) {
  const wsId = useWorkspaceId();
  const { toast } = useToast();
  const archive = useArchiveDepartment(wsId);
  const [submitting, setSubmitting] = useState(false);

  function confirm() {
    setSubmitting(true);
    archive.mutate(
      { departmentId: dept.id },
      {
        onSuccess: () => {
          toast({ title: 'Department archived', tone: 'success' });
          onClose();
          onSuccess?.();
        },
        onError: (err) => {
          toast({ title: 'Failed to archive department', description: err instanceof Error ? err.message : 'An error occurred.', tone: 'danger' });
          setSubmitting(false);
        },
      },
    );
  }

  return (
    <Modal
      open
      onClose={onClose}
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button variant="danger" onClick={confirm} leftIcon={<Archive className="h-4 w-4" />} disabled={submitting}>Archive Department</Button>
        </>
      }
    >
      <div className="flex flex-col items-center gap-3 text-center py-2">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-warning-50 text-warning-600 dark:bg-warning-100 dark:text-warning-500">
          <AlertTriangle className="h-6 w-6" />
        </span>
        <h3 className="text-page font-semibold text-text-primary">Archive "{dept.name}"?</h3>
        <p className="text-body text-text-secondary max-w-sm">
          The department will be moved to archived status. It will no longer appear in active lists and can be restored later.
        </p>
      </div>
    </Modal>
  );
}

function RestoreDepartmentModal({
  dept,
  onClose,
  onSuccess,
}: {
  dept: DepartmentRef;
  onClose: () => void;
  onSuccess?: () => void;
}) {
  const wsId = useWorkspaceId();
  const { toast } = useToast();
  const restore = useRestoreDepartment(wsId);
  const [submitting, setSubmitting] = useState(false);

  function confirm() {
    setSubmitting(true);
    restore.mutate(
      { departmentId: dept.id },
      {
        onSuccess: () => {
          toast({ title: 'Department restored', tone: 'success' });
          onClose();
          onSuccess?.();
        },
        onError: (err) => {
          toast({ title: 'Failed to restore department', description: err instanceof Error ? err.message : 'An error occurred.', tone: 'danger' });
          setSubmitting(false);
        },
      },
    );
  }

  return (
    <Modal
      open
      onClose={onClose}
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button onClick={confirm} leftIcon={<RotateCcw className="h-4 w-4" />} disabled={submitting}>Restore Department</Button>
        </>
      }
    >
      <div className="flex flex-col items-center gap-3 text-center py-2">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-accent-50 text-accent-600 dark:bg-accent-100 dark:text-accent-300">
          <RotateCcw className="h-6 w-6" />
        </span>
        <h3 className="text-page font-semibold text-text-primary">Restore "{dept.name}"?</h3>
        <p className="text-body text-text-secondary max-w-sm">
          The department will be restored to active status and will appear in active lists again.
        </p>
      </div>
    </Modal>
  );
}

function DeleteDepartmentPermanentlyModal({
  dept,
  onClose,
  onSuccess,
}: {
  dept: DepartmentRef;
  onClose: () => void;
  onSuccess?: () => void;
}) {
  const wsId = useWorkspaceId();
  const { toast } = useToast();
  const deletePermanently = useDeleteDepartmentPermanently(wsId);
  const [submitting, setSubmitting] = useState(false);

  function confirm() {
    setSubmitting(true);
    deletePermanently.mutate(
      { departmentId: dept.id },
      {
        onSuccess: () => {
          toast({ title: 'Department permanently deleted', tone: 'success' });
          onClose();
          onSuccess?.();
        },
        onError: (err) => {
          toast({ title: 'Failed to delete department', description: err instanceof Error ? err.message : 'An error occurred.', tone: 'danger' });
          setSubmitting(false);
        },
      },
    );
  }

  return (
    <Modal
      open
      onClose={onClose}
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button variant="danger" onClick={confirm} leftIcon={<Trash2 className="h-4 w-4" />} disabled={submitting}>
            Delete Permanently
          </Button>
        </>
      }
    >
      <div className="flex flex-col items-center gap-3 text-center py-2">
        <span className="flex h-12 w-12 items-center justify-center rounded-full bg-danger-50 text-danger-600 dark:bg-danger-100 dark:text-danger-500">
          <AlertTriangle className="h-6 w-6" />
        </span>
        <h3 className="text-page font-semibold text-text-primary">Delete Department Permanently?</h3>
        <p className="text-body text-text-secondary max-w-sm">
          This will permanently remove "{dept.name}" from the database and cannot be undone.
        </p>
      </div>
    </Modal>
  );
}

export default DepartmentModal;