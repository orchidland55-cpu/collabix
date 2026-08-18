package com.trio.backend.repository;

import com.trio.backend.entity.Alert;
import com.trio.backend.entity.Alert.AlertStatus;
import com.trio.backend.entity.Alert.AlertType;
import com.trio.backend.entity.Alert.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for the {@link Alert} entity.
 *
 * <p>Conventions:</p>
 * <ul>
 *     <li>Every query is scoped to a recipient and workspace to guarantee
 *         tenant + ownership isolation (a user can only ever see their own
 *         alerts).</li>
 *     <li>ARCHIVED alerts are excluded from default queries (soft-delete).</li>
 *     <li>Idempotent creation is supported via {@link #existsByDedupKey}.</li>
 *     <li>Pagination is applied for list operations to ensure performance.</li>
 * </ul>
 */
public interface AlertRepository extends JpaRepository<Alert, UUID>,
        JpaSpecificationExecutor<Alert> {

    // ==================== FIND BY RECIPIENT (ownership) ====================

    /**
     * Retrieves the non-archived alerts of a recipient within a workspace,
     * ordered by creation date descending.
     */
    @Query("SELECT a FROM Alert a " +
            "WHERE a.recipient.id = :recipientId " +
            "AND a.workspace.id = :workspaceId " +
            "AND a.status <> 'ARCHIVED' " +
            "ORDER BY a.createdAt DESC")
    Page<Alert> findByRecipientIdAndWorkspaceId(
            @Param("recipientId") UUID recipientId,
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Retrieves the non-archived alerts of a recipient within a workspace,
     * filtered by status.
     */
    @Query("SELECT a FROM Alert a " +
            "WHERE a.recipient.id = :recipientId " +
            "AND a.workspace.id = :workspaceId " +
            "AND a.status = :status " +
            "ORDER BY a.createdAt DESC")
    Page<Alert> findByRecipientIdAndWorkspaceIdAndStatus(
            @Param("recipientId") UUID recipientId,
            @Param("workspaceId") UUID workspaceId,
            @Param("status") AlertStatus status,
            Pageable pageable
    );

    /**
     * Retrieves the non-archived alerts of a recipient within a workspace,
     * filtered by alert type.
     */
    @Query("SELECT a FROM Alert a " +
            "WHERE a.recipient.id = :recipientId " +
            "AND a.workspace.id = :workspaceId " +
            "AND a.type = :type " +
            "AND a.status <> 'ARCHIVED' " +
            "ORDER BY a.createdAt DESC")
    Page<Alert> findByRecipientIdAndWorkspaceIdAndType(
            @Param("recipientId") UUID recipientId,
            @Param("workspaceId") UUID workspaceId,
            @Param("type") AlertType type,
            Pageable pageable
    );

    /**
     * Retrieves the non-archived alerts of a recipient within a workspace,
     * filtered by alert type and severity.
     */
    @Query("SELECT a FROM Alert a " +
            "WHERE a.recipient.id = :recipientId " +
            "AND a.workspace.id = :workspaceId " +
            "AND a.type = :type " +
            "AND a.severity = :severity " +
            "AND a.status <> 'ARCHIVED' " +
            "ORDER BY a.createdAt DESC")
    Page<Alert> findByRecipientIdAndWorkspaceIdAndTypeAndSeverity(
            @Param("recipientId") UUID recipientId,
            @Param("workspaceId") UUID workspaceId,
            @Param("type") AlertType type,
            @Param("severity") Severity severity,
            Pageable pageable
    );

    /**
     * Retrieves the non-archived alerts of a recipient within a workspace,
     * filtered by severity.
     */
    @Query("SELECT a FROM Alert a " +
            "WHERE a.recipient.id = :recipientId " +
            "AND a.workspace.id = :workspaceId " +
            "AND a.severity = :severity " +
            "AND a.status <> 'ARCHIVED' " +
            "ORDER BY a.createdAt DESC")
    Page<Alert> findByRecipientIdAndWorkspaceIdAndSeverity(
            @Param("recipientId") UUID recipientId,
            @Param("workspaceId") UUID workspaceId,
            @Param("severity") Severity severity,
            Pageable pageable
    );

    // ==================== UNREAD COUNTS ====================

    /**
     * Counts the unread alerts of a recipient within a workspace.
     */
    @Query("SELECT COUNT(a) FROM Alert a " +
            "WHERE a.recipient.id = :recipientId " +
            "AND a.workspace.id = :workspaceId " +
            "AND a.status = 'UNREAD'")
    long countUnreadByRecipientIdAndWorkspaceId(
            @Param("recipientId") UUID recipientId,
            @Param("workspaceId") UUID workspaceId
    );

    /**
     * Counts the non-archived alerts of a recipient within a workspace.
     */
    @Query("SELECT COUNT(a) FROM Alert a " +
            "WHERE a.recipient.id = :recipientId " +
            "AND a.workspace.id = :workspaceId " +
            "AND a.status <> 'ARCHIVED'")
    long countByRecipientIdAndWorkspaceId(
            @Param("recipientId") UUID recipientId,
            @Param("workspaceId") UUID workspaceId
    );

    // ==================== IDEMPOTENCY ====================

    /**
     * Checks whether an alert with the given dedup key already exists.
     * Used by the scheduler to avoid creating duplicate alerts on every run.
     */
    boolean existsByDedupKey(String dedupKey);

    // ==================== MARK AS READ ====================

    /**
     * Marks a specific alert as read, only if it belongs to the recipient
     * and workspace and is currently unread.
     */
    @Modifying
    @Query("UPDATE Alert a " +
            "SET a.status = 'READ', a.readAt = :now " +
            "WHERE a.id = :alertId " +
            "AND a.recipient.id = :recipientId " +
            "AND a.workspace.id = :workspaceId " +
            "AND a.status = 'UNREAD'")
    int markAsRead(
            @Param("alertId") UUID alertId,
            @Param("recipientId") UUID recipientId,
            @Param("workspaceId") UUID workspaceId,
            @Param("now") Instant now
    );

    /**
     * Marks all unread alerts of a recipient within a workspace as read.
     */
    @Modifying
    @Query("UPDATE Alert a " +
            "SET a.status = 'READ', a.readAt = :now " +
            "WHERE a.recipient.id = :recipientId " +
            "AND a.workspace.id = :workspaceId " +
            "AND a.status = 'UNREAD'")
    int markAllAsRead(
            @Param("recipientId") UUID recipientId,
            @Param("workspaceId") UUID workspaceId,
            @Param("now") Instant now
    );

    // ==================== DISMISS / ARCHIVE ====================

    /**
     * Archives (soft-deletes) a specific alert, only if it belongs to the
     * recipient and workspace.
     */
    @Modifying
    @Query("UPDATE Alert a " +
            "SET a.status = 'ARCHIVED' " +
            "WHERE a.id = :alertId " +
            "AND a.recipient.id = :recipientId " +
            "AND a.workspace.id = :workspaceId " +
            "AND a.status <> 'ARCHIVED'")
    int archive(
            @Param("alertId") UUID alertId,
            @Param("recipientId") UUID recipientId,
            @Param("workspaceId") UUID workspaceId
    );

    // ==================== SCHEDULER / CLEANUP ====================

    /**
     * Deletes alerts archived before a given date (purge).
     */
    @Modifying
    @Query("DELETE FROM Alert a WHERE a.status = 'ARCHIVED' AND a.updatedAt < :before")
    int deleteArchivedBefore(@Param("before") Instant before);

    /**
     * Retrieves archived alerts older than a given date.
     */
    @Query("SELECT a FROM Alert a " +
            "WHERE a.status = 'ARCHIVED' " +
            "AND a.updatedAt < :before")
    List<Alert> findArchivedBefore(@Param("before") Instant before);
}
