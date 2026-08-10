package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.organisation.handover.*;
import com.trio.backend.entity.HandoverEntry;
import com.trio.backend.service.HandoverAttachmentService;
import com.trio.backend.service.HandoverCommentService;
import com.trio.backend.service.HandoverEntryService;
import com.trio.backend.service.HandoverTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

/**
 * REST controller for the HandoverEntry workflow.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/handovers")
@RequiredArgsConstructor
@Tag(name = "Handovers", description = "Handover workflow management (sender/receiver)")
@SecurityRequirement(name = "bearer")
public class HandoverEntryController {

    private final HandoverEntryService handoverEntryService;
    private final HandoverCommentService handoverCommentService;
    private final HandoverAttachmentService handoverAttachmentService;
    private final HandoverTimelineService handoverTimelineService;

    // =========================================================================
    // Entry workflow
    // =========================================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_CREATE')")
    @Operation(summary = "Create a handover (draft)", description = "Creates a handover in DRAFT state.")
    public ApiResponse<HandoverEntryResponse> create(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateHandoverEntryRequest request
    ) {
        return ApiResponse.success("Handover created successfully.", handoverEntryService.create(workspaceId, request));
    }

    @GetMapping
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_READ')")
    @Operation(summary = "List handovers of a workspace", description = "Supports optional status / priority / projectId filters.")
    public ApiResponse<Page<HandoverEntryResponse>> list(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) HandoverEntry.HandoverStatus status,
            @RequestParam(required = false) HandoverEntry.Priority priority,
            @RequestParam(required = false) UUID projectId,
            Pageable pageable
    ) {
        return ApiResponse.success("Handovers retrieved successfully.",
                handoverEntryService.list(workspaceId, status, priority, projectId, pageable));
    }

    @GetMapping("/inbox")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_READ')")
    @Operation(summary = "List handovers received by the current user")
    public ApiResponse<Page<HandoverEntryResponse>> inbox(
            @PathVariable UUID workspaceId,
            Pageable pageable
    ) {
        return ApiResponse.success("Inbox retrieved successfully.", handoverEntryService.inbox(workspaceId, pageable));
    }

    @GetMapping("/sent")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_READ')")
    @Operation(summary = "List handovers sent by the current user")
    public ApiResponse<Page<HandoverEntryResponse>> sent(
            @PathVariable UUID workspaceId,
            Pageable pageable
    ) {
        return ApiResponse.success("Sent handovers retrieved successfully.", handoverEntryService.sent(workspaceId, pageable));
    }

    @GetMapping("/my-entries")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_READ')")
    @Operation(summary = "List the current user's handover entries (daily reports)", description = "Supports optional status / shift / entryDate / search filters.")
    public ApiResponse<Page<HandoverEntryResponse>> myEntries(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) HandoverEntry.HandoverStatus status,
            @RequestParam(required = false) HandoverEntry.Shift shift,
            @RequestParam(required = false) java.time.LocalDate entryDate,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return ApiResponse.success("My handover entries retrieved successfully.",
                handoverEntryService.myEntries(workspaceId, status, shift, entryDate, search, pageable));
    }

    @GetMapping("/{handoverEntryId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_READ')")
    @Operation(summary = "Get a handover")
    public ApiResponse<HandoverEntryResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId
    ) {
        return ApiResponse.success("Handover retrieved successfully.",
                handoverEntryService.getById(workspaceId, handoverEntryId));
    }

    @PutMapping("/{handoverEntryId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_UPDATE')")
    @Operation(summary = "Update a handover (DRAFT or REJECTED)")
    public ApiResponse<HandoverEntryResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId,
            @Valid @RequestBody UpdateHandoverEntryRequest request
    ) {
        return ApiResponse.success("Handover updated successfully.",
                handoverEntryService.update(workspaceId, handoverEntryId, request));
    }

    @PostMapping("/{handoverEntryId}/send")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_UPDATE')")
    @Operation(summary = "Send a handover to the receiver (DRAFT -> PENDING)")
    public ApiResponse<HandoverEntryResponse> send(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId,
            @RequestBody(required = false) HandoverStatusUpdateRequest request
    ) {
        return ApiResponse.success("Handover sent successfully.",
                handoverEntryService.send(workspaceId, handoverEntryId, request));
    }

    @PostMapping("/{handoverEntryId}/submit")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_UPDATE')")
    @Operation(summary = "Submit a daily report entry (DRAFT/REJECTED -> SUBMITTED)", description = "Submitted entries are aggregated by the AI journal generator.")
    public ApiResponse<HandoverEntryResponse> submit(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId,
            @RequestBody(required = false) HandoverStatusUpdateRequest request
    ) {
        return ApiResponse.success("Handover entry submitted successfully.",
                handoverEntryService.submit(workspaceId, handoverEntryId, request));
    }

    @PostMapping("/{handoverEntryId}/accept")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_UPDATE')")
    @Operation(summary = "Accept a handover (PENDING -> ACCEPTED)")
    public ApiResponse<HandoverEntryResponse> accept(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId,
            @RequestBody(required = false) HandoverStatusUpdateRequest request
    ) {
        return ApiResponse.success("Handover accepted successfully.",
                handoverEntryService.accept(workspaceId, handoverEntryId, request));
    }

    @PostMapping("/{handoverEntryId}/reject")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_UPDATE')")
    @Operation(summary = "Reject a handover (PENDING -> REJECTED)")
    public ApiResponse<HandoverEntryResponse> reject(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId,
            @RequestBody(required = false) HandoverStatusUpdateRequest request
    ) {
        return ApiResponse.success("Handover rejected successfully.",
                handoverEntryService.reject(workspaceId, handoverEntryId, request));
    }

    @PostMapping("/{handoverEntryId}/complete")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_UPDATE')")
    @Operation(summary = "Complete a handover (ACCEPTED -> COMPLETED)")
    public ApiResponse<HandoverEntryResponse> complete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId,
            @RequestBody(required = false) HandoverStatusUpdateRequest request
    ) {
        return ApiResponse.success("Handover completed successfully.",
                handoverEntryService.complete(workspaceId, handoverEntryId, request));
    }

    @PostMapping("/{handoverEntryId}/archive")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_UPDATE')")
    @Operation(summary = "Archive a handover")
    public ApiResponse<HandoverEntryResponse> archive(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId,
            @RequestBody(required = false) HandoverStatusUpdateRequest request
    ) {
        return ApiResponse.success("Handover archived successfully.",
                handoverEntryService.archive(workspaceId, handoverEntryId, request));
    }

    @DeleteMapping("/{handoverEntryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_DELETE')")
    @Operation(summary = "Soft delete a handover")
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId
    ) {
        handoverEntryService.delete(workspaceId, handoverEntryId);
    }

    // =========================================================================
    // Comments
    // =========================================================================

    @GetMapping("/{handoverEntryId}/comments")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_READ')")
    @Operation(summary = "List comments of a handover")
    public ApiResponse<List<HandoverCommentResponse>> listComments(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId
    ) {
        return ApiResponse.success("Comments retrieved successfully.",
                handoverCommentService.list(workspaceId, handoverEntryId));
    }

    @PostMapping("/{handoverEntryId}/comments")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_CREATE')")
    @Operation(summary = "Add a comment to a handover")
    public ApiResponse<HandoverCommentResponse> createComment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId,
            @Valid @RequestBody CreateHandoverCommentRequest request
    ) {
        return ApiResponse.success("Comment added successfully.",
                handoverCommentService.create(workspaceId, handoverEntryId, request));
    }

    @PutMapping("/{handoverEntryId}/comments/{commentId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_UPDATE')")
    @Operation(summary = "Update a comment")
    public ApiResponse<HandoverCommentResponse> updateComment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateHandoverCommentRequest request
    ) {
        return ApiResponse.success("Comment updated successfully.",
                handoverCommentService.update(workspaceId, handoverEntryId, commentId, request));
    }

    @DeleteMapping("/{handoverEntryId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_DELETE')")
    @Operation(summary = "Delete a comment")
    public void deleteComment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId,
            @PathVariable UUID commentId
    ) {
        handoverCommentService.delete(workspaceId, handoverEntryId, commentId);
    }

    // =========================================================================
    // Attachments
    // =========================================================================

    @GetMapping("/{handoverEntryId}/attachments")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_READ')")
    @Operation(summary = "List attachments of a handover")
    public ApiResponse<List<HandoverAttachmentResponse>> listAttachments(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId
    ) {
        return ApiResponse.success("Attachments retrieved successfully.",
                handoverAttachmentService.list(workspaceId, handoverEntryId));
    }

    @PostMapping("/{handoverEntryId}/attachments")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_UPDATE')")
    @Operation(summary = "Register an attachment on a handover")
    public ApiResponse<HandoverAttachmentResponse> createAttachment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId,
            @Valid @RequestBody CreateHandoverAttachmentRequest request
    ) {
        return ApiResponse.success("Attachment added successfully.",
                handoverAttachmentService.create(workspaceId, handoverEntryId, request));
    }

    @DeleteMapping("/{handoverEntryId}/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_DELETE')")
    @Operation(summary = "Remove an attachment from a handover")
    public void deleteAttachment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId,
            @PathVariable UUID attachmentId
    ) {
        handoverAttachmentService.delete(workspaceId, handoverEntryId, attachmentId);
    }

    // =========================================================================
    // Timeline
    // =========================================================================

    @GetMapping("/{handoverEntryId}/timeline")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_READ')")
    @Operation(summary = "Get the journal timeline of a handover")
    public ApiResponse<List<HandoverTimelineEventResponse>> getTimeline(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverEntryId
    ) {
        return ApiResponse.success("Timeline retrieved successfully.",
                handoverTimelineService.list(workspaceId, handoverEntryId));
    }
}
