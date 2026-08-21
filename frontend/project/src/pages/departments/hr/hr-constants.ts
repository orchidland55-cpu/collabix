import { z } from 'zod';

export const CANDIDATE_STATUSES = [
  'APPLIED', 'CV_REVIEW', 'HR_INTERVIEW', 'TECHNICAL_INTERVIEW',
  'FINAL_INTERVIEW', 'OFFER', 'HIRED', 'REJECTED', 'WITHDRAWN',
] as const;

export const candidateStatusColor: Record<string, string> = {
  APPLIED: 'info',
  CV_REVIEW: 'neutral',
  HR_INTERVIEW: 'accent',
  TECHNICAL_INTERVIEW: 'accent',
  FINAL_INTERVIEW: 'warning',
  OFFER: 'success',
  HIRED: 'success',
  REJECTED: 'danger',
  WITHDRAWN: 'neutral',
};

export const candidateStatusLabel: Record<string, string> = {
  APPLIED: 'Applied',
  CV_REVIEW: 'CV Review',
  HR_INTERVIEW: 'HR Interview',
  TECHNICAL_INTERVIEW: 'Technical Interview',
  FINAL_INTERVIEW: 'Final Interview',
  OFFER: 'Offer',
  HIRED: 'Hired',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn',
};

export const CANDIDATE_SOURCES = [
  'LINKEDIN', 'INDEED', 'JOBLY', 'GMAIL', 'FACEBOOK', 'WEBSITE',
] as const;

export const candidateSourceLabel: Record<string, string> = {
  LINKEDIN: 'LinkedIn',
  INDEED: 'Indeed',
  JOBLY: 'Jobly',
  GMAIL: 'Gmail',
  FACEBOOK: 'Facebook',
  WEBSITE: 'Website',
};

export const CONTRACT_TYPES = [
  'FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP', 'FREELANCE', 'TEMPORARY',
] as const;

export const contractTypeLabel: Record<string, string> = {
  FULL_TIME: 'Full Time',
  PART_TIME: 'Part Time',
  CONTRACT: 'Contract',
  INTERNSHIP: 'Internship',
  FREELANCE: 'Freelance',
  TEMPORARY: 'Temporary',
};

export const contractTypeColor: Record<string, string> = {
  FULL_TIME: 'accent',
  PART_TIME: 'info',
  CONTRACT: 'warning',
  INTERNSHIP: 'success',
  FREELANCE: 'neutral',
  TEMPORARY: 'neutral',
};

export const EMPLOYMENT_STATUSES = [
  'ONBOARDING', 'PROBATION', 'ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'RESIGNED', 'TERMINATED', 'RETIRED',
] as const;

export const employmentStatusLabel: Record<string, string> = {
  ONBOARDING: 'Onboarding',
  PROBATION: 'Probation',
  ACTIVE: 'Active',
  ON_LEAVE: 'On Leave',
  SUSPENDED: 'Suspended',
  RESIGNED: 'Resigned',
  TERMINATED: 'Terminated',
  RETIRED: 'Retired',
};

export const employmentStatusColor: Record<string, string> = {
  ONBOARDING: 'info',
  PROBATION: 'warning',
  ACTIVE: 'success',
  ON_LEAVE: 'warning',
  SUSPENDED: 'danger',
  RESIGNED: 'neutral',
  TERMINATED: 'danger',
  RETIRED: 'neutral',
};

const INACTIVE_EMPLOYMENT_STATUSES = ['RESIGNED', 'TERMINATED', 'RETIRED'];

/** Employees available for HR workflows (matches backend review eligibility). */
export function isReviewEligibleEmployee(status: string | undefined | null): boolean {
  if (!status) return true;
  return !INACTIVE_EMPLOYMENT_STATUSES.includes(status);
}

export function isActiveEmployee(status: string | undefined | null): boolean {
  return isReviewEligibleEmployee(status);
}

export const SKILL_CATEGORIES = [
  'TECHNICAL', 'PROGRAMMING', 'DATABASE', 'DEVOPS', 'CLOUD', 'AI', 'MARKETING', 'DESIGN',
  'MANAGEMENT', 'COMMUNICATION', 'LANGUAGE', 'SALES', 'HR', 'FINANCE', 'OTHER',
] as const;

export const skillCategoryLabel: Record<string, string> = {
  TECHNICAL: 'Technical',
  PROGRAMMING: 'Programming',
  DATABASE: 'Database',
  DEVOPS: 'DevOps',
  CLOUD: 'Cloud',
  AI: 'AI',
  MARKETING: 'Marketing',
  DESIGN: 'Design',
  MANAGEMENT: 'Management',
  COMMUNICATION: 'Communication',
  LANGUAGE: 'Language',
  SALES: 'Sales',
  HR: 'HR',
  FINANCE: 'Finance',
  OTHER: 'Other',
};

export const SKILL_LEVELS = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'] as const;

export const skillLevelColor: Record<string, string> = {
  BEGINNER: 'info',
  INTERMEDIATE: 'warning',
  ADVANCED: 'success',
  EXPERT: 'accent',
};

