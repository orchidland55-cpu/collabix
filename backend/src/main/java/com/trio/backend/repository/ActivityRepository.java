package com.trio.backend.repository;

import com.trio.backend.entity.Activity;
import com.trio.backend.enums.ActivityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for Activity.
 *
 * <p>Multi-tenancy is enforced via the chain:
 * Activity -> Task -> Project -> Department -> Workspace.</p>
 */
public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    @Query("""
            select a
            from Activity a
            join fetch a.actor
            join fetch a.task t
            join fetch t.project
            where a.task.id = :taskId
              and a.status = :status
            order by a.createdAt desc
            """)
    Page<Activity> findAllByTask_IdAndStatus(
            @Param("taskId") UUID taskId,
            @Param("status") ActivityStatus status,
            Pageable pageable
    );

    // Project scoped (derive workspace scope through joins)
    @Query("""
            select a
            from Activity a
            join fetch a.actor
            join fetch a.task t
            join fetch t.project
            where a.task.project.id = :projectId
              and a.status = :status
              and a.task.project.department.workspace.id = :workspaceId
            order by a.createdAt desc
            """)
    Page<Activity> findAllByProject_IdAndWorkspace_IdAndStatus(
            @Param("projectId") UUID projectId,
            @Param("workspaceId") UUID workspaceId,
            @Param("status") ActivityStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(a) FROM Activity a
            WHERE a.task.project.id = :projectId
              AND a.status = :status
            """)
    long countByProjectIdAndStatus(
            @Param("projectId") UUID projectId,
            @Param("status") ActivityStatus status
    );

    @Query("""
            SELECT COUNT(a) FROM Activity a
            WHERE a.task.project.id = :projectId
              AND a.status = :status
              AND a.createdAt >= :from
              AND a.createdAt <= :to
            """)
    long countByProjectIdAndStatusAndCreatedAtBetween(
            @Param("projectId") UUID projectId,
            @Param("status") ActivityStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    // Workspace scoped
    @Query("""
            select a
            from Activity a
            where a.task.project.department.workspace.id = :workspaceId
              and a.status = :status
            order by a.createdAt desc
            """)
    Page<Activity> findAllByWorkspace_IdAndStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") ActivityStatus status,
            Pageable pageable
    );

    // Personal Ã¢â‚¬â€ activities by actor, scoped to workspace
    @Query("""
            select a
            from Activity a
            join fetch a.task t
            join fetch t.project
            where a.actor.id = :actorId
              and a.task.project.department.workspace.id = :workspaceId
              and a.status = :status
            order by a.createdAt desc
            """)
    Page<Activity> findAllByActorIdAndWorkspaceIdAndStatus(
            @Param("actorId") UUID actorId,
            @Param("workspaceId") UUID workspaceId,
            @Param("status") ActivityStatus status,
            Pageable pageable
    );

    // Department scoped Ã¢â‚¬â€ activities in projects belonging to a department
    @Query("""
            select a
            from Activity a
            join fetch a.actor
            join fetch a.task t
            join fetch t.project
            where a.task.project.department.id = :departmentId
              and a.task.project.department.workspace.id = :workspaceId
              and a.status = :status
            order by a.createdAt desc
            """)
    Page<Activity> findAllByDepartmentIdAndWorkspaceIdAndStatus(
            @Param("departmentId") UUID departmentId,
            @Param("workspaceId") UUID workspaceId,
            @Param("status") ActivityStatus status,
            Pageable pageable
    );

    /**
     * Count active activities in a department.
     */
    @Query("""
            SELECT COUNT(a) FROM Activity a
            WHERE a.task.project.department.id = :departmentId
              AND a.status = :status
            """)
    long countByDepartmentIdAndStatus(
            @Param("departmentId") UUID departmentId,
            @Param("status") ActivityStatus status
    );

    // ==================== TEAM-SCOPED QUERIES ====================

    /**
     * Paginated activities by a set of actors (team members) in a workspace.
     */
    @Query("""
            select a
            from Activity a
            join fetch a.actor
            where a.actor.id in :actorIds
              and a.task.project.department.workspace.id = :workspaceId
              and a.status = :status
            order by a.createdAt desc
            """)
    Page<Activity> findAllByActorIdInAndWorkspaceIdAndStatus(
            @Param("actorIds") List<UUID> actorIds,
            @Param("workspaceId") UUID workspaceId,
            @Param("status") ActivityStatus status,
            Pageable pageable
    );

    /**
     * Count activities by a set of actors (team members) in a workspace.
     */
    @Query("""
            SELECT COUNT(a) FROM Activity a
            WHERE a.actor.id in :actorIds
              AND a.task.project.department.workspace.id = :workspaceId
              AND a.status = :status
            """)
    long countByActorIdInAndWorkspaceIdAndStatus(
            @Param("actorIds") List<UUID> actorIds,
            @Param("workspaceId") UUID workspaceId,
            @Param("status") ActivityStatus status
    );

    /**
     * Counts the activitÃƒÂ©s of an actor spÃƒÂ©cifique in a workspace.
     */
    @Query("""
            SELECT COUNT(a) FROM Activity a
            WHERE a.actor.id = :actorId
              AND a.task.project.department.workspace.id = :workspaceId
              AND a.status = :status
            """)
    long countByActorIdAndWorkspaceIdAndStatus(
            @Param("actorId") UUID actorId,
            @Param("workspaceId") UUID workspaceId,
            @Param("status") ActivityStatus status
    );

    // ==================== PROJECT-SCOPED QUERIES ====================

    /**
     * Count activities by a specific actor within a project.
     */
    @Query("""
            SELECT COUNT(a) FROM Activity a
            WHERE a.actor.id = :actorId
              AND a.task.project.id = :projectId
              AND a.status = :status
            """)
    long countByActorIdAndProjectIdAndStatus(
            @Param("actorId") UUID actorId,
            @Param("projectId") UUID projectId,
            @Param("status") ActivityStatus status);

    /**
     * Count activities grouped by task within a project, ordered by count descending.
     */
    @Query("""
            SELECT a.actor.id, COUNT(a) FROM Activity a
            WHERE a.task.project.id = :projectId
              AND a.status = :status
            GROUP BY a.actor.id
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> countByActorIdGroupByActorId(
            @Param("projectId") UUID projectId,
            @Param("status") ActivityStatus status);

    @Query("""
            SELECT a.task.id, COUNT(a) FROM Activity a
            WHERE a.task.project.id = :projectId
              AND a.status = :status
            GROUP BY a.task.id
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> countByTaskIdGroupByTaskId(
            @Param("projectId") UUID projectId,
            @Param("status") ActivityStatus status,
            Pageable pageable);

    /**
     * RÃƒÂ©cupÃƒÂ¨re les activitÃƒÂ©s of a workspace with loading de l'acteur (actor).
     *
     * <p>Utilise {@code JOIN FETCH} pour ÃƒÂ©viter le N+1 sur {@code activity.actor}
     * lors du mapping vers les DTOs du dashboard.</p>
     *
     * @param workspaceId the ID of the workspace
     * @param status      the status des activitÃƒÂ©s ÃƒÂ  filtersr
     * @param pageable    les paramÃƒÂ¨tres de pagination
     * @return une page of activitys avec acteur chargÃƒÂ©
     */
    @Query("""
            select a
            from Activity a
            join fetch a.actor
            join fetch a.task t
            join fetch t.project
            where a.task.project.department.workspace.id = :workspaceId
              and a.status = :status
            order by a.createdAt desc
            """)
    Page<Activity> findAllByWorkspaceIdAndStatusWithActor(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") ActivityStatus status,
            Pageable pageable
    );

    /**
     * Counts the activitÃƒÂ©s actives of a workspace.
     *
     * @param workspaceId the ID of the workspace
     * @param status      the status des activitÃƒÂ©s ÃƒÂ  compter
     * @return the namebre of activitys
     */
    @Query("""
            SELECT COUNT(a) FROM Activity a
            WHERE a.task.project.department.workspace.id = :workspaceId
              AND a.status = :status
            """)
    long countByWorkspaceIdAndStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") ActivityStatus status
    );

    @Query(value = """
            SELECT CAST(a.created_at AS date) AS day, COUNT(*) AS cnt
            FROM activities a
            INNER JOIN tasks t ON a.task_id = t.id
            INNER JOIN projects p ON t.project_id = p.id
            INNER JOIN departments d ON p.department_id = d.id
            WHERE d.workspace_id = :workspaceId
              AND a.status = 'ACTIVE'
              AND a.created_at >= :from
              AND a.created_at < :toExclusive
            GROUP BY CAST(a.created_at AS date)
            """, nativeQuery = true)
    List<Object[]> countActiveByWorkspaceIdGroupedByDay(
            @Param("workspaceId") UUID workspaceId,
            @Param("from") Instant from,
            @Param("toExclusive") Instant toExclusive
    );
}
