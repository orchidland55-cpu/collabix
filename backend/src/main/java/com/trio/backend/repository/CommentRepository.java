package com.trio.backend.repository;

import com.trio.backend.entity.Comment;
import com.trio.backend.enums.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {


    /**
     * Resorteves all the comments of a task.
     */
    List<Comment> findAllByTask_IdAndStatus(
            UUID taskId,
            CommentStatus status
    );

/**
     * Resorteves all the comments actives of a project, ordered by creation date descending.
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.task.project.id = :projectId " +
            "AND c.status = :status " +
            "ORDER BY c.createdAt DESC")
    List<Comment> findAllByProjectIdAndStatus(
            @Param("projectId") UUID projectId,
            @Param("status") CommentStatus status
    );

    /**
     * Resorteves the comments of an author (createdBy) in a workspace, ordered by creation date descending.
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.createdBy = :createdBy " +
            "AND c.task.project.department.workspace.id = :workspaceId " +
            "AND c.status = :status " +
            "ORDER BY c.createdAt DESC")
    List<Comment> findByCreatedByAndWorkspaceIdAndStatus(
            @Param("createdBy") UUID createdBy,
            @Param("workspaceId") UUID workspaceId,
            @Param("status") CommentStatus status
    );

    /**
     * Resorteves all the comments of a task with pagination.
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.task.id = :taskId " +
            "AND c.status = :status")
    Page<Comment> findAllByTask_IdAndStatus(
            @Param("taskId") UUID taskId,
            @Param("status") CommentStatus status,
            Pageable pageable
    );


    /**
     * Resorteves ae comment by ID en verifying that it belong Ã  la task.
     */
    @Query("select c from Comment c where c.id = :commentId and c.task.id = :taskId")
    Optional<Comment> findByIdAndTask_Id(
            @Param("commentId") UUID commentId,
            @Param("taskId") UUID taskId
    );

    /**
     * Checks existence of a comment by IDs et status.
     */
    boolean existsByIdAndTask_IdAndStatus(UUID commentId, UUID taskId, CommentStatus status);

    /**
     * Compte the comments of a task.
     */
    long countByTask_IdAndStatus(UUID taskId, CommentStatus status);

    /**
     * Compte the comments of a workspace (via task -> project -> department -> workspace).
     */
    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.task.project.department.workspace.id = :workspaceId " +
            "AND c.status = :status")
    long countByWorkspaceIdAndStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") CommentStatus status
    );

    /**
     * Compte the comments of a department (via task -> project -> department).
     */
    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.task.project.department.id = :departmentId " +
            "AND c.status = :status")
    long countByDepartmentIdAndStatus(
            @Param("departmentId") UUID departmentId,
            @Param("status") CommentStatus status
    );

    /**
     * Compte the comments created par un ensemble of users in a workspace.
     */
    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.createdBy IN :userIds " +
            "AND c.task.project.department.workspace.id = :workspaceId " +
            "AND c.status = :status")
    long countByCreatedByInAndWorkspaceIdAndStatus(
            @Param("userIds") List<UUID> userIds,
            @Param("workspaceId") UUID workspaceId,
            @Param("status") CommentStatus status
    );

    /**
     * Compte the comments of a user specific in a workspace.
     */
    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.createdBy = :userId " +
            "AND c.task.project.department.workspace.id = :workspaceId " +
            "AND c.status = :status")
    long countByCreatedByAndWorkspaceIdAndStatus(
            @Param("userId") UUID userId,
            @Param("workspaceId") UUID workspaceId,
            @Param("status") CommentStatus status
    );

    /**
     * Compte the comments of a project (via task -> project).
     */
    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.task.project.id = :projectId " +
            "AND c.status = :status")
    long countByProjectIdAndStatus(
            @Param("projectId") UUID projectId,
            @Param("status") CommentStatus status
    );

    /**
     * Compte the comments of a project created entre deux dates.
     */
    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.task.project.id = :projectId " +
            "AND c.status = :status " +
            "AND c.createdAt >= :from " +
            "AND c.createdAt <= :to")
    long countByProjectIdAndStatusAndCreatedAtBetween(
            @Param("projectId") UUID projectId,
            @Param("status") CommentStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    /**
     * Count comments grouped by task within a project, ordered by count descending.
     */
    @Query("SELECT c.task.id, COUNT(c) FROM Comment c " +
            "WHERE c.task.project.id = :projectId " +
            "AND c.status = :status " +
            "GROUP BY c.task.id " +
            "ORDER BY COUNT(c) DESC")
    List<Object[]> countByTaskIdGroupByTaskId(
            @Param("projectId") UUID projectId,
            @Param("status") CommentStatus status,
            Pageable pageable);

    /**
     * Resorteves the paginated comments of a project.
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.task.project.id = :projectId " +
            "AND c.status = :status " +
            "ORDER BY c.createdAt DESC")
    Page<Comment> findAllByProjectIdAndStatus(
            @Param("projectId") UUID projectId,
            @Param("status") CommentStatus status,
            Pageable pageable
    );

    /**
     * Resorteves all the comments of a workspace with pagination.
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.task.project.department.workspace.id = :workspaceId " +
            "AND c.status = :status")
    Page<Comment> findAllByWorkspaceIdAndStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") CommentStatus status,
            Pageable pageable
    );


    /**
     * Resorteves all the comments actives of a workspace.
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.task.project.department.workspace.id = :workspaceId " +
            "AND c.status = :status")
    List<Comment> findAllByWorkspaceIdAndStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") CommentStatus status
    );

    @Query(value = """
            SELECT CAST(c.created_at AS date) AS day, COUNT(*) AS cnt
            FROM comments c
            INNER JOIN tasks t ON c.task_id = t.id
            INNER JOIN projects p ON t.project_id = p.id
            INNER JOIN departments d ON p.department_id = d.id
            WHERE d.workspace_id = :workspaceId
              AND c.status = 'ACTIVE'
              AND c.created_at >= :from
              AND c.created_at < :toExclusive
            GROUP BY CAST(c.created_at AS date)
            """, nativeQuery = true)
    List<Object[]> countActiveByWorkspaceIdGroupedByDay(
            @Param("workspaceId") UUID workspaceId,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive
    );

}

