package com.trio.backend.repository;

import com.trio.backend.entity.HandoverJournal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for the HandoverJournal entity.
 *
 * <p>Conventions:</p>
 * <ul>
 *     <li>All queries filter by ACTIVE status by default.</li>
 *     <li>Workspace scope is validated through the entity chain.</li>
 *     <li>Pagination is applied for list operations.</li>
 * </ul>
 */
@Repository
public interface HandoverJournalRepository extends JpaRepository<HandoverJournal, UUID> {

    // ==================== CRUD ====================

    @Query("""
            SELECT hj FROM HandoverJournal hj
            WHERE hj.id = :handoverJournalId
              AND hj.status = 'ACTIVE'
              AND hj.workspace.id = :workspaceId
            """)
    Optional<HandoverJournal> findByIdAndWorkspace(
            @Param("handoverJournalId") UUID handoverJournalId,
            @Param("workspaceId") UUID workspaceId
    );

    @Query("""
            SELECT hj FROM HandoverJournal hj
            WHERE hj.id = :handoverJournalId
              AND hj.status = 'ACTIVE'
              AND hj.workspace.id = :workspaceId
              AND hj.department.id = :departmentId
            """)
    Optional<HandoverJournal> findByIdAndWorkspaceAndDepartment(
            @Param("handoverJournalId") UUID handoverJournalId,
            @Param("workspaceId") UUID workspaceId,
            @Param("departmentId") UUID departmentId
    );

    // ==================== FIND BY WORKSPACE ====================

