package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.organisation.project.ProjectResponse;
import com.trio.backend.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Workspace-scoped project listing used for the Admin "All departments" view.
 *
 * <p>Workspace ADMIN/OWNER can list projects across every department of the
 * workspace. MANAGER/MEMBER are automatically scoped to their primary department
 * by the service layer, which means this endpoint can never leak projects from
 * another department to them.</p>
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Workspace-wide project listing (Admin)")
public class WorkspaceProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'PROJECT_READ')")
    @Operation(summary = "List projects across departments", security = @SecurityRequirement(name = "bearer"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List retrieved", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission")
    })
    public ApiResponse<Page<ProjectResponse>> list(
            @org.springframework.web.bind.annotation.PathVariable UUID workspaceId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success("Projects retrieved successfully.", projectService.listAllPaginated(workspaceId, search, pageable));
    }
}