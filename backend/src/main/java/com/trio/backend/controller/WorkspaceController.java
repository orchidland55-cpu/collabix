package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.organisation.team.TeamSummaryResponse;
import com.trio.backend.dto.workspace.CreateWorkspaceRequest;
import com.trio.backend.dto.workspace.UpdateWorkspaceRequest;
import com.trio.backend.dto.workspace.WorkspaceResponse;
import com.trio.backend.dto.workspace.WorkspaceSummaryResponse;
import com.trio.backend.service.TeamService;
import com.trio.backend.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.UUID;

/**
 * REST controller responsible for managing Workspaces.
 *
 * Un Workspace reprÃƒÂ©sente the context de travail dans lequel all
 * collaborations s'perform. All futurs modules (ÃƒÂ©quipes, tÃƒÂ¢ches,
 * documents, notifications, etc.) seront rattachÃƒÂ©s ÃƒÂ  un Workspace.
 *
 * ResponsabilitÃƒÂ©s :
 * - CrÃƒÂ©er a new Workspace
 * - RÃƒÂ©cupÃƒÂ©rer un Workspace spÃƒÂ©cifique
 * - List the Workspaces de the user authentifiÃƒÂ©
 * - Mettre ÃƒÂ  jour les information of a workspace
 * - Delete (soft delete) un Workspace
 *
 * All endpoints requiÃƒÂ¨rent une authentication JWT.
 */
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
@Tag(
        name = "Workspaces",
        description = "Endpoints for managing Workspaces (contexts de travail)"
)
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    private final TeamService teamService;

    /**
     * Creates a nouveau Workspace.
     *
     * <p>Le Workspace est crÃƒÂ©ÃƒÂ© avec the status ACTIVE et the user authentifiÃƒÂ©
     * devient propriÃƒÂ©taire of the workspace. Le propriÃƒÂ©taire est automaticment
     * ajoutÃƒÂ© comme first member avec le rÃƒÂ´le OWNER.</p>
     *
     * @param request les donnÃƒÂ©es de crÃƒÂ©ation of the workspace
     * @return ApiResponse containing le Workspace crÃƒÂ©ÃƒÂ©
     * @throws ConflictException si un Workspace avec le mÃƒÂªme name existe dÃƒÂ©jÃƒÂ  pour ce propriÃƒÂ©taire
     */
    @PostMapping
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'WORKSPACE_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(

            summary = "Create a nouveau Workspace",
            description = "Creates a nouveau Workspace avec the status ACTIVE. L'user authentifiÃƒÂ© devient propriÃƒÂ©taire et est ajoutÃƒÂ© comme first member avec le rÃƒÂ´le OWNER.",
            security = @SecurityRequirement(name = "bearer")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Workspace created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed ou donnÃƒÂ©es invalid"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Un Workspace avec ce name existe dÃƒÂ©jÃƒÂ "
            )
    })
    public ApiResponse<WorkspaceResponse> create(
            @Valid @RequestBody CreateWorkspaceRequest request
    ) {
        return ApiResponse.success(
                "Workspace created successfully.",
                workspaceService.create(request)
        );
    }

    /**
     * RÃƒÂ©cupÃƒÂ¨re un Workspace par its ID.
     *
     * <p>Returns the information complÃƒÂ¨tes of the workspace avec the statistics
     * (namebre de members, namebre d'ÃƒÂ©quipes). L'user authentifiÃƒÂ© doit
     * ÃƒÂªtre member of the workspace pour y accÃƒÂ©der.</p>
     *
     * @param workspaceId the ID of the Workspace
     * @return ApiResponse containing le Workspace found
     * @throws ResourceNotFoundException si le Workspace does not exist
     * @throws ForbiddenException si the user is not member of the workspace
     */
    @GetMapping("/{workspaceId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'WORKSPACE_READ')")
    @Operation(
            summary = "Resorteve a Workspace by ID",

            description = "RÃƒÂ©cupÃƒÂ¨re les information complÃƒÂ¨tes of a workspace avec the statistics (members, ÃƒÂ©quipes). L'user doit ÃƒÂªtre member of the workspace.",
            security = @SecurityRequirement(name = "bearer")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Workspace found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User is not a workspace member"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Workspace not found"
            )
    })
    public ApiResponse<WorkspaceResponse> getById(
            @Parameter(description = "ID of the Workspace", required = true)
            @PathVariable UUID workspaceId
    ) {
        return ApiResponse.success(
                "Workspace resorteved successfully.",
                workspaceService.getById(workspaceId)
        );
    }

    /**
     * RÃƒÂ©cupÃƒÂ¨re the list of the Workspaces actives de the user authentifiÃƒÂ©.
     *
     * <p>Returns uniquement les Workspaces pour lesquels the user est
     * member active. Les Workspaces sont sortÃƒÂ©s by date de crÃƒÂ©ation dÃƒÂ©ascending
     * (plus rÃƒÂ©cents d'abord).</p>
     *
     * @return ApiResponse containing the list of the Workspaces de the user
     */
    @GetMapping
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'WORKSPACE_READ')")
    @Operation(
            summary = "List Workspaces de the user",

            description = "RÃƒÂ©cupÃƒÂ¨re the list de all Workspaces actives pour lesquels the user authentifiÃƒÂ© is a member. TriÃƒÂ©s by date dÃƒÂ©ascending.",
            security = @SecurityRequirement(name = "bearer")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste of the Workspaces rÃƒÂ©cupÃƒÂ©rÃƒÂ©e",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            )
    })
    public ApiResponse<List<WorkspaceSummaryResponse>> listByCurrentUser(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order
    ) {
        return ApiResponse.success(
                "Workspaces resorteved successfully.",
                workspaceService.listByCurrentUser(search, sort, order)
        );
    }

    /**
     * Met ÃƒÂ  jour les information of a workspace.
     *
     * <p>Seuls the name et la description peuvent ÃƒÂªtre modifiÃƒÂ©s. L'user
     * authentifiÃƒÂ© doit ÃƒÂªtre propriÃƒÂ©taire or administrator of the workspace.</p>
     *
     * @param workspaceId the ID of the Workspace ÃƒÂ  mettre ÃƒÂ  jour
     * @param request les donnÃƒÂ©es de updated
     * @return ApiResponse containing le Workspace modifiÃƒÂ©
     * @throws ResourceNotFoundException si le Workspace does not exist
     * @throws ForbiddenException si the user n'a pas the permission
     * @throws ConflictException si le nouveau name existe dÃƒÂ©jÃƒÂ 
     */
    @PutMapping("/{workspaceId}")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'WORKSPACE_UPDATE')")
    @Operation(
            summary = "Update a Workspace",

            description = "Met ÃƒÂ  jour les information of a workspace (name, description). Seuls le propriÃƒÂ©taire ou les administrators peuvent modify un Workspace.",
            security = @SecurityRequirement(name = "bearer")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Workspace updated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User n'a pas the permission de modify"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Workspace not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "The name existe dÃƒÂ©jÃƒÂ "
            )
    })
    public ApiResponse<WorkspaceResponse> update(
            @Parameter(description = "ID of the Workspace", required = true)
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request
    ) {
        return ApiResponse.success(
                "Workspace updated successfully.",
                workspaceService.update(workspaceId, request)
        );
    }

    /**
     * Supprime (soft delete) un Workspace.
     *
     * <p>Le Workspace is not physiquement deleted, its status passe de ACTIVE
     * ÃƒÂ  ARCHIVED. L'user authentifiÃƒÂ© doit ÃƒÂªtre propriÃƒÂ©taire or administrator
     * of the workspace.</p>
     *
     * @param workspaceId the ID of the Workspace ÃƒÂ  supprimer
     * @throws ResourceNotFoundException si le Workspace does not exist
     * @throws ForbiddenException si the user n'a pas the permission
     */
    @GetMapping("/{workspaceId}/teams")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TEAM_READ')")
    @Operation(summary = "List teams of a Workspace", description = "Returns all teams (active and archived) of the workspace with their department and manager.", security = @SecurityRequirement(name = "bearer"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Teams retrieved", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User is not a workspace member")
    })
    public ApiResponse<List<TeamSummaryResponse>> listWorkspaceTeams(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId
    ) {
        return ApiResponse.success(
                "Workspace teams retrieved successfully.",
                teamService.listByWorkspace(workspaceId)
        );
    }

    @PutMapping("/{workspaceId}/archive")
    @PreAuthorize("@workspaceAuth.canDeleteWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'WORKSPACE_DELETE')")
    @Operation(summary = "Archive a Workspace", description = "Sets workspace status to ARCHIVED.", security = @SecurityRequirement(name = "bearer"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Workspace archived"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permission denied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ApiResponse<WorkspaceResponse> archive(@PathVariable UUID workspaceId) {
        workspaceService.archive(workspaceId);
        return ApiResponse.success("Workspace archived successfully.", workspaceService.getById(workspaceId));
    }

    @PutMapping("/{workspaceId}/restore")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'WORKSPACE_UPDATE')")
    @Operation(summary = "Restore a Workspace", description = "Sets workspace status back to ACTIVE.", security = @SecurityRequirement(name = "bearer"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Workspace restored"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permission denied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Workspace not found")
    })
    public ApiResponse<WorkspaceResponse> restore(@PathVariable UUID workspaceId) {
        workspaceService.restore(workspaceId);
        return ApiResponse.success("Workspace restored successfully.", workspaceService.getById(workspaceId));
    }

    @GetMapping("/archived")
    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'WORKSPACE_READ')")
    @Operation(summary = "List archived Workspaces", description = "Retrieves all archived workspaces for the current user.", security = @SecurityRequirement(name = "bearer"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Archived workspaces retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ApiResponse<List<WorkspaceSummaryResponse>> listArchived() {
        return ApiResponse.success(
                "Archived workspaces retrieved successfully.",
                workspaceService.listArchived()
        );
    }

    @DeleteMapping("/{workspaceId}")
    @PreAuthorize("@workspaceAuth.canDeleteWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'WORKSPACE_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a Workspace",

            description = "Supprime (soft delete) un Workspace en changeant its status ÃƒÂ  ARCHIVED. Seul le propriÃƒÂ©taire ou les administrators peuvent supprimer un Workspace.",
            security = @SecurityRequirement(name = "bearer")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204",
                    description = "Workspace deleted avec succÃƒÂ¨s"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User n'a pas the permission de supprimer"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Workspace not found"
            )
    })
    public void delete(
            @Parameter(description = "ID of the Workspace", required = true)
            @PathVariable UUID workspaceId
    ) {
        workspaceService.delete(workspaceId);
    }

}
