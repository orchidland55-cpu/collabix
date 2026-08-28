import { useMemo } from 'react';
import { useAuth } from '../lib/auth-context';

export function useAIPermissions() {
  const { user } = useAuth();

  return useMemo(() => {
    const roles = user?.roles ?? [];
    const permissions = user?.permissions ?? [];
    const isAdmin = roles.some((r) => r === 'ADMIN' || r === 'SUPER_ADMIN');
    const isManager = roles.includes('MANAGER');
    const isMember = roles.includes('MEMBER') && !isAdmin && !isManager;

    // Visibility (FEATURE VISIBILITY): driven by the permission the user
    // actually holds. A Member can SEE a feature they are permitted to view
    // even if they are not allowed to perform mutating actions.
    const canViewAnalytics = permissions.includes('ANALYTICS_VIEW');
    const canViewReports = permissions.includes('REPORT_READ');
    const canViewHandover = permissions.includes('HANDOVER_READ');
    const canUseKnowledgeAI = permissions.includes('KNOWLEDGE_BASE_READ');

    // Action authorization (ACTION PERMISSIONS): Members are blocked by the
    // backend from generating AI analytics/reports/handover, so the generate
    // actions are disabled for them. Visibility is kept separate from this.
    const canGenerateAnalytics = canViewAnalytics && !isMember;
    const canGenerateReports = permissions.includes('REPORT_CREATE') && !isMember;
    const canGenerateHandover = permissions.includes('HANDOVER_CREATE') && !isMember;

    return {
      canViewAnalytics,
      canViewReports,
      canViewHandover,
      canUseKnowledgeAI,
      canGenerateAnalytics,
      canGenerateReports,
      canGenerateHandover,
      canReadHandover: canViewHandover,
      canReadReports: canViewReports,
      isAdmin,
      isManager,
      isMember,
      departmentId: user?.departmentId,
    };
  }, [user]);
}
