package com.trio.backend.entity;

import com.trio.backend.entity.ids.TeamMemberId;
import com.trio.backend.enums.WorkspaceMemberStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/**
 * TeamMember is the association between a Team and a User through membership.
 *
 * <p>This MVP version keeps compatibility with the existing codebase:</p>
 * <ul>
 *     <li>Composite identifier: (teamId, userId)</li>
 *     <li>References User directly via userId (current architecture choice in the repository).</li>
 * </ul>
 */
@Entity
@Table(
        name = "team_members",
        indexes = {
                @Index(name = "idx_team_members_team_id", columnList = "team_id"),
                @Index(name = "idx_team_members_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_team_members_team_id_user_id",
                        columnNames = {"team_id", "user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember {

    @EmbeddedId
    private TeamMemberId teamMemberId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("teamId")
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkspaceMemberStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        if (status == null) {
            status = WorkspaceMemberStatus.ACTIVE;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}

