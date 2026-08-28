import { Modal } from '../../../components/ui/Modal';
import { Button } from '../../../components/ui/Button';
import { useToast } from '../../../components/ui/Toast';
import { useNavigate } from 'react-router-dom';
import { useHardDeleteProject } from '../../../services/project-hooks';

interface HardDeleteProjectModalProps {
  open: boolean;
  onClose: () => void;
  wsId: string;
  deptId: string;
  projectId: string;
  projectName: string;
}

export function HardDeleteProjectModal({
  open,
  onClose,
  wsId,
  deptId,
  projectId,
  projectName,
}: HardDeleteProjectModalProps) {
  const { toast } = useToast();
  const navigate = useNavigate();
  const hardDeleteMutation = useHardDeleteProject();

  const handleHardDelete = async () => {
    try {
      await hardDeleteMutation.mutateAsync({ wsId, deptId, projectId });
      toast({
        title: 'Success',
        description: `Project "${projectName}" permanently deleted.`,
        tone: 'success',
      });
      onClose();
      navigate('/app/projects');
    } catch {
      toast({
        title: 'Error',
        description: 'Failed to permanently delete project.',
        tone: 'danger',
      });
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Permanently Delete Project"
      description={`This will permanently delete "${projectName}" and all of its tasks, documents, and related data.`}
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="danger" onClick={handleHardDelete} disabled={hardDeleteMutation.isPending}>
            {hardDeleteMutation.isPending ? 'Deleting...' : 'Delete Permanently'}
          </Button>
        </div>
      }
    >
      <p className="text-body text-text-secondary">
        This action cannot be undone. The project and all of its associated data will be removed
        permanently and cannot be restored.
      </p>
    </Modal>
  );
}
