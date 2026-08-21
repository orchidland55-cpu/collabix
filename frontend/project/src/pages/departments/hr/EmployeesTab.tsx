import { useState } from 'react';
import { Search, Plus, Pencil, Loader2, Trash2, Users, Eye, UserX } from 'lucide-react';
import { Card, CardBody } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Badge, type Tone } from '../../../components/ui/Badge';
import { Avatar } from '../../../components/ui/Avatar';
import { IconButton } from '../../../components/ui/IconButton';
import { Modal } from '../../../components/ui/Modal';
import { EmptyState } from '../../../components/ui/EmptyState';
import { Pagination } from '../../../components/ui/Pagination';
import { useToast } from '../../../components/ui/Toast';
import { useEmployeesList, useEmployeeStats, useCreateEmployee, useUpdateEmployee, useDeleteEmployee } from '../../../services/employee-hooks';
import type { EmployeeResponse, CreateEmployeeRequest, UpdateEmployeeRequest } from '../../../services/employee-service';
import { useTeamsByDepartment } from '../../../services/admin-hooks';
import { CONTRACT_TYPES, EMPLOYMENT_STATUSES, contractTypeLabel, contractTypeColor, employmentStatusLabel, employmentStatusColor, formatDate, isActiveEmployee } from './hr-constants';
import { EmployeeDetailModal } from './EmployeeDetailModal';

type EmployeeForm = CreateEmployeeRequest & { employmentStatus?: string };

function buildCreatePayload(form: EmployeeForm): CreateEmployeeRequest {
  return {
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    email: form.email.trim(),
    phone: form.phone?.trim() || undefined,
    position: form.position.trim(),
    employmentType: form.employmentType,
    startDate: form.startDate,
    teamId: form.teamId || undefined,
    managerId: form.managerId || undefined,
    candidateId: form.candidateId || undefined,
  };
}

function buildUpdatePayload(form: EmployeeForm): UpdateEmployeeRequest {
  return {
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    email: form.email.trim(),
    phone: form.phone?.trim() || undefined,
    position: form.position.trim(),
    teamId: form.teamId || undefined,
    managerId: form.managerId || undefined,
    employmentType: form.employmentType,
    employmentStatus: form.employmentStatus,
    startDate: form.startDate,
  };
}

