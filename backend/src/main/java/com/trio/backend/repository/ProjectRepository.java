package com.trio.backend.repository;

import com.trio.backend.entity.Project;
import com.trio.backend.enums.WorkspaceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findAllByDepartment_IdAndStatus(
            UUID departmentId,
            WorkspaceStatus status
    );

    Page<Project> findAllByDepartment_IdAndStatus(
            UUID departmentId,
            WorkspaceStatus status,
            Pageable pageable
    );

    @Query("select p from Project p where p.id = :projectId and p.department.id = :departmentId")
    Optional<Project> findByIdAndDepartment_Id(
            @Param("projectId") UUID projectId,
            @Param("departmentId") UUID departmentId
    );

    boolean existsByDepartment_IdAndName(UUID departmentId, String name);

    boolean existsByDepartment_IdAndNameIgnoreCase(UUID departmentId, String name);

    boolean existsByIdAndDepartment_IdAndStatus(UUID projectId, UUID departmentId, WorkspaceStatus status);

    @Query("SELECT p FROM Project p " +
            "WHERE p.department.id = :departmentId " +
            "AND p.status = :status " +
            "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "ORDER BY p.name ASC")
    List<Project> searchByDepartmentIdAndName(
            @Param("departmentId") UUID departmentId,
            @Param("status") WorkspaceStatus status,
            @Param("name") String name
    );

    @Query("SELECT p FROM Project p " +
            "WHERE p.department.id = :departmentId " +
            "AND p.status = :status " +
            "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "ORDER BY p.name ASC")
    Page<Project> searchByDepartmentIdAndName(
            @Param("departmentId") UUID departmentId,
            @Param("status") WorkspaceStatus status,
            @Param("name") String name,
            Pageable pageable
    );

    @Query("SELECT p FROM Project p " +
            "WHERE p.department.workspace.id = :workspaceId " +
            "AND p.status = :status " +
            "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) " +
            "ORDER BY p.name ASC")
    Page<Project> searchByWorkspaceIdAndName(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") WorkspaceStatus status,
            @Param("name") String name,
            Pageable pageable
    );

    long countByDepartment_IdAndStatus(UUID departmentId, WorkspaceStatus status);

    @Query("SELECT COUNT(p) FROM Project p " +
            "WHERE p.department.workspace.id = :workspaceId " +
            "AND p.status = :status")
    long countByWorkspaceIdAndStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") WorkspaceStatus status
    );

    @Query("SELECT p FROM Project p " +
            "WHERE p.department.workspace.id = :workspaceId " +
            "AND p.status = :status")
    Page<Project> findAllByWorkspaceIdAndStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") WorkspaceStatus status,
            Pageable pageable
    );

    @Query("SELECT p FROM Project p " +
            "WHERE p.department.id = :departmentId " +
            "AND p.status = :status " +
            "ORDER BY p.createdAt DESC")
    List<Project> findTopByDepartmentIdOrderByCreatedAtDesc(
            @Param("departmentId") UUID departmentId,
            @Param("status") WorkspaceStatus status,
            Pageable pageable
    );

    @Query("SELECT p FROM Project p " +
            "JOIN FETCH p.department " +
            "WHERE p.department.workspace.id = :workspaceId " +
            "AND p.status = :status")
    List<Project> findAllByWorkspaceIdAndStatus(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") WorkspaceStatus status
    );

    @Query("SELECT p FROM Project p " +
            "WHERE p.department.workspace.id = :workspaceId " +
            "AND p.status = :status " +
            "ORDER BY p.createdAt DESC")
    List<Project> findTopByWorkspaceIdOrderByCreatedAtDesc(
            @Param("workspaceId") UUID workspaceId,
            @Param("status") WorkspaceStatus status,
            Pageable pageable
    );

    @Query("SELECT p FROM Project p " +
            "WHERE p.department.id = :departmentId " +
            "AND p.status = :status " +
            "ORDER BY p.updatedAt DESC")
    List<Project> findTopByDepartmentIdOrderByUpdatedAtDesc(
            @Param("departmentId") UUID departmentId,
            @Param("status") WorkspaceStatus status,
            Pageable pageable
    );
}
