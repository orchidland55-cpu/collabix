package com.trio.backend.service;

import com.trio.backend.dto.organisation.project.CreateProjectRequest;
import com.trio.backend.dto.organisation.project.ProjectResponse;
import com.trio.backend.dto.organisation.project.UpdateProjectRequest;
import com.trio.backend.entity.Department;
import com.trio.backend.entity.Project;
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
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.ProjectMapper;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.ProjectRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;
import com.trio.backend.security.department.DepartmentScopeGuard;
import com.trio.backend.security.user.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private DepartmentScopeGuard departmentScopeGuard;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private UUID workspaceId;
    private UUID rhDepartmentId;
    private UUID devDepartmentId;
    private UUID projectId;
    private User admin;
    private User manager;
    private User member;
    private Workspace workspace;
    private Department rhDepartment;
    private Department devDepartment;
    private Project rhProject;
    private Project devProject;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        workspaceId = UUID.randomUUID();
        rhDepartmentId = UUID.randomUUID();
        devDepartmentId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        workspace = Workspace.builder()
                .name("Collabix")
                .status(WorkspaceStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(workspace, "id", workspaceId);

        rhDepartment = Department.builder()
                .name("RH")
                .status(WorkspaceStatus.ACTIVE)
                .workspace(workspace)
                .build();
        ReflectionTestUtils.setField(rhDepartment, "id", rhDepartmentId);

        devDepartment = Department.builder()
                .name("Development")
                .status(WorkspaceStatus.ACTIVE)
                .workspace(workspace)
                .build();
        ReflectionTestUtils.setField(devDepartment, "id", devDepartmentId);

        admin = buildUser("admin@example.com", rhDepartment, RoleName.ADMIN);
        manager = buildUser("manager@example.com", rhDepartment, RoleName.MANAGER);
        member = buildUser("member@example.com", rhDepartment, RoleName.MEMBER);

        rhProject = Project.builder()
                .name("rh project")
                .status(WorkspaceStatus.ACTIVE)
                .department(rhDepartment)
                .build();
        ReflectionTestUtils.setField(rhProject, "id", projectId);

        devProject = Project.builder()
                .name("dev project")
                .status(WorkspaceStatus.ACTIVE)
                .department(devDepartment)
                .build();
        ReflectionTestUtils.setField(devProject, "id", UUID.randomUUID());
    }

    @Test
    void listPaginatedShouldAllowAdminToListAllDepartmentProjects() {
        authenticate(admin, WorkspaceRole.ADMIN);
        stubActiveWorkspaceMember(admin, WorkspaceRole.ADMIN);
        doNothing().when(departmentScopeGuard).assertDepartmentAccessible(workspaceId, devDepartmentId, admin.getId());

        when(projectRepository.findAllByDepartment_IdAndStatus(eq(devDepartmentId), eq(WorkspaceStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(devProject)));
        when(projectMapper.toResponse(devProject)).thenReturn(new ProjectResponse());

        Page<ProjectResponse> result = projectService.listPaginated(
                workspaceId, devDepartmentId, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        verify(departmentScopeGuard).assertDepartmentAccessible(workspaceId, devDepartmentId, admin.getId());
    }

    @Test
    void listPaginatedShouldRejectManagerListingAnotherDepartment() {
        authenticate(manager, WorkspaceRole.MEMBER);
        stubActiveWorkspaceMember(manager, WorkspaceRole.MEMBER);
        doThrow(new ForbiddenException("You do not have access to this department."))
                .when(departmentScopeGuard).assertDepartmentAccessible(workspaceId, devDepartmentId, manager.getId());

        assertThrows(ForbiddenException.class, () ->
                projectService.listPaginated(workspaceId, devDepartmentId, null, PageRequest.of(0, 20)));
    }

    @Test
    void getByIdShouldAllowManagerForOwnDepartmentProject() {
        authenticate(manager, WorkspaceRole.MEMBER);
        stubActiveWorkspaceMember(manager, WorkspaceRole.MEMBER);
        doNothing().when(departmentScopeGuard).assertDepartmentAccessible(workspaceId, rhDepartmentId, manager.getId());
        when(projectRepository.findByIdAndDepartment_Id(projectId, rhDepartmentId))
                .thenReturn(Optional.of(rhProject));
        when(projectMapper.toResponse(rhProject)).thenReturn(new ProjectResponse());

        ProjectResponse result = projectService.getById(workspaceId, rhDepartmentId, projectId);

        assertNotNull(result);
    }

    @Test
    void getByIdShouldRejectManagerForAnotherDepartmentProject() {
        authenticate(manager, WorkspaceRole.MEMBER);
        stubActiveWorkspaceMember(manager, WorkspaceRole.MEMBER);
        doThrow(new ForbiddenException("You do not have access to this department."))
                .when(departmentScopeGuard).assertDepartmentAccessible(workspaceId, devDepartmentId, manager.getId());

        assertThrows(ForbiddenException.class, () ->
                projectService.getById(workspaceId, devDepartmentId, devProject.getId()));
    }

    @Test
    void getByIdShouldRejectMemberForAnotherDepartmentProject() {
        authenticate(member, WorkspaceRole.MEMBER);
        stubActiveWorkspaceMember(member, WorkspaceRole.MEMBER);
        doThrow(new ForbiddenException("You do not have access to this department."))
                .when(departmentScopeGuard).assertDepartmentAccessible(workspaceId, devDepartmentId, member.getId());

        assertThrows(ForbiddenException.class, () ->
                projectService.getById(workspaceId, devDepartmentId, devProject.getId()));
    }

    @Test
    void getByIdShouldRejectCrossWorkspaceProjectLookup() {
        authenticate(admin, WorkspaceRole.ADMIN);
        stubActiveWorkspaceMember(admin, WorkspaceRole.ADMIN);
        doNothing().when(departmentScopeGuard).assertDepartmentAccessible(workspaceId, rhDepartmentId, admin.getId());

        UUID otherWorkspaceId = UUID.randomUUID();
        Workspace otherWorkspace = Workspace.builder()
                .name("Other")
                .status(WorkspaceStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(otherWorkspace, "id", otherWorkspaceId);
        rhDepartment.setWorkspace(otherWorkspace);

        when(projectRepository.findByIdAndDepartment_Id(projectId, rhDepartmentId))
                .thenReturn(Optional.of(rhProject));

        assertThrows(ResourceNotFoundException.class, () ->
                projectService.getById(workspaceId, rhDepartmentId, projectId));
    }

    @Test
    void createShouldAllowAdminForAnotherDepartment() {
        authenticate(admin, WorkspaceRole.ADMIN);
        stubActiveWorkspaceMember(admin, WorkspaceRole.ADMIN);
        doNothing().when(departmentScopeGuard).assertCanManageProjects(workspaceId, devDepartmentId, admin.getId());
        doNothing().when(departmentScopeGuard).assertDepartmentAccessible(workspaceId, devDepartmentId, admin.getId());

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("New Dev Project");

        when(departmentRepository.findByIdAndWorkspace_Id(devDepartmentId, workspaceId))
                .thenReturn(Optional.of(devDepartment));
        when(projectRepository.existsByDepartment_IdAndNameIgnoreCase(devDepartmentId, "new dev project"))
                .thenReturn(false);
        when(projectMapper.toEntity(any(CreateProjectRequest.class))).thenReturn(new Project());
        when(projectRepository.save(any(Project.class))).thenReturn(devProject);
        when(projectMapper.toResponse(devProject)).thenReturn(new ProjectResponse());

        projectService.create(workspaceId, devDepartmentId, request);

        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createShouldAllowManagerForOwnDepartment() {
        authenticate(manager, WorkspaceRole.MEMBER);
        stubActiveWorkspaceMember(manager, WorkspaceRole.MEMBER);
        doNothing().when(departmentScopeGuard).assertCanManageProjects(workspaceId, rhDepartmentId, manager.getId());
        doNothing().when(departmentScopeGuard).assertDepartmentAccessible(workspaceId, rhDepartmentId, manager.getId());

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("New RH Project");

        when(departmentRepository.findByIdAndWorkspace_Id(rhDepartmentId, workspaceId))
                .thenReturn(Optional.of(rhDepartment));
        when(projectRepository.existsByDepartment_IdAndNameIgnoreCase(rhDepartmentId, "new rh project"))
                .thenReturn(false);
        when(projectMapper.toEntity(any(CreateProjectRequest.class))).thenReturn(new Project());
        when(projectRepository.save(any(Project.class))).thenReturn(rhProject);
        when(projectMapper.toResponse(rhProject)).thenReturn(new ProjectResponse());

        projectService.create(workspaceId, rhDepartmentId, request);

        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createShouldRejectManagerForAnotherDepartment() {
        authenticate(manager, WorkspaceRole.MEMBER);
        stubActiveWorkspaceMember(manager, WorkspaceRole.MEMBER);
        doThrow(new ForbiddenException("You do not have access to this department."))
                .when(departmentScopeGuard).assertCanManageProjects(workspaceId, devDepartmentId, manager.getId());

        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Invalid Project");

        assertThrows(ForbiddenException.class, () ->
                projectService.create(workspaceId, devDepartmentId, request));
    }

    @Test
    void updateShouldRejectManagerForAnotherDepartmentProject() {
        authenticate(manager, WorkspaceRole.MEMBER);
        stubActiveWorkspaceMember(manager, WorkspaceRole.MEMBER);
        doThrow(new ForbiddenException("You do not have access to this department."))
                .when(departmentScopeGuard).assertCanManageProjects(workspaceId, devDepartmentId, manager.getId());

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setName("Updated");

        assertThrows(ForbiddenException.class, () ->
                projectService.update(workspaceId, devDepartmentId, devProject.getId(), request));
    }

    @Test
    void listAllPaginatedShouldScopeManagerToPrimaryDepartment() {
        authenticate(manager, WorkspaceRole.MEMBER);
        stubActiveWorkspaceMember(manager, WorkspaceRole.MEMBER);
        when(departmentScopeGuard.resolveAccessibleDepartmentId(workspaceId, manager.getId()))
                .thenReturn(rhDepartmentId);
        doNothing().when(departmentScopeGuard).assertDepartmentAccessible(workspaceId, rhDepartmentId, manager.getId());
        when(projectRepository.findAllByDepartment_IdAndStatus(eq(rhDepartmentId), eq(WorkspaceStatus.ACTIVE), any()))
                .thenReturn(new PageImpl<>(List.of(rhProject)));
        when(projectMapper.toResponse(rhProject)).thenReturn(new ProjectResponse());

        Page<ProjectResponse> result = projectService.listAllPaginated(workspaceId, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        verify(projectRepository, never()).findAllByWorkspaceIdAndStatus(any(), any(), any());
    }

    @Test
    void listAllPaginatedShouldAllowAdminToListAllDepartments() {
        authenticate(admin, WorkspaceRole.ADMIN);
        stubActiveWorkspaceMember(admin, WorkspaceRole.ADMIN);
        when(departmentScopeGuard.resolveAccessibleDepartmentId(workspaceId, admin.getId())).thenReturn(null);
        when(projectRepository.findAllByWorkspaceIdAndStatus(workspaceId, WorkspaceStatus.ACTIVE, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(rhProject, devProject)));
        when(projectMapper.toResponse(any(Project.class))).thenReturn(new ProjectResponse());

        Page<ProjectResponse> result = projectService.listAllPaginated(workspaceId, null, PageRequest.of(0, 20));

        assertEquals(2, result.getTotalElements());
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

    private void authenticate(User user, WorkspaceRole workspaceRole) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null, List.of()));
    }

    private void stubActiveWorkspaceMember(User user, WorkspaceRole role) {
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
