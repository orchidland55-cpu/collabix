package com.trio.backend.repository;

import com.trio.backend.entity.HandoverEntry;
import com.trio.backend.entity.HandoverEntry.HandoverStatus;
import com.trio.backend.entity.HandoverEntry.Priority;
import com.trio.backend.entity.HandoverEntry.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for the HandoverEntry entity (workflow model).
 *
 * <p>Conventions:</p>
 * <ul>
 *     <li>All queries exclude soft-deleted entries by default.</li>
 *     <li>Workspace scope is validated through the entity chain.</li>
 *     <li>Pagination is applied for list operations.</li>
 * </ul>
 */
@Repository
public interface HandoverEntryRepository extends JpaRepository<HandoverEntry, UUID> {

    // ==================== CRUD (scoped) ====================

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.id = :id
              AND he.workspace.id = :workspaceId
              AND he.deleted = false
            """)
    Optional<HandoverEntry> findByIdAndWorkspace(
            @Param("id") UUID id,
            @Param("workspaceId") UUID workspaceId
    );

    // ==================== FIND BY WORKSPACE ====================

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.deleted = false
            ORDER BY he.updatedAt DESC
            """)
    Page<HandoverEntry> findByWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.status = :status
              AND he.deleted = false
            ORDER BY he.updatedAt DESC
            """)
    Page<HandoverEntry> findByWorkspaceAndStatusPaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") HandoverStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.status IN :statuses
              AND he.deleted = false
            ORDER BY he.updatedAt DESC
            """)
    Page<HandoverEntry> findByWorkspaceAndStatusInPaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("statuses") List<HandoverStatus> statuses,
            Pageable pageable
    );

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.priority = :priority
              AND he.deleted = false
            ORDER BY he.updatedAt DESC
            """)
    Page<HandoverEntry> findByWorkspaceAndPriorityPaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("priority") Priority priority,
            Pageable pageable
    );

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.project.id = :projectId
              AND he.deleted = false
            ORDER BY he.updatedAt DESC
            """)
    Page<HandoverEntry> findByWorkspaceAndProjectPaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("projectId") UUID projectId,
            Pageable pageable
    );

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND (:status IS NULL OR he.status = :status)
              AND (:priority IS NULL OR he.priority = :priority)
              AND (:projectId IS NULL OR he.project.id = :projectId)
              AND he.deleted = false
            ORDER BY he.updatedAt DESC
            """)
    Page<HandoverEntry> search(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") HandoverStatus status,
            @Param("priority") Priority priority,
            @Param("projectId") UUID projectId,
            Pageable pageable
    );

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.department.id = :departmentId
              AND (:status IS NULL OR he.status = :status)
              AND (:priority IS NULL OR he.priority = :priority)
              AND (:projectId IS NULL OR he.project.id = :projectId)
              AND he.deleted = false
            ORDER BY he.updatedAt DESC
            """)
    Page<HandoverEntry> searchByDepartment(
            @Param("workspaceId") UUID workspaceId,
            @Param("departmentId") UUID departmentId,
            @Param("status") HandoverStatus status,
            @Param("priority") Priority priority,
            @Param("projectId") UUID projectId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(he) FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.deleted = false
            """)
    long countByWorkspace(@Param("workspaceId") UUID workspaceId);

    // ==================== INBOX / SENT ====================

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.receiver.id = :userId
              AND he.status <> 'DRAFT'
              AND he.deleted = false
            ORDER BY he.updatedAt DESC
            """)
    Page<HandoverEntry> findInboxPaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.sender.id = :userId
              AND he.deleted = false
            ORDER BY he.updatedAt DESC
            """)
    Page<HandoverEntry> findSentPaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(he) FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.receiver.id = :userId
              AND he.status = 'PENDING'
              AND he.deleted = false
            """)
    long countPendingForReceiver(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId
    );

    // ==================== MY ENTRIES (daily reports) ====================

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.sender.id = :userId
              AND (:status IS NULL OR he.status = :status)
              AND (:shift IS NULL OR he.shift = :shift)
              AND (:entryDate IS NULL OR he.entryDate = :entryDate)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(COALESCE(he.title, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(he.completedTasks, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(he.currentProgress, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(he.pendingTasks, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(he.importantNotes, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND he.deleted = false
            ORDER BY he.updatedAt DESC
            """)
    Page<HandoverEntry> findMine(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId,
            @Param("status") HandoverStatus status,
            @Param("shift") Shift shift,
            @Param("entryDate") LocalDate entryDate,
            @Param("search") String search,
            Pageable pageable
    );

    // ==================== JOURNAL AGGREGATION ====================

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.department.id = :departmentId
              AND he.status = 'SUBMITTED'
              AND (:entryDate IS NULL OR he.entryDate = :entryDate)
              AND (:shift IS NULL OR he.shift = :shift)
              AND he.deleted = false
            ORDER BY he.entryDate ASC, he.createdAt ASC
            """)
    List<HandoverEntry> findSubmittedByDepartmentIdAndEntryDate(
            @Param("workspaceId") UUID workspaceId,
            @Param("departmentId") UUID departmentId,
            @Param("entryDate") LocalDate entryDate,
            @Param("shift") Shift shift
    );

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.project.id = :projectId
              AND he.createdAt >= :from
              AND he.createdAt <= :to
              AND he.deleted = false
            ORDER BY he.createdAt DESC
            """)
    List<HandoverEntry> findByProjectIdAndCreatedAtBetween(
            @Param("projectId") UUID projectId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.project.id = :projectId
              AND he.updatedAt >= :from
              AND he.updatedAt <= :to
              AND he.deleted = false
            ORDER BY he.updatedAt DESC
            """)
    List<HandoverEntry> findByProjectIdAndUpdatedAtBetween(
            @Param("projectId") UUID projectId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    // ==================== DASHBOARD / REMINDERS ====================

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND (he.sender.id = :userId OR he.receiver.id = :userId)
              AND he.createdAt >= :from
              AND he.createdAt <= :to
              AND he.deleted = false
            ORDER BY he.createdAt DESC
            """)
    List<HandoverEntry> findForUserBetween(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT he FROM HandoverEntry he
            WHERE he.workspace.id = :workspaceId
              AND he.status = 'PENDING'
              AND he.dueDate IS NOT NULL
              AND he.dueDate <= :soon
              AND he.deleted = false
            ORDER BY he.dueDate ASC
            """)
    List<HandoverEntry> findPendingDueBefore(
            @Param("workspaceId") UUID workspaceId,
            @Param("soon") LocalDateTime soon
    );
}
