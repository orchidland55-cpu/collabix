import { Navigate } from 'react-router-dom';
import { useWorkspacesList } from '../../services/workspace-hooks';
import { useWorkspaceId } from '../../hooks/useWorkspaceId';
import { useAuth } from '../../lib/auth-context';
import { isAdmin } from '../../lib/access';
import { onAuthEvent, type AuthEvent } from '../../lib/auth-events';
import { AlertCircle, RefreshCw } from 'lucide-react';
import { Button } from '../ui/Button';
import { Skeleton } from '../ui/Skeleton';
import type { ReactNode } from 'react';
import { useEffect } from 'react';

const WORKSPACE_ROUTES = new Set([
  'all-workspaces', 'create-workspace', 'edit-workspace',
]);

/** Routes that absolutely require a selected workspace context */
const WS_DEPENDENT_ROUTES = new Set([
  'workspace', 'workspace-overview', 'projects', 'tasks',
  'collaboration', 'documents', 'knowledge', 'handover', 'handover-entries',
  'notifications', 'alerts', 'reports', 'activity', 'calendar', 'archived-projects',
  'organization', 'departments', 'teams', 'members',
  'settings', 'ai',
]);

export function WorkspaceGuard({ children, routeKey }: { children: ReactNode; routeKey: string }) {
  const workspaceId = useWorkspaceId();
  const { user } = useAuth();
  const isAdminUser = isAdmin(user?.roles ?? []);
  const { data: workspaces, isLoading, isError, refetch } = useWorkspacesList();

  // Refetch workspaces automatically after a successful token refresh
  useEffect(() => {
    const unsubscribe = onAuthEvent((event: AuthEvent) => {
      if (event.type === 'token-refreshed') {
        refetch();
      }
    });
    return unsubscribe;
  }, [refetch]);

  // Skip guard for workspace management pages
  if (WORKSPACE_ROUTES.has(routeKey)) return <>{children}</>;

  // Allow if a workspace is already selected in URL
  if (workspaceId) return <>{children}</>;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Skeleton className="h-8 w-48" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-4">
        <AlertCircle className="h-12 w-12 text-danger-500" />
        <p className="text-body font-medium text-text-primary">Failed to reload workspace</p>
        <p className="text-caption text-text-tertiary">Could not retrieve workspace data. This may be due to an expired session.</p>
        <Button variant="primary" leftIcon={<RefreshCw />} onClick={() => refetch()}>Retry</Button>
      </div>
    );
  }

  // No workspaces at all
  if (!workspaces || workspaces?.length === 0) {
    // If on a workspace-dependent route, redirect to all-workspaces (admin) or the role-aware dashboard
    if (WS_DEPENDENT_ROUTES.has(routeKey)) {
      return <Navigate to={isAdminUser ? '/app/all-workspaces' : '/app/dashboard'} replace />;
    }
    return <>{children}</>;
  }

  // Has workspaces but none selected - let the selector handle auto-select
  return <>{children}</>;
}
