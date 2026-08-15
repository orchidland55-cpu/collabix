package com.trio.backend.security.ai;

import com.trio.backend.entity.Department;
import com.trio.backend.entity.Project;
import com.trio.backend.entity.Team;
import com.trio.backend.entity.User;
import com.trio.backend.entity.Workspace;
import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.entity.ids.WorkspaceMemberId;
import com.trio.backend.enums.AIScopeType;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.ProjectRepository;
import com.trio.backend.repository.TeamRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;
import com.trio.backend.security.user.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIScopeAuthorizationTest {

    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TeamRepository teamRepository;

    @InjectMocks
    private AIScopeAuthorization authorization;

    private final UUID workspaceId = UUID.randomUUID();
    private final UUID hrDeptId = UUID.randomUUID();
    private final UUID marketingDeptId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void managerCannotGenerateForOtherDepartment() {
        authenticateManager();
        stubActiveMember();
        stubPrimaryDepartment(hrDeptId);
        stubDepartment(marketingDeptId);

        assertThrows(ForbiddenException.class, () ->
                authorization.assertCanGenerate(workspaceId, AIScopeType.DEPARTMENT, marketingDeptId, null, null));
    }

    @Test
    void managerCanGenerateForOwnDepartment() {
        authenticateManager();
        stubActiveMember();
        stubPrimaryDepartment(hrDeptId);
        stubDepartment(hrDeptId);

        assertDoesNotThrow(() ->
                authorization.assertCanGenerate(workspaceId, AIScopeType.DEPARTMENT, hrDeptId, null, null));
    }

    @Test
    void memberCannotGenerateReports() {
        authenticateMember();
        stubActiveMember();
        stubPrimaryDepartment(hrDeptId);
        stubDepartment(hrDeptId);

        assertThrows(ForbiddenException.class, () ->
                authorization.assertCanGenerate(workspaceId, AIScopeType.DEPARTMENT, hrDeptId, null, null));
    }

    @Test
    void managerCannotGenerateWorkspaceScope() {
        authenticateManager();
        stubActiveMember();
        stubPrimaryDepartment(hrDeptId);

        assertThrows(ForbiddenException.class, () ->
                authorization.assertCanGenerate(workspaceId, AIScopeType.WORKSPACE, null, null, null));
    }

    private void authenticateManager() {
        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(userId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))));
    }

    private void authenticateMember() {
        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        when(principal.getId()).thenReturn(userId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))));
    }

    private void stubActiveMember() {
        WorkspaceMember member = new WorkspaceMember();
        member.setStatus(WorkspaceMemberStatus.ACTIVE);
        when(workspaceMemberRepository.findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId))
                .thenReturn(Optional.of(member));
        when(workspaceMemberRepository.existsWithRole(workspaceId, userId, WorkspaceRole.ADMIN)).thenReturn(false);
        when(workspaceMemberRepository.existsWithRole(workspaceId, userId, WorkspaceRole.MANAGER)).thenReturn(false);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(new Workspace()));
    }

    private void stubPrimaryDepartment(UUID departmentId) {
        Department department = org.mockito.Mockito.mock(Department.class);
        when(department.getId()).thenReturn(departmentId);
        User user = new User();
        user.setPrimaryDepartment(department);
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(userId)).thenReturn(Optional.of(user));
    }

    private void stubDepartment(UUID departmentId) {
        Department department = org.mockito.Mockito.mock(Department.class);
        when(department.getId()).thenReturn(departmentId);
        when(department.getStatus()).thenReturn(WorkspaceStatus.ACTIVE);
        Workspace workspace = org.mockito.Mockito.mock(Workspace.class);
        when(workspace.getId()).thenReturn(workspaceId);
        when(department.getWorkspace()).thenReturn(workspace);
        when(departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId))
                .thenReturn(Optional.of(department));
    }
}
