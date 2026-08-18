package com.trio.backend.service;

import com.trio.backend.dto.alert.AlertResponse;
import com.trio.backend.dto.alert.AlertSearchCriteria;
import com.trio.backend.dto.alert.CreateAlertCommand;
import com.trio.backend.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for the Alerts module.
 *
 * <p>Alerts are addressed to exactly one recipient user within one workspace.
 * All read operations are strictly scoped to the authenticated user and the
 * requested workspace, guaranteeing tenant + ownership isolation.</p>
 */
public interface AlertService {

    /**
     * Creates an alert from an internal command. Used by the scheduler and
     * business services (tasks, documents, AI, handover). Runs in a
     * {@code REQUIRES_NEW} transaction so alerts persist even when the caller
     * rolls back. Idempotent when a {@code dedupKey} is provided.
     */
    Alert createInternal(CreateAlertCommand command);

    /**
     * Retrieves one of the authenticated user's alerts within a workspace.
     */
    AlertResponse getById(UUID workspaceId, UUID alertId);

    /**
     * Lists the authenticated user's non-archived alerts within a workspace,
     * optionally filtered by status/type/severity.
     */
    Page<AlertResponse> list(UUID workspaceId, UUID recipientId, AlertSearchCriteria criteria, Pageable pageable);

    /**
     * Counts the authenticated user's unread alerts within a workspace.
     */
    long countUnread(UUID workspaceId, UUID recipientId);

    /**
     * Marks one of the authenticated user's alerts as read.
     */
    AlertResponse markAsRead(UUID workspaceId, UUID alertId);

    /**
     * Marks all of the authenticated user's alerts as read within a workspace.
     */
    void markAllAsRead(UUID workspaceId, UUID recipientId);

    /**
     * Dismisses (soft-deletes / archives) one of the authenticated user's alerts.
     */
    void dismiss(UUID workspaceId, UUID alertId);
}