export const ONBOARDING_STATUSES = ['NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'ON_HOLD'] as const;

export const onboardingStatusColor: Record<string, string> = {
  NOT_STARTED: 'neutral',
  IN_PROGRESS: 'warning',
  COMPLETED: 'success',
  CANCELLED: 'danger',
  ON_HOLD: 'info',
};

export const ONBOARDING_TASK_STATUSES = ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED'] as const;

export const onboardingTaskStatusColor: Record<string, string> = {
  PENDING: 'neutral',
  IN_PROGRESS: 'warning',
  COMPLETED: 'success',
  SKIPPED: 'neutral',
};

export const REVIEW_STATUSES = ['DRAFT', 'IN_PROGRESS', 'SUBMITTED', 'APPROVED', 'REJECTED', 'ARCHIVED'] as const;

export const reviewStatusColor: Record<string, string> = {
  DRAFT: 'neutral',
  IN_PROGRESS: 'info',
  SUBMITTED: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  ARCHIVED: 'neutral',
};

export const PERFORMANCE_LEVELS = [
  'OUTSTANDING', 'EXCELLENT', 'VERY_GOOD', 'GOOD', 'SATISFACTORY', 'NEEDS_IMPROVEMENT', 'UNSATISFACTORY',
] as const;

export const performanceLevelColor: Record<string, string> = {
  OUTSTANDING: 'success',
  EXCELLENT: 'success',
  VERY_GOOD: 'info',
  GOOD: 'info',
  SATISFACTORY: 'warning',
  NEEDS_IMPROVEMENT: 'danger',
  UNSATISFACTORY: 'danger',
};

export const REVIEW_PERIODS = ['MONTHLY', 'QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL', 'CUSTOM'] as const;

export const reviewPeriodLabel: Record<string, string> = {
  MONTHLY: 'Monthly',
  QUARTERLY: 'Quarterly',
  SEMI_ANNUAL: 'Semi-Annual',
  ANNUAL: 'Annual',
  CUSTOM: 'Custom',
};

export const INTERVIEW_TYPES = ['EMPLOYEE', 'INTERN', 'COMMERCIAL'] as const;

export const interviewTypeColor: Record<string, string> = {
  EMPLOYEE: 'info',
  INTERN: 'accent',
  COMMERCIAL: 'success',
};

export const INTERVIEW_STATUSES = ['SCHEDULED', 'COMPLETED', 'CANCELLED', 'RESCHEDULED', 'NO_SHOW'] as const;

export const interviewStatusColor: Record<string, string> = {
  SCHEDULED: 'info',
  COMPLETED: 'success',
  CANCELLED: 'danger',
  RESCHEDULED: 'warning',
  NO_SHOW: 'danger',
};

export const RECOMMENDATIONS = ['STRONG_HIRE', 'HIRE', 'NEUTRAL', 'NO_HIRE', 'STRONG_NO_HIRE'] as const;

export const recommendationColor: Record<string, string> = {
  STRONG_HIRE: 'success',
  HIRE: 'success',
  NEUTRAL: 'neutral',
  NO_HIRE: 'danger',
  STRONG_NO_HIRE: 'danger',
};

export const ATTENDANCE_STATUSES = [
  'PRESENT', 'ABSENT', 'LATE', 'HALF_DAY', 'REMOTE', 'VACATION', 'SICK_LEAVE', 'BUSINESS_TRIP', 'HOLIDAY',
] as const;

export const attendanceStatusColor: Record<string, string> = {
  PRESENT: 'success',
  ABSENT: 'danger',
  LATE: 'warning',
  HALF_DAY: 'info',
  REMOTE: 'accent',
  VACATION: 'info',
  SICK_LEAVE: 'warning',
  BUSINESS_TRIP: 'neutral',
  HOLIDAY: 'neutral',
};

export const NOTE_CATEGORIES = ['GENERAL', 'HR', 'TECHNICAL', 'INTERVIEW', 'SALARY', 'RISK', 'FOLLOW_UP', 'OFFER', 'OTHER'] as const;

export const noteCategoryLabel: Record<string, string> = {
  GENERAL: 'General',
  HR: 'HR',
  TECHNICAL: 'Technical',
  INTERVIEW: 'Interview',
  SALARY: 'Salary',
  RISK: 'Risk',
  FOLLOW_UP: 'Follow Up',
  OFFER: 'Offer',
  OTHER: 'Other',
};

export const NOTE_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;

export const notePriorityColor: Record<string, string> = {
  LOW: 'neutral',
  MEDIUM: 'info',
  HIGH: 'warning',
  CRITICAL: 'danger',
};

export const NOTE_VISIBILITIES = ['PRIVATE', 'DEPARTMENT', 'MANAGERS'] as const;

export const noteVisibilityLabel: Record<string, string> = {
  PRIVATE: 'Private',
  DEPARTMENT: 'Department',
  MANAGERS: 'Managers',
};

export const EMPLOYEE_DOCUMENT_TYPES = [
  'CONTRACT', 'NDA', 'IDENTITY', 'PASSPORT', 'WORK_PERMIT', 'DIPLOMA', 'CERTIFICATE', 'RESUME',
  'PERFORMANCE_REVIEW', 'PROMOTION', 'SALARY', 'MEDICAL', 'INSURANCE', 'TAX', 'RESIGNATION',
  'EXIT_DOCUMENT', 'OTHER',
] as const;

export const employeeDocumentTypeLabel: Record<string, string> = {
  CONTRACT: 'Contract',
  NDA: 'NDA',
  IDENTITY: 'Identity',
  PASSPORT: 'Passport',
  WORK_PERMIT: 'Work Permit',
  DIPLOMA: 'Diploma',
  CERTIFICATE: 'Certificate',
  RESUME: 'CV / Resume',
  PERFORMANCE_REVIEW: 'Performance Review',
  PROMOTION: 'Promotion',
  SALARY: 'Salary',
  MEDICAL: 'Medical',
  INSURANCE: 'Insurance',
  TAX: 'Tax',
  RESIGNATION: 'Resignation',
  EXIT_DOCUMENT: 'Exit Document',
  OTHER: 'Other',
};

export const CANDIDATE_ATTACHMENT_TYPES = [
  'CV', 'COVER_LETTER', 'DIPLOMA', 'CERTIFICATE', 'PORTFOLIO', 'IDENTITY',
  'RECOMMENDATION', 'OFFER_LETTER', 'CONTRACT', 'OTHER',
] as const;

export const candidateAttachmentTypeLabel: Record<string, string> = {
  CV: 'CV',
  COVER_LETTER: 'Cover Letter',
  DIPLOMA: 'Diploma',
  CERTIFICATE: 'Certificate',
  PORTFOLIO: 'Portfolio',
  IDENTITY: 'Identity',
  RECOMMENDATION: 'Recommendation',
  OFFER_LETTER: 'Offer Letter',
  CONTRACT: 'Contract',
  OTHER: 'Other',
};

export function formatEnum(value: string | undefined | null): string {
  if (!value) return '';
  return value.replace(/_/g, ' ');
}

export function formatDateTime(value: string | undefined | null): string {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString('en-US', {
    month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

export function formatDate(value: string | undefined | null): string {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

export function formatTime(value: string | undefined | null): string {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
}

const pad2 = (n: number) => String(n).padStart(2, '0');

export function toISOTimestamp(date: string | undefined | null, time?: string | undefined | null): string | undefined {
  if (!date) return undefined;
  const t = time && time.length > 0 ? time : '00:00';
  const d = new Date(`${date}T${t}:00`);
  return Number.isNaN(d.getTime()) ? undefined : d.toISOString();
}

export function toDateInputValue(iso: string | undefined | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

export function toTimeInputValue(iso: string | undefined | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

const DATE_INPUT_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
const TIME_INPUT_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/;

export function isHHMM(value: string | undefined | null): boolean {
  return typeof value === 'string' && TIME_INPUT_PATTERN.test(value);
}

export function parseDateInput(value: string | undefined | null): Date | null {
  if (!value || !DATE_INPUT_PATTERN.test(value)) return null;
  const [year, month, day] = value.split('-').map(Number);
  const d = new Date(year, month - 1, day);
  if (d.getFullYear() !== year || d.getMonth() !== month - 1 || d.getDate() !== day) return null;
  return d;
}

export function todayDateInputValue(): string {
  const d = new Date();
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

export const interviewScheduleSchema = z
  .object({
    candidateId: z.string().min(1, 'Candidate is required.'),
    type: z.string().min(1, 'Type is required.'),
    position: z.string().min(1, 'Position is required.'),
    scheduledDate: z.string().min(1, 'Date is required.'),
    startTime: z.string().min(1, 'Start time is required.'),
    endTime: z.string().min(1, 'End time is required.'),
  })
  .superRefine((val, ctx) => {
    const date = parseDateInput(val.scheduledDate);
    if (!date) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['scheduledDate'], message: 'Date must be a valid date and cannot be in the past.' });
      return;
    }
    const now = new Date();
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    if (date.getTime() < todayStart.getTime()) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['scheduledDate'], message: 'Date must be a valid date and cannot be in the past.' });
      return;
    }
    const isToday = date.getTime() === todayStart.getTime();

    const startOk = isHHMM(val.startTime);
    const endOk = isHHMM(val.endTime);
    if (!startOk) ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['startTime'], message: 'Start time must be a valid time (HH:mm).' });
    if (!endOk) ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['endTime'], message: 'End time must be a valid time (HH:mm).' });
    if (!startOk || !endOk) return;

    const startParts = val.startTime.split(':').map(Number);
    const endParts = val.endTime.split(':').map(Number);
    const startDate = new Date(date.getFullYear(), date.getMonth(), date.getDate(), startParts[0], startParts[1]);
    const endDate = new Date(date.getFullYear(), date.getMonth(), date.getDate(), endParts[0], endParts[1]);

    if (endDate.getTime() <= startDate.getTime()) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['endTime'], message: 'End time must be later than start time.' });
    }
    if (isToday && startDate.getTime() <= now.getTime()) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['startTime'], message: 'Start time must be later than the current time.' });
    }
  });
