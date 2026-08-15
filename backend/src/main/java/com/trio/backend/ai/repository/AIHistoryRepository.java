package com.trio.backend.ai.repository;

import com.trio.backend.ai.entity.AIHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AIHistoryRepository extends JpaRepository<AIHistory, UUID> {

    @Query("""
            SELECT h FROM AIHistory h
            WHERE h.workspace = :workspaceId
            ORDER BY h.createdAt DESC
            """)
    Page<AIHistory> findByWorkspacePaginated(
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable
    );

    @Query("""
            SELECT h FROM AIHistory h
            WHERE h.workspace = :workspaceId
              AND h.department = :departmentId
            ORDER BY h.createdAt DESC
            """)
    Page<AIHistory> findByWorkspaceAndDepartmentPaginated(
            @Param("workspaceId") UUID workspaceId,
            @Param("departmentId") UUID departmentId,
            Pageable pageable
    );
}
