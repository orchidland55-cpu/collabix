import { useState } from 'react';
import { Search, Plus, FolderKanban, AlertCircle, MoreHorizontal, Edit2, Archive, RefreshCw, FolderOpen } from 'lucide-react';
import { Card, CardBody } from '../../../components/ui/Card';
import { Modal } from '../../../components/ui/Modal';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Textarea } from '../../../components/ui/Textarea';
import { Badge, type Tone } from '../../../components/ui/Badge';
import { IconButton } from '../../../components/ui/IconButton';
import { Dropdown } from '../../../components/ui/Dropdown';
import { EmptyState } from '../../../components/ui/EmptyState';
import { Pagination } from '../../../components/ui/Pagination';
import { Skeleton } from '../../../components/ui/Skeleton';
import { useToast } from '../../../components/ui/Toast';
import { useProjectList, useCreateProject, useUpdateProject, useDeleteProject, useProjectAccess } from '../../../services/project-hooks';
import { useUsersList } from '../../../services/admin-hooks';
import { cn } from '../../../lib/cn';
import type { ProjectResponse, ProjectPriority } from '../../projects/projects-types';

const PAGE_SIZE = 8;

const priorityTone: Record<string, Tone> = {
  CRITICAL: 'danger',
  HIGH: 'warning',
  MEDIUM: 'info',
  LOW: 'neutral',
};

const priorityLabel: Record<string, string> = {
  CRITICAL: 'Critical',
  HIGH: 'High',
  MEDIUM: 'Medium',
  LOW: 'Low',
};

function StatCard({ label, value, tone, icon }: { label: string; value: number; tone: string; icon: React.ReactNode }) {
  const bg: Record<string, string> = {
    accent: 'bg-accent-50 dark:bg-accent-100 text-accent-700 dark:text-accent-200',
    success: 'bg-success-50 dark:bg-success-100 text-success-700 dark:text-success-200',
    warning: 'bg-warning-50 dark:bg-warning-100 text-warning-700 dark:text-warning-200',
    info: 'bg-info-50 dark:bg-info-100 text-info-700 dark:text-info-200',
  };
  return (
    <div className={cn('rounded-lg border border-border-subtle p-3', bg[tone])}>
      <div className="flex items-center justify-between">
        <p className="text-2xs font-medium opacity-75">{label}</p>
        <span className="[&>svg]:h-4 [&>svg]:w-4 opacity-75">{icon}</span>
      </div>
      <p className="text-section font-semibold mt-1">{value}</p>
    </div>
  );
}

interface ProjectFormState {
  name: string;
  description: string;
  priority: ProjectPriority;
  startDate: string;
  endDate: string;
  managerId: string;
}

const emptyForm: ProjectFormState = {
  name: '',
  description: '',
  priority: 'MEDIUM',
  startDate: '',
  endDate: '',
  managerId: '',
};

