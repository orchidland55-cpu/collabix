import { AIKnowledgePage } from '../../components/ai/business';
import { useEffectiveWorkspaceId } from '../../hooks/useEffectiveWorkspaceId';
import { useAIPermissions } from '../../hooks/useAIPermissions';

export function KnowledgeAIPage() {
  const workspaceId = useEffectiveWorkspaceId();
  const { departmentId } = useAIPermissions();
  return <AIKnowledgePage workspaceId={workspaceId} departmentId={departmentId ?? ''} />;
}
