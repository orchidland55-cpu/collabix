package com.trio.backend.dto.workspace;

import com.trio.backend.dto.user.UserResponse;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.enums.WorkspaceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponse {

    private UUID id;

    private String name;

    private String description;

    private WorkspaceStatus status;

    private UserResponse owner;

    private WorkspaceRole myRole;

    private Long memberCount;

    private Long teamCount;

    private Long projectCount;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;

}
