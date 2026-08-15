import { AIAnalyticsPage } from '../../components/ai/business';
import { useEffectiveWorkspaceId } from '../../hooks/useEffectiveWorkspaceId';
import { useAIPermissions } from '../../hooks/useAIPermissions';

export function AnalyticsAIPage() {
  const workspaceId = useEffectiveWorkspaceId();
  const { departmentId } = useAIPermissions();
  return <AIAnalyticsPage workspaceId={workspaceId} departmentId={departmentId ?? ''} />;
}
