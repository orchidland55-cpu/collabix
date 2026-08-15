import { AIHandoverPage } from '../../components/ai/business';
import { useEffectiveWorkspaceId } from '../../hooks/useEffectiveWorkspaceId';
import { useAIPermissions } from '../../hooks/useAIPermissions';

export function HandoverAIPage() {
  const workspaceId = useEffectiveWorkspaceId();
  const { departmentId } = useAIPermissions();
  return <AIHandoverPage workspaceId={workspaceId} departmentId={departmentId ?? ''} />;
}
