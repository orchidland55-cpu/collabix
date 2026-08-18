import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { alertService, type AlertSearchCriteria } from './alert-service';

const alertKeys = {
  all: (wsId: string) => ['alerts', wsId] as const,
  filtered: (wsId: string, criteria: AlertSearchCriteria | undefined) =>
    ['alerts', wsId, criteria] as const,
  count: (wsId: string) => ['alerts', 'count', wsId] as const,
};

export function useAlertsList(wsId: string, criteria?: AlertSearchCriteria) {
  return useQuery({
    queryKey: alertKeys.filtered(wsId, criteria),
    queryFn: () => alertService.list(wsId, criteria),
    enabled: !!wsId,
  });
}

export function useAlertUnreadCount(wsId: string) {
  return useQuery({
    queryKey: alertKeys.count(wsId),
    queryFn: () => alertService.unreadCount(wsId),
    enabled: !!wsId,
    refetchInterval: 60_000,
  });
}

export function useAlertMarkAsRead(wsId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (alertId: string) => alertService.markAsRead(wsId, alertId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: alertKeys.all(wsId) });
      qc.invalidateQueries({ queryKey: alertKeys.count(wsId) });
    },
  });
}

export function useAlertMarkAllAsRead(wsId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => alertService.markAllAsRead(wsId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: alertKeys.all(wsId) });
      qc.invalidateQueries({ queryKey: alertKeys.count(wsId) });
    },
  });
}

export function useAlertDismiss(wsId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (alertId: string) => alertService.dismiss(wsId, alertId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: alertKeys.all(wsId) });
      qc.invalidateQueries({ queryKey: alertKeys.count(wsId) });
    },
  });
}