package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.communication.ConversationMemberResponse;
import com.trio.backend.dto.communication.ConversationResponse;
import com.trio.backend.dto.communication.CreateConversationRequest;
import com.trio.backend.dto.communication.UpdateConversationRequest;
import com.trio.backend.service.ConversationService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Endpoints for workspace conversations/channels")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_CREATE')")
    @Operation(summary = "Create a conversation/channel", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<ConversationResponse> create(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        return ApiResponse.success(
                "Conversation created successfully.",
                conversationService.create(workspaceId, request)
        );
    }

    @GetMapping("/{conversationId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_READ')")
    @Operation(summary = "Get a conversation by ID", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<ConversationResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId
    ) {
        return ApiResponse.success(
                "Conversation retrieved successfully.",
                conversationService.getById(workspaceId, conversationId)
        );
    }

    @GetMapping
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_READ')")
    @Operation(summary = "List user conversations", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<Page<ConversationResponse>> listUserConversations(
            @PathVariable UUID workspaceId,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Conversations retrieved successfully.",
                conversationService.listUserConversations(workspaceId, pageable)
        );
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_READ')")
    @Operation(summary = "List conversations by type", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<Page<ConversationResponse>> listByType(
            @PathVariable UUID workspaceId,
            @PathVariable String type,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Conversations retrieved successfully.",
                conversationService.listByType(workspaceId, type, pageable)
        );
    }

    @GetMapping("/workspace-defaults")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_READ')")
    @Operation(summary = "List workspace default conversations", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<List<ConversationResponse>> listWorkspaceDefaults(
            @PathVariable UUID workspaceId
    ) {
        return ApiResponse.success(
                "Default conversations retrieved successfully.",
                conversationService.listWorkspaceDefaults(workspaceId)
        );
    }

    @GetMapping("/direct")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_READ')")
    @Operation(summary = "List direct conversations", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<List<ConversationResponse>> listDirectConversations(
            @PathVariable UUID workspaceId
    ) {
        return ApiResponse.success(
                "Direct conversations retrieved successfully.",
                conversationService.listDirectConversations(workspaceId)
        );
    }

    @PutMapping("/{conversationId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_UPDATE')")
    @Operation(summary = "Update a conversation", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<ConversationResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateConversationRequest request
    ) {
        return ApiResponse.success(
                "Conversation updated successfully.",
                conversationService.update(workspaceId, conversationId, request)
        );
    }

    @PostMapping("/{conversationId}/archive")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_UPDATE')")
    @Operation(summary = "Archive a conversation", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<Void> archive(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId
    ) {
        conversationService.archive(workspaceId, conversationId);
        return ApiResponse.success("Conversation archived successfully.");
    }

    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_DELETE')")
    @Operation(summary = "Delete a conversation", security = @SecurityRequirement(name = "bearer"))
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId
    ) {
        conversationService.delete(workspaceId, conversationId);
    }

    @PostMapping("/{conversationId}/members/{userId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_UPDATE')")
    @Operation(summary = "Add a member to conversation", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<ConversationResponse> addMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId,
            @PathVariable UUID userId
    ) {
        return ApiResponse.success(
                "Member added successfully.",
                conversationService.addMember(workspaceId, conversationId, userId)
        );
    }

    @DeleteMapping("/{conversationId}/members/{userId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_UPDATE')")
    @Operation(summary = "Remove a member from conversation", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<Void> removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId,
            @PathVariable UUID userId
    ) {
        conversationService.removeMember(workspaceId, conversationId, userId);
        return ApiResponse.success("Member removed successfully.");
    }

    @GetMapping("/{conversationId}/members")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_READ')")
    @Operation(summary = "List conversation members", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<List<ConversationMemberResponse>> listMembers(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId
    ) {
        return ApiResponse.success(
                "Members retrieved successfully.",
                conversationService.listMembers(workspaceId, conversationId)
        );
    }

    @GetMapping("/{conversationId}/unread-count")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'CONVERSATION_READ')")
    @Operation(summary = "Get unread message count", security = @SecurityRequirement(name = "bearer"))
    public ApiResponse<Long> getUnreadCount(
            @PathVariable UUID workspaceId,
            @PathVariable UUID conversationId
    ) {
        return ApiResponse.success(
                "Unread count retrieved successfully.",
                conversationService.getUnreadCount(workspaceId, conversationId)
        );
    }
}
