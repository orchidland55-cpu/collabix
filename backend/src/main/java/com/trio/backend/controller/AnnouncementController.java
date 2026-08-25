package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.announcement.AnnouncementResponse;
import com.trio.backend.dto.announcement.CreateAnnouncementRequest;
import com.trio.backend.dto.announcement.UpdateAnnouncementRequest;
import com.trio.backend.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements", description = "Endpoints for workspace/department/team announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ANNOUNCEMENT_CREATE')")
    @Operation(summary = "Create an announcement", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<AnnouncementResponse> create(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateAnnouncementRequest request
    ) {
        return ApiResponse.success(
                "Announcement created successfully.",
                announcementService.create(workspaceId, request)
        );
    }

    @GetMapping("/{announcementId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ANNOUNCEMENT_READ')")
    @Operation(summary = "Get an announcement by ID", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<AnnouncementResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID announcementId
    ) {
        return ApiResponse.success(
                "Announcement resorteved successfully.",
                announcementService.getById(workspaceId, announcementId)
        );
    }

    @GetMapping
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ANNOUNCEMENT_READ')")
    @Operation(summary = "List workspace announcements", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<Page<AnnouncementResponse>> listWorkspace(
            @PathVariable UUID workspaceId,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Announcements resorteved successfully.",
                announcementService.listWorkspaceAnnouncements(workspaceId, pageable)
        );
    }

    @GetMapping("/departments/{departmentId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ANNOUNCEMENT_READ')")
    @Operation(summary = "List department announcements", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<Page<AnnouncementResponse>> listDepartment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Department announcements resorteved successfully.",
                announcementService.listDepartmentAnnouncements(workspaceId, departmentId, pageable)
        );
    }

    @GetMapping("/teams/{teamId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ANNOUNCEMENT_READ')")
    @Operation(summary = "List team announcements", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<Page<AnnouncementResponse>> listTeam(
            @PathVariable UUID workspaceId,
            @PathVariable UUID teamId,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Team announcements resorteved successfully.",
                announcementService.listTeamAnnouncements(workspaceId, teamId, pageable)
        );
    }

    @GetMapping("/projects/{projectId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ANNOUNCEMENT_READ')")
    @Operation(summary = "List project announcements", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<Page<AnnouncementResponse>> listProject(
            @PathVariable UUID workspaceId,
            @PathVariable UUID projectId,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Project announcements resorteved successfully.",
                announcementService.listProjectAnnouncements(workspaceId, projectId, pageable)
        );
    }

    @PutMapping("/{announcementId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ANNOUNCEMENT_UPDATE')")
    @Operation(summary = "Update an announcement", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<AnnouncementResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID announcementId,
            @Valid @RequestBody UpdateAnnouncementRequest request
    ) {
        return ApiResponse.success(
                "Announcement updated successfully.",
                announcementService.update(workspaceId, announcementId, request)
        );
    }

    @DeleteMapping("/{announcementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ANNOUNCEMENT_DELETE')")
    @Operation(summary = "Delete (archive) an announcement", security = @SecurityRequirement(name = "bearer"))
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID announcementId
    ) {
        announcementService.delete(workspaceId, announcementId);
    }
}
