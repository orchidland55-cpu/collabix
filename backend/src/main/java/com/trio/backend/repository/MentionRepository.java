package com.trio.backend.repository;

import com.trio.backend.entity.Comment;
import com.trio.backend.entity.Mention;
import com.trio.backend.entity.User;
import com.trio.backend.entity.Workspace;
import com.trio.backend.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Mention entity.
 *
 * <p>Conventions:</p>
 * <ul>
 *     <li>All queries filter by ACTIVE status by default.</li>
 *     <li>Workspace scope is validated through the entity chain: Mention -> Comment -> Task -> ... -> Workspace.</li>
 *     <li>Pagination is applied for list operations to ensure performance.</li>
 *     <li>Methods are designed to support future Notification features.</li>
 * </ul>
 */
@Repository
public interface MentionRepository extends JpaRepository<Mention, UUID> {

    void deleteByUser_Id(UUID userId);

    // ==================== CRUD ====================

    /**
     * Find a mention by ID within a specific workspace.
     * Validates workspace scope through the entity chain.
     */
    @Query("""
            SELECT m FROM Mention m
            WHERE m.id = :mentionId
            AND m.comment.task.project.department.workspace.id = :workspaceId
            AND m.status = 'ACTIVE'
            """)
    Optional<Mention> findByIdAndWorkspace(
            @Param("mentionId") UUID mentionId,
            @Param("workspaceId") UUID workspaceId
    );

    // ==================== FIND BY COMMENT ====================

    /**
     * Find all active mentions for a specific comment.
     * Paginated for performance.
     */
    @Query("""
            SELECT m FROM Mention m
            WHERE m.comment.id = :commentId
            AND m.status = 'ACTIVE'
            ORDER BY m.createdAt DESC
            """)
    Page<Mention> findByCommentIdPaginated(
            @Param("commentId") UUID commentId,
            Pageable pageable
    );

    /**
     * Find all active mentions for a specific comment without pagination.
     * Useful for lightweight operations or notification processing.
     */
    @Query("""
            SELECT m FROM Mention m
            WHERE m.comment.id = :commentId
            AND m.status = 'ACTIVE'
            ORDER BY m.createdAt DESC
            """)
    List<Mention> findByCommentId(@Param("commentId") UUID commentId);

    /**
     * Count active mentions in a comment.
     */
    @Query("""
            SELECT COUNT(m) FROM Mention m
            WHERE m.comment.id = :commentId
            AND m.status = 'ACTIVE'
            """)
    long countByCommentId(@Param("commentId") UUID commentId);

    // ==================== FIND BY USER ====================

