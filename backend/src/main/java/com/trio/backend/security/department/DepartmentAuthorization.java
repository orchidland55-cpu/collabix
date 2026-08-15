package com.trio.backend.security.department;

import com.trio.backend.entity.Department;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.security.user.CustomUserDetails;
import com.trio.backend.security.workspace.WorkspaceAuthorization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Authorization bean for Department-scoped security checks.
 *
 * <p>This component centralizes all access rules related to {@link Department}.
 * It follows the same style and principles as {@link WorkspaceAuthorization}:
 * tenant isolation via {@code WorkspaceAuthorization} + Department scope validation.</p>
 *
 * <p>Authorization rules:</p>
 * <ul>
 *     <li>Workspace ADMIN/OWNER can view and manage any department.</li>
 *     <li>Regular members can only view their {@code User.primaryDepartment}.</li>
 * </ul>
 */
@Slf4j
@Component("departmentAuth")
@RequiredArgsConstructor
public class DepartmentAuthorization {

    private static final String SUPER_ADMIN_AUTHORITY = "ROLE_SUPER_ADMIN";
    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";
    private static final String MANAGER_AUTHORITY = "ROLE_MANAGER";

    private final DepartmentRepository departmentRepository;
    private final WorkspaceAuthorization workspaceAuthorization;
    private final UserRepository userRepository;

    // ============================================================================
    // Core checks (Department scope)
    // ============================================================================

    /**
     * Checks whether the authenticated user can view an ACTIVE Department.
     *
     * <p>Workspace ADMIN/OWNER can view any department. Regular members can only
     * view their primary department.</p>
     *
     * @param workspaceId tenant identifier
     * @param departmentId department identifier
     * @param authentication spring security authentication
     * @return true if allowed, false otherwise
     */
    public boolean canViewDepartment(UUID workspaceId, UUID departmentId, Authentication authentication) {
        if (isSuperAdmin(authentication)) {
            return true;
        }

        if (!workspaceAuthorization.canViewWorkspace(workspaceId, authentication)) {
            return false;
        }

        Optional<Department> departmentOpt = getActiveDepartment(workspaceId, departmentId);
        if (departmentOpt.isEmpty()) {
            return false;
        }

        // Workspace ADMIN/OWNER and global ADMIN can view any department
        if (workspaceAuthorization.canUpdateWorkspace(workspaceId, authentication)
                || hasRole(authentication, ADMIN_AUTHORITY)) {
            return true;
        }

        // MANAGER and MEMBER can only view their primary department
        return belongsToPrimaryDepartment(authentication, departmentId);
    }

    /**
     * Checks whether the authenticated user can manage (create/update/restore) projects
     * in the given department.
     *
     * <p>Workspace ADMIN/OWNER and global ADMIN can manage projects in any department.
     * A global MANAGER can manage projects only in their {@code User.primaryDepartment}.
     * Members are not granted project write access through this check.</p>
     */
    public boolean canManageDepartmentProjects(UUID workspaceId, UUID departmentId, Authentication authentication) {
        if (isSuperAdmin(authentication)) {
            return true;
        }

        if (!workspaceAuthorization.canViewWorkspace(workspaceId, authentication)) {
            return false;
        }

        if (getActiveDepartment(workspaceId, departmentId).isEmpty()) {
            return false;
        }

        if (workspaceAuthorization.canUpdateWorkspace(workspaceId, authentication)
                || hasRole(authentication, ADMIN_AUTHORITY)) {
            return true;
        }

        if (!hasRole(authentication, MANAGER_AUTHORITY)) {
            return false;
        }

        return belongsToPrimaryDepartment(authentication, departmentId);
    }

    /**
     * Checks whether the authenticated user can manage (create/update/restore) tasks
     * in the given department.
     *
     * <p>Follows the same rules as {@link #canManageDepartmentProjects}.</p>
     */
    public boolean canManageDepartmentTasks(UUID workspaceId, UUID departmentId, Authentication authentication) {
        return canManageDepartmentProjects(workspaceId, departmentId, authentication);
    }

    /**
     *
     * <p>Only workspace ADMIN/OWNER can manage departments. Department-level isolation
     * is not applied since write access implies full department management rights.</p>
     *
     * @param workspaceId tenant identifier
     * @param departmentId department identifier
     * @param authentication spring security authentication
     * @return true if allowed, false otherwise
     */
    public boolean canManageDepartment(UUID workspaceId, UUID departmentId, Authentication authentication) {
        if (isSuperAdmin(authentication)) {
            return true;
        }

        if (!workspaceAuthorization.canUpdateWorkspace(workspaceId, authentication)) {
            return false;
        }

        return getActiveDepartment(workspaceId, departmentId).isPresent();
    }

    // ============================================================================
    // Internal helpers
    // ============================================================================

    private Optional<Department> getActiveDepartment(UUID workspaceId, UUID departmentId) {
        return departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .filter(d -> d.getStatus() == WorkspaceStatus.ACTIVE);
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return hasRole(authentication, SUPER_ADMIN_AUTHORITY);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private boolean belongsToPrimaryDepartment(Authentication authentication, UUID departmentId) {
        UUID userId = extractUserId(authentication);
        if (userId == null) {
            return false;
        }

        return userRepository.findByIdWithRolesAndPrimaryDepartment(userId)
                .map(user -> user.getPrimaryDepartment() != null
                        && user.getPrimaryDepartment().getId().equals(departmentId))
                .orElse(false);
    }

    /**
     * Extract the authenticated user UUID.
     *
     * <p>Kept for future fine-grained rules consistent with {@link WorkspaceAuthorization}.</p>
     *
     * @param authentication spring security authentication
     * @return user UUID if extractable, null otherwise
     */
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

