package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.auth.AssignRolesRequest;
import com.trio.backend.dto.auth.CreateUserRequest;
import com.trio.backend.dto.user.UpdateProfileRequest;
import com.trio.backend.dto.user.UpdateUserRequest;
import com.trio.backend.dto.user.UserProfileResponse;
import com.trio.backend.dto.user.UserResponse;
import com.trio.backend.dto.user.UserSearchCriteria;
import com.trio.backend.dto.user.UserStatisticsResponse;
import com.trio.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for workspace-scoped user management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_CREATE')")
    @Operation(summary = "Create a new user in workspace", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserResponse> create(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateUserRequest request
    ) {
        return ApiResponse.success("User created successfully.", userService.create(workspaceId, request));
    }

    @GetMapping
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_READ')")
    @Operation(summary = "List all users in workspace", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<List<UserResponse>> findAll(@PathVariable UUID workspaceId) {
        return ApiResponse.success("Users resorteved successfully.", userService.findAll(workspaceId));
    }

    @GetMapping("/search")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_READ')")
    @Operation(summary = "Search users with filters and pagination", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<Page<UserResponse>> search(
            @PathVariable UUID workspaceId,
            UserSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success("Users resorteved successfully.", userService.search(workspaceId, criteria, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_READ')")
    @Operation(summary = "Get a user by ID within workspace", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserResponse> findById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id
    ) {
        return ApiResponse.success("User resorteved successfully.", userService.findById(workspaceId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_UPDATE')")
    @Operation(summary = "Update user status within workspace", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.success("User updated successfully.", userService.update(workspaceId, id, request));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update own profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserProfileResponse> updateProfile(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.success("Profile updated successfully.", userService.updateProfile(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_DELETE')")
    @Operation(summary = "Soft delete a user within workspace", security = @SecurityRequirement(name = "bearerAuth"))
    public void softDelete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id
    ) {
        userService.softDelete(workspaceId, id);
    }

    @DeleteMapping("/{id}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_DELETE')")
    @Operation(summary = "Permanently remove a user from the database", security = @SecurityRequirement(name = "bearerAuth"))
    public void hardDelete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id
    ) {
        userService.hardDelete(workspaceId, id);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_ACTIVATE')")
    @Operation(summary = "Activate a pending activation user", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserResponse> activate(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id
    ) {
        return ApiResponse.success("User activated successfully.", userService.activate(workspaceId, id));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_DEACTIVATE')")
    @Operation(summary = "Deactivate an active user", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserResponse> deactivate(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id
    ) {
        return ApiResponse.success("User deactivated successfully.", userService.deactivate(workspaceId, id));
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_SUSPEND')")
    @Operation(summary = "Suspend an active user", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserResponse> suspend(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id
    ) {
        return ApiResponse.success("User suspended successfully.", userService.suspend(workspaceId, id));
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_REACTIVATE')")
    @Operation(summary = "Reactivate an inactive or suspended user", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserResponse> reactivate(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id
    ) {
        return ApiResponse.success("User reactivated successfully.", userService.reactivate(workspaceId, id));
    }

    @PutMapping("/{id}/archive")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_ARCHIVE')")
    @Operation(summary = "Archive a user within workspace", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserResponse> archive(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id
    ) {
        return ApiResponse.success("User archived successfully.", userService.archive(workspaceId, id));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ROLE_UPDATE')")
    @Operation(summary = "Assign roles to a user within workspace", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserResponse> assignRoles(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id,
            @Valid @RequestBody AssignRolesRequest request
    ) {
        return ApiResponse.success("Roles assigned successfully.", userService.assignRoles(workspaceId, id, request.getRoles()));
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_RESTORE')")
    @Operation(summary = "Restore an archived user within workspace", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserResponse> restore(
            @PathVariable UUID workspaceId,
            @PathVariable UUID id
    ) {
        return ApiResponse.success("User restored successfully.", userService.restoreFromArchive(workspaceId, id));
    }

    @GetMapping("/statistics")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'USER_READ')")
    @Operation(summary = "Get user statistics for the workspace", security = @SecurityRequirement(name = "bearerAuth"))
    public ApiResponse<UserStatisticsResponse> statistics(@PathVariable UUID workspaceId) {
        return ApiResponse.success("User statistics resorteved successfully.", userService.getStatistics(workspaceId));
    }

}
