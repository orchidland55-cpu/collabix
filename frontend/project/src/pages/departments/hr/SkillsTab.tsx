import { useState } from 'react';
import { Search, Plus, Check, X, Pencil, Loader2 } from 'lucide-react';
import { Card, CardBody } from '../../../components/ui/Card';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Badge, type Tone } from '../../../components/ui/Badge';
import { IconButton } from '../../../components/ui/IconButton';
import { Modal } from '../../../components/ui/Modal';
import { EmptyState } from '../../../components/ui/EmptyState';
import { useToast } from '../../../components/ui/Toast';
import { useEmployeesList } from '../../../services/employee-hooks';
import { useEmployeeSkillsList, useEmployeeSkillStats, useCreateEmployeeSkill, useUpdateEmployeeSkill, useDeleteEmployeeSkill, useVerifyEmployeeSkill, useUnverifyEmployeeSkill } from '../../../services/employee-skill-hooks';
import type { CreateEmployeeSkillRequest } from '../../../services/employee-skill-service';
import type { EmployeeSkillResponse } from '../../../services/employee-skill-service';
import { SKILL_CATEGORIES, SKILL_LEVELS, skillCategoryLabel, skillLevelColor, formatEnum, isActiveEmployee } from './hr-constants';

type SkillForm = CreateEmployeeSkillRequest;

