import { useEffect, useState } from 'react';
import { cn } from '../../../lib/cn';
import { Button } from '../../ui/Button';
import type { AIScopeSelection, AIScopeType } from '../../../hooks/useAIScopeSelectors';

interface ScopeOption {
  value: string;
  label: string;
}

interface SelectField {
  id: string;
  label: string;
  placeholder: string;
  options: ScopeOption[];
  value: string;
  onChange: (value: string) => void;
  hidden?: boolean;
}

interface AIBusinessContextPanelProps {
  scopeOptions: ScopeOption[];
  departments: { id: string; name: string }[];
  projects: { id: string; name: string }[];
  teams: { id: string; name: string }[];
  defaultScope: AIScopeType;
  defaultDepartmentId?: string;
  onAnalyze: (selection: AIScopeSelection, question: string) => void;
  analyzeLabel?: string;
  inputPlaceholder?: string;
  showScopeSelectors?: boolean;
}

export function AIBusinessContextPanel({
  scopeOptions,
  departments,
  projects,
  teams,
  defaultScope,
  defaultDepartmentId,
  onAnalyze,
  analyzeLabel = 'Analyze',
  inputPlaceholder,
  showScopeSelectors = true,
}: AIBusinessContextPanelProps) {
  const [open, setOpen] = useState(true);
  const [scope, setScope] = useState<AIScopeType>(defaultScope);
  const [departmentId, setDepartmentId] = useState(defaultDepartmentId ?? '');
  const [projectId, setProjectId] = useState('');
  const [teamId, setTeamId] = useState('');
  const [question, setQuestion] = useState('');

  useEffect(() => {
    setScope(defaultScope);
  }, [defaultScope]);

  useEffect(() => {
    if (defaultDepartmentId) setDepartmentId(defaultDepartmentId);
  }, [defaultDepartmentId]);

  useEffect(() => {
    setProjectId('');
    setTeamId('');
  }, [departmentId, scope]);

  const fields: SelectField[] = [
    {
      id: 'scope',
      label: 'Scope',
      placeholder: 'Select scope...',
      options: scopeOptions,
      value: scope,
      onChange: (v) => setScope(v as AIScopeType),
      hidden: !showScopeSelectors || scopeOptions.length === 0,
    },
    {
      id: 'department',
      label: 'Department',
      placeholder: 'Select department...',
      options: departments.map((d) => ({ value: d.id, label: d.name })),
      value: departmentId,
      onChange: setDepartmentId,
      hidden: !showScopeSelectors || scope === 'WORKSPACE' || departments.length === 0,
    },
    {
      id: 'project',
      label: 'Project',
      placeholder: 'Select project...',
      options: projects.map((p) => ({ value: p.id, label: p.name })),
      value: projectId,
      onChange: setProjectId,
      hidden: !showScopeSelectors || scope !== 'PROJECT',
    },
    {
      id: 'team',
      label: 'Team',
      placeholder: 'Select team...',
      options: teams.map((t) => ({ value: t.id, label: t.name })),
      value: teamId,
      onChange: setTeamId,
      hidden: !showScopeSelectors || scope !== 'TEAM',
    },
  ];

  function handleSubmit() {
    onAnalyze(
      {
        scope,
        departmentId: scope === 'WORKSPACE' ? undefined : departmentId || undefined,
        projectId: scope === 'PROJECT' ? projectId || undefined : undefined,
        teamId: scope === 'TEAM' ? teamId || undefined : undefined,
      },
      question.trim(),
    );
  }

  return (
    <div className={cn('rounded-xl border border-border-subtle bg-elevated dark:bg-surface overflow-hidden transition-all')}>
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="flex w-full items-center justify-between px-5 py-4 bg-surface-2 hover:bg-surface transition-colors"
      >
        <p className="text-caption font-semibold text-text-primary">Context</p>
        <svg
          className={cn('h-4 w-4 text-text-tertiary transition-transform', open && 'rotate-180')}
          viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
        >
          <path d="M6 9l6 6 6-6" />
        </svg>
      </button>
      {open && (
        <div className="p-5 space-y-4">
          {fields.filter((f) => !f.hidden).map((field) => (
            <div key={field.id}>
              <label className="block text-2xs font-medium text-text-tertiary mb-1.5">{field.label}</label>
              <select
                aria-label={field.label}
                value={field.value}
                onChange={(e) => field.onChange(e.target.value)}
                className="w-full rounded-lg border border-border-subtle bg-surface px-3 py-2 text-caption text-text-primary focus:outline-none focus:ring-2 focus:ring-accent-500 focus:border-transparent transition-colors appearance-none"
              >
                <option value="">{field.placeholder}</option>
                {field.options.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>
          ))}

          {inputPlaceholder && (
            <div>
              <label className="block text-2xs font-medium text-text-tertiary mb-1.5">Question</label>
              <textarea
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder={inputPlaceholder}
                rows={3}
                className="w-full rounded-lg border border-border-subtle bg-surface px-3 py-2 text-caption text-text-primary placeholder:text-text-tertiary focus:outline-none focus:ring-2 focus:ring-accent-500 focus:border-transparent transition-colors resize-none"
              />
            </div>
          )}

          <Button fullWidth onClick={handleSubmit}>{analyzeLabel}</Button>
        </div>
      )}
    </div>
  );
}
