package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.ai.ReportingEditRequest;
import com.trio.backend.dto.ai.ReportingGenerateRequest;
import com.trio.backend.dto.ai.ReportingResponse;
import com.trio.backend.service.ReportingAIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports/ai")
@RequiredArgsConstructor
@Tag(name = "Reporting AI", description = "AI-powered Executive Report generation and management")
@SecurityRequirement(name = "bearerAuth")
public class ReportingAIController {

    private final ReportingAIService reportingAIService;

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'REPORT_CREATE')")
    @Operation(summary = "Generate an AI executive report")
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportingResponse> generate(@Valid @RequestBody ReportingGenerateRequest request) {
        ReportingResponse response = reportingAIService.generate(request);
        return ApiResponse.success("Executive report generated successfully.", response);
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'REPORT_CREATE')")
    @Operation(summary = "Regenerate an existing AI executive report")
    @PostMapping("/regenerate/{reportId}")
    public ApiResponse<ReportingResponse> regenerate(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReportingGenerateRequest request) {
        ReportingResponse response = reportingAIService.regenerate(
                request.getWorkspaceId(),
                request.getDepartmentId(),
                request.getProjectId(),
                reportId
        );
        return ApiResponse.success("Executive report regenerated successfully.", response);
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'REPORT_UPDATE')")
    @Operation(summary = "Edit an AI executive report")
    @PutMapping("/{reportId}")
    public ApiResponse<ReportingResponse> edit(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReportingEditRequest editRequest) {
        ReportingResponse response = reportingAIService.edit(
                editRequest.getWorkspaceId(),
                editRequest.getDepartmentId(),
                editRequest.getProjectId(),
                reportId,
                editRequest
        );
        return ApiResponse.success("Executive report updated successfully.", response);
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'REPORT_UPDATE')")
    @Operation(summary = "Approve an AI executive report")
    @PostMapping("/{reportId}/approve")
    public ApiResponse<ReportingResponse> approve(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReportingGenerateRequest request) {
        ReportingResponse response = reportingAIService.approve(
                request.getWorkspaceId(),
                request.getDepartmentId(),
                request.getProjectId(),
                reportId
        );
        return ApiResponse.success("Executive report approved.", response);
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'REPORT_UPDATE')")
    @Operation(summary = "Reject an AI executive report")
    @PostMapping("/{reportId}/reject")
    public ApiResponse<ReportingResponse> reject(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReportingGenerateRequest request) {
        ReportingResponse response = reportingAIService.reject(
                request.getWorkspaceId(),
                request.getDepartmentId(),
                request.getProjectId(),
                reportId
        );
        return ApiResponse.success("Executive report rejected.", response);
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'REPORT_READ')")
    @Operation(summary = "Retrieve executive report history")
    @GetMapping("/history")
    public ApiResponse<Page<ReportingResponse>> getHistory(
            @RequestParam UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ReportingResponse> history = reportingAIService.getHistory(workspaceId, page, size);
        return ApiResponse.success("History retrieved successfully.", history);
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'REPORT_READ')")
    @Operation(summary = "Retrieve a single executive report")
    @GetMapping("/{reportId}")
    public ApiResponse<ReportingResponse> getById(
            @PathVariable UUID reportId,
            @RequestParam UUID workspaceId) {
        return ApiResponse.success("Executive report retrieved successfully.",
                reportingAIService.getById(workspaceId, reportId));
    }
}
