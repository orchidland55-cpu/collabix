package com.trio.backend.ai.controller;

import com.trio.backend.ai.dto.response.AIPromptResponse;
import com.trio.backend.ai.service.AIPromptService;
import com.trio.backend.common.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/prompts")
@RequiredArgsConstructor
@Tag(name = "AI Prompts", description = "AI prompt template library")
@SecurityRequirement(name = "bearerAuth")
public class AIPromptController {

    private final AIPromptService aiPromptService;

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'AI_MODEL_READ')")
    @GetMapping
    public ApiResponse<List<AIPromptResponse>> listActive() {
        return ApiResponse.success("AI prompts retrieved successfully.", aiPromptService.findAllActive());
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'AI_MODEL_READ')")
    @GetMapping("/{id}")
    public ApiResponse<AIPromptResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success("AI prompt retrieved successfully.", aiPromptService.findById(id));
    }

    @PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'AI_MODEL_READ')")
    @GetMapping("/code/{code}")
    public ApiResponse<AIPromptResponse> getByCode(@PathVariable String code) {
        return ApiResponse.success("AI prompt retrieved successfully.", aiPromptService.findByCode(code));
    }
}
