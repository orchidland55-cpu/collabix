package com.trio.backend.service;

import com.trio.backend.dto.organisation.activity.ActivityResponse;
import com.trio.backend.dto.organisation.activity.CreateActivityRequest;
import com.trio.backend.dto.organisation.activity.UpdateActivityRequest;
import com.trio.backend.entity.Activity;
import com.trio.backend.entity.Task;
import com.trio.backend.entity.User;
import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.enums.ActivityStatus;
import com.trio.backend.enums.TaskStatus;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.ActivityMapper;
import com.trio.backend.repository.ActivityRepository;
import com.trio.backend.repository.TaskRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;
import com.trio.backend.security.department.DepartmentScopeGuard;
import com.trio.backend.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ActivityMapper activityMapper;
    private final DepartmentScopeGuard departmentScopeGuard;

    @Override
    public ActivityResponse create(UUID workspaceId,
                                   UUID departmentId,
                                   UUID projectId,
                                   UUID taskId,
                                   CreateActivityRequest request) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Task task = taskRepository.findByIdAndProject_Id(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));

        if (task.getStatus().isTerminal()) {
            throw new ResourceNotFoundException("Task not found.");
        }

        if (!task.getProject().getDepartment().getId().equals(departmentId)
                || !task.getProject().getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Task not found.");
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Activity activity = activityMapper.toEntity(request);
        activity.setTask(task);
        activity.setActor(actor);
        activity.setStatus(ActivityStatus.ACTIVE);

        Activity saved = activityRepository.save(activity);
        return activityMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityResponse getById(UUID workspaceId,
                                    UUID departmentId,
                                    UUID projectId,
                                    UUID taskId,
                                    UUID activityId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found."));

        if (activity.getStatus() != ActivityStatus.ACTIVE) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        if (!activity.getTask().getId().equals(taskId)) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        if (!activity.getTask().getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        if (!activity.getTask().getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        if (!activity.getTask().getProject().getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        return activityMapper.toResponse(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityResponse> list(UUID workspaceId,
                                       UUID departmentId,
                                       UUID projectId,
                                       UUID taskId,
                                       Pageable pageable) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        // Validation en depth de l'arborescence parente
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

        return activityRepository.findAllByTask_IdAndStatus(taskId, ActivityStatus.ACTIVE, pageable)
                .map(activityMapper::toResponse);
    }

    @Override
    public ActivityResponse update(UUID workspaceId,
                                   UUID departmentId,
                                   UUID projectId,
                                   UUID taskId,
                                   UUID activityId,
                                   UpdateActivityRequest request) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found."));

        if (activity.getStatus() != ActivityStatus.ACTIVE) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        if (!activity.getTask().getId().equals(taskId)) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        if (!activity.getTask().getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        if (!activity.getTask().getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        if (!activity.getTask().getProject().getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        activityMapper.updateActivity(request, activity);
        Activity saved = activityRepository.save(activity);
        return activityMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID workspaceId,
                       UUID departmentId,
                       UUID projectId,
                       UUID taskId,
                       UUID activityId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found."));

        if (activity.getStatus() != ActivityStatus.ACTIVE) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        if (!activity.getTask().getId().equals(taskId)) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        if (!activity.getTask().getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        if (!activity.getTask().getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        if (!activity.getTask().getProject().getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Activity not found.");
        }

        activity.setStatus(ActivityStatus.ARCHIVED);
        activityRepository.save(activity);
    }

    // ============================================================================
    // Helpers
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