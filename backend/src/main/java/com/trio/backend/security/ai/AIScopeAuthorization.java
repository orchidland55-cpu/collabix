package com.trio.backend.security.ai;

import com.trio.backend.entity.Department;
import com.trio.backend.entity.Project;
import com.trio.backend.entity.Team;
import com.trio.backend.entity.User;
import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.enums.AIScopeType;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.ProjectRepository;
import com.trio.backend.repository.TeamRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;
import com.trio.backend.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AIScopeAuthorization {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_MANAGER = "ROLE_MANAGER";
    private static final String ROLE_MEMBER = "ROLE_MEMBER";

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;

    public UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new BadRequestException("User is not authenticated.");
        }
        return user.getId();
    }

    public void assertActiveWorkspaceMember(UUID workspaceId, UUID userId) {
        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace."));
        if (member.getStatus() != WorkspaceMemberStatus.ACTIVE) {
            throw new ForbiddenException("You are not an active member of this workspace.");
        }
    }

    public void assertCanGenerate(UUID workspaceId, AIScopeType scope, UUID departmentId, UUID projectId, UUID teamId) {
        UUID userId = currentUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        if (isMemberOnly(userId, workspaceId)) {
            throw new ForbiddenException("Members are not allowed to generate AI reports or analytics.");
        }

        validateScopeEntities(workspaceId, scope, departmentId, projectId, teamId);

        if (isWorkspaceAdminOrOwner(workspaceId, userId) || hasGlobalRole(ROLE_SUPER_ADMIN) || hasGlobalRole(ROLE_ADMIN)) {
            return;
        }

        if (!isManager(userId, workspaceId)) {
            throw new ForbiddenException("You do not have permission to generate AI content for this scope.");
        }

        if (scope == AIScopeType.WORKSPACE) {
            throw new ForbiddenException("Managers cannot generate workspace-wide AI content.");
        }

        UUID primaryDepartmentId = requirePrimaryDepartmentId(userId);
        UUID effectiveDepartmentId = resolveDepartmentId(workspaceId, scope, departmentId, projectId, teamId);
        if (!primaryDepartmentId.equals(effectiveDepartmentId)) {
            throw new ForbiddenException("You do not have permission to generate AI content for this department.");
        }
    }

    public void assertCanReadDepartmentScopedContent(UUID workspaceId, UUID contentDepartmentId) {
        UUID userId = currentUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        if (isWorkspaceAdminOrOwner(workspaceId, userId) || hasGlobalRole(ROLE_SUPER_ADMIN) || hasGlobalRole(ROLE_ADMIN)) {
            return;
        }

        if (contentDepartmentId == null) {
            throw new ForbiddenException("You do not have permission to access workspace-wide AI content.");
        }

        UUID primaryDepartmentId = requirePrimaryDepartmentId(userId);
        if (!primaryDepartmentId.equals(contentDepartmentId)) {
            throw new ForbiddenException("You do not have permission to access this department's AI content.");
        }
    }

    public Optional<UUID> resolveReadableDepartmentFilter(UUID workspaceId) {
        UUID userId = currentUserId();
        if (isWorkspaceAdminOrOwner(workspaceId, userId) || hasGlobalRole(ROLE_SUPER_ADMIN) || hasGlobalRole(ROLE_ADMIN)) {
            return Optional.empty();
        }
        return Optional.of(requirePrimaryDepartmentId(userId));
    }

    public void assertCanAccessKnowledge(UUID workspaceId, UUID departmentId, UUID projectId) {
        UUID userId = currentUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        if (isWorkspaceAdminOrOwner(workspaceId, userId) || hasGlobalRole(ROLE_SUPER_ADMIN) || hasGlobalRole(ROLE_ADMIN)) {
            validateOptionalProject(workspaceId, departmentId, projectId);
            return;
        }

        UUID primaryDepartmentId = requirePrimaryDepartmentId(userId);
        if (departmentId != null && !primaryDepartmentId.equals(departmentId)) {
            throw new ForbiddenException("You do not have permission to access knowledge in this department.");
        }
        if (projectId != null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ForbiddenException("Project not found."));
            if (!project.getDepartment().getId().equals(primaryDepartmentId)) {
                throw new ForbiddenException("You do not have permission to access knowledge for this project.");
            }
        }
    }

    public boolean isMemberOnly(UUID userId, UUID workspaceId) {
        if (isWorkspaceAdminOrOwner(workspaceId, userId)) {
            return false;
        }
        if (hasGlobalRole(ROLE_SUPER_ADMIN) || hasGlobalRole(ROLE_ADMIN) || hasGlobalRole(ROLE_MANAGER)) {
            return false;
        }
        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId)
                .orElse(null);
        if (member != null && (member.getRole() == WorkspaceRole.ADMIN || member.getRole() == WorkspaceRole.MANAGER)) {
            return false;
        }
        return hasGlobalRole(ROLE_MEMBER);
    }

    private void validateScopeEntities(UUID workspaceId, AIScopeType scope, UUID departmentId, UUID projectId, UUID teamId) {
        switch (scope) {
            case WORKSPACE -> {
                // workspace-wide; no department required
            }
            case DEPARTMENT -> {
                requireDepartment(workspaceId, departmentId);
            }
            case PROJECT -> {
                requireDepartment(workspaceId, departmentId);
                requireProject(departmentId, projectId);
            }
            case TEAM -> {
                requireDepartment(workspaceId, departmentId);
                requireTeam(workspaceId, departmentId, teamId);
            }
        }
    }

    private UUID resolveDepartmentId(UUID workspaceId, AIScopeType scope, UUID departmentId, UUID projectId, UUID teamId) {
        return switch (scope) {
            case WORKSPACE -> throw new ForbiddenException("Managers cannot generate workspace-wide AI content.");
            case DEPARTMENT -> Objects.requireNonNull(departmentId, "departmentId is required");
            case PROJECT -> projectRepository.findById(Objects.requireNonNull(projectId, "projectId is required"))
                    .map(p -> p.getDepartment().getId())
                    .orElseThrow(() -> new BadRequestException("Project not found."));
            case TEAM -> teamRepository.findByIdAndWorkspace_Id(
                            Objects.requireNonNull(teamId, "teamId is required"), workspaceId)
                    .map(t -> t.getDepartment().getId())
                    .orElseThrow(() -> new BadRequestException("Team not found."));
        };
    }

    private Department requireDepartment(UUID workspaceId, UUID departmentId) {
        if (departmentId == null) {
            throw new BadRequestException("departmentId is required for this scope.");
        }
        return departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .filter(d -> d.getStatus() == WorkspaceStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Department not found in workspace."));
    }

    private Project requireProject(UUID departmentId, UUID projectId) {
        if (projectId == null) {
            throw new BadRequestException("projectId is required for project scope.");
        }
        return projectRepository.findByIdAndDepartment_Id(projectId, departmentId)
                .orElseThrow(() -> new BadRequestException("Project not found in department."));
    }

    private Team requireTeam(UUID workspaceId, UUID departmentId, UUID teamId) {
        if (teamId == null) {
            throw new BadRequestException("teamId is required for team scope.");
        }
        return teamRepository.findByIdAndWorkspace_Id(teamId, workspaceId)
                .filter(t -> t.getDepartment().getId().equals(departmentId))
                .orElseThrow(() -> new BadRequestException("Team not found in department."));
    }

    private void validateOptionalProject(UUID workspaceId, UUID departmentId, UUID projectId) {
        if (projectId == null) {
            return;
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BadRequestException("Project not found."));
        if (!project.getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ForbiddenException("Project does not belong to this workspace.");
        }
        if (departmentId != null && !project.getDepartment().getId().equals(departmentId)) {
            throw new BadRequestException("Project does not belong to the requested department.");
        }
    }

    private UUID requirePrimaryDepartmentId(UUID userId) {
        return userRepository.findByIdWithRolesAndPrimaryDepartment(userId)
                .map(User::getPrimaryDepartment)
                .filter(Objects::nonNull)
                .map(Department::getId)
                .orElseThrow(() -> new ForbiddenException("You are not assigned to a department."));
    }

    private boolean isWorkspaceAdminOrOwner(UUID workspaceId, UUID userId) {
        boolean isAdmin = workspaceMemberRepository.existsWithRole(workspaceId, userId, WorkspaceRole.ADMIN);
        boolean isOwner = workspaceRepository.findById(workspaceId)
                .map(ws -> ws.getOwner() != null && ws.getOwner().getId().equals(userId))
                .orElse(false);
        return isAdmin || isOwner;
    }

    private boolean isManager(UUID userId, UUID workspaceId) {
        if (hasGlobalRole(ROLE_MANAGER)) {
            return true;
        }
        return workspaceMemberRepository.existsWithRole(workspaceId, userId, WorkspaceRole.MANAGER);
    }

    private boolean hasGlobalRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
