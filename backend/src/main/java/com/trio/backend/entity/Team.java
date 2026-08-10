package com.trio.backend.entity;

import com.trio.backend.entity.base.AuditableEntity;
import com.trio.backend.enums.WorkspaceStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Team is an operational unit within a Department.
 *
 * <p>Architecture notes:</p>
 * <ul>
 *     <li>Users are not linked directly to Team. Membership is handled by TeamMember.</li>
 *     <li>Tenant isolation is ensured by Department -> Workspace relationship.</li>
 * </ul>
 */
@Entity
@Table(
        name = "teams",
        indexes = {
                @Index(name = "idx_teams_department_id", columnList = "department_id"),
                @Index(name = "idx_teams_status", columnList = "status"),
                @Index(name = "idx_teams_manager_id", columnList = "manager_id"),
                @Index(name = "idx_teams_department_name", columnList = "department_id, name")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_teams_department_id_name",
                        columnNames = {"department_id", "name"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team extends AuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Manager (User) responsible for the team.
     *
     * <p>Optional : a team may have no manager assigned yet. The manager must
     * be an active member of the parent workspace.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkspaceStatus status;

    @PrePersist
    private void prePersist() {
        if (status == null) {
            status = WorkspaceStatus.ACTIVE;
        }
    }

    @Builder.Default
    @OneToMany(
            mappedBy = "team",
            fetch = FetchType.LAZY
            // No cascade/orphanRemoval by default to avoid unintended deletes.
    )
    private Set<TeamMember> members = new HashSet<>();

}

