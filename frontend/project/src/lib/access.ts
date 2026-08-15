import type { User } from './auth-context';

export type DeptType = 'hr' | 'development' | 'ai' | 'marketing' | 'cybersecurity' | 'generic';

export const SUPER_ADMIN_ROLE = 'SUPER_ADMIN';
export const ADMIN_ROLE = 'ADMIN';
export const MANAGER_ROLE = 'MANAGER';
export const MEMBER_ROLE = 'MEMBER';

export const ADMIN_ROLES: string[] = [SUPER_ADMIN_ROLE, ADMIN_ROLE];

export function isAdmin(roles?: string[] | null): boolean {
  return !!roles?.some((r) => r === ADMIN_ROLE || r === SUPER_ADMIN_ROLE);
}

export function isSuperAdmin(roles?: string[] | null): boolean {
  return !!roles?.some((r) => r === SUPER_ADMIN_ROLE);
}

export function isManager(roles?: string[] | null): boolean {
  return !!roles?.some((r) => r === MANAGER_ROLE);
}

export function isMember(roles?: string[] | null): boolean {
  return !!roles?.some((r) => r === MEMBER_ROLE);
}

export function hasPermission(user: Pick<User, 'permissions'> | null | undefined, permission: string): boolean {
  return !!user?.permissions?.includes(permission);
}

export function detectDeptType(name: string | undefined): DeptType {
  if (!name) return 'generic';
  const n = name.toLowerCase();
  if (/(people|people operations|people opps|human resource|humain|resource\.?s\.? humaines|rh\b|\bhr\b|\brh$|talent|culture|recruit|hiring|personnel|staff)/.test(n)) return 'hr';
  if (/(software|engineering|development|dev|tech|product)/.test(n)) return 'development';
  if (/(ai|artificial intelligence|machine learning|data science|ml|research)/.test(n)) return 'ai';
  if (/(market|growth|brand|sales|communication|content)/.test(n)) return 'marketing';
  if (/(security|cyber|risk|compliance|infosec|soc)/.test(n)) return 'cybersecurity';
  return 'generic';
}

export function deptTypeLabel(type: DeptType): string {
  switch (type) {
    case 'hr': return 'HR';
    case 'development': return 'Development';
    case 'ai': return 'AI';
    case 'marketing': return 'Marketing';
    case 'cybersecurity': return 'Cybersecurity';
    default: return 'Department';
  }
}
