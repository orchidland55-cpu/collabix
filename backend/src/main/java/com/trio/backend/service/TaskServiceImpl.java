package com.trio.backend.service;

import com.trio.backend.dto.organisation.task.CreateTaskRequest;
import com.trio.backend.dto.organisation.task.TaskResponse;
import com.trio.backend.dto.organisation.task.UpdateTaskRequest;
import com.trio.backend.entity.*;
import com.trio.backend.enums.ActivityStatus;
import com.trio.backend.enums.RoleName;
import com.trio.backend.enums.TaskPriority;
import com.trio.backend.enums.TaskStatus;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ConflictException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.TaskMapper;
import com.trio.backend.repository.*;
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

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {

    private static final Set<TaskStatus> KANBAN_WORKFLOW_STATUSES = EnumSet.of(
            TaskStatus.ACTIVE,
            TaskStatus.TODO,
            TaskStatus.IN_PROGRESS,
            TaskStatus.IN_REVIEW,
            TaskStatus.BLOCKED,
            TaskStatus.COMPLETED
    );

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final SecurityAuditRepository securityAuditRepository;
    private final MarketingCampaignRepository marketingCampaignRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TaskMapper taskMapper;
    private final DepartmentScopeGuard departmentScopeGuard;

    @Override
    public TaskResponse create(UUID workspaceId, UUID departmentId, UUID projectId, CreateTaskRequest request) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertCanManageTasks(workspaceId, departmentId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Project project = findActiveProject(workspaceId, departmentId, projectId);

        String normalizedTitle = normalizeTitle(request.getTitle());
        request.setTitle(normalizedTitle);

        if (taskRepository.existsByProject_IdAndTitle(projectId, normalizedTitle)) {
            throw new ConflictException("Task with this title already exists.");
        }

        Task task = taskMapper.toEntity(request);
        task.setProject(project);
        task.setStatus(TaskStatus.ACTIVE);

        if (request.getDueAt() != null) {
            task.setDueAt(request.getDueAt());
        }

        if (request.getStartDate() != null) {
            task.setStartDate(request.getStartDate());
        }

        if (request.getAssigneeId() != null) {
            task.setAssignee(resolveValidatedAssignee(workspaceId, departmentId, request.getAssigneeId()));
        }

        resolveOptionalRelations(task, request, departmentId);

        Task saved = taskRepository.save(task);
        logActivity(saved, userId, "created this task.");
        return taskMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getById(UUID workspaceId, UUID departmentId, UUID projectId, UUID taskId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);
        Task task = findActiveTask(taskId, projectId, departmentId, workspaceId);
        assertTaskVisibleToUser(task, workspaceId, userId);
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> list(UUID workspaceId, UUID departmentId, UUID projectId,
                                    String search, String statusFilter, String priorityFilter,
                                    UUID assigneeFilter, Pageable pageable) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);
        findActiveProject(workspaceId, departmentId, projectId);

        UUID effectiveAssigneeFilter = resolveAssigneeFilter(workspaceId, userId, assigneeFilter);
        List<TaskStatus> statusFilters = resolveStatusFilters(statusFilter);
        boolean statusFilterDisabled = statusFilters == null;
        TaskPriority priority = resolvePriorityFilter(priorityFilter);

        return taskRepository.findFiltered(
                        projectId,
                        search,
                        statusFilterDisabled ? List.of() : statusFilters,
                        statusFilterDisabled,
                        priority,
                        effectiveAssigneeFilter,
                        pageable)
                .map(taskMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listArchived(UUID workspaceId, UUID departmentId, UUID projectId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);
        findActiveProject(workspaceId, departmentId, projectId);
        if (isAssignedOnlyUser(workspaceId, userId)) {
            return taskRepository.findAllByProject_IdAndStatus(projectId, TaskStatus.ARCHIVED)
                    .stream()
                    .filter(task -> task.getAssignee() != null && task.getAssignee().getId().equals(userId))
                    .map(taskMapper::toResponse)
                    .toList();
        }
        return taskRepository.findAllByProject_IdAndStatus(projectId, TaskStatus.ARCHIVED)
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @Override
    public TaskResponse update(UUID workspaceId, UUID departmentId, UUID projectId, UUID taskId, UpdateTaskRequest request) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Task task = taskRepository.findByIdAndProject_Id(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));

        if (task.getStatus() == TaskStatus.ARCHIVED || task.getStatus() == TaskStatus.CANCELLED) {
            throw new ResourceNotFoundException("Task not found.");
        }

        validateTaskChain(workspaceId, departmentId, projectId, task);

        if (isAssignedOnlyUser(workspaceId, userId)) {
            return updateOwnAssignedTask(task, request, userId, workspaceId);
        }

        departmentScopeGuard.assertCanManageTasks(workspaceId, departmentId, userId);
        return updateManagedTask(task, request, userId, workspaceId, departmentId, projectId);
    }

    private TaskResponse updateOwnAssignedTask(Task task, UpdateTaskRequest request, UUID userId, UUID workspaceId) {
        assertTaskVisibleToUser(task, workspaceId, userId);
        assertMemberSelfServiceUpdate(request);

        StringBuilder changes = new StringBuilder();

        if (request.getStatus() != null && request.getStatus() != task.getStatus()) {
            applyAssigneeWorkflowStatusChange(task, request.getStatus(), userId);
            changes.append("status, ");
        }

        if (request.getStoryPoints() != null) {
            task.setStoryPoints(request.getStoryPoints());
            changes.append("story points, ");
        }

        Task saved = taskRepository.save(task);

        if (!changes.isEmpty()) {
            String desc = "updated " + changes.substring(0, changes.length() - 2) + ".";
            logActivity(saved, userId, desc);
        }

        return taskMapper.toResponse(saved);
    }

    private TaskResponse updateManagedTask(
            Task task,
            UpdateTaskRequest request,
            UUID userId,
            UUID workspaceId,
            UUID departmentId,
            UUID projectId) {

        StringBuilder changes = new StringBuilder();

        if (request.getTitle() != null) {
            String normalizedTitle = normalizeTitle(request.getTitle());
            if (!normalizedTitle.equalsIgnoreCase(task.getTitle())
                    && taskRepository.existsByProject_IdAndTitle(projectId, normalizedTitle)) {
                throw new ConflictException("Task with this title already exists.");
            }
            request.setTitle(normalizedTitle);
            changes.append("title, ");
        }

        if (request.getDueAt() != null) {
            task.setDueAt(request.getDueAt());
            changes.append("due date, ");
        }

        if (request.getStartDate() != null) {
            task.setStartDate(request.getStartDate());
            changes.append("start date, ");
        }

        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
            changes.append("priority, ");
        }

        if (request.getAssigneeId() != null) {
            task.setAssignee(resolveValidatedAssignee(workspaceId, departmentId, request.getAssigneeId()));
            changes.append("assignee, ");
        }

        if (request.getStatus() != null && request.getStatus() != task.getStatus()) {
            applyManagedWorkflowStatusChange(task, request.getStatus());
            changes.append("status, ");
        }

        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findByIdAndDepartment_Id(request.getSprintId(), departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint not found."));
            task.setSprint(sprint);
            changes.append("sprint, ");
        }

        if (request.getSecurityAuditId() != null) {
            SecurityAudit audit = securityAuditRepository.findByIdAndDepartment_Id(
                            request.getSecurityAuditId(), departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Security audit not found."));
            task.setSecurityAudit(audit);
        }

        if (request.getMarketingCampaignId() != null) {
            MarketingCampaign campaign = marketingCampaignRepository.findByIdAndDepartment_Id(
                            request.getMarketingCampaignId(), departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Marketing campaign not found."));
            task.setMarketingCampaign(campaign);
        }

        taskMapper.updateTask(request, task);
        Task saved = taskRepository.save(task);

        if (!changes.isEmpty()) {
            String desc = "updated " + changes.substring(0, changes.length() - 2) + ".";
            logActivity(saved, userId, desc);
        }

        return taskMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID workspaceId, UUID departmentId, UUID projectId, UUID taskId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertCanManageTasks(workspaceId, departmentId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Task task = taskRepository.findByIdAndProject_Id(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));

        validateTaskChain(workspaceId, departmentId, projectId, task);

        if (task.getStatus() == TaskStatus.ARCHIVED) {
            return;
        }

        task.setStatus(TaskStatus.ARCHIVED);
        taskRepository.save(task);
        logActivity(task, userId, "archived this task.");
    }

    @Override
    public TaskResponse restore(UUID workspaceId, UUID departmentId, UUID projectId, UUID taskId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertCanManageTasks(workspaceId, departmentId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Task task = taskRepository.findByIdAndProject_Id(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));

        validateTaskChain(workspaceId, departmentId, projectId, task);

        if (task.getStatus() != TaskStatus.ARCHIVED) {
            throw new BadRequestException("Task is not archived.");
        }

        task.setStatus(TaskStatus.ACTIVE);
        Task saved = taskRepository.save(task);
        logActivity(saved, userId, "restored this task.");
        return taskMapper.toResponse(saved);
    }

    private void logActivity(Task task, UUID actorId, String description) {
        try {
            User actor = userRepository.findById(actorId).orElse(null);
            if (actor == null) return;

            Activity activity = Activity.builder()
                    .task(task)
                    .actor(actor)
                    .description(description)
                    .status(ActivityStatus.ACTIVE)
                    .build();
            activityRepository.save(activity);
        } catch (Exception e) {
            log.warn("Failed to log activity for task {}: {}", task.getId(), e.getMessage());
        }
    }

    private Task findActiveTask(UUID taskId, UUID projectId, UUID departmentId, UUID workspaceId) {
        Task task = taskRepository.findByIdAndProject_Id(taskId, projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found."));
        if (task.getStatus() == TaskStatus.ARCHIVED || task.getStatus() == TaskStatus.CANCELLED) {
            throw new ResourceNotFoundException("Task not found.");
        }
        validateTaskChain(workspaceId, departmentId, projectId, task);
        return task;
    }

    private Project findActiveProject(UUID workspaceId, UUID departmentId, UUID projectId) {
        Project project = projectRepository.findByIdAndDepartment_Id(projectId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
        if (project.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Project not found.");
        }
        if (!project.getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Project not found.");
        }
        return project;
    }

    private void validateTaskChain(UUID workspaceId, UUID departmentId, UUID projectId, Task task) {
        if (!task.getProject().getId().equals(projectId)
                || !task.getProject().getDepartment().getId().equals(departmentId)
                || !task.getProject().getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Task not found.");
        }
    }

    private void resolveOptionalRelations(Task task, CreateTaskRequest request, UUID departmentId) {
        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findByIdAndDepartment_Id(request.getSprintId(), departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sprint not found."));
            task.setSprint(sprint);
        }
        if (request.getSecurityAuditId() != null) {
            SecurityAudit audit = securityAuditRepository.findByIdAndDepartment_Id(
                            request.getSecurityAuditId(), departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Security audit not found."));
            task.setSecurityAudit(audit);
        }
        if (request.getMarketingCampaignId() != null) {
            MarketingCampaign campaign = marketingCampaignRepository.findByIdAndDepartment_Id(
                            request.getMarketingCampaignId(), departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Marketing campaign not found."));
            task.setMarketingCampaign(campaign);
        }
    }

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

    /**
     * Members (non-manager, non-admin) may only view tasks assigned to themselves.
     */
    private boolean isAssignedOnlyUser(UUID workspaceId, UUID userId) {
        WorkspaceMember wm = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace."));

        if (wm.getRole() == WorkspaceRole.OWNER || wm.getRole() == WorkspaceRole.ADMIN) {
            return false;
        }

        User user = userRepository.findByIdWithRolesAndPrimaryDepartment(userId)
                .orElseThrow(() -> new ForbiddenException("User not found."));

        return user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .noneMatch(name -> name == RoleName.ADMIN
                        || name == RoleName.SUPER_ADMIN
                        || name == RoleName.MANAGER);
    }

    private UUID resolveAssigneeFilter(UUID workspaceId, UUID userId, UUID requestedAssignee) {
        if (isAssignedOnlyUser(workspaceId, userId)) {
            return userId;
        }
        return requestedAssignee;
    }

    private void assertTaskVisibleToUser(Task task, UUID workspaceId, UUID userId) {
        if (!isAssignedOnlyUser(workspaceId, userId)) {
            return;
        }
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(userId)) {
            throw new ForbiddenException("You do not have access to this task.");
        }
    }

    private User resolveValidatedAssignee(UUID workspaceId, UUID departmentId, UUID assigneeId) {
        User assignee = userRepository.findByIdWithRolesAndPrimaryDepartment(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignee not found."));

        assertActiveWorkspaceMember(workspaceId, assigneeId);

        if (assignee.getPrimaryDepartment() == null
                || !assignee.getPrimaryDepartment().getId().equals(departmentId)) {
            throw new ForbiddenException("Assignee must belong to the task department.");
        }

        return assignee;
    }

    private void assertMemberSelfServiceUpdate(UpdateTaskRequest request) {
        if (request.getTitle() != null
                || request.getDescription() != null
                || request.getAssigneeId() != null
                || request.getPriority() != null
                || request.getDueAt() != null
                || request.getStartDate() != null
                || request.getSprintId() != null
                || request.getSecurityAuditId() != null
                || request.getMarketingCampaignId() != null) {
            throw new ForbiddenException("You can only update status or progress on tasks assigned to you.");
        }
        if (request.getStatus() == null && request.getStoryPoints() == null) {
            throw new BadRequestException("No updatable fields provided.");
        }
    }

    private void applyAssigneeWorkflowStatusChange(Task task, TaskStatus newStatus, UUID userId) {
        assertAssigneeForWorkflowStatusChange(task, userId);
        validateStatusTransition(task.getStatus(), newStatus);
        task.setStatus(newStatus);
    }

    private void applyManagedWorkflowStatusChange(Task task, TaskStatus newStatus) {
        validateStatusTransition(task.getStatus(), newStatus);
        task.setStatus(newStatus);
    }

    private List<TaskStatus> resolveStatusFilters(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return null;
        }
        TaskStatus status;
        try {
            status = TaskStatus.valueOf(statusFilter.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid status filter: " + statusFilter);
        }
        if (status == TaskStatus.ACTIVE || status == TaskStatus.TODO) {
            return List.of(TaskStatus.ACTIVE, TaskStatus.TODO);
        }
        return List.of(status);
    }

    private TaskPriority resolvePriorityFilter(String priorityFilter) {
        if (priorityFilter == null || priorityFilter.isBlank()) {
            return null;
        }
        try {
            return TaskPriority.valueOf(priorityFilter.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid priority filter: " + priorityFilter);
        }
    }

    private void assertAssigneeForWorkflowStatusChange(Task task, UUID userId) {
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(userId)) {
            throw new ForbiddenException("Only the assigned member can move this task through the workflow.");
        }
    }

    private void validateStatusTransition(TaskStatus currentStatus, TaskStatus newStatus) {
        if (!KANBAN_WORKFLOW_STATUSES.contains(newStatus)) {
            throw new BadRequestException("Invalid task status for workflow update.");
        }
        if (currentStatus == TaskStatus.ARCHIVED || currentStatus == TaskStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status of an archived or cancelled task.");
        }
        if (!KANBAN_WORKFLOW_STATUSES.contains(currentStatus)) {
            throw new BadRequestException("Invalid current task status for workflow update.");
        }
    }

    private String normalizeTitle(String value) {
        if (value == null) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
