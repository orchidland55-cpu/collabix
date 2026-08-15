package com.trio.backend.service;

import com.trio.backend.dto.organisation.attachment.AttachmentResponse;
import com.trio.backend.dto.organisation.comment.CreateCommentRequest;
import com.trio.backend.dto.organisation.comment.CommentResponse;
import com.trio.backend.dto.organisation.comment.UpdateCommentRequest;
import com.trio.backend.entity.Attachment;
import com.trio.backend.entity.Comment;
import com.trio.backend.entity.Project;
import com.trio.backend.entity.Task;
import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.enums.CommentStatus;
import com.trio.backend.enums.TaskStatus;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ConflictException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.AttachmentMapper;
import com.trio.backend.mapper.CommentMapper;
import com.trio.backend.repository.AttachmentRepository;
import com.trio.backend.repository.ProjectRepository;
import com.trio.backend.repository.TaskRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;
import com.trio.backend.security.department.DepartmentScopeGuard;
import com.trio.backend.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation for Comment CRUD.
 *
 * <p>Validation chain:</p>
 * <pre>
 * Workspace -> Department -> Project -> Task -> Comment
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final com.trio.backend.repository.CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final AttachmentRepository attachmentRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final CommentMapper commentMapper;
    private final AttachmentMapper attachmentMapper;
    private final DepartmentScopeGuard departmentScopeGuard;

    @Transactional
    public CommentResponse create(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID taskId,
            CreateCommentRequest request
    ) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Task task = taskRepository.findByIdAndProject_Id(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));

        if (task.getStatus().isTerminal()) {
            throw new ResourceNotFoundException("Task not found.");
        }

        if (!task.getProject().getId().equals(projectId)
                || !task.getProject().getDepartment().getId().equals(departmentId)
                || !task.getProject().getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Task not found.");
        }

        Comment comment = commentMapper.toEntity(request);
        comment.setTask(task);

        if (request.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found."));
            if (parent.getStatus() != CommentStatus.ACTIVE) {
                throw new ResourceNotFoundException("Parent comment not found.");
            }
            comment.setParentCommentId(request.getParentCommentId());
        }

        Comment saved = commentRepository.save(comment);
        return buildCommentResponse(saved);
    }

    @Transactional(readOnly = true)
    public CommentResponse getById(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID taskId,
            UUID commentId
    ) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Comment comment = commentRepository.findByIdAndTask_Id(commentId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found."));

        if (comment.getStatus() != CommentStatus.ACTIVE) {
            throw new ResourceNotFoundException("Comment not found.");
        }

        if (!comment.getTask().getId().equals(taskId)) {
            throw new ResourceNotFoundException("Comment not found.");
        }

        if (!comment.getTask().getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Comment not found.");
        }

        if (!comment.getTask().getProject().getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Comment not found.");
        }

        return buildCommentResponse(comment);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CommentResponse> list(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID taskId,
            org.springframework.data.domain.Pageable pageable
    ) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Task task = taskRepository.findByIdAndProject_Id(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));

        if (task.getStatus().isTerminal()) {
            throw new ResourceNotFoundException("Task not found.");
        }

        if (!task.getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Task not found.");
        }

        if (!task.getProject().getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Task not found.");
        }

        Page<Comment> commentPage = commentRepository.findAllByTask_IdAndStatus(taskId, CommentStatus.ACTIVE, pageable);
        Map<UUID, List<AttachmentResponse>> attachmentMap = loadAttachmentsForComments(commentPage.getContent());
        return commentPage.map(c -> buildCommentResponse(c, attachmentMap));
    }

    public CommentResponse update(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID taskId,
            UUID commentId,
            UpdateCommentRequest request
    ) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);

        Comment comment = commentRepository.findByIdAndTask_Id(commentId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found."));

        if (comment.getStatus() != CommentStatus.ACTIVE) {
            throw new ResourceNotFoundException("Comment not found.");
        }

        if (!comment.getTask().getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Comment not found.");
        }

        if (!comment.getTask().getProject().getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Comment not found.");
        }

        commentMapper.updateComment(request, comment);
        Comment saved = commentRepository.save(comment);
        return buildCommentResponse(saved);
    }

    public void delete(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID taskId,
            UUID commentId
    ) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);

        Comment comment = commentRepository.findByIdAndTask_Id(commentId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found."));

        if (comment.getStatus() != CommentStatus.ACTIVE) {
            return; // idempotent
        }

        if (!comment.getTask().getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Comment not found.");
        }

        if (!comment.getTask().getProject().getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Comment not found.");
        }

        comment.setStatus(CommentStatus.ARCHIVED);
        commentRepository.save(comment);
    }

    private CommentResponse buildCommentResponse(Comment comment) {
        CommentResponse response = commentMapper.toResponse(comment);
        response.setAttachments(
                attachmentRepository.findByCommentId(comment.getId())
                        .stream()
                        .map(attachmentMapper::toResponse)
                        .toList()
        );
        return response;
    }

    private CommentResponse buildCommentResponse(Comment comment, Map<UUID, List<AttachmentResponse>> attachmentMap) {
        CommentResponse response = commentMapper.toResponse(comment);
        response.setAttachments(attachmentMap.getOrDefault(comment.getId(), List.of()));
        return response;
    }

    private Map<UUID, List<AttachmentResponse>> loadAttachmentsForComments(List<Comment> comments) {
        if (comments.isEmpty()) {
            return Map.of();
        }
        List<UUID> commentIds = comments.stream().map(Comment::getId).toList();
        List<Attachment> attachments = attachmentRepository.findByCommentIdIn(commentIds);
        return attachments.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getComment().getId(),
                        Collectors.mapping(attachmentMapper::toResponse, Collectors.toList())
                ));
    }

    // ============================================================================
    // Helpers (pattern inspired by TaskServiceImpl)
    // ============================================================================

    private UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails main)) {
            throw new BadRequestException("User is not authenticated.");
        }
        return main.getId();
    }

    private void assertActiveWorkspaceMember(UUID workspaceId, UUID userId) {
        WorkspaceMember wm = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace."));

        if (wm.getStatus() != WorkspaceMemberStatus.ACTIVE) {
            throw new ForbiddenException("You are not an active member of this workspace.");
        }
    }

    private void assertWorkspaceAdminOrOwner(UUID workspaceId, UUID userId) {
        boolean isAdmin = workspaceMemberRepository.existsWithRole(workspaceId, userId, WorkspaceRole.ADMIN);
        boolean isOwner = workspaceRepository.findById(workspaceId)
                .map(ws -> ws.getOwner().getId().equals(userId))
                .orElse(false);

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You do not have permission for this operation.");
        }
    }
}

