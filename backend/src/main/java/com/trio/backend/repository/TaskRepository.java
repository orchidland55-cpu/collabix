package com.trio.backend.repository;

import com.trio.backend.entity.Task;
import com.trio.backend.enums.TaskPriority;
import com.trio.backend.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByProject_IdAndStatus(UUID projectId, TaskStatus status);

    Page<Task> findAllByProject_IdAndStatus(UUID projectId, TaskStatus status, Pageable pageable);

    @Query("select t from Task t where t.id = :taskId and t.project.id = :projectId")
    Optional<Task> findByIdAndProject_Id(@Param("taskId") UUID taskId, @Param("projectId") UUID projectId);

    boolean existsByProject_IdAndTitle(UUID projectId, String title);

    boolean existsByIdAndProject_IdAndStatus(UUID taskId, UUID projectId, TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId " +
            "AND LOWER(t.title) LIKE LOWER(CONCAT('%', COALESCE(:search, ''), '%')) " +
            "AND (:statusFilterDisabled = true OR t.status IN :statuses) " +
            "AND (:priority IS NULL OR t.priority = :priority) " +
            "AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId) " +
            "ORDER BY t.updatedAt DESC")
    Page<Task> findFiltered(@Param("projectId") UUID projectId,
                            @Param("search") String search,
                            @Param("statuses") List<TaskStatus> statuses,
                            @Param("statusFilterDisabled") boolean statusFilterDisabled,
                            @Param("priority") TaskPriority priority,
                            @Param("assigneeId") UUID assigneeId,
                            Pageable pageable);

    long countByProject_IdAndStatus(UUID projectId, TaskStatus status);

    @Query("SELECT t.project.id, COUNT(t) FROM Task t WHERE t.project.id IN :projectIds AND t.status = :status GROUP BY t.project.id")
    List<Object[]> countByProjectIdsAndStatus(@Param("projectIds") List<UUID> projectIds, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status = :status AND t.updatedAt >= :from AND t.updatedAt <= :to")
    long countByProjectIdAndStatusAndUpdatedAtBetween(
            @Param("projectId") UUID projectId,
            @Param("status") TaskStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.workspace.id = :workspaceId AND t.status = :status")
    long countByWorkspaceIdAndStatus(@Param("workspaceId") UUID workspaceId, @Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.project.department.workspace.id = :workspaceId AND t.status = :status")
    Page<Task> findAllByWorkspaceIdAndStatus(@Param("workspaceId") UUID workspaceId, @Param("status") TaskStatus status, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.project.department.workspace.id = :workspaceId AND t.status = :status")
    List<Task> findAllByWorkspaceIdAndStatus(@Param("workspaceId") UUID workspaceId, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.workspace.id = :workspaceId AND t.dueAt IS NOT NULL AND t.dueAt < :now AND t.status = :status")
    long countOverdueByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.workspace.id = :workspaceId AND t.dueAt IS NOT NULL AND t.dueAt >= :startOfDay AND t.dueAt <= :endOfDay AND t.status = :status")
    long countDueTodayByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.workspace.id = :workspaceId AND t.dueAt IS NOT NULL AND t.dueAt >= :startOfWeek AND t.dueAt <= :endOfWeek AND t.status = :status")
    long countDueThisWeekByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("startOfWeek") Instant startOfWeek, @Param("endOfWeek") Instant endOfWeek, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.dueAt IS NOT NULL AND t.dueAt < :now AND t.status = :status")
    long countOverdueByProjectId(@Param("projectId") UUID projectId, @Param("now") Instant now, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.id = :departmentId AND t.dueAt IS NOT NULL AND t.dueAt < :now AND t.status = :status")
    long countOverdueByDepartmentId(@Param("departmentId") UUID departmentId, @Param("now") Instant now, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.id = :departmentId AND t.dueAt IS NOT NULL AND t.dueAt >= :startOfDay AND t.dueAt <= :endOfDay AND t.status = :status")
    long countDueTodayByDepartmentId(@Param("departmentId") UUID departmentId, @Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.id = :departmentId AND t.dueAt IS NOT NULL AND t.dueAt >= :startOfWeek AND t.dueAt <= :endOfWeek AND t.status = :status")
    long countDueThisWeekByDepartmentId(@Param("departmentId") UUID departmentId, @Param("startOfWeek") Instant startOfWeek, @Param("endOfWeek") Instant endOfWeek, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.id = :departmentId AND t.status = :status")
    long countByDepartmentIdAndStatus(@Param("departmentId") UUID departmentId, @Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.status = :status")
    List<Task> findAllByProjectIdAndStatus(@Param("projectId") UUID projectId, @Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.status = :status ORDER BY t.updatedAt DESC")
    List<Task> findLatestByProjectIdAndStatus(@Param("projectId") UUID projectId, @Param("status") TaskStatus status, Pageable pageable);

    @Query("SELECT t FROM Task t JOIN FETCH t.project WHERE t.createdBy = :userId AND t.project.department.workspace.id = :workspaceId AND t.status = :status ORDER BY t.updatedAt DESC")
    List<Task> findLatestByCreatedByAndWorkspaceIdAndStatus(@Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId, @Param("status") TaskStatus status, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.createdBy = :userId AND t.project.department.workspace.id = :workspaceId AND t.dueAt IS NOT NULL AND t.dueAt < :now AND t.status = :status")
    long countOverdueByCreatedByAndWorkspaceId(@Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId, @Param("now") Instant now, @Param("status") TaskStatus status);

    long countBySprint_Id(UUID sprintId);
    long countBySprint_IdAndStatus(UUID sprintId, TaskStatus status);

    @Query("SELECT COALESCE(SUM(t.storyPoints), 0) FROM Task t WHERE t.sprint.id = :sprintId")
    int sumStoryPointsBySprint_Id(@Param("sprintId") UUID sprintId);

    @Query("SELECT COALESCE(SUM(t.storyPoints), 0) FROM Task t WHERE t.sprint.id = :sprintId AND t.status = :status")
    int sumStoryPointsBySprint_IdAndStatus(@Param("sprintId") UUID sprintId, @Param("status") TaskStatus status);

    long countBySecurityAudit_Id(UUID securityAuditId);
    long countBySecurityAudit_IdAndStatus(UUID securityAuditId, TaskStatus status);
    long countByMarketingCampaign_Id(UUID marketingCampaignId);
    long countByMarketingCampaign_IdAndStatus(UUID marketingCampaignId, TaskStatus status);

    // =========================================================================
    // Active (non-terminal) task queries
    // A task is "active" when its status is NOT ARCHIVED and NOT CANCELLED.
    // These methods replace calls that used TaskStatus.ACTIVE as a literal
    // filter, because task status can legitimately be TODO, IN_PROGRESS,
    // IN_REVIEW, BLOCKED, or COMPLETED — all of which represent an open,
    // non-archived task.
    // =========================================================================

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.workspace.id = :workspaceId AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countActiveByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countActiveByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    List<Task> findAllActiveByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED) ORDER BY t.updatedAt DESC")
    List<Task> findLatestActiveByProjectId(@Param("projectId") UUID projectId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.dueAt IS NOT NULL AND t.dueAt < :now AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countOverdueActiveByProjectId(@Param("projectId") UUID projectId, @Param("now") Instant now);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.id = :departmentId AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countActiveByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.id = :departmentId AND t.dueAt IS NOT NULL AND t.dueAt < :now AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countOverdueActiveByDepartmentId(@Param("departmentId") UUID departmentId, @Param("now") Instant now);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.id = :departmentId AND t.dueAt IS NOT NULL AND t.dueAt >= :startOfDay AND t.dueAt <= :endOfDay AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countDueTodayByDepartmentId(@Param("departmentId") UUID departmentId, @Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.id = :departmentId AND t.dueAt IS NOT NULL AND t.dueAt >= :startOfWeek AND t.dueAt <= :endOfWeek AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countDueThisWeekByDepartmentId(@Param("departmentId") UUID departmentId, @Param("startOfWeek") Instant startOfWeek, @Param("endOfWeek") Instant endOfWeek);

    @Query("SELECT t.project.id, COUNT(t) FROM Task t WHERE t.project.id IN :projectIds AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED) GROUP BY t.project.id")
    List<Object[]> countActiveByProjectIds(@Param("projectIds") List<UUID> projectIds);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.workspace.id = :workspaceId AND t.dueAt IS NOT NULL AND t.dueAt < :now AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countOverdueByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.workspace.id = :workspaceId AND t.dueAt IS NOT NULL AND t.dueAt >= :startOfDay AND t.dueAt <= :endOfDay AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countDueTodayByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("startOfDay") Instant startOfDay, @Param("endOfDay") Instant endOfDay);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.department.workspace.id = :workspaceId AND t.dueAt IS NOT NULL AND t.dueAt >= :startOfWeek AND t.dueAt <= :endOfWeek AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countDueThisWeekByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("startOfWeek") Instant startOfWeek, @Param("endOfWeek") Instant endOfWeek);

    @Query("SELECT t FROM Task t JOIN FETCH t.project WHERE t.createdBy = :userId AND t.project.department.workspace.id = :workspaceId AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED) ORDER BY t.updatedAt DESC")
    List<Task> findLatestByCreatedByAndWorkspaceId(@Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.createdBy = :userId AND t.project.department.workspace.id = :workspaceId AND t.dueAt IS NOT NULL AND t.dueAt < :now AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countOverdueByCreatedByAndWorkspaceId(@Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId, @Param("now") Instant now);

    @Query("""
            SELECT t FROM Task t
            JOIN FETCH t.project p
            WHERE p.department.workspace.id = :workspaceId
            AND (p.department.id = :departmentId OR t.assignee.id = :userId)
            AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)
            ORDER BY t.updatedAt DESC
            """)
    List<Task> findLatestManagerTasks(@Param("workspaceId") UUID workspaceId,
                                      @Param("departmentId") UUID departmentId,
                                      @Param("userId") UUID userId,
                                      Pageable pageable);

    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.project.department.workspace.id = :workspaceId
            AND (t.project.department.id = :departmentId OR t.assignee.id = :userId)
            AND t.dueAt IS NOT NULL AND t.dueAt < :now
            AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)
            """)
    long countOverdueManagerTasks(@Param("workspaceId") UUID workspaceId,
                                  @Param("departmentId") UUID departmentId,
                                  @Param("userId") UUID userId,
                                  @Param("now") Instant now);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.sprint.id = :sprintId AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countActiveBySprintId(@Param("sprintId") UUID sprintId);

    @Query("SELECT COALESCE(SUM(t.storyPoints), 0) FROM Task t WHERE t.sprint.id = :sprintId AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    int sumActiveStoryPointsBySprintId(@Param("sprintId") UUID sprintId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.securityAudit.id = :securityAuditId AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countActiveBySecurityAuditId(@Param("securityAuditId") UUID securityAuditId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.marketingCampaign.id = :marketingCampaignId AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
    long countActiveByMarketingCampaignId(@Param("marketingCampaignId") UUID marketingCampaignId);

    @Query(value = """
            SELECT CAST(t.created_at AS date) AS day, COUNT(*) AS cnt
            FROM tasks t
            INNER JOIN projects p ON t.project_id = p.id
            INNER JOIN departments d ON p.department_id = d.id
            WHERE d.workspace_id = :workspaceId
              AND t.created_at >= :from
              AND t.created_at < :toExclusive
            GROUP BY CAST(t.created_at AS date)
            """, nativeQuery = true)
    List<Object[]> countCreatedByWorkspaceIdGroupedByDay(
            @Param("workspaceId") UUID workspaceId,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive
    );

    @Query("""
            SELECT t.project.id, t.status, COUNT(t)
            FROM Task t
            WHERE t.project.department.workspace.id = :workspaceId
              AND t.project.status = com.trio.backend.enums.WorkspaceStatus.ACTIVE
              AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)
            GROUP BY t.project.id, t.status
            """)
    List<Object[]> countActiveTasksByProjectAndStatusForWorkspace(@Param("workspaceId") UUID workspaceId);

    // =========================================================================
    // Alert scheduler queries
    // =========================================================================

    /**
     * Finds open tasks with an assignee whose due date has passed, within a
     * workspace. "Open" excludes terminal statuses (ARCHIVED, CANCELLED) and
     * COMPLETED, since completed tasks are no longer overdue.
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.project.department.workspace.id = :workspaceId
              AND t.assignee IS NOT NULL
              AND t.dueAt IS NOT NULL
              AND t.dueAt < :now
              AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED, com.trio.backend.enums.TaskStatus.COMPLETED)
            """)
    List<Task> findOverdueAssignedTasksByWorkspaceId(
            @Param("workspaceId") UUID workspaceId,
            @Param("now") Instant now
    );

    /**
     * Finds open tasks with an assignee whose due date falls within the next
     * alert window, within a workspace.
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.project.department.workspace.id = :workspaceId
              AND t.assignee IS NOT NULL
              AND t.dueAt IS NOT NULL
              AND t.dueAt >= :from
              AND t.dueAt <= :to
              AND t.status NOT IN (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED, com.trio.backend.enums.TaskStatus.COMPLETED)
            """)
    List<Task> findDueSoonAssignedTasksByWorkspaceId(
            @Param("workspaceId") UUID workspaceId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    /**
     * Finds blocked tasks with an assignee within a workspace.
     */
    @Query("""
            SELECT t FROM Task t
            WHERE t.project.department.workspace.id = :workspaceId
              AND t.assignee IS NOT NULL
              AND t.status = com.trio.backend.enums.TaskStatus.BLOCKED
            """)
    List<Task> findBlockedAssignedTasksByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
