package com.trio.backend.security.department;

import com.trio.backend.entity.Department;
import com.trio.backend.entity.User;
import com.trio.backend.entity.Workspace;
import com.trio.backend.enums.MemberType;
import com.trio.backend.enums.UserStatus;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.security.user.CustomUserDetails;
import com.trio.backend.security.workspace.WorkspaceAuthorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentAuthorizationTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private WorkspaceAuthorization workspaceAuthorization;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DepartmentAuthorization departmentAuthorization;

    private UUID workspaceId;
    private UUID rhDepartmentId;
    private UUID devDepartmentId;
    private User manager;
    private User admin;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        rhDepartmentId = UUID.randomUUID();
        devDepartmentId = UUID.randomUUID();

        Workspace workspace = Workspace.builder()
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

        manager = User.builder()
                .email("manager@example.com")
                .password("secret")
                .firstName("Mgr")
                .lastName("User")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .primaryDepartment(rh)
                .build();
        ReflectionTestUtils.setField(manager, "id", UUID.randomUUID());

        admin = User.builder()
                .email("admin@example.com")
                .password("secret")
                .firstName("Admin")
                .lastName("User")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .primaryDepartment(rh)
                .build();
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
    }

    @Test
    void canViewDepartmentShouldAllowAdminForAnyDepartment() {
        var auth = authFor(admin, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(workspaceAuthorization.canViewWorkspace(workspaceId, auth)).thenReturn(true);
        when(workspaceAuthorization.canUpdateWorkspace(workspaceId, auth)).thenReturn(true);
        when(departmentRepository.findByIdAndWorkspace_Id(devDepartmentId, workspaceId))
                .thenReturn(Optional.of(activeDepartment(devDepartmentId)));

        assertTrue(departmentAuthorization.canViewDepartment(workspaceId, devDepartmentId, auth));
    }

    @Test
    void canViewDepartmentShouldAllowManagerForOwnDepartment() {
        var auth = authFor(manager, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        when(workspaceAuthorization.canViewWorkspace(workspaceId, auth)).thenReturn(true);
        when(workspaceAuthorization.canUpdateWorkspace(workspaceId, auth)).thenReturn(false);
        when(departmentRepository.findByIdAndWorkspace_Id(rhDepartmentId, workspaceId))
                .thenReturn(Optional.of(activeDepartment(rhDepartmentId)));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(manager.getId()))
                .thenReturn(Optional.of(manager));

        assertTrue(departmentAuthorization.canViewDepartment(workspaceId, rhDepartmentId, auth));
    }

    @Test
    void canViewDepartmentShouldRejectManagerForAnotherDepartment() {
        var auth = authFor(manager, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        when(workspaceAuthorization.canViewWorkspace(workspaceId, auth)).thenReturn(true);
        when(workspaceAuthorization.canUpdateWorkspace(workspaceId, auth)).thenReturn(false);
        when(departmentRepository.findByIdAndWorkspace_Id(devDepartmentId, workspaceId))
                .thenReturn(Optional.of(activeDepartment(devDepartmentId)));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(manager.getId()))
                .thenReturn(Optional.of(manager));

        assertFalse(departmentAuthorization.canViewDepartment(workspaceId, devDepartmentId, auth));
    }

    @Test
    void canManageDepartmentProjectsShouldAllowAdminForAnyDepartment() {
        var auth = authFor(admin, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(workspaceAuthorization.canViewWorkspace(workspaceId, auth)).thenReturn(true);
        when(workspaceAuthorization.canUpdateWorkspace(workspaceId, auth)).thenReturn(true);
        when(departmentRepository.findByIdAndWorkspace_Id(devDepartmentId, workspaceId))
                .thenReturn(Optional.of(activeDepartment(devDepartmentId)));

        assertTrue(departmentAuthorization.canManageDepartmentProjects(workspaceId, devDepartmentId, auth));
    }

    @Test
    void canManageDepartmentProjectsShouldAllowManagerForOwnDepartment() {
        var auth = authFor(manager, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        when(workspaceAuthorization.canViewWorkspace(workspaceId, auth)).thenReturn(true);
        when(workspaceAuthorization.canUpdateWorkspace(workspaceId, auth)).thenReturn(false);
        when(departmentRepository.findByIdAndWorkspace_Id(rhDepartmentId, workspaceId))
                .thenReturn(Optional.of(activeDepartment(rhDepartmentId)));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(manager.getId()))
                .thenReturn(Optional.of(manager));

        assertTrue(departmentAuthorization.canManageDepartmentProjects(workspaceId, rhDepartmentId, auth));
    }

    @Test
    void canManageDepartmentProjectsShouldRejectManagerForAnotherDepartment() {
        var auth = authFor(manager, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        when(workspaceAuthorization.canViewWorkspace(workspaceId, auth)).thenReturn(true);
        when(workspaceAuthorization.canUpdateWorkspace(workspaceId, auth)).thenReturn(false);
        when(departmentRepository.findByIdAndWorkspace_Id(devDepartmentId, workspaceId))
                .thenReturn(Optional.of(activeDepartment(devDepartmentId)));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(manager.getId()))
                .thenReturn(Optional.of(manager));

        assertFalse(departmentAuthorization.canManageDepartmentProjects(workspaceId, devDepartmentId, auth));
    }

    @Test
    void canManageDepartmentProjectsShouldRejectMember() {
        var auth = authFor(manager, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
        when(workspaceAuthorization.canViewWorkspace(workspaceId, auth)).thenReturn(true);
        when(workspaceAuthorization.canUpdateWorkspace(workspaceId, auth)).thenReturn(false);
        when(departmentRepository.findByIdAndWorkspace_Id(rhDepartmentId, workspaceId))
                .thenReturn(Optional.of(activeDepartment(rhDepartmentId)));

        assertFalse(departmentAuthorization.canManageDepartmentProjects(workspaceId, rhDepartmentId, auth));
    }

    @Test
    void canManageDepartmentTasksShouldMirrorProjectRules() {
        var auth = authFor(manager, List.of(new SimpleGrantedAuthority("ROLE_MANAGER")));
        when(workspaceAuthorization.canViewWorkspace(workspaceId, auth)).thenReturn(true);
        when(workspaceAuthorization.canUpdateWorkspace(workspaceId, auth)).thenReturn(false);
        when(departmentRepository.findByIdAndWorkspace_Id(rhDepartmentId, workspaceId))
                .thenReturn(Optional.of(activeDepartment(rhDepartmentId)));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(manager.getId()))
                .thenReturn(Optional.of(manager));

        assertTrue(departmentAuthorization.canManageDepartmentTasks(workspaceId, rhDepartmentId, auth));
        assertFalse(departmentAuthorization.canManageDepartmentTasks(workspaceId, devDepartmentId, auth));
    }

    private Department activeDepartment(UUID id) {
        Workspace workspace = Workspace.builder()
                .name("Collabix")
                .status(WorkspaceStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(workspace, "id", workspaceId);
        Department department = Department.builder()
                .name("Dept")
                .status(WorkspaceStatus.ACTIVE)
                .workspace(workspace)
                .build();
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }

    private UsernamePasswordAuthenticationToken authFor(User user, List<SimpleGrantedAuthority> authorities) {
        return new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null, authorities);
    }
}
