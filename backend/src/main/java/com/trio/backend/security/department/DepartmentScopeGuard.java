package com.trio.backend.security.department;

import com.trio.backend.entity.User;
import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.enums.RoleName;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Programmatic department-scope guard used by service implementations as a
 * defense-in-depth layer against requests that tamper with the
 * {@code departmentId} path variable on project/task endpoints.
 *
 * <p>Access rules:</p>
 * <ul>
 *     <li>Workspace OWNER/ADMIN may operate on any department of the workspace.</li>
 *     <li>MANAGER and MEMBER may only operate on their {@code User.primaryDepartment}.
 *         A user without an assigned primary department cannot access any department.</li>
 * </ul>
 *
 * <p>This mirrors {@link com.trio.backend.security.department.DepartmentAuthorization#canViewDepartment}
 * at the service layer, where the department id comes from an untrusted URL/body value.</p>
 */
@Component
@RequiredArgsConstructor
public class DepartmentScopeGuard {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    /**
     * Throws {@link ForbiddenException} unless the user may operate on the given
     * department within the workspace.
     *
     * @param workspaceId  tenant identifier
     * @param departmentId department identifier (from request)
     * @param userId       authenticated user id
     */
    public void assertDepartmentAccessible(UUID workspaceId, UUID departmentId, UUID userId) {
        WorkspaceMember wm = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace."));

        if (wm.getStatus() != WorkspaceMemberStatus.ACTIVE) {
            throw new ForbiddenException("You are not an active member of this workspace.");
        }

        if (wm.getRole() == WorkspaceRole.OWNER || wm.getRole() == WorkspaceRole.ADMIN) {
            return;
        }

        User user = userRepository.findByIdWithRolesAndPrimaryDepartment(userId)
                .orElseThrow(() -> new ForbiddenException("User not found."));

        if (user.getPrimaryDepartment() == null
                || !user.getPrimaryDepartment().getId().equals(departmentId)) {
            throw new ForbiddenException("You do not have access to this department.");
        }
    }

    /**
     * Throws {@link ForbiddenException} unless the user may create, update, or restore
     * projects in the given department.
     *
     * <p>Workspace OWNER/ADMIN and global ADMIN may manage projects in any department.
     * A global MANAGER may manage projects only in their primary department.
     * Members are denied.</p>
     */
    public void assertCanManageProjects(UUID workspaceId, UUID departmentId, UUID userId) {
        WorkspaceMember wm = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace."));

        if (wm.getStatus() != WorkspaceMemberStatus.ACTIVE) {
            throw new ForbiddenException("You are not an active member of this workspace.");
        }

        if (wm.getRole() == WorkspaceRole.OWNER || wm.getRole() == WorkspaceRole.ADMIN) {
            return;
        }

        User user = userRepository.findByIdWithRolesAndPrimaryDepartment(userId)
                .orElseThrow(() -> new ForbiddenException("User not found."));

        if (user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getName() == RoleName.ADMIN
                        || ur.getRole().getName() == RoleName.SUPER_ADMIN)) {
            return;
        }

        boolean isManager = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getName() == RoleName.MANAGER);
        if (!isManager) {
            throw new ForbiddenException("You do not have permission for this operation.");
        }

        if (user.getPrimaryDepartment() == null
                || !user.getPrimaryDepartment().getId().equals(departmentId)) {
            throw new ForbiddenException("You do not have access to this department.");
        }
    }

    /**
     * Throws {@link ForbiddenException} unless the user may create, update, or restore
     * tasks in the given department.
     *
     * <p>Follows the same rules as {@link #assertCanManageProjects}.</p>
     */
    public void assertCanManageTasks(UUID workspaceId, UUID departmentId, UUID userId) {
        assertCanManageProjects(workspaceId, departmentId, userId);
    }

    /**
     *
     * @return the department id for MANAGER/MEMBER, or {@code null} when the user
     *         is a workspace OWNER/ADMIN (any department allowed)
     * @throws ForbiddenException if a non-admin user has no primary department
     */
    public UUID resolveAccessibleDepartmentId(UUID workspaceId, UUID userId) {
        WorkspaceMember wm = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace."));

        if (wm.getStatus() != WorkspaceMemberStatus.ACTIVE) {
            throw new ForbiddenException("You are not an active member of this workspace.");
        }

        if (wm.getRole() == WorkspaceRole.OWNER || wm.getRole() == WorkspaceRole.ADMIN) {
            return null;
        }

        User user = userRepository.findByIdWithRolesAndPrimaryDepartment(userId)
                .orElseThrow(() -> new ForbiddenException("User not found."));

        if (user.getPrimaryDepartment() == null) {
            throw new ForbiddenException("You do not have a department assigned.");
        }
        return user.getPrimaryDepartment().getId();
    }
}