    @Query("""
            SELECT hj FROM HandoverJournal hj
            WHERE hj.workspace.id = :workspaceId
              AND hj.status = 'ACTIVE'
            ORDER BY hj.journalDate DESC
            """)
    Page<HandoverJournal> findByWorkspaceIdPaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(hj) FROM HandoverJournal hj
            WHERE hj.workspace.id = :workspaceId
              AND hj.status = 'ACTIVE'
            """)
    long countByWorkspace(@Param("workspaceId") UUID workspaceId);

    // ==================== FIND BY DEPARTMENT ====================

    @Query("""
            SELECT hj FROM HandoverJournal hj
            WHERE hj.department.id = :departmentId
              AND hj.status = 'ACTIVE'
            ORDER BY hj.journalDate DESC
            """)
    Page<HandoverJournal> findByDepartmentIdPaginated(
            @Param("departmentId") UUID departmentId,
            Pageable pageable
    );

    /**
     * Department-scoped, filterable journal listing used by the accessible endpoints.
     * <p>All filters are optional; {@code departmentId} is already resolved by the service
     * (locked to the caller's own department unless the caller is a workspace admin).</p>
     */
    @Query("""
            SELECT hj FROM HandoverJournal hj
            WHERE hj.workspace.id = :workspaceId
              AND hj.status = 'ACTIVE'
              AND (:departmentId IS NULL OR hj.department.id = :departmentId)
              AND (:projectId IS NULL OR hj.project.id = :projectId)
              AND (:shift IS NULL OR hj.shift = :shift)
              AND (:from IS NULL OR (hj.journalDate >= :from AND hj.journalDate < :to))
            ORDER BY hj.journalDate DESC, hj.generationDate DESC
            """)
    Page<HandoverJournal> findAccessiblePaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("departmentId") UUID departmentId,
            @Param("projectId") UUID projectId,
            @Param("shift") com.trio.backend.entity.HandoverEntry.Shift shift,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(hj) FROM HandoverJournal hj
            WHERE hj.department.id = :departmentId
              AND hj.status = 'ACTIVE'
            """)
    long countByDepartmentId(@Param("departmentId") UUID departmentId);

    // ==================== FIND BY PROJECT ====================

    @Query("""
            SELECT hj FROM HandoverJournal hj
            WHERE hj.project.id = :projectId
              AND hj.status = 'ACTIVE'
            ORDER BY hj.journalDate DESC
            """)
    Page<HandoverJournal> findByProjectIdPaginated(
            @Param("projectId") UUID projectId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(hj) FROM HandoverJournal hj
            WHERE hj.project.id = :projectId
              AND hj.status = 'ACTIVE'
            """)
    long countByProjectId(@Param("projectId") UUID projectId);

    // ==================== FIND BY DATE (RANGE) ====================

    @Query("""
            SELECT hj FROM HandoverJournal hj
            WHERE hj.workspace.id = :workspaceId
              AND hj.journalDate >= :from
              AND hj.journalDate <= :to
              AND hj.status = 'ACTIVE'
            ORDER BY hj.journalDate DESC
            """)
    Page<HandoverJournal> findByWorkspaceAndJournalDateBetweenPaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(hj) FROM HandoverJournal hj
            WHERE hj.workspace.id = :workspaceId
              AND hj.journalDate >= :from
              AND hj.journalDate <= :to
              AND hj.status = 'ACTIVE'
            """)
    long countByWorkspaceAndJournalDateBetween(
            @Param("workspaceId") UUID workspaceId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // ==================== STATISTICS / ANALYTICS ====================

    @Query("""
            SELECT COUNT(hj) FROM HandoverJournal hj
            WHERE hj.workspace.id = :workspaceId
              AND hj.generationStatus = :generationStatus
              AND hj.status = 'ACTIVE'
            """)
    long countByWorkspaceAndGenerationStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("generationStatus") HandoverJournal.GenerationStatus generationStatus
    );

    @Query("""
            SELECT hj FROM HandoverJournal hj
            WHERE hj.workspace.id = :workspaceId
              AND hj.generationStatus = :generationStatus
              AND hj.status = 'ACTIVE'
            ORDER BY hj.generationDate DESC NULLS LAST
            """)
    Page<HandoverJournal> findByWorkspaceAndGenerationStatusPaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("generationStatus") HandoverJournal.GenerationStatus generationStatus,
            Pageable pageable
    );

    // ==================== EXISTS BY PROJECT + DATE RANGE ====================

    @Query("""
            SELECT CASE WHEN COUNT(hj) > 0 THEN TRUE ELSE FALSE END
            FROM HandoverJournal hj
            WHERE hj.project.id = :projectId
              AND hj.journalDate >= :from
              AND hj.journalDate <= :to
              AND hj.status = 'ACTIVE'
            """)
    boolean existsByProjectIdAndJournalDateBetween(
            @Param("projectId") UUID projectId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT hj FROM HandoverJournal hj
            WHERE hj.project.id = :projectId
              AND hj.journalDate >= :from
              AND hj.journalDate < :to
              AND hj.status = 'ACTIVE'
            ORDER BY hj.generationDate DESC
            """)
    List<HandoverJournal> findByProjectIdAndJournalDateBetween(
            @Param("projectId") UUID projectId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT hj FROM HandoverJournal hj
            WHERE hj.project.id = :projectId
              AND hj.journalDate >= :from
              AND hj.journalDate < :to
              AND hj.status = 'ACTIVE'
            ORDER BY hj.generationDate DESC
            LIMIT 1
            """)
    Optional<HandoverJournal> findActiveByProjectIdAndJournalDate(
            @Param("projectId") UUID projectId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // ==================== SOFT DELETE (status updates) ====================

    @Query("""
            UPDATE HandoverJournal hj
            SET hj.status = 'DELETED'
            WHERE hj.id = :handoverJournalId
              AND hj.workspace.id = :workspaceId
            """)
    void softDelete(
            @Param("handoverJournalId") UUID handoverJournalId,
            @Param("workspaceId") UUID workspaceId
    );

    @Query("""
            UPDATE HandoverJournal hj
            SET hj.status = 'ARCHIVED'
            WHERE hj.id = :handoverJournalId
              AND hj.workspace.id = :workspaceId
            """)
    void archive(
            @Param("handoverJournalId") UUID handoverJournalId,
            @Param("workspaceId") UUID workspaceId
    );
}
