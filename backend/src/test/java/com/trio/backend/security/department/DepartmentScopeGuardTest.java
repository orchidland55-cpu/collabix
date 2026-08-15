package com.trio.backend.security.department;

import com.trio.backend.entity.Department;
import com.trio.backend.entity.Role;
import com.trio.backend.entity.User;
import com.trio.backend.entity.UserRole;
import com.trio.backend.entity.Workspace;
import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.entity.ids.UserRoleId;
import com.trio.backend.entity.ids.WorkspaceMemberId;
import com.trio.backend.enums.MemberType;
import com.trio.backend.enums.RoleName;
import com.trio.backend.enums.UserStatus;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentScopeGuardTest {

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DepartmentScopeGuard departmentScopeGuard;

    private UUID workspaceId;
    private UUID rhDepartmentId;
    private UUID devDepartmentId;
    private User manager;
    private User admin;
    private User member;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        rhDepartmentId = UUID.randomUUID();
        devDepartmentId = UUID.randomUUID();

        workspace = Workspace.builder()
                .name("Collabix")
                .status(WorkspaceStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(workspace, "id", workspaceId);

        Department rh = Department.builder()
                .name("RH")
                .status(WorkspaceStatus.ACTIVE)
                .workspace(workspace)
                .build();
        ReflectionTestUtils.setField(rh, "id", rhDepartmentId);

        Department dev = Department.builder()
                .name("Development")
                .status(WorkspaceStatus.ACTIVE)
                .workspace(workspace)
                .build();
        ReflectionTestUtils.setField(dev, "id", devDepartmentId);

        manager = buildUser("manager@example.com", rh, RoleName.MANAGER);
        admin = buildUser("admin@example.com", rh, RoleName.ADMIN);
        member = buildUser("member@example.com", rh, RoleName.MEMBER);
    }

    @Test
    void assertDepartmentAccessibleShouldAllowWorkspaceAdminForAnyDepartment() {
        stubWorkspaceMember(admin, WorkspaceRole.ADMIN);

        assertDoesNotThrow(() ->
                departmentScopeGuard.assertDepartmentAccessible(workspaceId, devDepartmentId, admin.getId()));
    }

    @Test
    void assertDepartmentAccessibleShouldAllowManagerForOwnDepartment() {
        stubWorkspaceMember(manager, WorkspaceRole.MEMBER);
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(manager.getId()))
                .thenReturn(Optional.of(manager));

        assertDoesNotThrow(() ->
                departmentScopeGuard.assertDepartmentAccessible(workspaceId, rhDepartmentId, manager.getId()));
    }

    @Test
    void assertDepartmentAccessibleShouldRejectManagerForAnotherDepartment() {
        stubWorkspaceMember(manager, WorkspaceRole.MEMBER);
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(manager.getId()))
                .thenReturn(Optional.of(manager));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                departmentScopeGuard.assertDepartmentAccessible(workspaceId, devDepartmentId, manager.getId()));

        assertTrue(ex.getMessage().contains("department"));
    }

    @Test
    void assertDepartmentAccessibleShouldRejectMemberForAnotherDepartment() {
        stubWorkspaceMember(member, WorkspaceRole.MEMBER);
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(member.getId()))
                .thenReturn(Optional.of(member));

        assertThrows(ForbiddenException.class, () ->
                departmentScopeGuard.assertDepartmentAccessible(workspaceId, devDepartmentId, member.getId()));
    }

    @Test
    void assertCanManageProjectsShouldAllowManagerForOwnDepartment() {
        stubWorkspaceMember(manager, WorkspaceRole.MEMBER);
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(manager.getId()))
                .thenReturn(Optional.of(manager));

        assertDoesNotThrow(() ->
                departmentScopeGuard.assertCanManageProjects(workspaceId, rhDepartmentId, manager.getId()));
    }

    @Test
    void assertCanManageProjectsShouldRejectManagerForAnotherDepartment() {
        stubWorkspaceMember(manager, WorkspaceRole.MEMBER);
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(manager.getId()))
                .thenReturn(Optional.of(manager));

        assertThrows(ForbiddenException.class, () ->
                departmentScopeGuard.assertCanManageProjects(workspaceId, devDepartmentId, manager.getId()));
    }

    @Test
    void assertCanManageProjectsShouldRejectMember() {
        stubWorkspaceMember(member, WorkspaceRole.MEMBER);
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(member.getId()))
                .thenReturn(Optional.of(member));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                departmentScopeGuard.assertCanManageProjects(workspaceId, rhDepartmentId, member.getId()));

        assertTrue(ex.getMessage().contains("permission"));
    }

    @Test
    void resolveAccessibleDepartmentIdShouldReturnNullForWorkspaceAdmin() {
        stubWorkspaceMember(admin, WorkspaceRole.ADMIN);

        UUID result = departmentScopeGuard.resolveAccessibleDepartmentId(workspaceId, admin.getId());

        assertNull(result);
    }

    @Test
    void resolveAccessibleDepartmentIdShouldReturnPrimaryDepartmentForManager() {
        stubWorkspaceMember(manager, WorkspaceRole.MEMBER);
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(manager.getId()))
                .thenReturn(Optional.of(manager));

        UUID result = departmentScopeGuard.resolveAccessibleDepartmentId(workspaceId, manager.getId());

        assertEquals(rhDepartmentId, result);
    }

    private User buildUser(String email, Department department, RoleName roleName) {
        User user = User.builder()
                .email(email)
                .password("secret")
                .firstName("Test")
                .lastName("User")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .primaryDepartment(department)
                .build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        Role role = Role.builder().name(roleName).build();
        ReflectionTestUtils.setField(role, "id", UUID.randomUUID());

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(user.getId(), role.getId()))
                .user(user)
                .role(role)
                .build();
        user.setUserRoles(Set.of(userRole));
        return user;
    }

    private void stubWorkspaceMember(User user, WorkspaceRole role) {
        WorkspaceMember wm = WorkspaceMember.builder()
                .workspaceMemberId(new WorkspaceMemberId(workspaceId, user.getId()))
                .workspace(workspace)
                .user(user)
                .role(role)
                .status(WorkspaceMemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        when(workspaceMemberRepository.findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(
                workspaceId, user.getId())).thenReturn(Optional.of(wm));
    }
}
