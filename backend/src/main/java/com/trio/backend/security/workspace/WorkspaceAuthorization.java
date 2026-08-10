package com.trio.backend.security.workspace;

import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.repository.ProjectRepository;
import com.trio.backend.repository.TeamMemberRepository;
import com.trio.backend.repository.TeamRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Authorization bean for workspace-scoped security checks.
 *
 * <p>This component is referenced via SpEL in {@code @PreAuthorize} annotations
 * across workspace-related controllers. It encapsulates all workspace membership
 * and role verification logic in a single, testable location.</p>
 */
@Slf4j
@Component("workspaceAuth")
@RequiredArgsConstructor
public class WorkspaceAuthorization {

    private static final String SUPER_ADMIN_AUTHORITY = "ROLE_SUPER_ADMIN";
    private static final String MANAGER_AUTHORITY = "ROLE_MANAGER";

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    // ============================================================================
    // Controller-matching methods
    // ============================================================================

    public boolean canViewWorkspace(UUID workspaceId, Authentication authentication) {
        if (isSuperAdmin(authentication)) {
            return true;
        }

        Optional<WorkspaceMember> membership = getMembership(workspaceId, authentication);
        if (membership.isEmpty()) {
            log.debug("User not member of workspace {}", workspaceId);
            return false;
        }

        WorkspaceMember member = membership.get();
        boolean active = member.getStatus() == WorkspaceMemberStatus.ACTIVE;
        if (!active) {
            log.debug("Workspace membership inactive for user {} in workspace {}", member.getWorkspaceMemberId().getUserId(), workspaceId);
        }

        return active;
    }

    public boolean canUpdateWorkspace(UUID workspaceId, Authentication authentication) {
        if (isSuperAdmin(authentication)) {
            return true;
        }

        return hasActiveRoleInWorkspace(workspaceId, authentication, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
    }

    public boolean canDeleteWorkspace(UUID workspaceId, Authentication authentication) {
        if (isSuperAdmin(authentication)) {
            return true;
        }

        return hasActiveRoleInWorkspace(workspaceId, authentication, WorkspaceRole.OWNER);
    }

    /**
     * Checks whether the authenticated user can write HR data (employees, candidates,
     * interviews, reviews, attendance, onboarding, documents, skills, notes) for a department.
     *
     * <p>Workspace ADMIN/OWNER can manage HR data for any department. A global MANAGER
     * can only manage HR data for their own primary department. Super admins bypass all checks.</p>
     *
     * @param workspaceId tenant identifier
     * @param departmentId department identifier
     * @param authentication spring security authentication
     * @return true if allowed, false otherwise
     */
    public boolean canManageDepartmentHR(UUID workspaceId, UUID departmentId, Authentication authentication) {
        if (isSuperAdmin(authentication)) {
            return true;
        }

        if (!canViewWorkspace(workspaceId, authentication)) {
            return false;
        }

        // Workspace ADMIN/OWNER can manage HR data for any department
        if (canUpdateWorkspace(workspaceId, authentication)) {
            return true;
        }

        // A MANAGER can manage HR data only for their primary department
        if (!hasRole(authentication, MANAGER_AUTHORITY)) {
            return false;
        }

        UUID userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }

        return userRepository.findById(userId)
                .map(user -> user.getPrimaryDepartment() != null
                        && user.getPrimaryDepartment().getId().equals(departmentId))
                .orElse(false);
    }

    // ============================================================================
    // Internal helpers
    // ============================================================================

    private Optional<WorkspaceMember> getMembership(UUID workspaceId, Authentication authentication) {
        UUID userId = extractUserId(authentication);
        if (userId == null) {
            return Optional.empty();
        }

        return workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId);
    }

    private boolean hasActiveRoleInWorkspace(UUID workspaceId,
                                              Authentication authentication,
                                              WorkspaceRole... allowedRoles) {

        Optional<WorkspaceMember> membership = getMembership(workspaceId, authentication);
        if (membership.isEmpty()) {
            log.debug("User not member of workspace {}", workspaceId);
            return false;
        }

        WorkspaceMember member = membership.get();
        if (member.getStatus() != WorkspaceMemberStatus.ACTIVE) {
            log.debug("Workspace membership inactive for user {} in workspace {}",
                    member.getWorkspaceMemberId().getUserId(), workspaceId);
            return false;
        }

        WorkspaceRole userRole = member.getRole();

        boolean allowed = java.util.Set.of(allowedRoles).contains(userRole);
        if (!allowed) {
            log.debug("User role {} not allowed in workspace {}. Allowed: {}", userRole, workspaceId, java.util.Set.of(allowedRoles));
        }

        return allowed;
    }


    private boolean isSuperAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(SUPER_ADMIN_AUTHORITY::equals);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

