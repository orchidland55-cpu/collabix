package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.organisation.project.CreateProjectRequest;
import com.trio.backend.dto.organisation.project.ProjectResponse;
import com.trio.backend.dto.organisation.project.UpdateProjectRequest;
import com.trio.backend.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/departments/{departmentId}/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Endpoints for managing Projects")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@departmentAuth.canManageDepartmentProjects(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'PROJECT_CREATE')")
    @Operation(summary = "Create a project", security = @SecurityRequirement(name = "bearer"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Project created", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Department not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Name already in use")
    })
    public ApiResponse<ProjectResponse> create(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        return ApiResponse.success("Project created successfully.", projectService.create(workspaceId, departmentId, request));
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'PROJECT_READ')")
    @Operation(summary = "Get a project", security = @SecurityRequirement(name = "bearer"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Project found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ApiResponse<ProjectResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId
    ) {
        return ApiResponse.success("Project retrieved successfully.", projectService.getById(workspaceId, departmentId, projectId));
    }

    @GetMapping
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'PROJECT_READ')")
    @Operation(summary = "List projects", security = @SecurityRequirement(name = "bearer"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List retrieved", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission")
    })
    public ApiResponse<Page<ProjectResponse>> list(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success("Projects retrieved successfully.", projectService.listPaginated(workspaceId, departmentId, search, pageable));
    }

    @GetMapping("/archived")
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'PROJECT_READ')")
    @Operation(summary = "List archived projects", security = @SecurityRequirement(name = "bearer"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Archived projects retrieved", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission")
    })
    public ApiResponse<List<ProjectResponse>> listArchived(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId
    ) {
        return ApiResponse.success("Archived projects retrieved successfully.", projectService.listArchived(workspaceId, departmentId));
    }

    @PutMapping("/{projectId}")
    @PreAuthorize("@departmentAuth.canManageDepartmentProjects(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'PROJECT_UPDATE')")
    @Operation(summary = "Update a project", security = @SecurityRequirement(name = "bearer"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Project updated", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Name already in use")
    })
    public ApiResponse<ProjectResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        return ApiResponse.success("Project updated successfully.", projectService.update(workspaceId, departmentId, projectId, request));
    }

    @PutMapping("/{projectId}/restore")
    @PreAuthorize("@departmentAuth.canManageDepartmentProjects(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'PROJECT_UPDATE')")
    @Operation(summary = "Restore an archived project", security = @SecurityRequirement(name = "bearer"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Project restored", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Project is not archived"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    public ApiResponse<ProjectResponse> restore(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId
    ) {
        return ApiResponse.success("Project restored successfully.", projectService.restore(workspaceId, departmentId, projectId));
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canDeleteWorkspace(#workspaceId, authentication) && @departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'PROJECT_DELETE')")
    @Operation(summary = "Delete (archive) a project", security = @SecurityRequirement(name = "bearer"))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Project archived"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Project not found")
    })
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId
    ) {
        projectService.delete(workspaceId, departmentId, projectId);
    }
}
