import { QueryClient } from '@tanstack/react-query';

/**
 * Shared QueryClient instance. Created at the module level so the auth layer
 * can clear the cache when the authenticated user changes. React Query keys
 * are derived from workspace/business ids — never the userId — so without an
 * explicit clear() the dashboard/notification data of the previous user can
 * leak into the next session (staleTime 30s, gasTime 10min).
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 10 * 60 * 1000,
      retry: 2,
      refetchOnWindowFocus: false,
    },
  },
});