public boolean canAccessDepartment(UUID workspaceId, UUID departmentId, Authentication authentication) {
        // Department is context only for MVP: access follows workspace membership.
        // Department entity is validated in DepartmentAuthorization / service when needed.
        return canViewWorkspace(workspaceId, authentication);
    }

    public boolean canAccessTeam(UUID workspaceId, UUID teamId, Authentication authentication) {
        if (isSuperAdmin(authentication)) {
            return true;
        }

        if (!canViewWorkspace(workspaceId, authentication)) {
            return false;
        }

        // Workspace ADMIN/OWNER can access any team
        if (canUpdateWorkspace(workspaceId, authentication)) {
            return true;
        }

        // Regular users can only access teams they belong to
        UUID userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }

        return teamMemberRepository.existsByTeamMemberId_TeamIdAndTeamMemberId_UserId(teamId, userId);
    }

    public boolean canAccessProject(UUID workspaceId, UUID projectId, Authentication authentication) {
        if (isSuperAdmin(authentication)) {
            return true;
        }

        if (!canViewWorkspace(workspaceId, authentication)) {
            return false;
        }

        // Workspace ADMIN/OWNER can access any project
        if (canUpdateWorkspace(workspaceId, authentication)) {
            return true;
        }

        // Regular users can only access projects in their primary department
        UUID userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }

        return projectRepository.findById(projectId)
                .map(project -> userRepository.findById(userId)
                        .map(user -> user.getPrimaryDepartment() != null
                                && user.getPrimaryDepartment().getId().equals(project.getDepartment().getId()))
                        .orElse(false))
                .orElse(false);
    }

    public boolean canManageTeam(UUID workspaceId, UUID teamId, Authentication authentication) {
        // For MVP, manage operations at Team scope are governed by workspace ADMIN/OWNER.
        return canUpdateWorkspace(workspaceId, authentication);
    }

    public boolean canManageTeamMember(UUID workspaceId, UUID teamId, UUID targetUserId, Authentication authentication) {
        // For MVP, team member management is governed by workspace ADMIN/OWNER.
        // targetUserId is intentionally not used yet (no fine-grained team member permissions exist in the codebase).
        return canUpdateWorkspace(workspaceId, authentication);
    }

    /**
     * Checks whether the authenticated user may permanently delete a team.
     *
     * <p>Allowed for a Super Admin (bypass), a Workspace Admin/Owner, or the
     * manager of this specific team. All other users are denied so the backend
     * always returns 403 for unauthorized direct API requests.</p>
     */
    public boolean canPermanentlyDeleteTeam(UUID workspaceId, UUID teamId, Authentication authentication) {
        if (isSuperAdmin(authentication)) {
            return true;
        }

        if (canUpdateWorkspace(workspaceId, authentication)) {
            return true;
        }

        UUID userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }

        return teamRepository.findByIdAndWorkspace_Id(teamId, workspaceId)
                .map(team -> team.getManager() != null && team.getManager().getId().equals(userId))
                .orElse(false);
    }

    public boolean canCreateArtifact(UUID workspaceId, UUID departmentId, UUID teamId, Authentication authentication) {
        // Artifacts (KB/Docs/Handovers/Notifications/Dashboard) creation is governed by workspace ADMIN/OWNER.
        // Department/Team are context only for MVP.
        return canUpdateWorkspace(workspaceId, authentication);
    }



    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }

        log.warn("Principal type {} not directly extractable to UUID", principal == null ? "null" : principal.getClass().getName());
        return null;
    }
}

