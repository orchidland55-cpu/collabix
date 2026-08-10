package com.trio.backend.dto.organisation.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Create request of a Team.
 */
@Getter
@Setter
public class CreateTeamRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String description;

    /**
     * ID of the user manager of the team (optional).
     */
    private UUID managerId;
}