export function EmployeesTab({ wsId, deptId }: { wsId: string; deptId: string }) {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<EmployeeResponse | null>(null);
  const [viewing, setViewing] = useState<EmployeeResponse | null>(null);
  const [form, setForm] = useState<EmployeeForm>({
    firstName: '', lastName: '', email: '', phone: '', position: '', employmentType: 'FULL_TIME', startDate: '',
  });

  const { toast } = useToast();

  const { data, isLoading, isError } = useEmployeesList(wsId, deptId, page);
  const { data: stats } = useEmployeeStats(wsId, deptId);
  const createEmp = useCreateEmployee(wsId, deptId);
  const updateEmp = useUpdateEmployee(wsId, deptId, editing?.id ?? '');
  const deactivateEmp = useUpdateEmployee(wsId, deptId, viewing?.id ?? '');
  const deleteEmp = useDeleteEmployee(wsId, deptId);
  const { data: teams } = useTeamsByDepartment(wsId, deptId);

  const employees = data?.content ?? [];
  const totalPages = data?.page?.totalPages ?? 1;
  const managerOptions = employees
    .filter((e) => isActiveEmployee(e.employmentStatus) && e.id !== editing?.id)
    .map((e) => ({ value: e.id, label: `${e.firstName} ${e.lastName}` }));
  const teamOptions = (teams ?? []).filter((t) => t.status === 'ACTIVE').map((t) => ({ value: t.id, label: t.name }));

  const filtered = employees.filter((e) => {
    if (statusFilter && e.employmentStatus !== statusFilter) return false;
    if (!search) return true;
    const q = search.toLowerCase();
    return e.firstName.toLowerCase().includes(q) || e.lastName.toLowerCase().includes(q) || e.email.toLowerCase().includes(q) || e.position.toLowerCase().includes(q);
  });

  const openCreate = () => {
    setEditing(null);
    setForm({ firstName: '', lastName: '', email: '', phone: '', position: '', employmentType: 'FULL_TIME', startDate: '' });
    setShowForm(true);
  };

  const openEdit = (e: EmployeeResponse) => {
    setEditing(e);
    setForm({ firstName: e.firstName, lastName: e.lastName, email: e.email, phone: e.phone ?? '', position: e.position, teamId: e.teamId ?? '', managerId: e.managerId ?? '', employmentType: e.employmentType, employmentStatus: e.employmentStatus, startDate: e.startDate });
    setShowForm(true);
  };

  const handleSubmit = () => {
    if (editing) {
      updateEmp.mutate(buildUpdatePayload(form), {
        onSuccess: () => { toast({ title: 'Employee updated', tone: 'success' }); setShowForm(false); },
        onError: (err) => toast({ title: 'Failed to update employee', description: err instanceof Error ? err.message : undefined, tone: 'danger' }),
      });
    } else {
      createEmp.mutate(buildCreatePayload(form), {
        onSuccess: () => { toast({ title: 'Employee created', tone: 'success' }); setShowForm(false); },
        onError: (err) => toast({ title: 'Failed to create employee', description: err instanceof Error ? err.message : undefined, tone: 'danger' }),
      });
    }
  };

  const handleDelete = (e: EmployeeResponse) => {
    if (!window.confirm(`Delete employee "${e.firstName} ${e.lastName}"? This cannot be undone.`)) return;
    deleteEmp.mutate(e.id, {
      onSuccess: () => toast({ title: 'Employee deleted', tone: 'success' }),
      onError: (err) => toast({ title: 'Failed to delete employee', description: err instanceof Error ? err.message : undefined, tone: 'danger' }),
    });
  };

  const handleDeactivate = (e: EmployeeResponse) => {
    if (!window.confirm(`Deactivate "${e.firstName} ${e.lastName}"? Their employment status will be set to Suspended.`)) return;
    deactivateEmp.mutate({ employmentStatus: 'SUSPENDED' }, {
      onSuccess: () => { toast({ title: 'Employee deactivated', tone: 'success' }); setViewing(null); },
      onError: (err) => toast({ title: 'Failed to deactivate employee', description: err instanceof Error ? err.message : undefined, tone: 'danger' }),
    });
  };

  if (isLoading) {
    return <div className="flex items-center justify-center py-20"><Loader2 className="h-8 w-8 animate-spin text-text-tertiary" /></div>;
  }

  if (isError) {
    return <div className="flex flex-col items-center justify-center py-20 gap-3"><p className="text-body font-medium text-danger-600">Failed to load employees</p><p className="text-caption text-text-tertiary">Please try again later.</p></div>;
  }

  return (
    <div className="flex flex-col gap-4">
      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
          <StatCard label="Total Employees" value={stats.totalEmployees} />
          <StatCard label="Active" value={stats.activeEmployees} tone="success" />
          <StatCard label="On Leave" value={stats.onLeaveCount} tone="warning" />
          <StatCard label="Probation" value={stats.probationCount} tone="info" />
          <StatCard label="New Hires (Month)" value={stats.newHiresThisMonth} tone="accent" />
        </div>
      )}

      <div className="flex flex-wrap items-center gap-2">
        <Input
          placeholder="Search employees..." leftIcon={<Search />}
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          containerClassName="flex-1 min-w-[200px]"
        />
        <Select
          containerClassName="w-48"
          value={statusFilter}
          onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          options={[{ value: '', label: 'All Statuses' }, ...EMPLOYMENT_STATUSES.map((s) => ({ value: s, label: employmentStatusLabel[s] }))]}
        />
        <Button leftIcon={<Plus />} onClick={openCreate}>Add Employee</Button>
      </div>

      <Modal
        open={showForm}
        onClose={() => setShowForm(false)}
        title={editing ? 'Edit Employee' : 'New Employee'}
        footer={
          <>
            <Button variant="outline" onClick={() => setShowForm(false)}>Cancel</Button>
            <Button onClick={handleSubmit} disabled={!form.firstName || !form.lastName || !form.email || !form.position || !form.startDate}>{editing ? 'Save' : 'Create'}</Button>
          </>
        }
      >
        <div className="grid grid-cols-2 gap-3">
          <Input label="First Name" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
          <Input label="Last Name" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
          <Input label="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <Input label="Phone" value={form.phone ?? ''} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          <div className="col-span-2">
            <Input label="Position" value={form.position} onChange={(e) => setForm({ ...form, position: e.target.value })} />
          </div>
          <Select label="Employment Type" value={form.employmentType} onChange={(e) => setForm({ ...form, employmentType: e.target.value })}
            options={CONTRACT_TYPES.map((t) => ({ value: t, label: contractTypeLabel[t] }))} />
          <Input label="Start Date" type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
          {editing && (
            <Select label="Employment Status" value={form.employmentStatus ?? ''} onChange={(e) => setForm({ ...form, employmentStatus: e.target.value })}
              options={EMPLOYMENT_STATUSES.map((s) => ({ value: s, label: employmentStatusLabel[s] }))} />
          )}
          <Select label="Manager" value={form.managerId ?? ''} onChange={(e) => setForm({ ...form, managerId: e.target.value })}
            options={[{ value: '', label: 'No manager' }, ...managerOptions]}
            helperText={managerOptions.length === 0 ? 'Add employees first to assign a manager.' : undefined}
          />
          <Select label="Team" value={form.teamId ?? ''} onChange={(e) => setForm({ ...form, teamId: e.target.value })}
            options={[{ value: '', label: 'No team' }, ...teamOptions]} />
        </div>
      </Modal>

      {filtered.length === 0 ? (
        <Card>
          <CardBody className="py-16">
            <EmptyState icon={<Users />} title="No employees found" description="Add employees to start managing your team." />
          </CardBody>
        </Card>
      ) : (
        <div className="space-y-2">
          {filtered.map((e) => (
            <div key={e.id} className="flex flex-wrap items-center gap-4 p-4 rounded-lg border border-border-subtle bg-surface hover:bg-surface-2 transition-colors">
              <Avatar name={`${e.firstName} ${e.lastName}`} size="sm" tone={0} />
              <div className="flex-1 min-w-[180px]">
                <p className="text-body font-medium text-text-primary">{e.firstName} {e.lastName}</p>
                <p className="text-caption text-text-tertiary">{e.position} • {e.employeeNumber}</p>
              </div>
              <div className="hidden md:flex items-center gap-1 flex-wrap">
                <Badge tone={(contractTypeColor[e.employmentType] ?? 'neutral') as Tone} variant="soft">{contractTypeLabel[e.employmentType] ?? e.employmentType.replace(/_/g, ' ')}</Badge>
                <Badge tone={(employmentStatusColor[e.employmentStatus] ?? 'neutral') as Tone} variant="soft" dot>{employmentStatusLabel[e.employmentStatus] ?? e.employmentStatus.replace(/_/g, ' ')}</Badge>
              </div>
              <span className="hidden sm:block text-caption text-text-tertiary">{formatDate(e.startDate)}</span>
              <div className="flex items-center gap-1">
                <IconButton label="View" variant="ghost" size="sm" onClick={() => setViewing(e)}>
                  <Eye className="h-4 w-4" />
                </IconButton>
                {e.employmentStatus === 'ACTIVE' && (
                  <IconButton label="Deactivate" variant="ghost" size="sm" className="text-danger-600" onClick={() => handleDeactivate(e)}>
                    <UserX className="h-4 w-4" />
                  </IconButton>
                )}
                <IconButton label="Edit" variant="ghost" size="sm" onClick={() => openEdit(e)}>
                  <Pencil className="h-4 w-4" />
                </IconButton>
                <IconButton label="Delete" variant="ghost" size="sm" className="text-danger-600" onClick={() => handleDelete(e)}>
                  <Trash2 className="h-4 w-4" />
                </IconButton>
              </div>
            </div>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex justify-center pt-2">
          <Pagination page={page + 1} totalPages={totalPages} onPageChange={(p) => setPage(p - 1)} />
        </div>
      )}

      {viewing && (
        <EmployeeDetailModal
          wsId={wsId}
          deptId={deptId}
          employee={viewing}
          onClose={() => setViewing(null)}
          onEdit={(e) => { setViewing(null); openEdit(e); }}
        />
      )}
    </div>
  );
}

function StatCard({ label, value, tone }: { label: string; value: number; tone?: 'success' | 'warning' | 'info' | 'accent' }) {
  const color = tone === 'success' ? 'text-success-600' : tone === 'warning' ? 'text-warning-600' : tone === 'info' ? 'text-info-600' : 'text-text-primary';
  return (
    <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
      <span className="text-2xs text-text-tertiary">{label}</span>
      <span className={`text-section font-bold ${color}`}>{value}</span>
    </div>
  );
}
