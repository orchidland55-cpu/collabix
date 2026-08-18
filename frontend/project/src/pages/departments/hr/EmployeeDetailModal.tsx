import { useMemo } from 'react';
import { Loader2, Download, CalendarClock } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { getApiBaseUrl } from '../../../lib/api-base';
import { Modal } from '../../../components/ui/Modal';
import { Badge, type Tone } from '../../../components/ui/Badge';
import { Avatar } from '../../../components/ui/Avatar';
import { Button } from '../../../components/ui/Button';
import { useEmployeeDetail, useEmployeeTimeline } from '../../../services/employee-hooks';
import { useAttendanceByEmployee } from '../../../services/attendance-hooks';
import { useEmployeeSkillsList } from '../../../services/employee-skill-hooks';
import { useEmployeeDocuments } from '../../../services/employee-document-hooks';
import { performanceReviewService } from '../../../services/performance-review-service';
import type { EmployeeResponse } from '../../../services/employee-service';
import {
  contractTypeLabel, employmentStatusLabel, employmentStatusColor, contractTypeColor,
  attendanceStatusColor, employeeDocumentTypeLabel,
} from './hr-constants';

interface Props {
  wsId: string;
  deptId: string;
  employee: EmployeeResponse;
  onClose: () => void;
  onEdit: (employee: EmployeeResponse) => void;
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <h3 className="text-sm font-semibold text-text-primary mb-2">{children}</h3>;
}