export function SkillsTab({ wsId, deptId }: { wsId: string; deptId: string }) {
  const [search, setSearch] = useState('');
  const [selectedEmp, setSelectedEmp] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingSkill, setEditingSkill] = useState<EmployeeSkillResponse | null>(null);
  const [skillForm, setSkillForm] = useState<SkillForm>({
    skillName: '', category: 'TECHNICAL', proficiencyLevel: 'INTERMEDIATE',
  });

  const { toast } = useToast();

  const { data: empData } = useEmployeesList(wsId, deptId);
  const { data: skillData, isLoading } = useEmployeeSkillsList(wsId, deptId, selectedEmp ?? '');
  const { data: stats } = useEmployeeSkillStats(wsId, deptId);
  const createSkill = useCreateEmployeeSkill(wsId, deptId, selectedEmp ?? '');
  const updateSkill = useUpdateEmployeeSkill(wsId, deptId, selectedEmp ?? '', editingSkill?.id ?? '');
  const deleteSkill = useDeleteEmployeeSkill(wsId, deptId, selectedEmp ?? '');
  const verifySkill = useVerifyEmployeeSkill(wsId, deptId, selectedEmp ?? '');
  const unverifySkill = useUnverifyEmployeeSkill(wsId, deptId, selectedEmp ?? '');

  const employees = empData?.content ?? [];
  const activeEmployees = employees.filter((e) => isActiveEmployee(e.employmentStatus));
  const skills = skillData?.content ?? [];

  const filteredSkills = skills.filter((s) => {
    if (!search) return true;
    return s.skillName.toLowerCase().includes(search.toLowerCase()) || s.category.toLowerCase().includes(search.toLowerCase());
  });

  const openCreate = () => {
    setEditingSkill(null);
    setSkillForm({ skillName: '', category: 'TECHNICAL', proficiencyLevel: 'INTERMEDIATE' });
    setShowForm(true);
  };

  const openEdit = (s: EmployeeSkillResponse) => {
    setEditingSkill(s);
    setSkillForm({
      skillName: s.skillName,
      category: s.category,
      proficiencyLevel: s.proficiencyLevel,
      yearsOfExperience: s.yearsOfExperience,
      certificationName: s.certificationName,
      certificationIssuer: s.certificationIssuer,
      notes: s.notes,
    });
    setShowForm(true);
  };

  const handleSubmit = () => {
    if (!selectedEmp) return;
    if (editingSkill) {
      updateSkill.mutate(skillForm, {
        onSuccess: () => { toast({ title: 'Skill updated', tone: 'success' }); setShowForm(false); },
        onError: () => toast({ title: 'Failed to update skill', tone: 'danger' }),
      });
    } else {
      createSkill.mutate(skillForm, {
        onSuccess: () => { toast({ title: 'Skill added', tone: 'success' }); setShowForm(false); },
        onError: () => toast({ title: 'Failed to add skill', tone: 'danger' }),
      });
    }
  };

  const handleToggleVerify = (s: EmployeeSkillResponse) => {
    if (s.verified) {
      unverifySkill.mutate(s.id, {
        onSuccess: () => toast({ title: 'Skill unverified', tone: 'success' }),
        onError: () => toast({ title: 'Failed to update skill', tone: 'danger' }),
      });
    } else {
      verifySkill.mutate(s.id, {
        onSuccess: () => toast({ title: 'Skill verified', tone: 'success' }),
        onError: () => toast({ title: 'Failed to verify skill', tone: 'danger' }),
      });
    }
  };

  const handleDelete = (s: EmployeeSkillResponse) => {
    if (!window.confirm(`Delete skill "${s.skillName}"?`)) return;
    deleteSkill.mutate(s.id, {
      onSuccess: () => toast({ title: 'Skill deleted', tone: 'success' }),
      onError: () => toast({ title: 'Failed to delete skill', tone: 'danger' }),
    });
  };

  return (
    <div className="flex flex-col gap-4">
      {stats && (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
            <Stat label="Total Skills" value={stats.totalSkills} />
            <Stat label="Verified" value={stats.verifiedCount} tone="success" />
            <Stat label="Unverified" value={stats.unverifiedCount} tone="warning" />
            <Stat label="Certifications" value={stats.certificationCount} tone="info" />
            <Stat label="Expiring Certs" value={stats.expiringCertificationCount} tone="danger" />
          </div>

          <div className="grid md:grid-cols-3 gap-4">
            <Card>
              <CardBody>
                <p className="text-sm font-semibold text-text-primary mb-3">Skills by Category</p>
                {(stats.skillsByCategory && Object.keys(stats.skillsByCategory).length > 0) ? (
                  <div className="space-y-2">
                    {Object.entries(stats.skillsByCategory).sort(([, a], [, b]) => b - a).map(([cat, count]) => (
                      <div key={cat} className="flex items-center gap-2">
                        <span className="text-2xs text-text-tertiary w-24 shrink-0 truncate">{skillCategoryLabel[cat] ?? formatEnum(cat)}</span>
                        <div className="flex-1 h-2 rounded-full bg-surface-2 overflow-hidden">
                          <div className="h-full rounded-full bg-accent-500" style={{ width: `${Math.min(100, (count / Math.max(1, Math.max(...Object.values(stats.skillsByCategory)))) * 100)}%` }} />
                        </div>
                        <span className="text-2xs text-text-secondary w-6 text-right">{count}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-caption text-text-tertiary">No category data yet.</p>
                )}
              </CardBody>
            </Card>
            <Card>
              <CardBody>
                <p className="text-sm font-semibold text-text-primary mb-3">Skills by Proficiency</p>
                {(stats.skillsByLevel && Object.keys(stats.skillsByLevel).length > 0) ? (
                  <div className="space-y-2">
                    {Object.entries(stats.skillsByLevel).sort(([, a], [, b]) => b - a).map(([level, count]) => (
                      <div key={level} className="flex items-center gap-2">
                        <span className="text-2xs text-text-tertiary w-24 shrink-0 truncate">{formatEnum(level)}</span>
                        <div className="flex-1 h-2 rounded-full bg-surface-2 overflow-hidden">
                          <div className="h-full rounded-full bg-primary" style={{ width: `${Math.min(100, (count / Math.max(1, Math.max(...Object.values(stats.skillsByLevel)))) * 100)}%` }} />
                        </div>
                        <span className="text-2xs text-text-secondary w-6 text-right">{count}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-caption text-text-tertiary">No proficiency data yet.</p>
                )}
              </CardBody>
            </Card>
            <Card>
              <CardBody>
                <p className="text-sm font-semibold text-text-primary mb-3">Top Skills</p>
                {(stats.topSkills && stats.topSkills.length > 0) ? (
                  <ul className="space-y-2">
                    {stats.topSkills.map((t, i) => (
                      <li key={`${t.skillName}-${i}`} className="flex items-center justify-between">
                        <div>
                          <p className="text-body font-medium text-text-primary">{t.skillName}</p>
                          <p className="text-2xs text-text-tertiary">{skillCategoryLabel[t.category] ?? formatEnum(t.category)} • {formatEnum(t.level)}</p>
                        </div>
                        <Badge tone="neutral" variant="soft">{t.count} emp{t.count === 1 ? '' : 's'}</Badge>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-caption text-text-tertiary">No skill data yet.</p>
                )}
              </CardBody>
            </Card>
          </div>
        </>
      )}

      <div className="flex items-center gap-2">
        <Select
          containerClassName="max-w-xs"
          value={selectedEmp ?? ''}
          onChange={(e) => { setSelectedEmp(e.target.value || null); setShowForm(false); }}
          options={[{ value: '', label: 'Select an employee...' }, ...activeEmployees.map((e) => ({ value: e.id, label: `${e.firstName} ${e.lastName}` }))]}
        />
        {selectedEmp && (
          <Button leftIcon={<Plus />} size="sm" onClick={openCreate}>Add Skill</Button>
        )}
      </div>

      {!selectedEmp && (
        <EmptyState icon={<Search />} title="Select an employee" description="Choose an employee to view and manage their skills." />
      )}

      {selectedEmp && isLoading && (
        <div className="flex items-center justify-center py-20"><Loader2 className="h-8 w-8 animate-spin text-text-tertiary" /></div>
      )}

      {selectedEmp && !isLoading && (
        <>
          <Input placeholder="Search skills..." leftIcon={<Search />} value={search} onChange={(e) => setSearch(e.target.value)} containerClassName="max-w-sm" />
          {filteredSkills.length === 0 ? (
            <EmptyState icon={<Search />} title="No skills found" description="Add skills to track employee capabilities." />
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {filteredSkills.map((s) => (
                <Card key={s.id}>
                  <CardBody className="flex flex-col gap-2">
                    <div className="flex items-start justify-between">
                      <div>
                        <p className="text-body font-medium text-text-primary">{s.skillName}</p>
                        <p className="text-2xs text-text-tertiary">{skillCategoryLabel[s.category] ?? formatEnum(s.category)}</p>
                      </div>
                      <div className="flex items-center gap-1">
                        {s.verified && <Check className="h-4 w-4 text-success-500" />}
                        <IconButton label="Edit" variant="ghost" size="sm" onClick={() => openEdit(s)}>
                          <Pencil className="h-4 w-4" />
                        </IconButton>
                        <IconButton label="Delete" variant="ghost" size="sm" className="text-danger-600" onClick={() => handleDelete(s)}>
                          <X className="h-4 w-4" />
                        </IconButton>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge tone={(skillLevelColor[s.proficiencyLevel] ?? 'neutral') as Tone} variant="soft">{formatEnum(s.proficiencyLevel)}</Badge>
                      {s.yearsOfExperience != null && <span className="text-2xs text-text-tertiary">{s.yearsOfExperience}y exp</span>}
                    </div>
                    {!s.verified && (
                      <Button variant="outline" size="sm" onClick={() => handleToggleVerify(s)}>Verify</Button>
                    )}
                    {s.verified && (
                      <Button variant="outline" size="sm" onClick={() => handleToggleVerify(s)}>Unverify</Button>
                    )}
                    {s.certificationName && (
                      <div className="border-t border-border-subtle pt-2 mt-1">
                        <p className="text-2xs text-text-tertiary">Cert: {s.certificationName}</p>
                        {s.certificationIssuer && <p className="text-2xs text-text-tertiary">Issuer: {s.certificationIssuer}</p>}
                      </div>
                    )}
                  </CardBody>
                </Card>
              ))}
            </div>
          )}
        </>
      )}

      <Modal
        open={showForm}
        onClose={() => setShowForm(false)}
        title={editingSkill ? 'Edit Skill' : 'Add Skill'}
        description={editingSkill ? `Editing ${editingSkill.skillName}` : undefined}
        footer={
          <>
            <Button variant="outline" onClick={() => setShowForm(false)}>Cancel</Button>
            <Button onClick={handleSubmit} disabled={!skillForm.skillName}>{editingSkill ? 'Save' : 'Add'}</Button>
          </>
        }
      >
        <div className="grid grid-cols-2 gap-3">
          <div className="col-span-2">
            <Input label="Skill Name" value={skillForm.skillName} onChange={(e) => setSkillForm({ ...skillForm, skillName: e.target.value })} />
          </div>
          <Select label="Category" value={skillForm.category} onChange={(e) => setSkillForm({ ...skillForm, category: e.target.value })}
            options={SKILL_CATEGORIES.map((c) => ({ value: c, label: skillCategoryLabel[c] }))} />
          <Select label="Proficiency" value={skillForm.proficiencyLevel} onChange={(e) => setSkillForm({ ...skillForm, proficiencyLevel: e.target.value })}
            options={SKILL_LEVELS.map((l) => ({ value: l, label: formatEnum(l) }))} />
          <Input label="Years of Experience" type="number" min={0}
            value={skillForm.yearsOfExperience != null ? String(skillForm.yearsOfExperience) : ''}
            onChange={(e) => setSkillForm({ ...skillForm, yearsOfExperience: e.target.value === '' ? undefined : Number(e.target.value) })} />
          <Input label="Certification Name" value={skillForm.certificationName ?? ''} onChange={(e) => setSkillForm({ ...skillForm, certificationName: e.target.value })} />
          <div className="col-span-2">
            <Input label="Certification Issuer" value={skillForm.certificationIssuer ?? ''} onChange={(e) => setSkillForm({ ...skillForm, certificationIssuer: e.target.value })} />
          </div>
          <div className="col-span-2">
            <Input label="Notes" value={skillForm.notes ?? ''} onChange={(e) => setSkillForm({ ...skillForm, notes: e.target.value })} />
          </div>
        </div>
      </Modal>
    </div>
  );
}

function Stat({ label, value, tone }: { label: string; value: number; tone?: 'success' | 'warning' | 'info' | 'danger' }) {
  const color = tone === 'success' ? 'text-success-600' : tone === 'warning' ? 'text-warning-600' : tone === 'info' ? 'text-info-600' : tone === 'danger' ? 'text-danger-600' : 'text-text-primary';
  return (
    <div className="flex flex-col gap-1 p-3 rounded-lg border border-border-subtle">
      <span className="text-2xs text-text-tertiary">{label}</span>
      <span className={`text-section font-bold ${color}`}>{value}</span>
    </div>
  );
}
