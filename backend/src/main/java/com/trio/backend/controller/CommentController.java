package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.organisation.comment.CommentResponse;
import com.trio.backend.dto.organisation.comment.CreateCommentRequest;
import com.trio.backend.dto.organisation.comment.UpdateCommentRequest;
import com.trio.backend.service.CommentServiceImpl;
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

import java.util.UUID;

/**
 * REST controller responsible for managing Comments.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/departments/{departmentId}/projects/{projectId}/tasks/{taskId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Endpoints for managing Comments (organization)")
public class CommentController {

    private final CommentServiceImpl commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@departmentAuth.canManageDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'COMMENT_CREATE')")
    @Operation(
            summary = "Create a comment",
            security = @SecurityRequirement(name = "bearer"),
            description = "Creates a comment in the task."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Comment createde", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Task not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
    })
    public ApiResponse<CommentResponse> create(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @Parameter(description = "ID of the task", required = true)
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return ApiResponse.success(
                "Comment created successfully.",
                commentService.create(workspaceId, departmentId, projectId, taskId, request)
        );
    }

    @GetMapping("/{commentId}")
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'COMMENT_READ')")
    @Operation(
            summary = "Resorteve a comment",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the information of a comment."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comment found", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment not found")
    })
    public ApiResponse<CommentResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId
    ) {
        return ApiResponse.success(
                "Comment resorteved successfully.",
                commentService.getById(workspaceId, departmentId, projectId, taskId, commentId)
        );
    }

    @GetMapping
    @PreAuthorize("@departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'COMMENT_READ')")
    @Operation(
            summary = "List the comments of a task",
            security = @SecurityRequirement(name = "bearer"),
            description = "Returns the list of active comments of a task."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List resorteved", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission")
    })
    public ApiResponse<Page<CommentResponse>> list(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            Pageable pageable
    ) {
        return ApiResponse.success(
                "Comments resorteved successfully.",
                commentService.list(workspaceId, departmentId, projectId, taskId, pageable)
        );
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("@departmentAuth.canManageDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'COMMENT_UPDATE')")
    @Operation(
            summary = "Update a comment",
            security = @SecurityRequirement(name = "bearer"),
            description = "Updates a comment (partial update)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comment updated", content = @Content(schema = @Schema(implementation = com.trio.backend.common.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
    })
    public ApiResponse<CommentResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return ApiResponse.success(
                "Comment updated successfully.",
                commentService.update(workspaceId, departmentId, projectId, taskId, commentId, request)
        );
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canDeleteWorkspace(#workspaceId, authentication) && @departmentAuth.canViewDepartment(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'COMMENT_DELETE')")
    @Operation(
            summary = "Delete a comment",
            security = @SecurityRequirement(name = "bearer"),
            description = "Supprime (soft delete) une comment."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Comment deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User without permission"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment not found")
    })
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID commentId
    ) {
        commentService.delete(workspaceId, departmentId, projectId, taskId, commentId);
    }
}

