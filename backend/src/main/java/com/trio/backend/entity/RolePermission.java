package com.trio.backend.entity;

import com.trio.backend.entity.ids.RolePermissionId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {

    @EmbeddedId
    private RolePermissionId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

}
