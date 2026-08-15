package com.trio.backend.repository;

import com.trio.backend.entity.User;
import com.trio.backend.entity.UserRole;
import com.trio.backend.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    @Query("""
            SELECT DISTINCT u
            FROM User u
            LEFT JOIN FETCH u.userRoles ur
            LEFT JOIN FETCH ur.role
            LEFT JOIN FETCH u.primaryDepartment
            WHERE u.id = :userId
            """)
    Optional<User> findByIdWithRolesAndPrimaryDepartment(@Param("userId") UUID userId);

    @Query("""
            SELECT DISTINCT u
            FROM User u
            LEFT JOIN FETCH u.userRoles ur
            LEFT JOIN FETCH ur.role r
            LEFT JOIN FETCH r.rolePermissions rp
            LEFT JOIN FETCH rp.permission
            WHERE u.email = :email
            """)
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);

    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u JOIN u.workspaceMembers wm WHERE wm.workspace.id = :workspaceId")
    List<User> findByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT u FROM User u JOIN u.workspaceMembers wm WHERE u.id = :userId AND wm.workspace.id = :workspaceId")
    Optional<User> findByIdAndWorkspaceId(@Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.workspaceMembers wm WHERE wm.workspace.id = :workspaceId")
    long countByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.workspaceMembers wm WHERE wm.workspace.id = :workspaceId AND u.status = :status")
    long countByWorkspaceIdAndStatus(@Param("workspaceId") UUID workspaceId, @Param("status") UserStatus status);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.workspaceMembers wm WHERE wm.workspace.id = :workspaceId AND u.createdAt >= :since")
    long countByWorkspaceIdAndCreatedAtAfter(@Param("workspaceId") UUID workspaceId, @Param("since") Instant since);

    @Query("""
            SELECT d.name, COUNT(DISTINCT u)
            FROM User u
            JOIN u.workspaceMembers wm
            LEFT JOIN u.primaryDepartment d
            WHERE wm.workspace.id = :workspaceId
            GROUP BY d.name
            """)
    List<Object[]> countPerDepartmentByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("""
            SELECT t.name, COUNT(DISTINCT u)
            FROM User u
            JOIN u.workspaceMembers wm
            JOIN u.teamMembers tm
            JOIN tm.team t
            WHERE wm.workspace.id = :workspaceId
            GROUP BY t.name
            """)
    List<Object[]> countPerTeamByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("""
            SELECT r.name, COUNT(DISTINCT u)
            FROM User u
            JOIN u.workspaceMembers wm
            JOIN u.userRoles ur
            JOIN ur.role r
            WHERE wm.workspace.id = :workspaceId
            GROUP BY r.name
            """)
    List<Object[]> countPerRoleByWorkspaceId(@Param("workspaceId") UUID workspaceId);

}
