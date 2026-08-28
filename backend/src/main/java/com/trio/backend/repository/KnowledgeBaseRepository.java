package com.trio.backend.repository;

import com.trio.backend.entity.KnowledgeBase;
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
 * Repository for KnowledgeBase entity.
 *
 * <p>Conventions:</p>
 * <ul>
 *     <li>All queries filter by ACTIVE status by default.</li>
 *     <li>Workspace scope is validated through the entity chain: KnowledgeBase -> Project -> Department -> Workspace.</li>
 *     <li>Pagination is applied for list operations to ensure performance.</li>
 *     <li>Methods are designed to support future statistics, analytics, and AI search features.</li>
 * </ul>
 */
@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, UUID> {

    // ==================== CRUD ====================

    /**
     * Find a knowledge lowe article by ID within a specific workspace.
     * Validates workspace scope through the entity chain.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            JOIN FETCH kb.project p
            JOIN FETCH p.department dept
            JOIN FETCH dept.workspace w
            WHERE kb.id = :KnowledgeBaseId
            AND w.id = :workspaceId
            AND kb.status IN ('ACTIVE', 'ARCHIVED')
            """)
    Optional<KnowledgeBase> findByIdAndWorkspace(
            @Param("KnowledgeBaseId") UUID KnowledgeBaseId,
            @Param("workspaceId") UUID workspaceId
    );

    // ==================== FIND BY PROJECT ====================

    /**
     * Find all active knowledge lowe articles for a specific project.
     * Paginated for performance.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND kb.status = 'ACTIVE'
            ORDER BY kb.isPinned DESC, kb.createdAt DESC
            """)
    Page<KnowledgeBase> findByProjectIdPaginated(
            @Param("projectId") UUID projectId,
            Pageable pageable
    );

    /**
     * Find all active knowledge lowe articles for a specific project without pagination.
     * Useful for lightweight operations or export.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND kb.status = 'ACTIVE'
            ORDER BY kb.isPinned DESC, kb.createdAt DESC
            """)
    List<KnowledgeBase> findByProjectId(@Param("projectId") UUID projectId);

    /**
     * Count active knowledge lowe articles in a project.
     * Useful for project summary views and validation.
     */
    @Query("""
            SELECT COUNT(kb) FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND kb.status = 'ACTIVE'
            """)
    long countByProjectId(@Param("projectId") UUID projectId);

    /**
     * Find pinned knowledge lowe articles for a specific project.
     * Paginated for performance.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND kb.isPinned = true
            AND kb.status = 'ACTIVE'
            ORDER BY kb.createdAt DESC
            """)
    Page<KnowledgeBase> findPinnedByProjectIdPaginated(
            @Param("projectId") UUID projectId,
            Pageable pageable
    );

    /**
     * Count pinned articles in a project.
     * Useful for pinned articles widget.
     */
    @Query("""
            SELECT COUNT(kb) FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND kb.isPinned = true
            AND kb.status = 'ACTIVE'
            """)
    long countPinnedByProjectId(@Param("projectId") UUID projectId);

    // ==================== WORKSPACE SCOPE ====================

    /**
     * Find all active knowledge lowe articles within a workspace.
     * Paginated for performance.
     * Useful for workspace-wide audit or analytics.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.department.workspace.id = :workspaceId
            AND kb.status = 'ACTIVE'
            ORDER BY kb.isPinned DESC, kb.createdAt DESC
            """)
    Page<KnowledgeBase> findByWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Count all active knowledge lowe articles in a workspace.
     * Useful for quota checks and analytics.
     */
    @Query("""
            SELECT COUNT(kb) FROM KnowledgeBase kb
            WHERE kb.project.department.workspace.id = :workspaceId
            AND kb.status = 'ACTIVE'
            """)
    long countByWorkspace(@Param("workspaceId") UUID workspaceId);

    // ==================== SEARCH BY TITLE ====================

    /**
     * Search knowledge lowe articles by title within a project.
     * Case-insensitive partial matching.
     * Paginated for performance.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND LOWER(kb.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            AND kb.status = 'ACTIVE'
            ORDER BY kb.isPinned DESC, kb.createdAt DESC
            """)
    Page<KnowledgeBase> searchByTitleInProjectPaginated(
            @Param("projectId") UUID projectId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * Search knowledge lowe articles by title within a workspace.
     * Case-insensitive partial matching.
     * Paginated for performance.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.department.workspace.id = :workspaceId
            AND LOWER(kb.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            AND kb.status = 'ACTIVE'
            ORDER BY kb.isPinned DESC, kb.createdAt DESC
            """)
    Page<KnowledgeBase> searchByTitleInWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * Count articles matching title search in a project.
     * Useful for search result count.
     */
    @Query("""
            SELECT COUNT(kb) FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND LOWER(kb.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            AND kb.status = 'ACTIVE'
            """)
    long countSearchByTitleInProject(
            @Param("projectId") UUID projectId,
            @Param("searchTerm") String searchTerm
    );

    // ==================== SEARCH TEXTUAL ====================

    /**
     * Search knowledge lowe articles by content (full text search).
     * Searches both title and content.
     * Case-insensitive partial matching.
     * Paginated for performance.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND (LOWER(kb.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                 OR LOWER(kb.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                 OR LOWER(kb.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            AND kb.status = 'ACTIVE'
            ORDER BY kb.isPinned DESC, kb.createdAt DESC
            """)
    Page<KnowledgeBase> searchByContentInProjectPaginated(
            @Param("projectId") UUID projectId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * Search knowledge lowe articles by content within a workspace.
     * Searches both title and content.
     * Case-insensitive partial matching.
     * Paginated for performance.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.department.workspace.id = :workspaceId
            AND (LOWER(kb.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                 OR LOWER(kb.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                 OR LOWER(kb.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            AND kb.status = 'ACTIVE'
            ORDER BY kb.isPinned DESC, kb.createdAt DESC
            """)
    Page<KnowledgeBase> searchByContentInWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * Search knowledge base articles by content within a department.
     * Used to scope workspace-wide knowledge searches to the caller's own department.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.department.id = :departmentId
            AND (LOWER(kb.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                 OR LOWER(kb.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                 OR LOWER(kb.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            AND kb.status = 'ACTIVE'
            ORDER BY kb.isPinned DESC, kb.createdAt DESC
            """)
    Page<KnowledgeBase> searchByContentInDepartmentPaginated(
            @Param("departmentId") UUID departmentId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * Count articles matching content search in a project.
     * Useful for search result count.
     */
    @Query("""
            SELECT COUNT(kb) FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND (LOWER(kb.title) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                 OR LOWER(kb.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                 OR LOWER(kb.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            AND kb.status = 'ACTIVE'
            """)
    long countSearchByContentInProject(
            @Param("projectId") UUID projectId,
            @Param("searchTerm") String searchTerm
    );

    // ==================== SEARCH BY CATEGORY ====================

    /**
     * Find knowledge lowe articles by category within a project.
     * Paginated for performance.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND kb.category = :category
            AND kb.status = 'ACTIVE'
            ORDER BY kb.isPinned DESC, kb.createdAt DESC
            """)
    Page<KnowledgeBase> findByCategoryInProjectPaginated(
            @Param("projectId") UUID projectId,
            @Param("category") String category,
            Pageable pageable
    );

    /**
     * Count articles by category in a project.
     * Useful for category-lowed analytics.
     */
    @Query("""
            SELECT COUNT(kb) FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND kb.category = :category
            AND kb.status = 'ACTIVE'
            """)
    long countByCategoryInProject(
            @Param("projectId") UUID projectId,
            @Param("category") String category
    );

    /**
     * Get distinct categories used by active articles in a project.
     * Useful for category filter dropdowns in the UI.
     */
    @Query("""
            SELECT DISTINCT kb.category FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND kb.status = 'ACTIVE'
            AND kb.category IS NOT NULL
            ORDER BY kb.category
            """)
    List<String> findDistinctCategoriesByProjectId(@Param("projectId") UUID projectId);

    // ==================== STATISTICS & DASHBOARD ====================

    /**
     * Find all active knowledge lowe articles for a specific department.
     * Supports department-level dashboard and analytics.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.department.id = :departmentId
            AND kb.status = 'ACTIVE'
            ORDER BY kb.isPinned DESC, kb.createdAt DESC
            """)
    Page<KnowledgeBase> findByDepartmentIdPaginated(
            @Param("departmentId") UUID departmentId,
            Pageable pageable
    );

    /**
     * Count active articles in a department.
     * Useful for department dashboard metrics.
     */
    @Query("""
            SELECT COUNT(kb) FROM KnowledgeBase kb
            WHERE kb.project.department.id = :departmentId
            AND kb.status = 'ACTIVE'
            """)
    long countByDepartmentId(@Param("departmentId") UUID departmentId);

    /**
     * Find most viewed articles in a workspace.
     * Useful for popular articles dashboard.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.department.workspace.id = :workspaceId
            AND kb.status = 'ACTIVE'
            ORDER BY kb.viewCount DESC, kb.createdAt DESC
            """)
    Page<KnowledgeBase> findMostViewedInWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Find most favorited articles in a workspace.
     * Useful for favorite articles dashboard.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.department.workspace.id = :workspaceId
            AND kb.status = 'ACTIVE'
            ORDER BY kb.favoriteCount DESC, kb.createdAt DESC
            """)
    Page<KnowledgeBase> findMostFavoritedInWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Count articles by AI processing status in a workspace.
     * Useful for AI processing analytics and monitoring.
     */
    @Query("""
            SELECT COUNT(kb) FROM KnowledgeBase kb
            WHERE kb.project.department.workspace.id = :workspaceId
            AND kb.aiProcessed = :aiProcessed
            AND kb.status = 'ACTIVE'
            """)
    long countByWorkspaceAndAiProcessed(
            @Param("workspaceId") UUID workspaceId,
            @Param("aiProcessed") Boolean aiProcessed
    );

    /**
     * Count articles with RAG embeddings available in a workspace.
     * Useful for RAG/Vector Search availability tracking.
     */
    @Query("""
            SELECT COUNT(kb) FROM KnowledgeBase kb
            WHERE kb.project.department.workspace.id = :workspaceId
            AND kb.ragEmbeddingsAvailable = true
            AND kb.status = 'ACTIVE'
            """)
    long countWithRagEmbeddingsAvailable(@Param("workspaceId") UUID workspaceId);

    /**
     * Find articles requiring AI processing in a workspace.
     * Supports batch AI processing workflows.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.department.workspace.id = :workspaceId
            AND kb.aiProcessed = false
            AND kb.status = 'ACTIVE'
            ORDER BY kb.createdAt ASC
            """)
    Page<KnowledgeBase> findUnprocessedByAiPaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Count articles requiring AI processing in a workspace.
     */
    @Query("""
            SELECT COUNT(kb) FROM KnowledgeBase kb
            WHERE kb.project.department.workspace.id = :workspaceId
            AND kb.aiProcessed = false
            AND kb.status = 'ACTIVE'
            """)
    long countUnprocessedByAi(@Param("workspaceId") UUID workspaceId);

    /**
     * Find articles requiring RAG embeddings generation in a workspace.
     * Supports batch RAG embedding generation workflows.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.department.workspace.id = :workspaceId
            AND kb.ragEmbeddingsAvailable = false
            AND kb.status = 'ACTIVE'
            ORDER BY kb.createdAt ASC
            """)
    Page<KnowledgeBase> findWithoutRagEmbeddingsPaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    /**
     * Count articles requiring RAG embeddings generation in a workspace.
     */
    @Query("""
            SELECT COUNT(kb) FROM KnowledgeBase kb
            WHERE kb.project.department.workspace.id = :workspaceId
            AND kb.ragEmbeddingsAvailable = false
            AND kb.status = 'ACTIVE'
            """)
    long countWithoutRagEmbeddings(@Param("workspaceId") UUID workspaceId);

    /**
     * Find recently created or updated articles in a project.
     * Useful for "Recent" dashboard widget.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.project.id = :projectId
            AND kb.status = 'ACTIVE'
            ORDER BY kb.updatedAt DESC
            """)
    Page<KnowledgeBase> findRecentInProjectPaginated(
            @Param("projectId") UUID projectId,
            Pageable pageable
    );

    /**
     * Find all versions of an article for versioning support.
     * Prepared for future Versioning implementation.
     */
    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.title = :title
            AND kb.project.id = :projectId
            ORDER BY kb.articleVersion DESC
            """)
    List<KnowledgeBase> findAllVersions(
            @Param("projectId") UUID projectId,
            @Param("title") String title
    );

    // ==================== SOFT DELETE ====================

    /**
     * Soft-delete a knowledge lowe article by setting status to DELETED.
     * Used when an article needs to be logically removed without losing audit trail.
     */
    @Query("""
            UPDATE KnowledgeBase kb
            SET kb.status = 'DELETED'
            WHERE kb.id = :KnowledgeBaseId
            AND kb.project.department.workspace.id = :workspaceId
            """)
    void softDelete(
            @Param("KnowledgeBaseId") UUID KnowledgeBaseId,
            @Param("workspaceId") UUID workspaceId
    );

    /**
     * Archive a knowledge lowe article by setting status to ARCHIVED.
     * Used for article lifecycle management.
     */
    @Query("""
            UPDATE KnowledgeBase kb
            SET kb.status = 'ARCHIVED'
            WHERE kb.id = :KnowledgeBaseId
            AND kb.project.department.workspace.id = :workspaceId
            """)
    void archive(
            @Param("KnowledgeBaseId") UUID KnowledgeBaseId,
            @Param("workspaceId") UUID workspaceId
    );
}
