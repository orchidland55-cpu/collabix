import { HistoryPage as History } from '../../components/ai/history/HistoryPage';
import { useEffectiveWorkspaceId } from '../../hooks/useEffectiveWorkspaceId';

export function HistoryPage() {
  const workspaceId = useEffectiveWorkspaceId();
  return <History workspaceId={workspaceId || undefined} />;
}
