package com.trio.backend.repository;

import com.trio.backend.entity.AnalyticsReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalyticsReportRepository extends JpaRepository<AnalyticsReport, UUID> {

    @Query("""
            SELECT ar FROM AnalyticsReport ar
            WHERE ar.id = :reportId
              AND ar.status = 'ACTIVE'
              AND ar.workspace.id = :workspaceId
            """)
    Optional<AnalyticsReport> findByIdAndWorkspace(
            @Param("reportId") UUID reportId,
            @Param("workspaceId") UUID workspaceId
    );

    @Query("""
            SELECT ar FROM AnalyticsReport ar
            WHERE ar.workspace.id = :workspaceId
              AND ar.status = 'ACTIVE'
            ORDER BY ar.createdAt DESC
            """)
    Page<AnalyticsReport> findByWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    @Query("""
            SELECT ar FROM AnalyticsReport ar
            WHERE ar.workspace.id = :workspaceId
              AND ar.department.id = :departmentId
              AND ar.status = 'ACTIVE'
            ORDER BY ar.createdAt DESC
            """)
    Page<AnalyticsReport> findByWorkspaceAndDepartmentPaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("departmentId") UUID departmentId,
            Pageable pageable
    );

    @Query("""
            UPDATE AnalyticsReport ar
            SET ar.status = 'DELETED'
            WHERE ar.id = :reportId
              AND ar.workspace.id = :workspaceId
            """)
    void softDelete(
            @Param("reportId") UUID reportId,
            @Param("workspaceId") UUID workspaceId
    );
}
