package com.trio.backend.repository;

import com.trio.backend.entity.ExecutiveReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExecutiveReportRepository extends JpaRepository<ExecutiveReport, UUID> {

    @Query("""
            SELECT er FROM ExecutiveReport er
            WHERE er.id = :reportId
              AND er.status = 'ACTIVE'
              AND er.workspace.id = :workspaceId
            """)
    Optional<ExecutiveReport> findByIdAndWorkspace(
            @Param("reportId") UUID reportId,
            @Param("workspaceId") UUID workspaceId
    );

    @Query("""
            SELECT er FROM ExecutiveReport er
            WHERE er.workspace.id = :workspaceId
              AND er.status = 'ACTIVE'
            ORDER BY er.createdAt DESC
            """)
    Page<ExecutiveReport> findByWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    @Query("""
            SELECT er FROM ExecutiveReport er
            WHERE er.workspace.id = :workspaceId
              AND er.department.id = :departmentId
              AND er.status = 'ACTIVE'
            ORDER BY er.createdAt DESC
            """)
    Page<ExecutiveReport> findByWorkspaceAndDepartmentPaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("departmentId") UUID departmentId,
            Pageable pageable
    );

    @Query("""
            UPDATE ExecutiveReport er
            SET er.status = 'DELETED'
            WHERE er.id = :reportId
              AND er.workspace.id = :workspaceId
            """)
    void softDelete(
            @Param("reportId") UUID reportId,
            @Param("workspaceId") UUID workspaceId
    );
}
