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

    return {
      canGenerateAnalytics: permissions.includes('ANALYTICS_VIEW') && !isMember,
      canGenerateReports: permissions.includes('REPORT_CREATE') && !isMember,
      canReadReports: permissions.includes('REPORT_READ'),
      canGenerateHandover: permissions.includes('HANDOVER_CREATE') && !isMember,
      canReadHandover: permissions.includes('HANDOVER_READ'),
      canUseKnowledgeAI: permissions.includes('KNOWLEDGE_BASE_READ'),
      isAdmin,
      isManager,
      isMember,
      departmentId: user?.departmentId,
    };
  }, [user]);
}