export function DevelopmentProjectsTab({ wsId, deptId }: { wsId: string; deptId: string }) {
  const { toast } = useToast();
  const [search, setSearch] = useState('');
  const [priorityFilter, setPriorityFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [page, setPage] = useState(1);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<ProjectResponse | null>(null);
  const [form, setForm] = useState<ProjectFormState>(emptyForm);
  const [confirmDelete, setConfirmDelete] = useState<ProjectResponse | null>(null);

  const { data: projectsPage, isLoading, isError, error, refetch } = useProjectList(wsId, deptId, search || undefined, page - 1);
  const { data: users } = useUsersList();
  const createProject = useCreateProject();
  const updateProject = useUpdateProject();
  const deleteProject = useDeleteProject();
  const { canCreate, canUpdate, canArchive } = useProjectAccess(wsId);

  const projects = projectsPage?.content ?? [];
  const total = projectsPage?.page?.totalElements ?? 0;

  const filtered = projects.filter((p) => {
    if (priorityFilter && p.priority !== priorityFilter) return false;
    if (statusFilter && p.status !== statusFilter) return false;
    return true;
  });

  const openProjects = filtered.filter((p) => p.status === 'ACTIVE');
  const totalActive = projectsPage?.page?.totalElements ?? 0;

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setFormOpen(true);
  };

  const openEdit = (p: ProjectResponse) => {
    setEditing(p);
    setForm({
      name: p.name,
      description: p.description ?? '',
      priority: p.priority ?? 'MEDIUM',
      startDate: p.startDate ?? '',
      endDate: p.endDate ?? '',
      managerId: p.managerId ?? '',
    });
    setFormOpen(true);
  };

  const handleSave = async () => {
    if (!form.name.trim()) {
      toast({ title: 'Project name is required', tone: 'warning' });
      return;
    }
    const data = {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      priority: form.priority,
      startDate: form.startDate || undefined,
      endDate: form.endDate || undefined,
      managerId: form.managerId || undefined,
    };
    try {
      if (editing) {
        await updateProject.mutateAsync({ wsId, deptId, projectId: editing.id, data });
        toast({ title: 'Project updated', tone: 'success' });
      } else {
        await createProject.mutateAsync({ wsId, deptId, data });
        toast({ title: 'Project created', tone: 'success' });
      }
      setFormOpen(false);
    } catch (err: unknown) {
      toast({ title: err instanceof Error ? err.message : 'Failed to save project', tone: 'danger' });
    }
  };

  const handleDelete = async () => {
    if (!confirmDelete) return;
    try {
      await deleteProject.mutateAsync({ wsId, deptId, projectId: confirmDelete.id });
      toast({ title: 'Project archived', tone: 'success' });
      setConfirmDelete(null);
    } catch (err: unknown) {
      toast({ title: err instanceof Error ? err.message : 'Failed to delete project', tone: 'danger' });
    }
  };

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {[1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-20 rounded-lg" />)}
        </div>
        <Skeleton className="h-96 rounded-xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <EmptyState
        icon={<AlertCircle className="h-6 w-6" />}
        title="Failed to load projects"
        description={error instanceof Error ? error.message : 'An error occurred.'}
        action={<Button variant="outline" size="sm" leftIcon={<RefreshCw className="h-4 w-4" />} onClick={() => refetch()}>Retry</Button>}
      />
    );
  }

  return (
    <div className="flex flex-col gap-6 animate-fade-in">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-page font-semibold text-text-primary">Development Projects</h1>
          <p className="text-body text-text-secondary">Plan and track engineering projects for this department.</p>
        </div>
        {canCreate && <Button variant="primary" leftIcon={<Plus className="h-4 w-4" />} onClick={openCreate}>New Project</Button>}
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard label="Total Projects" value={totalActive} icon={<FolderKanban />} tone="accent" />
        <StatCard label="Active" value={openProjects.length} icon={<FolderOpen />} tone="success" />
        <StatCard label="High Priority" value={filtered.filter((p) => p.priority === 'HIGH' || p.priority === 'CRITICAL').length} icon={<AlertCircle />} tone="warning" />
        <StatCard label="Archived" value={projectsPage?.page?.totalElements ? projects.filter((p) => p.status === 'ARCHIVED').length : 0} icon={<Archive />} tone="info" />
      </div>

      <div className="flex flex-col gap-2 lg:flex-row lg:gap-2">
        <div className="flex-1">
          <Input
            placeholder="Search projects..."
            leftIcon={<Search />}
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(1); }}
          />
        </div>
        <div className="flex flex-wrap gap-2">
          <Select
            className="w-44"
            value={priorityFilter}
            onChange={(e) => { setPriorityFilter(e.target.value); setPage(1); }}
            options={[
              { value: '', label: 'All Priorities' },
              { value: 'CRITICAL', label: 'Critical' },
              { value: 'HIGH', label: 'High' },
              { value: 'MEDIUM', label: 'Medium' },
              { value: 'LOW', label: 'Low' },
            ]}
          />
          <Select
            className="w-44"
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(1); }}
            options={[
              { value: '', label: 'All Statuses' },
              { value: 'ACTIVE', label: 'Active' },
              { value: 'ARCHIVED', label: 'Archived' },
            ]}
          />
        </div>
      </div>

      {filtered.length === 0 ? (
        <Card>
          <CardBody className="py-16">
            <EmptyState
              icon={<FolderKanban className="h-6 w-6" />}
              title={search || priorityFilter || statusFilter ? 'No projects match your filters' : 'No projects yet'}
              description={search || priorityFilter || statusFilter ? 'Try adjusting your filters.' : 'Create your first development project to get started.'}
              action={!(search || priorityFilter || statusFilter) ? (
                canCreate && <Button variant="primary" size="sm" leftIcon={<Plus className="h-4 w-4" />} onClick={openCreate}>New Project</Button>
              ) : undefined}
            />
          </CardBody>
        </Card>
      ) : (
        <Card>
          <CardBody className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full" role="table" aria-label="Development projects table">
                <thead>
                  <tr className="border-b border-border-subtle">
                    <th className="px-4 py-3 text-left text-caption font-semibold text-text-secondary">Project</th>
                    <th className="px-4 py-3 text-left text-caption font-semibold text-text-secondary">Priority</th>
                    <th className="px-4 py-3 text-left text-caption font-semibold text-text-secondary">Status</th>
                    <th className="px-4 py-3 text-left text-caption font-semibold text-text-secondary">Manager</th>
                    <th className="px-4 py-3 text-left text-caption font-semibold text-text-secondary">Timeline</th>
                    <th className="px-4 py-3 text-right text-caption font-semibold text-text-secondary">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((p) => (
                    <tr key={p.id} className="border-b border-border-subtle hover:bg-surface-2 transition-colors">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <span
                            className="flex h-9 w-9 items-center justify-center rounded-lg"
                            style={{ backgroundColor: p.color ?? 'rgb(var(--accent-50))' }}
                          >
                            <FolderKanban className="h-4 w-4 text-text-primary" />
                          </span>
                          <div>
                            <p className="text-body font-medium text-text-primary">{p.name}</p>
                            {p.description && <p className="text-caption text-text-tertiary truncate max-w-xs">{p.description}</p>}
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        {p.priority ? <Badge tone={priorityTone[p.priority] ?? 'info'} variant="soft">{priorityLabel[p.priority] ?? p.priority}</Badge> : <span className="text-text-tertiary text-caption">—</span>}
                      </td>
                      <td className="px-4 py-3">
                        <Badge tone={p.status === 'ACTIVE' ? 'success' : 'neutral'} variant="soft">{p.status}</Badge>
                      </td>
                      <td className="px-4 py-3">
                        <p className="text-body text-text-secondary">{p.managerName ?? '—'}</p>
                      </td>
                      <td className="px-4 py-3">
                        <p className="text-caption text-text-secondary">
                          {p.startDate ? new Date(p.startDate).toLocaleDateString() : '—'} → {p.endDate ? new Date(p.endDate).toLocaleDateString() : '—'}
                        </p>
                      </td>
                      <td className="px-4 py-3 text-right">
                        {(canUpdate || canArchive) && (
                          <Dropdown
                            trigger={<IconButton label="Project actions" variant="ghost"><MoreHorizontal className="h-4 w-4" /></IconButton>}
                            align="right"
                            items={[
                              ...(canUpdate ? [{ label: 'Edit Project', icon: <Edit2 className="h-4 w-4" />, onClick: () => openEdit(p) }] : []),
                              ...(canUpdate && canArchive ? [{ divider: true }] : []),
                              ...(canArchive ? [{ label: 'Archive', icon: <Archive className="h-4 w-4" />, danger: true, onClick: () => setConfirmDelete(p) }] : []),
                            ]}
                          />
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {projectsPage && projectsPage.page.totalPages > 1 && (
              <div className="flex items-center justify-between px-4 py-3 border-t border-border-subtle">
                <p className="text-caption text-text-tertiary">
                  Showing {projectsPage.page.page + 1} of {projectsPage.page.totalPages}
                </p>
                <Pagination page={projectsPage.page.page + 1} totalPages={projectsPage.page.totalPages} onPageChange={(p) => setPage(p)} />
              </div>
            )}
          </CardBody>
        </Card>
      )}

      <Modal
        open={formOpen}
        onClose={() => setFormOpen(false)}
        title={editing ? 'Edit Project' : 'New Project'}
        size="md"
      >
        <div className="flex flex-col gap-4">
          <Input label="Project Name" placeholder="e.g. Mobile App v2" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <Textarea label="Description" placeholder="Brief description of the project" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <div className="grid grid-cols-2 gap-3">
            <Select
              label="Priority"
              value={form.priority}
              onChange={(e) => setForm({ ...form, priority: e.target.value as ProjectPriority })}
              options={[
                { value: 'CRITICAL', label: 'Critical' },
                { value: 'HIGH', label: 'High' },
                { value: 'MEDIUM', label: 'Medium' },
                { value: 'LOW', label: 'Low' },
              ]}
            />
            <Select
              label="Manager"
              value={form.managerId}
              onChange={(e) => setForm({ ...form, managerId: e.target.value })}
              options={[
                { value: '', label: 'Not assigned' },
                ...(users ?? []).map((u) => ({ value: u.id, label: `${u.firstName} ${u.lastName}` })),
              ]}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Start Date" type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
            <Input label="End Date" type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
          </div>
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-border-subtle">
            <Button variant="outline" onClick={() => setFormOpen(false)}>Cancel</Button>
            <Button variant="primary" onClick={handleSave} disabled={!form.name.trim()}>{editing ? 'Save Changes' : 'Create Project'}</Button>
          </div>
        </div>
      </Modal>

      <Modal open={!!confirmDelete} onClose={() => setConfirmDelete(null)} title="Archive Project" size="sm">
        <p className="text-body text-text-secondary">
          Are you sure you want to archive "{confirmDelete?.name}"? The project will be moved to the archived list and can be restored later.
        </p>
        <div className="flex items-center justify-end gap-3 mt-6 pt-5 border-t border-border-subtle">
          <Button variant="outline" onClick={() => setConfirmDelete(null)}>Cancel</Button>
          <Button variant="danger" onClick={handleDelete}>Archive</Button>
        </div>
      </Modal>
    </div>
  );
}

export default DevelopmentProjectsTab;
