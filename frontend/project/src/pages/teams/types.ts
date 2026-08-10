export type TeamStatus = 'active' | 'archived';

export interface TeamMember {
  id: string;
  name: string;
}

export interface Team {
  id: string;
  name: string;
  description: string;
  department: string;
  departmentId: string;
  manager: string;
  managerId?: string;
  memberCount: number;
  status: TeamStatus;
  createdAt: string;
  members: TeamMember[];
}

export type ModalState =
  | { kind: 'create' }
  | { kind: 'edit'; team: Team }
  | { kind: 'archive'; team: Team }
  | { kind: 'restore'; team: Team }
  | { kind: 'delete'; team: Team }
  | { kind: 'change-manager'; team: Team }
  | null;

export const statusBadge: Record<TeamStatus, { tone: 'success' | 'info' | 'warning' | 'neutral'; label: string }> = {
  active: { tone: 'success', label: 'Active' },
  archived: { tone: 'neutral', label: 'Archived' },
};

export const statusMap: Record<string, TeamStatus> = {
  ACTIVE: 'active',
  ARCHIVED: 'archived',
};