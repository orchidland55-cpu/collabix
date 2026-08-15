package com.trio.backend.repository;

import com.trio.backend.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Document entity.
 *
 * <p>Conventions:</p>
 * <ul>
 *     <li>All queries filter by ACTIVE status by default.</li>
 *     <li>Workspace scope is validated through the entity chain: Document -> Project -> Department -> Workspace.</li>
 *     <li>Pagination is applied for list operations to ensure performance.</li>
 *     <li>Methods are designed to support future statistics and dashboard features.</li>
 * </ul>
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // ==================== CRUD ====================

    /**
     * Find a document by ID within a specific workspace.
     * Validates workspace scope through the entity chain.
     */
    @Query("""
            SELECT d FROM Document d
            JOIN FETCH d.project p
            JOIN FETCH p.department dept
            JOIN FETCH dept.workspace w
            WHERE d.id = :documentId
            AND w.id = :workspaceId
            AND d.status IN ('ACTIVE', 'ARCHIVED')
            """)
    Optional<Document> findByIdAndWorkspace(
            @Param("documentId") UUID documentId,
            @Param("workspaceId") UUID workspaceId
    );

    // ==================== FIND BY PROJECT ====================

    /**
     * Find all active documents for a specific project.
     * Paginated for performance.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.project.id = :projectId
            AND d.task IS NULL
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findByProjectIdPaginated(
            @Param("projectId") UUID projectId,
            Pageable pageable
    );

    /**
     * Find all active documents for a specific project without pagination.
     * Useful for lightweight operations or export.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.project.id = :projectId
            AND d.task IS NULL
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    List<Document> findByProjectId(@Param("projectId") UUID projectId);

    /**
     * Count active documents at project level (task IS NULL).
     * Useful for project summary views and validation.
     */
    @Query("""
            SELECT COUNT(d) FROM Document d
            WHERE d.project.id = :projectId
            AND d.task IS NULL
            AND d.status = 'ACTIVE'
            """)
    long countByProjectId(@Param("projectId") UUID projectId);

    /**
     * Count active documents created between two dates at project level.
     */
    @Query("""
            SELECT COUNT(d) FROM Document d
            WHERE d.project.id = :projectId
            AND d.task IS NULL
            AND d.status = 'ACTIVE'
            AND d.createdAt >= :from
            AND d.createdAt <= :to
            """)
    long countByProjectIdAndCreatedAtBetween(
            @Param("projectId") UUID projectId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    /**
     * Calculate total size of active documents at project level.
     * Useful for storage quota management and reporting.
     */
    @Query("""
            SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d
            WHERE d.project.id = :projectId
            AND d.task IS NULL
            AND d.status = 'ACTIVE'
            """)
    Long getTotalSizeByProjectId(@Param("projectId") UUID projectId);

    // ==================== FIND BY TASK ====================

    /**
     * Find all active documents for a specific task.
     * Paginated for performance.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.task.id = :taskId
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findByTaskIdPaginated(
            @Param("taskId") UUID taskId,
            Pageable pageable
    );

    /**
     * Find all active documents for a specific task without pagination.
     * Useful for lightweight operations or export.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.task.id = :taskId
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    List<Document> findByTaskId(@Param("taskId") UUID taskId);

    /**
     * Count active documents in a task.
     * Useful for task summary views and validation.
     */
    @Query("""
            SELECT COUNT(d) FROM Document d
            WHERE d.task.id = :taskId
            AND d.status = 'ACTIVE'
            """)
    long countByTaskId(@Param("taskId") UUID taskId);

    /**
     * Calculate total size of active documents in a task.
     * Useful for storage quota management and reporting.
     */
    @Query("""
            SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d
            WHERE d.task.id = :taskId
            AND d.status = 'ACTIVE'
            """)
    Long getTotalSizeByTaskId(@Param("taskId") UUID taskId);

    // ==================== WORKSPACE SCOPE ====================

    /**
     * Find all active documents within a workspace.
     * Paginated for performance.
     * Useful for workspace-wide audit or analytics.
     */
    @Query("""
            SELECT d FROM Document d
            JOIN FETCH d.project
            WHERE d.project.department.workspace.id = :workspaceId
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findByWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Count all active documents in a workspace.
     * Useful for quota checks and analytics.
     */
    @Query("""
            SELECT COUNT(d) FROM Document d
            WHERE d.project.department.workspace.id = :workspaceId
            AND d.status = 'ACTIVE'
            """)
    long countByWorkspace(@Param("workspaceId") UUID workspaceId);

    /**
     * Calculate total size of all active documents in a workspace.
     * Essential for storage quota management.
     */
    @Query("""
            SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d
            WHERE d.project.department.workspace.id = :workspaceId
            AND d.status = 'ACTIVE'
            """)
    Long getTotalSizeByWorkspace(@Param("workspaceId") UUID workspaceId);

    // ==================== SEARCH BY NAME ====================

    /**
     * Search documents by title within a project.
     * Case-insensitive partial matching.
     * Paginated for performance.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.project.id = :projectId
            AND LOWER(d.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    Page<Document> searchByTitleInProjectPaginated(
            @Param("projectId") UUID projectId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * Search documents by title within a workspace.
     * Case-insensitive partial matching.
     * Paginated for performance.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.project.department.workspace.id = :workspaceId
            AND LOWER(d.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    Page<Document> searchByTitleInWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * Search documents by fileName within a project.
     * Case-insensitive partial matching.
     * Paginated for performance.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.project.id = :projectId
            AND LOWER(d.fileName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    Page<Document> searchByFileNameInProjectPaginated(
            @Param("projectId") UUID projectId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * Search documents by fileName within a workspace.
     * Case-insensitive partial matching.
     * Paginated for performance.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.project.department.workspace.id = :workspaceId
            AND LOWER(d.fileName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    Page<Document> searchByFileNameInWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    // ==================== STATISTICS & DASHBOARD ====================

    /**
     * Find all active documents for a specific department.
     * Supports department-level dashboard and analytics.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.project.department.id = :departmentId
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findByDepartmentIdPaginated(
            @Param("departmentId") UUID departmentId,
            Pageable pageable
    );

    /**
     * Find all active documents for a specific department with the owning
     * project eagerly fetched (avoids N+1 on {@code d.project}).
     */
    @Query("""
            SELECT d FROM Document d
            JOIN FETCH d.project
            WHERE d.project.department.id = :departmentId
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findRecentByDepartmentIdPaginated(
            @Param("departmentId") UUID departmentId,
            Pageable pageable
    );

    /**
     * Count active documents in a department.
     * Useful for department dashboard metrics.
     */
    @Query("""
            SELECT COUNT(d) FROM Document d
            WHERE d.project.department.id = :departmentId
            AND d.status = 'ACTIVE'
            """)
    long countByDepartmentId(@Param("departmentId") UUID departmentId);

    /**
     * Calculate total size of active documents in a department.
     * Useful for department-level storage reporting.
     */
    @Query("""
            SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d
            WHERE d.project.department.id = :departmentId
            AND d.status = 'ACTIVE'
            """)
    Long getTotalSizeByDepartmentId(@Param("departmentId") UUID departmentId);

    /**
     * Count active documents by MIME type in a workspace.
     * Useful for content type analytics.
     */
    @Query("""
            SELECT COUNT(d) FROM Document d
            WHERE d.project.department.workspace.id = :workspaceId
            AND d.mimeType = :mimeType
            AND d.status = 'ACTIVE'
            """)
    long countByWorkspaceAndMimeType(
            @Param("workspaceId") UUID workspaceId,
            @Param("mimeType") String mimeType
    );

    /**
     * Count documents by AI processing status in a workspace.
     * Useful for AI processing analytics and monitoring.
     */
    @Query("""
            SELECT COUNT(d) FROM Document d
            WHERE d.project.department.workspace.id = :workspaceId
            AND d.aiProcessed = :aiProcessed
            AND d.status = 'ACTIVE'
            """)
    long countByWorkspaceAndAiProcessed(
            @Param("workspaceId") UUID workspaceId,
            @Param("aiProcessed") Boolean aiProcessed
    );

    /**
     * Count documents by storage type in a workspace.
     * Useful for storage integration analytics.
     */
    @Query("""
            SELECT COUNT(d) FROM Document d
            WHERE d.project.department.workspace.id = :workspaceId
            AND d.storageType = :storageType
            AND d.status = 'ACTIVE'
            """)
    long countByWorkspaceAndStorageType(
            @Param("workspaceId") UUID workspaceId,
            @Param("storageType") String storageType
    );

    /**
     * Find documents with PDF export available in a workspace.
     * Supports PDF export functionality and reporting.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.project.department.workspace.id = :workspaceId
            AND d.pdfExportAvailable = true
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findWithPdfExportAvailablePaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Count documents with PDF export available in a workspace.
     */
    @Query("""
            SELECT COUNT(d) FROM Document d
            WHERE d.project.department.workspace.id = :workspaceId
            AND d.pdfExportAvailable = true
            AND d.status = 'ACTIVE'
            """)
    long countWithPdfExportAvailable(@Param("workspaceId") UUID workspaceId);

    /**
     * Find documents requiring AI processing in a workspace.
     * Supports batch AI processing workflows.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.project.department.workspace.id = :workspaceId
            AND d.aiProcessed = false
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt ASC
            """)
    Page<Document> findUnprocessedByAiPaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Count documents requiring AI processing in a workspace.
     */
    @Query("""
            SELECT COUNT(d) FROM Document d
            WHERE d.project.department.workspace.id = :workspaceId
            AND d.aiProcessed = false
            AND d.status = 'ACTIVE'
            """)
    long countUnprocessedByAi(@Param("workspaceId") UUID workspaceId);

    /**
     * Find all versions of a document for versioning support.
     * Prepared for future Versioning implementation.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.title = :title
            AND d.project.id = :projectId
            ORDER BY d.documentVersion DESC
            """)
    List<Document> findAllVersions(
            @Param("projectId") UUID projectId,
            @Param("title") String title
    );

    // ==================== PROJECT-SCOPED QUERIES ====================

    /**
     * Find all active documents in a project (including task-linked documents).
     * Ordered by creation date descending.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.project.id = :projectId
            AND d.status = 'ACTIVE'
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findAllByProjectIdPaginated(
            @Param("projectId") UUID projectId,
            Pageable pageable);

    /**
     * Find the largest documents in a project by file size.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE d.project.id = :projectId
            AND d.status = 'ACTIVE'
            ORDER BY d.fileSize DESC
            """)
    List<Document> findLargestByProjectId(
            @Param("projectId") UUID projectId,
            Pageable pageable);

    // ==================== SOFT DELETE ====================

    /**
     * Soft-delete a document by setting status to DELETED.
     * Used when a document needs to be logically removed without losing audit trail.
     */
    @Query("""
            UPDATE Document d
            SET d.status = 'DELETED'
            WHERE d.id = :documentId
            AND d.project.department.workspace.id = :workspaceId
            """)
    void softDelete(
            @Param("documentId") UUID documentId,
            @Param("workspaceId") UUID workspaceId
    );

    /**
     * Archive a document by setting status to ARCHIVED.
     * Used for document lifecycle management.
     */
    @Query("""
            UPDATE Document d
            SET d.status = 'ARCHIVED'
            WHERE d.id = :documentId
            AND d.project.department.workspace.id = :workspaceId
            """)
    void archive(
            @Param("documentId") UUID documentId,
            @Param("workspaceId") UUID workspaceId
    );
}