export function EmployeeDetailModal({ wsId, deptId, employee, onClose, onEdit }: Props) {
  const { data: detail } = useEmployeeDetail(wsId, deptId, employee.id);
  const { data: timeline, isLoading: timelineLoading } = useEmployeeTimeline(wsId, deptId, employee.id);
  const { data: attendanceData } = useAttendanceByEmployee(wsId, deptId, employee.id);
  const { data: skillsData } = useEmployeeSkillsList(wsId, deptId, employee.id);
  const { data: docsData } = useEmployeeDocuments(wsId, deptId, employee.id);
  const { data: reviewsData } = useQuery({
    queryKey: ['employee-reviews', wsId, deptId, employee.id],
    queryFn: () => performanceReviewService.list(wsId, deptId, { page: 0, size: 5, employeeId: employee.id }),
    enabled: !!wsId && !!deptId && !!employee.id,
  });

  const emp = detail ?? employee;
  const attendance = attendanceData?.content ?? [];
  const skills = skillsData?.content ?? [];
  const docs = docsData?.content ?? [];
  const reviews = reviewsData?.content ?? [];

  const info: Array<[string, string]> = useMemo(() => [
    ['Employee Number', emp.employeeNumber],
    ['Position', emp.position],
    ['Email', emp.email],
    ['Phone', emp.phone ?? '-'],
    ['Date of Birth', formatDate(emp.dateOfBirth)],
    ['Nationality', emp.nationality ?? '-'],
    ['Emergency Contact', emp.emergencyContact ?? '-'],
    ['Start Date', formatDate(emp.startDate)],
    ['End Date', emp.endDate ? formatDate(emp.endDate) : 'Current'],
    ['Employment Type', contractTypeLabel[emp.employmentType] ?? emp.employmentType.replace(/_/g, ' ')],
  ], [emp]);

  const downloadDocumentUrl = (docId: string) =>
    `${getApiBaseUrl()}/workspaces/${wsId}/departments/${deptId}/employees/${employee.id}/documents/${docId}/download`;

  return (
    <Modal open onClose={onClose} title="Employee Profile" size="xl"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>Close</Button>
          <Button onClick={() => onEdit(employee)}>Edit</Button>
        </>
      }
    >
      <div className="flex items-center gap-4 border-b border-border-subtle pb-4 mb-4">
        <Avatar name={`${emp.firstName} ${emp.lastName}`} size="lg" tone={0} />
        <div className="flex-1">
          <p className="text-page font-semibold text-text-primary">{emp.firstName} {emp.lastName}</p>
          <p className="text-caption text-text-tertiary">{emp.position} • {emp.employeeNumber}</p>
        </div>
        <div className="flex flex-col items-end gap-1">
          <Badge tone={(employmentStatusColor[emp.employmentStatus] ?? 'neutral') as Tone} variant="soft" dot>{employmentStatusLabel[emp.employmentStatus] ?? emp.employmentStatus.replace(/_/g, ' ')}</Badge>
          <Badge tone={(contractTypeColor[emp.employmentType] ?? 'neutral') as Tone} variant="soft">{contractTypeLabel[emp.employmentType] ?? emp.employmentType.replace(/_/g, ' ')}</Badge>
        </div>
      </div>

      <div className="grid md:grid-cols-2 gap-6">
        <section className="space-y-4">
          <div>
            <SectionTitle>Personal & Employment Information</SectionTitle>
            <div className="grid grid-cols-2 gap-x-4 gap-y-2">
              {info.map(([k, v]) => (
                <div key={k}>
                  <p className="text-2xs text-text-tertiary">{k}</p>
                  <p className="text-body text-text-primary">{v}</p>
                </div>
              ))}
            </div>
          </div>

          <div>
            <SectionTitle>Performance Reviews</SectionTitle>
            {reviews.length === 0 ? (
              <p className="text-caption text-text-tertiary">No performance reviews yet.</p>
            ) : (
              <ul className="space-y-2">
                {reviews.map((r) => (
                  <li key={r.id} className="flex items-center justify-between p-2 rounded-md border border-border-subtle">
                    <div>
                      <p className="text-body font-medium text-text-primary">{r.reviewPeriod.replace(/_/g, ' ')}</p>
                      <p className="text-caption text-text-tertiary">{formatDate(r.reviewDate)} • {r.status.replace(/_/g, ' ')}</p>
                    </div>
                    <Badge tone="neutral" variant="soft">{r.percentage ?? r.totalScore}/{r.maxScore ?? 0}</Badge>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </section>

        <section className="space-y-4">
          <div>
            <SectionTitle>Attendance History</SectionTitle>
            {attendance.length === 0 ? (
              <p className="text-caption text-text-tertiary">No attendance records.</p>
            ) : (
              <ul className="space-y-2">
                {attendance.slice(0, 6).map((a) => (
                  <li key={a.id} className="flex items-center justify-between p-2 rounded-md border border-border-subtle">
                    <div className="flex items-center gap-2">
                      <CalendarClock className="h-4 w-4 text-text-tertiary" />
                      <span className="text-body text-text-primary">{formatDate(a.date)}</span>
                    </div>
                    <Badge tone={(attendanceStatusColor[a.status] ?? 'neutral') as Tone} variant="soft">{a.status.replace(/_/g, ' ')}</Badge>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div>
            <SectionTitle>Skills</SectionTitle>
            {skills.length === 0 ? (
              <p className="text-caption text-text-tertiary">No skills recorded.</p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {skills.map((s) => (
                  <Badge key={s.id} tone="neutral" variant="soft">{s.skillName} • {s.proficiencyLevel.replace(/_/g, ' ')}</Badge>
                ))}
              </div>
            )}
          </div>
        </section>
      </div>

      <div className="mt-6">
        <SectionTitle>Documents</SectionTitle>
        {docs.length === 0 ? (
          <p className="text-caption text-text-tertiary">No documents.</p>
        ) : (
          <ul className="space-y-2">
            {docs.slice(0, 6).map((d) => (
              <li key={d.id} className="flex items-center justify-between p-2 rounded-md border border-border-subtle">
                <div>
                  <p className="text-body font-medium text-text-primary">{d.title || employeeDocumentTypeLabel[d.documentType] || d.documentType}</p>
                  <p className="text-caption text-text-tertiary">v{d.fileVersion ?? 1} • {d.originalFileName}</p>
                </div>
                <a href={downloadDocumentUrl(d.id)} target="_blank" rel="noreferrer" className="text-text-tertiary hover:text-text-primary transition-colors">
                  <Download className="h-4 w-4" />
                </a>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="mt-6">
        <SectionTitle>Activity Timeline</SectionTitle>
        {timelineLoading ? (
          <div className="flex items-center gap-2 py-4 text-text-tertiary"><Loader2 className="h-4 w-4 animate-spin" /> Loading history...</div>
        ) : !timeline || timeline.length === 0 ? (
          <p className="text-caption text-text-tertiary">No activity yet.</p>
        ) : (
          <ul className="space-y-3">
            {timeline.map((t) => (
              <li key={t.id} className="flex gap-3">
                <div className="flex flex-col items-center">
                  <span className="h-2 w-2 rounded-full bg-primary" />
                  <span className="w-px flex-1 bg-border-subtle" />
                </div>
                <div className="pb-1">
                  <p className="text-body font-medium text-text-primary">{t.title}</p>
                  {t.description && <p className="text-caption text-text-tertiary">{t.description}</p>}
                  <p className="text-2xs text-text-tertiary">{t.eventType.replace(/_/g, ' ')} • {formatDateTime(t.occurredAt)}</p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </Modal>
  );
}

function formatDate(value?: string): string {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function formatDateTime(value?: string): string {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}