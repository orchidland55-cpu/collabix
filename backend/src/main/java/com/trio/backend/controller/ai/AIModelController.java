package com.trio.backend.controller.ai;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.ai.AIModelResponse;
import com.trio.backend.dto.ai.AIModelSearchCriteria;
import com.trio.backend.dto.ai.AIModelStatistics;
import com.trio.backend.dto.ai.CreateAIModelRequest;
import com.trio.backend.dto.ai.UpdateAIModelRequest;
import com.trio.backend.service.ai.AIModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/departments/{departmentId}/models")
@RequiredArgsConstructor
public class AIModelController {

    private final AIModelService aiModelService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'AI_MODEL_CREATE')")
    public ApiResponse<AIModelResponse> create(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @Valid @RequestBody CreateAIModelRequest request) {
        return ApiResponse.success("AI model created successfully.",
                aiModelService.create(departmentId, request));
    }

    @GetMapping("/{modelId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'AI_MODEL_READ')")
    public ApiResponse<AIModelResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID modelId) {
        return ApiResponse.success("AI model resorteved successfully.",
                aiModelService.findById(departmentId, modelId));
    }

    @GetMapping
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'AI_MODEL_READ')")
    public ApiResponse<List<AIModelResponse>> list(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            AIModelSearchCriteria criteria) {
        return ApiResponse.success("AI models resorteved successfully.",
                aiModelService.search(departmentId, criteria));
    }

    @PutMapping("/{modelId}")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'AI_MODEL_UPDATE')")
    public ApiResponse<AIModelResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID modelId,
            @Valid @RequestBody UpdateAIModelRequest request) {
        return ApiResponse.success("AI model updated successfully.",
                aiModelService.update(departmentId, modelId, request));
    }

    @PutMapping("/{modelId}/status")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'AI_MODEL_UPDATE')")
    public ApiResponse<AIModelResponse> updateStatus(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID modelId,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        return ApiResponse.success("AI model status updated successfully.",
                aiModelService.updateStatus(departmentId, modelId, newStatus));
    }

    @PutMapping("/{modelId}/archive")
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'AI_MODEL_ARCHIVE')")
    public ApiResponse<AIModelResponse> archive(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID modelId) {
        return ApiResponse.success("AI model archived successfully.",
                aiModelService.archive(departmentId, modelId));
    }

    @GetMapping("/stats")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'AI_MODEL_READ')")
    public ApiResponse<AIModelStatistics> getStatistics(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId) {
        return ApiResponse.success("AI model statistics resorteved successfully.",
                aiModelService.getStatistics(departmentId));
    }
}
