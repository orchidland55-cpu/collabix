package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.ai.HandoverAIEditRequest;
import com.trio.backend.dto.ai.HandoverAIGenerateRequest;
import com.trio.backend.dto.ai.HandoverAIResponse;
import com.trio.backend.service.HandoverAIService;
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
@RequestMapping("/api/handover/ai")
@RequiredArgsConstructor
@Tag(name = "Handover AI", description = "AI-powered Handover Journal generation and management")
@SecurityRequirement(name = "bearerAuth")
public class HandoverAIController {

    private final HandoverAIService handoverAIService;

    @PreAuthorize("@workspaceAuth.canCreateArtifact(#request.workspaceId, #request.departmentId, #request.projectId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_CREATE')")
    @Operation(summary = "Generate an AI handover journal")
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HandoverAIResponse> generate(@Valid @RequestBody HandoverAIGenerateRequest request) {
        HandoverAIResponse response = handoverAIService.generate(
                request.getWorkspaceId(),
                request.getDepartmentId(),
                request.getProjectId(),
                request.getDate(),
                request.getShift()
        );
        return ApiResponse.success("AI handover journal generated successfully.", response);
    }

    @PreAuthorize("@workspaceAuth.canCreateArtifact(#request.workspaceId, #request.departmentId, #request.projectId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_CREATE')")
    @Operation(summary = "Regenerate an existing AI handover journal")
    @PostMapping("/regenerate/{journalId}")
    public ApiResponse<HandoverAIResponse> regenerate(
            @PathVariable UUID journalId,
            @Valid @RequestBody HandoverAIGenerateRequest request) {
        HandoverAIResponse response = handoverAIService.regenerate(
                request.getWorkspaceId(),
                request.getDepartmentId(),
                request.getProjectId(),
                journalId,
                request.getDate(),
                request.getShift()
        );
        return ApiResponse.success("AI handover journal regenerated successfully.", response);
    }

    @PreAuthorize("@workspaceAuth.canCreateArtifact(#editRequest.workspaceId, #editRequest.departmentId, #editRequest.projectId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_UPDATE')")
    @Operation(summary = "Edit an AI handover journal")
    @PutMapping("/{journalId}")
    public ApiResponse<HandoverAIResponse> edit(
            @PathVariable UUID journalId,
            @Valid @RequestBody HandoverAIEditRequest editRequest) {
        HandoverAIResponse response = handoverAIService.edit(
                editRequest.getWorkspaceId(),
                editRequest.getDepartmentId(),
                editRequest.getProjectId(),
                journalId,
                editRequest
        );
        return ApiResponse.success("AI handover journal updated successfully.", response);
    }

    @PreAuthorize("@workspaceAuth.canCreateArtifact(#request.workspaceId, #request.departmentId, #request.projectId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_UPDATE')")
    @Operation(summary = "Approve an AI handover journal")
    @PostMapping("/{journalId}/approve")
    public ApiResponse<HandoverAIResponse> approve(
            @PathVariable UUID journalId,
            @Valid @RequestBody HandoverAIGenerateRequest request) {
        HandoverAIResponse response = handoverAIService.approve(
                request.getWorkspaceId(),
                request.getDepartmentId(),
                request.getProjectId(),
                journalId
        );
        return ApiResponse.success("AI handover journal approved.", response);
    }

    @PreAuthorize("@workspaceAuth.canCreateArtifact(#request.workspaceId, #request.departmentId, #request.projectId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_UPDATE')")
    @Operation(summary = "Reject an AI handover journal")
    @PostMapping("/{journalId}/reject")
    public ApiResponse<HandoverAIResponse> reject(
            @PathVariable UUID journalId,
            @Valid @RequestBody HandoverAIGenerateRequest request) {
        HandoverAIResponse response = handoverAIService.reject(
                request.getWorkspaceId(),
                request.getDepartmentId(),
                request.getProjectId(),
                journalId
        );
        return ApiResponse.success("AI handover journal rejected.", response);
    }
}
