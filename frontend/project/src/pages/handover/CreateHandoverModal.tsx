import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Send } from 'lucide-react';
import { Modal } from '../../components/ui/Modal';
import { Input } from '../../components/ui/Input';
import { Textarea } from '../../components/ui/Textarea';
import { Select } from '../../components/ui/Select';
import { Button } from '../../components/ui/Button';
import { useToast } from '../../components/ui/Toast';
import { useDepartmentList } from '../../services/department-hooks';
import { useProjectList } from '../../services/project-hooks';
import { useCreateHandoverEntry, useSendHandover } from '../../services/handover-hooks';
import { userService } from '../../services/user-service';
import { useAuth } from '../../lib/auth-context';
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

export function CreateHandoverModal({
  open,
  onClose,
  workspaceId,
}: {
  open: boolean;
  onClose: () => void;
  workspaceId: string;
}) {
  const { toast } = useToast();
  const { user } = useAuth();
  const { data: departments } = useDepartmentList(workspaceId);
  // Department is auto-detected from the logged-in user; the backend only
  // accepts handovers created in the sender's own department.
  const departmentId = user?.departmentId ?? '';
  const departmentName =
    (departments ?? []).find((d) => d.id === departmentId)?.name ?? (departmentId ? 'My department' : '');
  const { data: projectsPage } = useProjectList(workspaceId, departmentId || undefined);
  const projects = projectsPage?.content ?? [];

  const { data: members } = useQuery({
    queryKey: ['handover', 'members', workspaceId],
    queryFn: () => userService(workspaceId).list(),
    enabled: !!workspaceId && open,
  });

  const createMutation = useCreateHandoverEntry(workspaceId);
  const sendMutation = useSendHandover(workspaceId);

  const [form, setForm] = useState({
    projectId: '',
    receiverId: '',
    title: '',
    content: '',
    priority: 'MEDIUM',
    dueDate: '',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (open) {
      setForm({ projectId: '', receiverId: '', title: '', content: '', priority: 'MEDIUM', dueDate: '' });
      setErrors({});
    }
  }, [open]);

  const setField = (key: keyof typeof form, value: string) => {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((e) => ({ ...e, [key]: '' }));
  };

  const handleSubmit = async () => {
    const next: Record<string, string> = {};
    if (!departmentId) next.departmentId = 'Your department could not be determined. Make sure you are assigned to a department.';
    if (!form.projectId) next.projectId = 'Select a project';
    if (!form.receiverId) next.receiverId = 'Select a receiver';
    if (!form.title.trim()) next.title = 'Title is required';
    if (!form.content.trim()) next.content = 'Content is required';
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    try {
      const created = await createMutation.mutateAsync({
        departmentId,
        projectId: form.projectId,
        receiverId: form.receiverId,
        title: form.title.trim(),
        content: form.content.trim(),
        priority: form.priority as 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT',
        dueDate: form.dueDate || undefined,
      });
      try {
        await sendMutation.mutateAsync({ entryId: created.id });
      } catch {
        toast({
          title: 'Handover saved as draft',
          description: 'The handover was created but could not be sent to the receiver yet.',
          tone: 'warning',
        });
        onClose();
        return;
      }
      toast({ title: 'Handover sent', description: 'The handover has been sent to the receiver.', tone: 'success' });
      onClose();
    } catch (err) {
      toast({
        title: 'Failed to create handover',
        description: (err as { message?: string })?.message ?? 'An unexpected error occurred.',
        tone: 'danger',
      });
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="lg"
      title="New Handover"
      description="Create a handover entry and send it to a colleague."
      footer={
        <>
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button onClick={handleSubmit} loading={createMutation.isPending || sendMutation.isPending} leftIcon={<Send />}>
            Create & Send
          </Button>
        </>
      }
    >
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        {departmentId ? (
          <Input label="Department (auto-detected)" value={departmentName} disabled />
        ) : (
          <div className="flex items-end">
            <p className="text-caption text-danger-600">
              Your department could not be determined. Make sure you are assigned to a department.
            </p>
          </div>
        )}
        <Select
          label="Project"
          value={form.projectId}
          onChange={(e) => setField('projectId', e.target.value)}
          invalid={!!errors.projectId}
          errorText={errors.projectId}
          disabled={!departmentId}
          options={[
            { value: '', label: departmentId ? 'Select project' : 'Select a department first' },
            ...projects.map((p) => ({ value: p.id, label: p.name })),
          ]}
        />
        <Select
          label="Receiver"
          value={form.receiverId}
          onChange={(e) => setField('receiverId', e.target.value)}
          invalid={!!errors.receiverId}
          errorText={errors.receiverId}
          options={[
            { value: '', label: 'Select receiver' },
            ...(members ?? [])
              .filter((m) => m.status === 'ACTIVE')
              .map((m) => ({
                value: m.id,
                label: `${m.firstName} ${m.lastName}`,
              })),
          ]}
        />
        <Select
          label="Priority"
          value={form.priority}
          onChange={(e) => setField('priority', e.target.value)}
          options={PRIORITIES.map((p) => ({ value: p, label: p }))}
        />
      </div>
      <div className="mt-4">
        <Input
          label="Title"
          placeholder="e.g. Weekly handover - production deployment"
          value={form.title}
          onChange={(e) => setField('title', e.target.value)}
          invalid={!!errors.title}
          errorText={errors.title}
        />
      </div>
      <div className="mt-4">
        <Textarea
          label="Content"
          placeholder="Describe the work, tasks, blockers and context being handed over..."
          value={form.content}
          onChange={(e) => setField('content', e.target.value)}
          invalid={!!errors.content}
          errorText={errors.content}
        />
      </div>
      <div className="mt-4">
        <Input
          label="Due date (optional)"
          type="datetime-local"
          value={form.dueDate}
          onChange={(e) => setField('dueDate', e.target.value)}
        />
      </div>
    </Modal>
  );
}
