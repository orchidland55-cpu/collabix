package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.organisation.team.CreateTeamRequest;
import com.trio.backend.dto.organisation.team.TeamDetailsResponse;
import com.trio.backend.dto.organisation.team.TeamResponse;
import com.trio.backend.dto.organisation.team.TeamSummaryResponse;
import com.trio.backend.dto.organisation.team.UpdateTeamRequest;
import com.trio.backend.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller responsible for managing Team.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/departments/{departmentId}/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Endpoints for managing Teams (organization)")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TEAM_CREATE')")
    @Operation(
            summary = "Create a team",
            security = @SecurityRequirement(name = "bearer"),
            description = "Creates a team dans le department." )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Team createde", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Department not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Name already in use")
    })
    public ApiResponse<TeamResponse> create(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @Valid @RequestBody CreateTeamRequest request
    ) {
        return ApiResponse.success(
                "Team created successfully.",
                teamService.create(workspaceId, departmentId, request)
        );
    }

    @GetMapping("/{teamId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TEAM_READ')")
    @Operation(
            summary = "Resorteve a team",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the information of a team." )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Team found", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ApiResponse<TeamResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID teamId
    ) {
        return ApiResponse.success(
                "Team resorteved successfully.",
                teamService.getById(workspaceId, departmentId, teamId)
        );
    }

    @GetMapping
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TEAM_READ')")
    @Operation(
            summary = "List teams d'un department",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the list des teams actives d'un department." )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List resorteved", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission")
    })
    public ApiResponse<List<TeamSummaryResponse>> listByDepartment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId
    ) {
        return ApiResponse.success(
                "Teams resorteved successfully.",
                teamService.listByDepartment(workspaceId, departmentId)
        );
    }
    @GetMapping("/{teamId}/details")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TEAM_READ')")
    @Operation(
            summary = "Details of a team",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns une variante details of a team." )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Details resorteved", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ApiResponse<TeamDetailsResponse> getDetails(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID teamId
    ) {
        return ApiResponse.success(
                "Team details resorteved successfully.",
                teamService.getDetails(workspaceId, departmentId, teamId)
        );
    }

    @PutMapping("/{teamId}")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TEAM_UPDATE')")
    @Operation(
            summary = "Update a team",
            security = @SecurityRequirement(name = "bearer"),
            description = "Updates a team (partial update)." )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Team updated", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Team not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Name already in use")
    })
    public ApiResponse<TeamResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID teamId,
            @Valid @RequestBody UpdateTeamRequest request
    ) {
        return ApiResponse.success(
                "Team updated successfully.",
                teamService.update(workspaceId, departmentId, teamId, request)
        );
    }

    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canDeleteWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TEAM_DELETE')")
    @Operation(
            summary = "Delete a team",
            security = @SecurityRequirement(name = "bearer"),
            description = "Supprime (soft delete) une team." )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Team deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Team not found")
    })
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID teamId
    ) {
        teamService.delete(workspaceId, departmentId, teamId);
    }

    @PutMapping("/{teamId}/restore")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TEAM_UPDATE')")
    @Operation(
            summary = "Restore an archived team",
            security = @SecurityRequirement(name = "bearer"),
            description = "Restores an archived team back to ACTIVE status.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Team restored", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ApiResponse<TeamResponse> restore(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID teamId
    ) {
        return ApiResponse.success(
                "Team restored successfully.",
                teamService.restore(workspaceId, departmentId, teamId)
        );
    }

    @DeleteMapping("/{teamId}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canPermanentlyDeleteTeam(#workspaceId, #teamId, authentication)")
    @Operation(
            summary = "Permanently delete a team",
            security = @SecurityRequirement(name = "bearer"),
            description = "Permanently removes a team from the database. This is irreversible. " +
                    "Only a Workspace Admin/Owner or the manager of the team is allowed.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Team permanently deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Team not found")
    })
    public void deletePermanently(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID teamId
    ) {
        teamService.deletePermanently(workspaceId, departmentId, teamId);
    }
}

