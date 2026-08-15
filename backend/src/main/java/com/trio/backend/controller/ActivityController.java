package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.organisation.activity.ActivityResponse;
import com.trio.backend.dto.organisation.activity.CreateActivityRequest;
import com.trio.backend.dto.organisation.activity.UpdateActivityRequest;
import com.trio.backend.service.ActivityService;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller responsible for managing Activities.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/departments/{departmentId}/projects/{projectId}/tasks/{taskId}/activities")
@RequiredArgsConstructor
@Tag(name = "Activities", description = "Endpoints for managing Activities (organization)")
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@departmentAuth.canManageDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ACTIVITY_CREATE')")
    @Operation(
            summary = "Create a activity",
            security = @SecurityRequirement(name = "bearer"),
            description = "Creates a activity in the task."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Activity createde", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ApiResponse<ActivityResponse> create(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId,
            @Parameter(description = "ID of the department", required = true)
            @PathVariable UUID departmentId,
            @Parameter(description = "ID of the project", required = true)
            @PathVariable UUID projectId,
            @Parameter(description = "ID of the task", required = true)
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateActivityRequest request
    ) {
        return ApiResponse.success(
                "Activity created successfully.",
                activityService.create(workspaceId, departmentId, projectId, taskId, request)
        );
    }

    @GetMapping("/{activityId}")
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ACTIVITY_READ')")
    @Operation(
            summary = "Resorteve a activity",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the information of a activity."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Activity found", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ApiResponse<ActivityResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID activityId
    ) {
        return ApiResponse.success(
                "Activity resorteved successfully.",
                activityService.getById(workspaceId, departmentId, projectId, taskId, activityId)
        );
    }

    @GetMapping
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ACTIVITY_READ')")
    @Operation(
            summary = "List activities of a task",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the list des activities actives of a task."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List resorteved", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission")
    })
    public ApiResponse<Page<ActivityResponse>> list(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Activities resorteved successfully.",
                activityService.list(workspaceId, departmentId, projectId, taskId, pageable)
        );
    }

    @PutMapping("/{activityId}")
    @PreAuthorize("@departmentAuth.canManageDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ACTIVITY_UPDATE')")
    @Operation(
            summary = "Update a activity",
            security = @SecurityRequirement(name = "bearer"),
            description = "Updates a activity (partial update)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Activity updated", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public ApiResponse<ActivityResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID activityId,
            @Valid @RequestBody UpdateActivityRequest request
    ) {
        return ApiResponse.success(
                "Activity updated successfully.",
                activityService.update(workspaceId, departmentId, projectId, taskId, activityId, request)
        );
    }

    @DeleteMapping("/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canDeleteWorkspace(#workspaceId, authentication) && @departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ACTIVITY_DELETE')")
    @Operation(
            summary = "Delete a activity",
            security = @SecurityRequirement(name = "bearer"),
            description = "Supprime (soft delete) une activity."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Activity deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Activity not found")
    })
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID activityId
    ) {
        activityService.delete(workspaceId, departmentId, projectId, taskId, activityId);
    }
}