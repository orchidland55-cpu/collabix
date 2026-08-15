package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.organisation.task.CreateTaskRequest;
import com.trio.backend.dto.organisation.task.TaskResponse;
import com.trio.backend.dto.organisation.task.UpdateTaskRequest;
import com.trio.backend.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/workspaces/{workspaceId}/departments/{departmentId}/projects/{projectId}/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Endpoints for managing Tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@departmentAuth.canManageDepartmentTasks(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TASK_CREATE')")
    @Operation(summary = "Create a task", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<TaskResponse> create(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        return ApiResponse.success("Task created successfully.", taskService.create(workspaceId, departmentId, projectId, request));
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TASK_READ')")
    @Operation(summary = "Get a task", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<TaskResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId
    ) {
        return ApiResponse.success("Task retrieved successfully.", taskService.getById(workspaceId, departmentId, projectId, taskId));
    }

    @GetMapping
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TASK_READ')")
    @Operation(summary = "List tasks with search and filters", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<Page<TaskResponse>> list(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) UUID assignee,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success("Tasks retrieved successfully.",
                taskService.list(workspaceId, departmentId, projectId, search, status, priority, assignee, pageable));
    }

    @GetMapping("/archived")
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TASK_READ')")
    @Operation(summary = "List archived tasks", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<List<TaskResponse>> listArchived(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId
    ) {
        return ApiResponse.success("Archived tasks retrieved successfully.", taskService.listArchived(workspaceId, departmentId, projectId));
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && (@permissionEvaluator.hasPermission(authentication, 'TASK_UPDATE') || @permissionEvaluator.hasPermission(authentication, 'TASK_READ'))")
    @Operation(summary = "Update a task", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<TaskResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return ApiResponse.success("Task updated successfully.", taskService.update(workspaceId, departmentId, projectId, taskId, request));
    }

    @PutMapping("/{taskId}/restore")
    @PreAuthorize("@departmentAuth.canManageDepartmentTasks(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TASK_UPDATE')")
    @Operation(summary = "Restore an archived task", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<TaskResponse> restore(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId
    ) {
        return ApiResponse.success("Task restored successfully.", taskService.restore(workspaceId, departmentId, projectId, taskId));
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@departmentAuth.canManageDepartmentTasks(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'TASK_DELETE')")
    @Operation(summary = "Archive a task", security = @SecurityRequirement(name = "bearer"))
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId
    ) {
        taskService.delete(workspaceId, departmentId, projectId, taskId);
    }
}
