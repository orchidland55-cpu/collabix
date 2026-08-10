package com.trio.backend.dto.organisation.team;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Request de updated of a Team.
 *
 * <p>Partial update : the fields null sont ignorÃƒÂ©s cÃƒÂ´tÃƒÂ© service.</p>
 */
@Getter
@Setter
public class UpdateTeamRequest {

    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String description;

    /**
     * ID of the user manager of the team. When non-null, the manager is
     * reassigned. Null leaves the current manager unchanged.
     */
    private UUID managerId;

    /**
     * When {@code true}, removes the team manager (sets it back to
     * "Unassigned"). Takes precedence over {@link #managerId}.
     */
    private Boolean clearManager;
}

