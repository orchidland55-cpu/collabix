import { AIReportPage } from '../../components/ai/business';
import { useEffectiveWorkspaceId } from '../../hooks/useEffectiveWorkspaceId';
import { useAIPermissions } from '../../hooks/useAIPermissions';

export function ReportAIPage() {
  const workspaceId = useEffectiveWorkspaceId();
  const { departmentId } = useAIPermissions();
  return <AIReportPage workspaceId={workspaceId} departmentId={departmentId ?? ''} />;
}
