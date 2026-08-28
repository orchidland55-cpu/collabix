package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.enums.AdminAnalyticsPeriod;
import com.trio.backend.reporting.analytics.dto.admin.ActivityOverviewResponse;
import com.trio.backend.reporting.analytics.dto.admin.AdminProjectStatusResponse;
import com.trio.backend.reporting.analytics.dto.chart.ChartData;
import com.trio.backend.reporting.analytics.dto.metrics.ActivityMetrics;
import com.trio.backend.reporting.analytics.dto.metrics.DocumentMetrics;
import com.trio.backend.reporting.analytics.dto.metrics.NotificationMetrics;
import com.trio.backend.reporting.analytics.dto.metrics.TaskMetrics;
import com.trio.backend.reporting.analytics.dto.metrics.WorkspaceAnalyticsResponse;
import com.trio.backend.service.AdminDashboardAnalyticsService;
import com.trio.backend.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Endpoints for mÃ©sortques et l'analytique workspace")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AdminDashboardAnalyticsService adminDashboardAnalyticsService;

    @GetMapping
    @PreAuthorize("(@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) || hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')) && @permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(
            summary = "Analytique Complete of the workspace",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns l'ensemble des mÃ©sortques of the workspace (tasks, activitys, documents, notifications)."
    )
    public ApiResponse<WorkspaceAnalyticsResponse> getWorkspaceAnalytics(
            @PathVariable UUID workspaceId
    ) {
        return ApiResponse.success(
                "Workspace analytics resorteved successfully.",
                analyticsService.getWorkspaceAnalytics(workspaceId)
        );
    }

    @GetMapping("/tasks")
    @PreAuthorize("(@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) || hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')) && @permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(
            summary = "MÃ©sortques of tasks",
            security = @SecurityRequirement(name = "bearer")
    )
    public ApiResponse<TaskMetrics> getTaskMetrics(
            @PathVariable UUID workspaceId
    ) {
        return ApiResponse.success(
                "Task metrics resorteved successfully.",
                analyticsService.getWorkspaceAnalytics(workspaceId).getTasks()
        );
    }

    @GetMapping("/activities")
    @PreAuthorize("(@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) || hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')) && @permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(
            summary = "MÃ©sortques des activitys",
            security = @SecurityRequirement(name = "bearer")
    )
    public ApiResponse<ActivityMetrics> getActivityMetrics(
            @PathVariable UUID workspaceId
    ) {
        return ApiResponse.success(
                "Activity metrics resorteved successfully.",
                analyticsService.getWorkspaceAnalytics(workspaceId).getActivities()
        );
    }

    @GetMapping("/documents")
    @PreAuthorize("(@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) || hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')) && @permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(
            summary = "MÃ©sortques des documents",
            security = @SecurityRequirement(name = "bearer")
    )
    public ApiResponse<DocumentMetrics> getDocumentMetrics(
            @PathVariable UUID workspaceId
    ) {
        return ApiResponse.success(
                "Document metrics resorteved successfully.",
                analyticsService.getWorkspaceAnalytics(workspaceId).getDocuments()
        );
    }

    @GetMapping("/notifications")
    @PreAuthorize("(@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) || hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')) && @permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(
            summary = "MÃ©sortques of notifications",
            security = @SecurityRequirement(name = "bearer")
    )
    public ApiResponse<NotificationMetrics> getNotificationMetrics(
            @PathVariable UUID workspaceId
    ) {
        return ApiResponse.success(
                "Notification metrics resorteved successfully.",
                analyticsService.getWorkspaceAnalytics(workspaceId).getNotifications()
        );
    }

    @GetMapping("/charts")
    @PreAuthorize("(@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) || hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')) && @permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(
            summary = "Data pour les graphicals",
            security = @SecurityRequirement(name = "bearer")
    )
    public ApiResponse<List<ChartData>> getChartData(
            @PathVariable UUID workspaceId
    ) {
        return ApiResponse.success(
                "Chart data resorteved successfully.",
                analyticsService.getWorkspaceAnalytics(workspaceId).getCharts()
        );
    }

    @GetMapping("/admin/activity-overview")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(
            summary = "Admin activity overview time series",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns daily activity counts across the workspace for admin dashboard charts."
    )
    public ApiResponse<ActivityOverviewResponse> getAdminActivityOverview(
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "THIS_WEEK") AdminAnalyticsPeriod period
    ) {
        return ApiResponse.success(
                "Admin activity overview retrieved successfully.",
                adminDashboardAnalyticsService.getActivityOverview(workspaceId, period)
        );
    }

    @GetMapping("/admin/project-status")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(
            summary = "Admin project status distribution",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns workflow status distribution for all active projects in the workspace."
    )
    public ApiResponse<AdminProjectStatusResponse> getAdminProjectStatus(
            @PathVariable UUID workspaceId
    ) {
        return ApiResponse.success(
                "Admin project status retrieved successfully.",
                adminDashboardAnalyticsService.getProjectStatus(workspaceId)
        );
    }
}
