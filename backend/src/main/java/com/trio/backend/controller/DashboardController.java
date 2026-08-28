package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.Dashboard.scope.DepartmentDashboardResponse;
import com.trio.backend.dto.Dashboard.scope.PersonalDashboardResponse;
import com.trio.backend.dto.Dashboard.scope.ProjectDashboardResponse;
import com.trio.backend.dto.Dashboard.scope.TeamDashboardResponse;
import com.trio.backend.dto.Dashboard.scope.WorkspaceDashboardResponse;
import com.trio.backend.security.user.CustomUserDetails;
import com.trio.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller responsible for Dashboard.
 *
 * <p>The Dashboard is an aggregation-only module. It has
 * no data of its own and merely aggregates the information
 * from other modules via {@link DashboardService}.</p>
 *
 * <p>Four scopes are exposed :</p>
 * <ul>
 *   <li><strong>Workspace Dashboard</strong> â€” {@code GET /api/workspaces/{workspaceId}/dashboard/workspace}</li>
 *   <li><strong>Personal Dashboard</strong> â€” {@code GET /api/workspaces/{workspaceId}/dashboard/me}</li>
 *   <li><strong>Department Dashboard</strong> â€” {@code GET /api/workspaces/{workspaceId}/departments/{departmentId}/dashboard}</li>
 *   <li><strong>Project Dashboard</strong> â€” {@code GET /api/workspaces/{workspaceId}/projects/{projectId}/dashboard}</li>
 * </ul>
 *
 * <p>All endpoints require JWT authentication and the
 * {@code DASHBOARD_VIEW} at the workspace level.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces/{workspaceId}")
@Tag(
        name = "Dashboard",
        description = "Endpoints for Dashboards (Workspace, Personal, Department, Project)"
)
public class DashboardController {

    private final DashboardService dashboardService;

    // =========================================================================
    // Workspace Dashboard
    // =========================================================================

