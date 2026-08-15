package com.trio.backend.repository;

import com.trio.backend.entity.Role;
import com.trio.backend.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);

    @Query("""
            SELECT DISTINCT r
            FROM Role r
            LEFT JOIN FETCH r.rolePermissions rp
            LEFT JOIN FETCH rp.permission
            WHERE r.name = :name
            """)
    Optional<Role> findByNameWithPermissions(@Param("name") RoleName name);

    boolean existsByName(RoleName name);

}