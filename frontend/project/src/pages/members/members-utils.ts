import type { UserResponse } from '../../types';
import { MemberType, UserStatus } from '../../types';
import type { Availability, EmploymentType, MemberProfile, MemberStatus } from './members-types';

const employmentTypeByMemberType: Record<MemberType, EmploymentType> = {
  [MemberType.EMPLOYEE]: 'full-time',
  [MemberType.INTERN]: 'intern',
  [MemberType.COMMERCIAL]: 'contract',
};

const memberStatusMap: Record<UserStatus, MemberStatus> = {
  [UserStatus.ACTIVE]: 'active',
  [UserStatus.PENDING_ACTIVATION]: 'inactive',
  [UserStatus.INACTIVE]: 'inactive',
  [UserStatus.LOCKED]: 'inactive',
  [UserStatus.SUSPENDED]: 'inactive',
  [UserStatus.ARCHIVED]: 'inactive',
  [UserStatus.SOFT_DELETED]: 'inactive',
};

const availabilityByStatus: Record<UserStatus, Availability> = {
  [UserStatus.ACTIVE]: 'available',
  [UserStatus.PENDING_ACTIVATION]: 'away',
  [UserStatus.INACTIVE]: 'offline',
  [UserStatus.LOCKED]: 'offline',
  [UserStatus.SUSPENDED]: 'offline',
  [UserStatus.ARCHIVED]: 'offline',
  [UserStatus.SOFT_DELETED]: 'offline',
};

function hashTone(id: string): number {
  let hash = 0;
  for (let i = 0; i < id.length; i += 1) {
    hash = (hash * 31 + id.charCodeAt(i)) >>> 0;
  }
  return (hash % 6) + 1;
}

function formatTitleCase(value: string): string {
  return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}

export function mapUserToMemberProfile(user: UserResponse): MemberProfile {
  return {
    id: user.id,
    name: `${user.firstName} ${user.lastName}`.trim(),
    email: user.email,
    avatar: user.profilePicture,
    tone: hashTone(user.id),
    jobTitle: user.role ? formatTitleCase(user.role) : 'Member',
    department: user.departmentName ?? 'Not Assigned',
    team: user.teamName ?? 'Not Assigned',
    role: user.role ? user.role.toLowerCase() : 'member',
    employmentType: employmentTypeByMemberType[user.memberType] ?? 'full-time',
    status: memberStatusMap[user.status] ?? 'inactive',
    availability: availabilityByStatus[user.status] ?? 'offline',
    workload: 0,
    lastActive: user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleDateString() : 'Never',
    joinedDate: user.createdAt ?? new Date().toISOString(),
    directReports: 0,
    skills: [],
    currentProjects: 0,
    currentTasks: 0,
    completedTasks: 0,
    taskCompletionRate: 0,
    averageWorkload: 0,
  };
}