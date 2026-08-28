package com.trio.backend.service;

import com.trio.backend.dto.organisation.project.CreateProjectRequest;
import com.trio.backend.dto.organisation.project.ProjectResponse;
import com.trio.backend.dto.organisation.project.UpdateProjectRequest;
import com.trio.backend.entity.Department;
import com.trio.backend.entity.Project;
import com.trio.backend.entity.User;
import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ConflictException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.ProjectMapper;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.ProjectRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final DepartmentScopeGuard departmentScopeGuard;

    @Override
    public ProjectResponse create(UUID workspaceId, UUID departmentId, CreateProjectRequest request) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertCanManageProjects(workspaceId, departmentId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Department department = departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        if (department.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Department not found.");
        }

        String normalizedName = normalizeName(request.getName());
        validateProjectDates(request.getStartDate(), request.getEndDate());

        if (projectRepository.existsByDepartment_IdAndNameIgnoreCase(departmentId, normalizedName)) {
            throw new ConflictException("Project with this name already exists.");
        }

        request.setName(normalizedName);

        Project project = projectMapper.toEntity(request);
        project.setDepartment(department);
        project.setStatus(WorkspaceStatus.ACTIVE);

        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found."));
            project.setManager(manager);
        }

        Project saved = projectRepository.save(project);
        return projectMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getById(UUID workspaceId, UUID departmentId, UUID projectId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Project project = findProjectInDepartment(workspaceId, departmentId, projectId);

        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID workspaceId, UUID departmentId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        return projectRepository.findAllByDepartment_IdAndStatus(departmentId, WorkspaceStatus.ACTIVE)
                .stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponse> listPaginated(UUID workspaceId, UUID departmentId, String search, Pageable pageable) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        if (search != null && !search.isBlank()) {
            return projectRepository.searchByDepartmentIdAndName(departmentId, WorkspaceStatus.ACTIVE, search.trim(), pageable)
                    .map(projectMapper::toResponse);
        }

        return projectRepository.findAllByDepartment_IdAndStatus(departmentId, WorkspaceStatus.ACTIVE, pageable)
                .map(projectMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectResponse> listAllPaginated(UUID workspaceId, String search, Pageable pageable) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        // Workspace ADMIN/OWNER may see every department. MANAGER/MEMBER are
        // automatically scoped to their primary department (never all projects).
        UUID accessibleDepartmentId = departmentScopeGuard.resolveAccessibleDepartmentId(workspaceId, userId);
        if (accessibleDepartmentId == null) {
            if (search != null && !search.isBlank()) {
                return projectRepository.searchByWorkspaceIdAndName(
                                workspaceId, WorkspaceStatus.ACTIVE, search.trim(), pageable)
                        .map(projectMapper::toResponse);
            }
            return projectRepository.findAllByWorkspaceIdAndStatus(workspaceId, WorkspaceStatus.ACTIVE, pageable)
                    .map(projectMapper::toResponse);
        }

        return listPaginated(workspaceId, accessibleDepartmentId, search, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> listArchived(UUID workspaceId, UUID departmentId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        return projectRepository.findAllByDepartment_IdAndStatus(departmentId, WorkspaceStatus.ARCHIVED)
                .stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Override
    public ProjectResponse update(UUID workspaceId, UUID departmentId, UUID projectId, UpdateProjectRequest request) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertCanManageProjects(workspaceId, departmentId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Project project = findActiveProjectInDepartment(workspaceId, departmentId, projectId);

        validateProjectDates(request.getStartDate(), request.getEndDate());

        if (request.getName() != null) {
            String normalizedName = normalizeName(request.getName());
            if (!normalizedName.equals(normalizeName(project.getName()))
                    && projectRepository.existsByDepartment_IdAndNameIgnoreCase(departmentId, normalizedName)) {
                throw new ConflictException("Project with this name already exists.");
            }
            request.setName(normalizedName);
        }

        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found."));
            project.setManager(manager);
        }

        projectMapper.updateProject(request, project);
        Project saved = projectRepository.save(project);
        return projectMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID workspaceId, UUID departmentId, UUID projectId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Project project = findProjectInDepartment(workspaceId, departmentId, projectId);

        if (project.getStatus() != WorkspaceStatus.ACTIVE) {
            return;
        }

        project.setStatus(WorkspaceStatus.ARCHIVED);
        projectRepository.save(project);
    }

    @Override
    public void hardDelete(UUID workspaceId, UUID departmentId, UUID projectId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Project project = findProjectInDepartment(workspaceId, departmentId, projectId);

        // Removing the row cascades to all project-scoped children
        // (tasks, documents, sprints, reports, ...).
        projectRepository.delete(project);
    }

    @Override
    public ProjectResponse restore(UUID workspaceId, UUID departmentId, UUID projectId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertCanManageProjects(workspaceId, departmentId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Project project = findProjectInDepartment(workspaceId, departmentId, projectId);

        if (project.getStatus() != WorkspaceStatus.ARCHIVED) {
            throw new BadRequestException("Project is not archived.");
        }

        project.setStatus(WorkspaceStatus.ACTIVE);
        Project saved = projectRepository.save(project);
        return projectMapper.toResponse(saved);
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

    private Project findActiveProjectInDepartment(UUID workspaceId, UUID departmentId, UUID projectId) {
        Project project = findProjectInDepartment(workspaceId, departmentId, projectId);
        if (project.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Project not found.");
        }
        return project;
    }

    private Project findProjectInDepartment(UUID workspaceId, UUID departmentId, UUID projectId) {
        Project project = projectRepository.findByIdAndDepartment_Id(projectId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
        if (!project.getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Project not found.");
        }
        return project;
    }

    private void validateProjectDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("End date cannot be before start date.");
        }
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
