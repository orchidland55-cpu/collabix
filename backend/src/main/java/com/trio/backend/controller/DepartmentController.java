package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.organisation.department.CreateDepartmentRequest;
import com.trio.backend.dto.organisation.department.DepartmentDetailsResponse;
import com.trio.backend.dto.organisation.department.DepartmentResponse;
import com.trio.backend.dto.organisation.department.DepartmentSummaryResponse;
import com.trio.backend.dto.organisation.department.UpdateDepartmentRequest;
import com.trio.backend.service.DepartmentService;
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
 * REST controller responsible for managing Department.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Endpoints for managing Departments (organization)")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'DEPARTMENT_CREATE')")
    @Operation(
            summary = "Create a department",
            security = @SecurityRequirement(name = "bearer"),
            description = "Creates a department in the workspace.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Department created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Workspace not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Department name already in use")
    })
    public ApiResponse<DepartmentResponse> create(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateDepartmentRequest request
    ) {
        return ApiResponse.success("Department created successfully.", departmentService.create(workspaceId, request));
    }

    @GetMapping("/{departmentId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'DEPARTMENT_READ')")
    @Operation(
            summary = "Resorteve a department",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the information d'un department." )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Department found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ApiResponse<DepartmentResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId
    ) {
        return ApiResponse.success("Department resorteved successfully.", departmentService.getById(workspaceId, departmentId));
    }

    @GetMapping
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'DEPARTMENT_READ')")
    @Operation(
            summary = "List departments",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the list des departments of the workspace. " +
                    "Active only by default; pass includeArchived=true to also include archived departments." )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "List resorteved",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ApiResponse<List<DepartmentSummaryResponse>> listByWorkspace(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return ApiResponse.success("Departments resorteved successfully.", departmentService.listByWorkspace(workspaceId, includeArchived));
    }

    @GetMapping("/{departmentId}/details")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'DEPARTMENT_READ')")
    @Operation(
            summary = "Details of a department",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns a detailed variant of a department." )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Details resorteved",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ApiResponse<DepartmentDetailsResponse> getDetails(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId
    ) {
        return ApiResponse.success("Department details resorteved successfully.", departmentService.getDetails(workspaceId, departmentId));
    }

    @PutMapping("/{departmentId}")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'DEPARTMENT_UPDATE')")
    @Operation(
            summary = "Update a department",
            security = @SecurityRequirement(name = "bearer"),
            description = "Updates a department (partial update).")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Department updated",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Department not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Name already in use")
    })
    public ApiResponse<DepartmentResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @Valid @RequestBody UpdateDepartmentRequest request
    ) {
        return ApiResponse.success("Department updated successfully.", departmentService.update(workspaceId, departmentId, request));
    }

    @PutMapping("/{departmentId}/restore")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'DEPARTMENT_UPDATE')")
    @Operation(
            summary = "Restore an archived department",
            security = @SecurityRequirement(name = "bearer"),
            description = "Restores an archived department back to ACTIVE status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Department restored",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Department is not archived"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ApiResponse<DepartmentResponse> restore(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId
    ) {
        return ApiResponse.success("Department restored successfully.", departmentService.restore(workspaceId, departmentId));
    }

    @DeleteMapping("/{departmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'DEPARTMENT_DELETE')")
    @Operation(
            summary = "Archive a department",
            security = @SecurityRequirement(name = "bearer"),
            description = "Supprime (soft delete) un department. Refused if the department contains active teams." )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Department deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Department not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Department still contains active teams")
    })
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId
    ) {
        departmentService.delete(workspaceId, departmentId);
    }

    @DeleteMapping("/{departmentId}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'DEPARTMENT_DELETE')")
    @Operation(
            summary = "Permanently delete a department",
            security = @SecurityRequirement(name = "bearer"),
            description = "Permanently removes a department from the database. This is irreversible. " +
                    "Refused if the department still contains related business records (teams, projects, HR data, etc.).")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Department permanently deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Department not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Department still contains related records")
    })
    public void deletePermanently(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId
    ) {
        departmentService.deletePermanently(workspaceId, departmentId);
    }
}

