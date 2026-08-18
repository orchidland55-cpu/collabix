package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.alert.AlertResponse;
import com.trio.backend.dto.alert.AlertSearchCriteria;
import com.trio.backend.security.user.CustomUserDetails;
import com.trio.backend.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing Alerts.
 *
 * <p>All endpoints operate on the authenticated user's own alerts within a
 * workspace. Ownership is enforced in the service layer; the permission
 * checks here gate feature access per role.</p>
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Endpoints for managing alerts")
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/{alertId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ALERT_READ')")
    @Operation(
            summary = "Retrieve an alert",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the information of a specific alert owned by the authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alert found", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public ApiResponse<AlertResponse> getById(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId,
            @Parameter(description = "ID of the alert", required = true)
            @PathVariable UUID alertId
    ) {
        return ApiResponse.success(
                "Alert retrieved successfully.",
                alertService.getById(workspaceId, alertId)
        );
    }

    @GetMapping
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ALERT_READ')")
    @Operation(
            summary = "List alerts of the connected user",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the paginated list of the authenticated user's non-archived alerts in a workspace, optionally filtered by status, type and severity."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List retrieved", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission")
    })
    public ApiResponse<Page<AlertResponse>> list(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            AlertSearchCriteria criteria,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Alerts retrieved successfully.",
                alertService.list(workspaceId, currentUser.getId(), criteria, pageable)
        );
    }

    @GetMapping("/unread/count")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ALERT_READ')")
    @Operation(
            summary = "Count unread alerts",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the number of unread alerts for the authenticated user in a workspace."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Count retrieved", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission")
    })
    public ApiResponse<Long> countUnread(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Unread count retrieved successfully.",
                alertService.countUnread(workspaceId, currentUser.getId())
        );
    }

    @PutMapping("/{alertId}/read")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ALERT_UPDATE')")
    @Operation(
            summary = "Mark an alert as read",
            security = @SecurityRequirement(name = "bearer"),
            description = "Marks a specific alert of the authenticated user as read."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alert marked as read", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public ApiResponse<AlertResponse> markAsRead(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId,
            @Parameter(description = "ID of the alert", required = true)
            @PathVariable UUID alertId
    ) {
        return ApiResponse.success(
                "Alert marked as read.",
                alertService.markAsRead(workspaceId, alertId)
        );
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ALERT_UPDATE')")
    @Operation(
            summary = "Mark all alerts as read",
            security = @SecurityRequirement(name = "bearer"),
            description = "Marks all unread alerts of the authenticated user in a workspace as read."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Alerts marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission")
    })
    public ApiResponse<Void> markAllAsRead(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        alertService.markAllAsRead(workspaceId, currentUser.getId());
        return ApiResponse.success("All alerts marked as read.");
    }

    @DeleteMapping("/{alertId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ALERT_DELETE')")
    @Operation(
            summary = "Dismiss an alert",
            security = @SecurityRequirement(name = "bearer"),
            description = "Dismisses (soft-deletes) a specific alert of the authenticated user."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Alert dismissed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public void dismiss(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId,
            @Parameter(description = "ID of the alert", required = true)
            @PathVariable UUID alertId
    ) {
        alertService.dismiss(workspaceId, alertId);
    }
}
