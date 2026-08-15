package com.trio.backend.service;

import com.trio.backend.dto.organisation.task.UpdateTaskRequest;
import com.trio.backend.entity.*;
import com.trio.backend.entity.ids.UserRoleId;
import com.trio.backend.entity.ids.WorkspaceMemberId;
import com.trio.backend.enums.*;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.mapper.TaskMapper;
import com.trio.backend.repository.*;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private SprintRepository sprintRepository;
    @Mock private SecurityAuditRepository securityAuditRepository;
    @Mock private MarketingCampaignRepository marketingCampaignRepository;
    @Mock private UserRepository userRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private TaskMapper taskMapper;
    @Mock private DepartmentScopeGuard departmentScopeGuard;

    @InjectMocks
    private TaskServiceImpl taskService;

    private UUID workspaceId;
    private UUID departmentId;
    private UUID projectId;
    private UUID taskId;
    private UUID memberId;
    private User member;
    private WorkspaceMember workspaceMember;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        departmentId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        memberId = UUID.randomUUID();

        member = User.builder()
                .email("member@test.local")
                .password("x")
                .firstName("Mem")
                .lastName("Ber")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);

        Role memberRole = Role.builder().name(RoleName.MEMBER).build();
        ReflectionTestUtils.setField(memberRole, "id", UUID.randomUUID());
        UserRole ur = UserRole.builder()
                .id(new UserRoleId(memberId, memberRole.getId()))
                .user(member)
                .role(memberRole)
                .build();
        member.setUserRoles(Set.of(ur));

        workspaceMember = WorkspaceMember.builder()
                .workspaceMemberId(new WorkspaceMemberId(workspaceId, memberId))
                .role(WorkspaceRole.MEMBER)
                .status(WorkspaceMemberStatus.ACTIVE)
                .build();

        CustomUserDetails principal = new CustomUserDetails(member);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Test
    void listShouldForceAssigneeFilterForMembers() {
        when(workspaceMemberRepository.findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, memberId))
                .thenReturn(Optional.of(workspaceMember));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(memberId))
                .thenReturn(Optional.of(member));
        when(projectRepository.findByIdAndDepartment_Id(projectId, departmentId))
                .thenReturn(Optional.of(activeProject()));
        when(taskRepository.findFiltered(eq(projectId), isNull(), eq(List.of()), eq(true), isNull(), eq(memberId), any()))
                .thenReturn(Page.empty());

        taskService.list(workspaceId, departmentId, projectId, null, null, null, UUID.randomUUID(), PageRequest.of(0, 20));

        verify(taskRepository).findFiltered(eq(projectId), isNull(), eq(List.of()), eq(true), isNull(), eq(memberId), any());
    }

    @Test
    void getByIdShouldRejectUnassignedTaskForMember() {
        when(workspaceMemberRepository.findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, memberId))
                .thenReturn(Optional.of(workspaceMember));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(memberId))
                .thenReturn(Optional.of(member));

        Task task = activeTask(null);
        when(taskRepository.findByIdAndProject_Id(taskId, projectId)).thenReturn(Optional.of(task));

        assertThrows(ForbiddenException.class,
                () -> taskService.getById(workspaceId, departmentId, projectId, taskId));
    }

    @Test
    void updateShouldRejectCrossDepartmentAssigneeForManager() {
        UUID otherDeptId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();

        User manager = User.builder()
                .email("mgr@test.local")
                .password("x")
                .firstName("Mgr")
                .lastName("User")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .build();
        UUID managerId = UUID.randomUUID();
        ReflectionTestUtils.setField(manager, "id", managerId);
        Role managerRole = Role.builder().name(RoleName.MANAGER).build();
        manager.setUserRoles(Set.of(UserRole.builder()
                .id(new UserRoleId(managerId, UUID.randomUUID()))
                .user(manager)
                .role(managerRole)
                .build()));

        CustomUserDetails principal = new CustomUserDetails(manager);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        WorkspaceMember mgrMember = WorkspaceMember.builder()
                .workspaceMemberId(new WorkspaceMemberId(workspaceId, managerId))
                .role(WorkspaceRole.MEMBER)
                .status(WorkspaceMemberStatus.ACTIVE)
                .build();

        User assignee = User.builder()
                .email("other@test.local")
                .password("x")
                .firstName("Other")
                .lastName("Dept")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .primaryDepartment(Department.builder().name("Other").status(WorkspaceStatus.ACTIVE).build())
                .build();
        ReflectionTestUtils.setField(assignee.getPrimaryDepartment(), "id", otherDeptId);
        ReflectionTestUtils.setField(assignee, "id", assigneeId);

        Task task = activeTask(member);
        when(workspaceMemberRepository.findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, managerId))
                .thenReturn(Optional.of(mgrMember));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(managerId)).thenReturn(Optional.of(manager));
        when(taskRepository.findByIdAndProject_Id(taskId, projectId)).thenReturn(Optional.of(task));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(assigneeId)).thenReturn(Optional.of(assignee));
        when(workspaceMemberRepository.findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, assigneeId))
                .thenReturn(Optional.of(mgrMember));

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssigneeId(assigneeId);

        assertThrows(ForbiddenException.class,
                () -> taskService.update(workspaceId, departmentId, projectId, taskId, request));
    }

    @Test
    void memberCanUpdateStatusOnAssignedTask() {
        Task task = activeTask(member);
        when(workspaceMemberRepository.findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, memberId))
                .thenReturn(Optional.of(workspaceMember));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(memberId))
                .thenReturn(Optional.of(member));
        when(taskRepository.findByIdAndProject_Id(taskId, projectId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskMapper.toResponse(any(Task.class))).thenReturn(null);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setStatus(TaskStatus.COMPLETED);

        assertDoesNotThrow(() -> taskService.update(workspaceId, departmentId, projectId, taskId, request));
    }

    @Test
    void memberCannotReassignTask() {
        Task task = activeTask(member);
        when(workspaceMemberRepository.findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, memberId))
                .thenReturn(Optional.of(workspaceMember));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(memberId))
                .thenReturn(Optional.of(member));
        when(taskRepository.findByIdAndProject_Id(taskId, projectId)).thenReturn(Optional.of(task));

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setAssigneeId(UUID.randomUUID());

        assertThrows(ForbiddenException.class,
                () -> taskService.update(workspaceId, departmentId, projectId, taskId, request));
    }

    @Test
    void managerCanChangeWorkflowStatusForDepartmentTask() {
        User manager = User.builder()
                .email("mgr@test.local")
                .password("x")
                .firstName("Mgr")
                .lastName("User")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .build();
        UUID managerId = UUID.randomUUID();
        ReflectionTestUtils.setField(manager, "id", managerId);
        Role managerRole = Role.builder().name(RoleName.MANAGER).build();
        manager.setUserRoles(Set.of(UserRole.builder()
                .id(new UserRoleId(managerId, UUID.randomUUID()))
                .user(manager)
                .role(managerRole)
                .build()));

        CustomUserDetails principal = new CustomUserDetails(manager);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        WorkspaceMember mgrMember = WorkspaceMember.builder()
                .workspaceMemberId(new WorkspaceMemberId(workspaceId, managerId))
                .role(WorkspaceRole.MEMBER)
                .status(WorkspaceMemberStatus.ACTIVE)
                .build();

        Task task = activeTask(member);
        when(workspaceMemberRepository.findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, managerId))
                .thenReturn(Optional.of(mgrMember));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(managerId)).thenReturn(Optional.of(manager));
        when(taskRepository.findByIdAndProject_Id(taskId, projectId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskMapper.toResponse(any(Task.class))).thenReturn(null);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        assertDoesNotThrow(() -> taskService.update(workspaceId, departmentId, projectId, taskId, request));
    }

    @Test
    void assigneeCanMoveTaskToValidWorkflowStatus() {
        Task task = activeTask(member);
        when(workspaceMemberRepository.findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, memberId))
                .thenReturn(Optional.of(workspaceMember));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(memberId))
                .thenReturn(Optional.of(member));
        when(taskRepository.findByIdAndProject_Id(taskId, projectId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskMapper.toResponse(any(Task.class))).thenReturn(null);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        assertDoesNotThrow(() -> taskService.update(workspaceId, departmentId, projectId, taskId, request));
    }

    @Test
    void assigneeCannotSetArchivedStatusViaWorkflowUpdate() {
        Task task = activeTask(member);
        when(workspaceMemberRepository.findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, memberId))
                .thenReturn(Optional.of(workspaceMember));
        when(userRepository.findByIdWithRolesAndPrimaryDepartment(memberId))
                .thenReturn(Optional.of(member));
        when(taskRepository.findByIdAndProject_Id(taskId, projectId)).thenReturn(Optional.of(task));

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setStatus(TaskStatus.ARCHIVED);

        assertThrows(BadRequestException.class,
                () -> taskService.update(workspaceId, departmentId, projectId, taskId, request));
    }

    private Project activeProject() {
        Workspace workspace = Workspace.builder().name("WS").status(WorkspaceStatus.ACTIVE).build();
        ReflectionTestUtils.setField(workspace, "id", workspaceId);
        Department department = Department.builder().name("Dev").status(WorkspaceStatus.ACTIVE).workspace(workspace).build();
        ReflectionTestUtils.setField(department, "id", departmentId);
        Project project = Project.builder().name("P").department(department).status(WorkspaceStatus.ACTIVE).build();
        ReflectionTestUtils.setField(project, "id", projectId);
        return project;
    }

    private Task activeTask(User assignee) {
        Task task = Task.builder()
                .title("task")
                .project(activeProject())
                .status(TaskStatus.ACTIVE)
                .assignee(assignee)
                .build();
        ReflectionTestUtils.setField(task, "id", taskId);
        return task;
    }
}
