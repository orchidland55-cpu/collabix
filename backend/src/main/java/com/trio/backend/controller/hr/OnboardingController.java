package com.trio.backend.controller.hr;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.hr.CreateOnboardingRequest;
import com.trio.backend.dto.hr.CreateOnboardingTaskRequest;
import com.trio.backend.dto.hr.OnboardingResponse;
import com.trio.backend.dto.hr.OnboardingSearchCriteria;
import com.trio.backend.dto.hr.OnboardingStatistics;
import com.trio.backend.dto.hr.OnboardingTaskResponse;
import com.trio.backend.dto.hr.UpdateOnboardingRequest;
import com.trio.backend.dto.hr.UpdateOnboardingTaskRequest;
import com.trio.backend.service.hr.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/departments/{departmentId}/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ONBOARDING_CREATE')")
    public ApiResponse<OnboardingResponse> create(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @Valid @RequestBody CreateOnboardingRequest request) {
        return ApiResponse.success("Onboarding created successfully.",
                onboardingService.create(workspaceId, departmentId, request));
    }

    @GetMapping("/{onboardingId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ONBOARDING_READ')")
    public ApiResponse<OnboardingResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID onboardingId) {
        return ApiResponse.success("Onboarding resorteved successfully.",
                onboardingService.getById(workspaceId, departmentId, onboardingId));
    }

    @GetMapping
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ONBOARDING_READ')")
    public ApiResponse<Page<OnboardingResponse>> list(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            OnboardingSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success("Onboardings resorteved successfully.",
                onboardingService.list(workspaceId, departmentId, criteria, pageable));
    }

    @PutMapping("/{onboardingId}")
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ONBOARDING_UPDATE')")
    public ApiResponse<OnboardingResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID onboardingId,
            @Valid @RequestBody UpdateOnboardingRequest request) {
        return ApiResponse.success("Onboarding updated successfully.",
                onboardingService.update(workspaceId, departmentId, onboardingId, request));
    }

    @DeleteMapping("/{onboardingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ONBOARDING_DELETE')")
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID onboardingId) {
        onboardingService.delete(workspaceId, departmentId, onboardingId);
    }

    @PostMapping("/{onboardingId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ONBOARDING_TASK_MANAGE')")
    public ApiResponse<OnboardingTaskResponse> addTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID onboardingId,
            @Valid @RequestBody CreateOnboardingTaskRequest request) {
        return ApiResponse.success("Task added successfully.",
                onboardingService.addTask(workspaceId, departmentId, onboardingId, request));
    }

    @GetMapping("/{onboardingId}/tasks")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ONBOARDING_READ')")
    public ApiResponse<List<OnboardingTaskResponse>> listTasks(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID onboardingId) {
        return ApiResponse.success("Tasks resorteved successfully.",
                onboardingService.listTasks(workspaceId, departmentId, onboardingId));
    }

    @PutMapping("/{onboardingId}/tasks/{taskId}")
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ONBOARDING_TASK_MANAGE')")
    public ApiResponse<OnboardingTaskResponse> updateTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID onboardingId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateOnboardingTaskRequest request) {
        return ApiResponse.success("Task updated successfully.",
                onboardingService.updateTask(workspaceId, departmentId, onboardingId, taskId, request));
    }

    @PutMapping("/{onboardingId}/tasks/{taskId}/complete")
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ONBOARDING_TASK_MANAGE')")
    public ApiResponse<OnboardingTaskResponse> completeTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID onboardingId,
            @PathVariable UUID taskId) {
        return ApiResponse.success("Task Completed successfully.",
                onboardingService.CompleteTask(workspaceId, departmentId, onboardingId, taskId));
    }

    @PutMapping("/{onboardingId}/tasks/{taskId}/skip")
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ONBOARDING_TASK_MANAGE')")
    public ApiResponse<OnboardingTaskResponse> skipTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID onboardingId,
            @PathVariable UUID taskId) {
        return ApiResponse.success("Task skipped successfully.",
                onboardingService.skipTask(workspaceId, departmentId, onboardingId, taskId));
    }

    @DeleteMapping("/{onboardingId}/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ONBOARDING_TASK_MANAGE')")
    public void deleteTask(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID onboardingId,
            @PathVariable UUID taskId) {
        onboardingService.deleteTask(workspaceId, departmentId, onboardingId, taskId);
    }

    @GetMapping("/stats")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ONBOARDING_READ')")
    public ApiResponse<OnboardingStatistics> getStatistics(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId) {
        return ApiResponse.success("Onboarding statistics resorteved successfully.",
                onboardingService.getStatistics(workspaceId, departmentId));
    }
}
