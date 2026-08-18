import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../lib/auth-context';
import { getApiBaseUrl } from '../lib/api-base';

/**
 * Opens a WebSocket to /ws/notifications?token=<accessToken> and invalidates
 * the notification queries whenever the server pushes a new notification.
 * The backend authenticates the connection from the signed access token and
 * derives the recipient user id, ignoring any client-supplied user id.
 */
export function useNotificationSocket() {
  const { accessToken, isAuthenticated } = useAuth();
  const qc = useQueryClient();

  useEffect(() => {
    if (!isAuthenticated || !accessToken) return;

    const wsProto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const apiBase = getApiBaseUrl();
    const wsHost = apiBase.startsWith('http')
      ? apiBase.replace(/\/api\/?$/, '').replace(/^https?:\/\//, '')
      : window.location.host;
    const url = `${wsProto}://${wsHost}/ws/notifications?token=${encodeURIComponent(accessToken)}`;
    const socket = new WebSocket(url);

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(String(event.data));
        const wsId: string | undefined = payload?.workspaceId;
        if (wsId) {
          qc.invalidateQueries({ queryKey: ['notifications', wsId] });
          qc.invalidateQueries({ queryKey: ['notifications', 'unread', wsId] });
          qc.invalidateQueries({ queryKey: ['notifications', 'count', wsId] });
        }
        qc.invalidateQueries({ queryKey: ['notifications', 'count'] });
      } catch {
        // ignore malformed payloads
      }
    };

    return () => {
      socket.close();
    };
  }, [accessToken, isAuthenticated, qc]);
}
