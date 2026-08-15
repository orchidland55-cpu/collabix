package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.organisation.attachment.CreateAttachmentRequest;
import com.trio.backend.dto.organisation.attachment.UpdateAttachmentRequest;
import com.trio.backend.dto.organisation.attachment.AttachmentResponse;
import com.trio.backend.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
 * REST controller responsible for managing Attachments.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/departments/{departmentId}/projects/{projectId}/tasks/{taskId}/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Endpoints for managing Attachments (collaboration)")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@departmentAuth.canManageDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ATTACHMENT_UPLOAD')")
    @Operation(
            summary = "Create a attachment",
            security = @SecurityRequirement(name = "bearer"),
            description = "Creates a attachment attachÃƒÂ© ÃƒÂ  la task."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Attachment created", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Task not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
    })
    public ApiResponse<AttachmentResponse> create(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @Parameter(description = "Data de creation de l'attachment", required = true)
            @Valid @RequestBody CreateAttachmentRequest request
    ) {
        return ApiResponse.success(
                "Attachment created successfully.",
                attachmentService.create(workspaceId, departmentId, projectId, taskId, request)
        );
    }

    @GetMapping("/{attachmentId}")
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ATTACHMENT_READ')")
    @Operation(
            summary = "Resorteve a attachment",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the information d'un attachment."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attachment found", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    public ApiResponse<AttachmentResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId
    ) {
        return ApiResponse.success(
                "Attachment resorteved successfully.",
                attachmentService.getById(workspaceId, departmentId, projectId, taskId, attachmentId)
        );
    }

    @GetMapping
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ATTACHMENT_READ')")
    @Operation(
            summary = "List attachments of a task",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the list des attachments actives of a task."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List resorteved", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission")
    })
    public ApiResponse<Page<AttachmentResponse>> list(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Attachments resorteved successfully.",
                attachmentService.list(workspaceId, departmentId, projectId, taskId, pageable)
        );
    }

    @PutMapping("/{attachmentId}")
    @PreAuthorize("@departmentAuth.canManageDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ATTACHMENT_UPDATE')")
    @Operation(
            summary = "Update a attachment",
            security = @SecurityRequirement(name = "bearer"),
            description = "Updates a attachment (partial update)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attachment updated", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Attachment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
    })
    public ApiResponse<AttachmentResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId,
            @Valid @RequestBody UpdateAttachmentRequest request
    ) {
        return ApiResponse.success(
                "Attachment updated successfully.",
                attachmentService.update(workspaceId, departmentId, projectId, taskId, attachmentId, request)
        );
    }

    // ==================== COMMENT ATTACHMENTS ====================

    @GetMapping("/comments/{commentId}/attachments")
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ATTACHMENT_READ')")
    @Operation(
            summary = "List attachments d'un comment",
            security = @SecurityRequirement(name = "bearer")
    )
    public ApiResponse<List<AttachmentResponse>> listByComment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId
    ) {
        return ApiResponse.success(
                "Attachments resorteved successfully.",
                attachmentService.listByComment(workspaceId, departmentId, projectId, taskId, commentId)
        );
    }

    @PostMapping("/comments/{commentId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuth.canUpdateWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ATTACHMENT_UPLOAD')")
    @Operation(
            summary = "Attacher un file ÃƒÂ  un comment",
            security = @SecurityRequirement(name = "bearer")
    )
    public ApiResponse<AttachmentResponse> createForComment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @Valid @RequestBody CreateAttachmentRequest request
    ) {
        request.setCommentId(commentId);
        return ApiResponse.success(
                "Attachment created successfully.",
                attachmentService.create(workspaceId, departmentId, projectId, taskId, request)
        );
    }

    @DeleteMapping("/comments/{commentId}/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canDeleteWorkspace(#workspaceId, authentication) && @departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ATTACHMENT_DELETE')")
    @Operation(
            summary = "Delete a attachment d'un comment",
            security = @SecurityRequirement(name = "bearer")
    )
    public void deleteFromComment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @PathVariable UUID attachmentId
    ) {
        attachmentService.delete(workspaceId, departmentId, projectId, taskId, attachmentId);
    }

    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canDeleteWorkspace(#workspaceId, authentication) && @departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'ATTACHMENT_DELETE')")
    @Operation(
            summary = "Delete a attachment",
            security = @SecurityRequirement(name = "bearer"),
            description = "Supprime (soft delete) un attachment."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Attachment deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId
    ) {
        attachmentService.delete(workspaceId, departmentId, projectId, taskId, attachmentId);
    }
}
