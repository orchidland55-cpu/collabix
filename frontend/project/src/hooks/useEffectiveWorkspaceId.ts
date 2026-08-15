import { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useWorkspacesList } from '../services/workspace-hooks';

/** Workspace ID from URL query, falling back to the user's first accessible workspace. */
export function useEffectiveWorkspaceId(): string {
  const [searchParams] = useSearchParams();
  const wsFromUrl = searchParams.get('ws') ?? '';
  const { data: workspaces } = useWorkspacesList();

  return useMemo(() => {
    if (wsFromUrl) return wsFromUrl;
    return workspaces?.[0]?.id ?? '';
  }, [wsFromUrl, workspaces]);
}

export function aiPath(path: string, workspaceId: string): string {
  if (!workspaceId) return path;
  const separator = path.includes('?') ? '&' : '?';
  return `${path}${separator}ws=${workspaceId}`;
}
