package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.ai.AnalyticsAIEditRequest;
import com.trio.backend.dto.ai.AnalyticsAIGenerateRequest;
import com.trio.backend.dto.ai.AnalyticsAIResponse;
import com.trio.backend.service.AnalyticsAIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics/ai")
@RequiredArgsConstructor
@Tag(name = "Analytics AI", description = "AI-powered Analytics Executive Report generation and management")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsAIController {

    private final AnalyticsAIService analyticsAIService;

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(summary = "Generate an AI analytics executive report")
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AnalyticsAIResponse> generate(@Valid @RequestBody AnalyticsAIGenerateRequest request) {
        AnalyticsAIResponse response = analyticsAIService.generate(
                request.getWorkspaceId(),
                request.getDepartmentId(),
                request.getProjectId(),
                request.getTeamId(),
                request.getScope(),
                request.getStartDate(),
                request.getEndDate()
        );
        return ApiResponse.success("AI analytics report generated successfully.", response);
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(summary = "Regenerate an existing AI analytics report")
    @PostMapping("/regenerate/{reportId}")
    public ApiResponse<AnalyticsAIResponse> regenerate(
            @PathVariable UUID reportId,
            @Valid @RequestBody AnalyticsAIGenerateRequest request) {
        AnalyticsAIResponse response = analyticsAIService.regenerate(
                request.getWorkspaceId(),
                request.getDepartmentId(),
                request.getProjectId(),
                reportId
        );
        return ApiResponse.success("AI analytics report regenerated successfully.", response);
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(summary = "Edit an AI analytics report")
    @PutMapping("/{reportId}")
    public ApiResponse<AnalyticsAIResponse> edit(
            @PathVariable UUID reportId,
            @Valid @RequestBody AnalyticsAIEditRequest editRequest) {
        AnalyticsAIResponse response = analyticsAIService.edit(
                editRequest.getWorkspaceId(),
                editRequest.getDepartmentId(),
                editRequest.getProjectId(),
                reportId,
                editRequest
        );
        return ApiResponse.success("AI analytics report updated successfully.", response);
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(summary = "Approve an AI analytics report")
    @PostMapping("/{reportId}/approve")
    public ApiResponse<AnalyticsAIResponse> approve(
            @PathVariable UUID reportId,
            @Valid @RequestBody AnalyticsAIGenerateRequest request) {
        AnalyticsAIResponse response = analyticsAIService.approve(
                request.getWorkspaceId(),
                request.getDepartmentId(),
                request.getProjectId(),
                reportId
        );
        return ApiResponse.success("AI analytics report approved.", response);
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'ANALYTICS_VIEW')")
    @Operation(summary = "Reject an AI analytics report")
    @PostMapping("/{reportId}/reject")
    public ApiResponse<AnalyticsAIResponse> reject(
            @PathVariable UUID reportId,
            @Valid @RequestBody AnalyticsAIGenerateRequest request) {
        AnalyticsAIResponse response = analyticsAIService.reject(
                request.getWorkspaceId(),
                request.getDepartmentId(),
                request.getProjectId(),
                reportId
        );
        return ApiResponse.success("AI analytics report rejected.", response);
    }
}