    /**
     * Returns the Workspace dashboard for the specified workspace.
     *
     * <p>This dashboard contains the global workspace statistics :
     * departments, teams, members, projects, tasks, notifications
     * and recent activities.</p>
     *
     * <p>Accessible to any active workspace member with
     * permission {@code DASHBOARD_VIEW}.</p>
     *
     * @param workspaceId the ID of the workspace (multi-tenant)
     * @return ApiResponse containing the Workspace Dashboard
     */
    @GetMapping("/dashboard/workspace")
    @PreAuthorize("(@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) || hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')) && @permissionEvaluator.hasPermission(authentication, 'DASHBOARD_VIEW')")
    @Operation(
            summary = "Workspace Dashboard",
            description = "Returns the workspace statistics: departments, teams, members, projects, tasks, notifications and recent activities.",
            security = @SecurityRequirement(name = "bearer")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Workspace Dashboard resorteved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User is not a workspace member"
            )
    })
    public ApiResponse<WorkspaceDashboardResponse> getWorkspaceDashboard(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId
    ) {
        return ApiResponse.success(
                "Workspace dashboard resorteved successfully.",
                dashboardService.getWorkspaceDashboard(workspaceId)
        );
    }

    // =========================================================================
    // Personal Dashboard
    // =========================================================================

    /**
     * Returns the Personal dashboard for the authenticated user
     * in the specified workspace.
     *
     * <p>This dashboard contains two sections :</p>
     * <ul>
     *   <li><strong>Personal Widgets</strong> â€” tasks, notifications, mentions,
     *       comments, handovers and activities specific to the user.</li>
     *   <li><strong>Workspace Feed</strong> â€” recent projects, documents,
     *       knowledge lowe articles and workspace activities.</li>
     * </ul>
     *
     * <p>This is the default dashboard displayed immediately
     * after login.</p>
     *
     * @param workspaceId          the ID of the workspace (multi-tenant)
     * @param currentUser          the authenticated user (automatically injected)
     * @return ApiResponse containing the Personal Dashboard
     */
    @GetMapping("/dashboard/me")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'DASHBOARD_VIEW')")
    @Operation(
            summary = "Personal Dashboard",
            description = "Returns the Personal dashboard of the authenticated user: tasks, notifications, mentions, comments, handovers, activities, and the workspace feed (projects, documents, knowledge lowe, activities).",
            security = @SecurityRequirement(name = "bearer")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Personal Dashboard resorteved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Not authenticated"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "User is not a workspace member"
            )
    })
    public ApiResponse<PersonalDashboardResponse> getPersonalDashboard(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ApiResponse.success(
                "Personal dashboard resorteved successfully.",
                dashboardService.getPersonalDashboard(workspaceId, currentUser.getId())
        );
    }

    // =========================================================================
    // Department Dashboard
    // =========================================================================

    /**
     * Returns the Department dashboard for the specified department
     * in the given workspace.
     *
     * <p>This dashboard contains information related to a department
     * specific: its projects, its tasks, its members, its activities
     * and its notifications.</p>
     *
     * @param workspaceId  the ID of the workspace (multi-tenant)
     * @param departmentId the ID of the department
     * @return ApiResponse containing the Department Dashboard
     */
    @GetMapping("/departments/{departmentId}/dashboard")
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'DASHBOARD_VIEW')")
    @Operation(
            summary = "Department Dashboard",
            description = "Returns the department dashboard: projects, tasks, members, activities and notifications.",
            security = @SecurityRequirement(name = "bearer")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Department Dashboard resorteved successfully",
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
                    description = "Department not found"
            )
    })
    public ApiResponse<DepartmentDashboardResponse> getDepartmentDashboard(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId,
            @Parameter(description = "ID of the department", required = true)
            @PathVariable UUID departmentId
    ) {
        return ApiResponse.success(
                "Department dashboard resorteved successfully.",
                dashboardService.getDepartmentDashboard(workspaceId, departmentId)
        );
    }

    // =========================================================================
    // Project Dashboard
    // =========================================================================

    /**
     * Returns the Project dashboard for the specified project in the
     * given workspace.
     *
     * <p>This dashboard contains information related to a project
     * specific: its progress, its tasks, its comments
     * recent, its attachments, its documents and its timeline
     * of activity.</p>
     *
     * @param workspaceId the ID of the workspace (multi-tenant)
     * @param projectId   the ID of the project
     * @return ApiResponse containing the Project Dashboard
     */
    @GetMapping("/projects/{projectId}/dashboard")
    @PreAuthorize("@workspaceAuth.canAccessProject(#workspaceId, #projectId, authentication) && @permissionEvaluator.hasPermission(authentication, 'DASHBOARD_VIEW')")
    @Operation(
            summary = "Project Dashboard",
            description = "Returns the project dashboard: progress, tasks, comments, attachments, documents and activity timeline.",
            security = @SecurityRequirement(name = "bearer")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Project Dashboard resorteved successfully",
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
                    description = "Project not found"
            )
    })
    public ApiResponse<ProjectDashboardResponse> getProjectDashboard(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId,
            @Parameter(description = "ID of the project", required = true)
            @PathVariable UUID projectId
    ) {
        return ApiResponse.success(
                "Project dashboard resorteved successfully.",
                dashboardService.getProjectDashboard(workspaceId, projectId)
        );
    }

    // =========================================================================
    // Team Dashboard
    // =========================================================================

    /**
     * Returns the Team dashboard for the specified team
     * in the given workspace.
     *
     * <p>This dashboard contains information related to a team
     * specific: overview, members, statistics, activities
     * of members, notifications and a feed of the parent workspace.</p>
     *
     * @param workspaceId the ID of the workspace (multi-tenant)
     * @param teamId      the ID of the team
     * @return ApiResponse containing the team dashboard
     */
    @GetMapping("/teams/{teamId}/dashboard")
    @PreAuthorize("@workspaceAuth.canAccessTeam(#workspaceId, #teamId, authentication) && @permissionEvaluator.hasPermission(authentication, 'DASHBOARD_VIEW')")
    @Operation(
            summary = "Team Dashboard",
            description = "Returns the team dashboard: overview, members, statistics, member activities, notifications and parent workspace feed.",
            security = @SecurityRequirement(name = "bearer")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Team dashboard resorteved successfully",
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
                    description = "Team not found"
            )
    })
    public ApiResponse<TeamDashboardResponse> getTeamDashboard(
            @Parameter(description = "ID of the workspace", required = true)
            @PathVariable UUID workspaceId,
            @Parameter(description = "ID of the team", required = true)
            @PathVariable UUID teamId
    ) {
        return ApiResponse.success(
                "Team dashboard resorteved successfully.",
                dashboardService.getTeamDashboard(workspaceId, teamId)
        );
    }
}