    /**
     * Find all active mentions for a specific user (mentions that reference this user).
     * Paginated for performance.
     * Useful for user's notification inbox or activity tracking.
     */
    @Query("""
            SELECT m FROM Mention m
            WHERE m.user.id = :userId
            AND m.status = 'ACTIVE'
            AND m.comment.task.project.department.workspace.id = :workspaceId
            ORDER BY m.createdAt DESC
            """)
    Page<Mention> findByUserIdAndWorkspacePaginated(
            @Param("userId") UUID userId,
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Find all active mentions for a specific user without pagination.
     * Useful for notification routing or user activity feeds.
     */
    @Query("""
            SELECT m FROM Mention m
            WHERE m.user.id = :userId
            AND m.status = 'ACTIVE'
            AND m.comment.task.project.department.workspace.id = :workspaceId
            ORDER BY m.createdAt DESC
            """)
    List<Mention> findByUserIdAndWorkspace(
            @Param("userId") UUID userId,
            @Param("workspaceId") UUID workspaceId
    );

    /**
     * Count active mentions for a specific user in a workspace.
     */
    @Query("""
            SELECT COUNT(m) FROM Mention m
            WHERE m.user.id = :userId
            AND m.status = 'ACTIVE'
            AND m.comment.task.project.department.workspace.id = :workspaceId
            """)
    long countByUserIdAndWorkspace(
            @Param("userId") UUID userId,
            @Param("workspaceId") UUID workspaceId
    );

    // ==================== WORKSPACE SCOPE ====================

    /**
     * Find all active mentions within a workspace.
     * Paginated for performance.
     * Useful for workspace-wide audit or analytics.
     */
    @Query("""
            SELECT m FROM Mention m
            WHERE m.comment.task.project.department.workspace.id = :workspaceId
            AND m.status = 'ACTIVE'
            ORDER BY m.createdAt DESC
            """)
    Page<Mention> findByWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Count all active mentions in a workspace.
     */
    @Query("""
            SELECT COUNT(m) FROM Mention m
            WHERE m.comment.task.project.department.workspace.id = :workspaceId
            AND m.status = 'ACTIVE'
            """)
    long countByWorkspace(@Param("workspaceId") UUID workspaceId);

    /**
     * Count all active mentions in a department.
     */
    @Query("""
            SELECT COUNT(m) FROM Mention m
            WHERE m.comment.task.project.department.id = :departmentId
            AND m.status = 'ACTIVE'
            """)
    long countByDepartmentId(@Param("departmentId") UUID departmentId);

    /**
     * Count all active mentions of a set of users in a workspace.
     */
    @Query("""
            SELECT COUNT(m) FROM Mention m
            WHERE m.user.id IN :userIds
            AND m.comment.task.project.department.workspace.id = :workspaceId
            AND m.status = 'ACTIVE'
            """)
    long countByUserIdInAndWorkspaceId(
            @Param("userIds") List<UUID> userIds,
            @Param("workspaceId") UUID workspaceId
    );

    /**
     * Count all active mentions in a project.
     */
    @Query("""
            SELECT COUNT(m) FROM Mention m
            WHERE m.comment.task.project.id = :projectId
            AND m.status = 'ACTIVE'
            """)
    long countByProjectId(@Param("projectId") UUID projectId);

    // ==================== NOTIFICATIONS ====================

    /**
     * Find unsent active mentions for a specific user.
     * Used for notification delivery and batch processing.
     */
    @Query("""
            SELECT m FROM Mention m
            WHERE m.user.id = :userId
            AND m.status = 'ACTIVE'
            AND m.notificationSent = false
            AND m.comment.task.project.department.workspace.id = :workspaceId
            ORDER BY m.createdAt ASC
            """)
    List<Mention> findUnsentMentionsForUser(
            @Param("userId") UUID userId,
            @Param("workspaceId") UUID workspaceId
    );

    /**
     * Find unsent active mentions for a specific user with pagination.
     * Supports batch notification processing.
     */
    @Query("""
            SELECT m FROM Mention m
            WHERE m.user.id = :userId
            AND m.status = 'ACTIVE'
            AND m.notificationSent = false
            AND m.comment.task.project.department.workspace.id = :workspaceId
            ORDER BY m.createdAt ASC
            """)
    Page<Mention> findUnsentMentionsForUserPaginated(
            @Param("userId") UUID userId,
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Count unsent mentions for a user.
     * Useful for notification badges or summary endpoints.
     */
    @Query("""
            SELECT COUNT(m) FROM Mention m
            WHERE m.user.id = :userId
            AND m.status = 'ACTIVE'
            AND m.notificationSent = false
            AND m.comment.task.project.department.workspace.id = :workspaceId
            """)
    long countUnsentMentions(
            @Param("userId") UUID userId,
            @Param("workspaceId") UUID workspaceId
    );

    /**
     * Find unsent active mentions in a workspace.
     * Used for scheduled notification delivery tasks.
     */
    @Query("""
            SELECT m FROM Mention m
            WHERE m.status = 'ACTIVE'
            AND m.notificationSent = false
            AND m.comment.task.project.department.workspace.id = :workspaceId
            ORDER BY m.createdAt ASC
            """)
    List<Mention> findUnsentMentionsInWorkspace(@Param("workspaceId") UUID workspaceId);

    /**
     * Find unsent active mentions in a workspace with pagination.
     * Supports batch processing for notification delivery.
     */
    @Query("""
            SELECT m FROM Mention m
            WHERE m.status = 'ACTIVE'
            AND m.notificationSent = false
            AND m.comment.task.project.department.workspace.id = :workspaceId
            ORDER BY m.createdAt ASC
            """)
    Page<Mention> findUnsentMentionsInWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Count unsent mentions in a workspace.
     * Useful for monitoring notification backlog.
     */
    @Query("""
            SELECT COUNT(m) FROM Mention m
            WHERE m.status = 'ACTIVE'
            AND m.notificationSent = false
            AND m.comment.task.project.department.workspace.id = :workspaceId
            """)
    long countUnsentMentionsInWorkspace(@Param("workspaceId") UUID workspaceId);

    // ==================== DASHBOARD-SPECIFIC QUERIES ====================

    /**
     * Resorteves the mentions actives of a user with loading des relations
     * required pour le Personal Dashboard.
     *
     * <p>Utilise {@code JOIN FETCH} to avoid les N+1 sur :
     * <ul>
     *   <li>{@code mention.user} â€” name de l'acteur</li>
     *   <li>{@code mention.comment.task} â€” context (title de the task)</li>
     * </ul>
     * </p>
     *
     * @param userId      l'identifiant de the user mentionnÃ©
     * @param workspaceId the ID of the workspace (multi-tenant)
     * @return list of mentions avec relations loadedes
     */
    @Query("""
            SELECT m FROM Mention m
            JOIN FETCH m.user
            JOIN FETCH m.comment c
            JOIN FETCH c.task t
            WHERE m.user.id = :userId
            AND m.status = 'ACTIVE'
            AND m.comment.task.project.department.workspace.id = :workspaceId
            ORDER BY m.createdAt DESC
            """)
    List<Mention> findByUserIdAndWorkspaceWithRelations(
            @Param("userId") UUID userId,
            @Param("workspaceId") UUID workspaceId
    );

    // ==================== SOFT DELETE ====================

    /**
     * Soft-delete a mention by setting status to ARCHIVED.
     * Used when a mention needs to be logically removed without losing audit trail.
     */
    @Query("""
            UPDATE Mention m
            SET m.status = 'ARCHIVED'
            WHERE m.id = :mentionId
            AND m.comment.task.project.department.workspace.id = :workspaceId
            """)
    void softDelete(
            @Param("mentionId") UUID mentionId,
            @Param("workspaceId") UUID workspaceId
    );
}