package com.trio.backend.dto.organisation.team;

import com.trio.backend.enums.WorkspaceStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Response Complete pour une Team.
 */
@Getter
@Setter
public class TeamResponse {

    private UUID id;

    private UUID departmentId;

    private String departmentName;

    private String name;

    private String description;

    private WorkspaceStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    private Long memberCount;

    private UUID managerId;

    private String managerName;
}

