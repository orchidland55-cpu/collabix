import type { LucideIcon } from 'lucide-react';
import {
  LayoutGrid,
  Users,
  Briefcase,
  GraduationCap,
  ClipboardCheck,
  Star,
  FileText,
  Activity,
  TrendingUp,
  Settings,
  Cpu,
  Shield,
  Megaphone,
  GitBranch,
  FolderKanban,
  CheckSquare,
  CalendarDays,
  Timer,
  Bell,
} from 'lucide-react';
import type { DeptType } from '../../lib/access';

export interface DepartmentTab {
  id: string;
  label: string;
  icon: LucideIcon;
}

/**
 * Single source of truth for department tab navigation.
 * The shared layout stays identical across departments; only these
 * domain-specific workflows differ.
 */
export const DEPT_TABS: Record<DeptType, DepartmentTab[]> = {
  hr: [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutGrid },
    { id: 'employees', label: 'Employees', icon: Users },
    { id: 'candidates', label: 'Candidates', icon: Briefcase },
    { id: 'interviews', label: 'Interviews', icon: CalendarDays },
    { id: 'skills', label: 'Skills', icon: GraduationCap },
    { id: 'onboarding', label: 'Onboarding', icon: ClipboardCheck },
    { id: 'reviews', label: 'Performance Reviews', icon: Star },
    { id: 'attendance', label: 'Attendance', icon: Timer },
    { id: 'documents', label: 'Documents', icon: FileText },
    { id: 'notifications', label: 'Notifications', icon: Bell },
    { id: 'reports', label: 'Reports', icon: FileText },
    { id: 'analytics', label: 'Analytics', icon: TrendingUp },
  ],
  development: [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutGrid },
    { id: 'projects', label: 'Projects', icon: FolderKanban },
    { id: 'sprints', label: 'Sprints', icon: GitBranch },
    { id: 'tasks', label: 'Tasks', icon: CheckSquare },
    { id: 'documents', label: 'Documents', icon: FileText },
    { id: 'reports', label: 'Reports', icon: FileText },
    { id: 'analytics', label: 'Analytics', icon: TrendingUp },
    { id: 'activity', label: 'Activity', icon: Activity },
    { id: 'settings', label: 'Settings', icon: Settings },
  ],
  ai: [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutGrid },
    { id: 'models', label: 'Models', icon: Cpu },
    { id: 'documents', label: 'Documents', icon: FileText },
    { id: 'reports', label: 'Reports', icon: FileText },
    { id: 'analytics', label: 'Analytics', icon: TrendingUp },
    { id: 'activity', label: 'Activity', icon: Activity },
    { id: 'settings', label: 'Settings', icon: Settings },
  ],
  marketing: [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutGrid },
    { id: 'campaigns', label: 'Campaigns', icon: Megaphone },
    { id: 'analytics', label: 'Campaign Analytics', icon: TrendingUp },
    { id: 'reports', label: 'Reports', icon: FileText },
    { id: 'documents', label: 'Documents', icon: FileText },
    { id: 'activity', label: 'Activity', icon: Activity },
    { id: 'settings', label: 'Settings', icon: Settings },
  ],
  cybersecurity: [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutGrid },
    { id: 'audits', label: 'Audits', icon: Shield },
    { id: 'reports', label: 'Reports', icon: FileText },
    { id: 'analytics', label: 'Analytics', icon: TrendingUp },
    { id: 'documents', label: 'Documents', icon: FileText },
    { id: 'activity', label: 'Activity', icon: Activity },
    { id: 'settings', label: 'Settings', icon: Settings },
  ],
  generic: [
    { id: 'dashboard', label: 'Dashboard', icon: LayoutGrid },
    { id: 'documents', label: 'Documents', icon: FileText },
    { id: 'reports', label: 'Reports', icon: FileText },
    { id: 'analytics', label: 'Analytics', icon: TrendingUp },
    { id: 'activity', label: 'Activity', icon: Activity },
    { id: 'settings', label: 'Settings', icon: Settings },
  ],
};
