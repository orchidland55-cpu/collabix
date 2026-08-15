package com.trio.backend.integration.projects;

import com.trio.backend.entity.*;
import com.trio.backend.entity.ids.RolePermissionId;
import com.trio.backend.entity.ids.UserRoleId;
import com.trio.backend.entity.ids.WorkspaceMemberId;
import com.trio.backend.enums.*;
import com.trio.backend.repository.*;
import com.trio.backend.security.jwt.JwtService;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Builds a multi-department workspace with real JPA entities for project isolation tests.
 * Department names mirror the application's departments but are only used in test setup —
 * production code never references them.
 */
@Getter
public class ProjectIsolationTestFixtures {

    /** Department names present in the Collabix application under test. */
    public static final List<String> APPLICATION_DEPARTMENT_NAMES = List.of(
            "AI",
            "Cybersecurity",
            "Development",
            "Marketing",
            "RH"
    );

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    private UUID workspaceId;
    private User adminUser;
    private String adminToken;
    private User noDepartmentManager;
    private String noDepartmentManagerToken;
    private final Map<String, DepartmentFixture> departmentsByName = new LinkedHashMap<>();

    public ProjectIsolationTestFixtures(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            DepartmentRepository departmentRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EntityManager entityManager,
            TransactionTemplate transactionTemplate) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.departmentRepository = departmentRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }

    public void seed() {
        transactionTemplate.executeWithoutResult(status -> seedInTransaction());
    }

    private void seedInTransaction() {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        Role adminRole = ensureRole(RoleName.ADMIN, projectPermissions(true, true, true, true));
        Role managerRole = ensureRole(RoleName.MANAGER, projectPermissions(true, true, true, true));
        Role memberRole = ensureRole(RoleName.MEMBER, projectPermissions(true, false, false, false));

        adminUser = createUser("admin-" + runId + "@test.local", adminRole, null);
        adminToken = tokenFor(adminUser);

        Workspace workspace = Workspace.builder()
                .name("Isolation Test Workspace " + runId)
                .status(WorkspaceStatus.ACTIVE)
                .owner(adminUser)
                .build();
        workspace = workspaceRepository.save(workspace);
        workspaceId = workspace.getId();

        workspaceMemberRepository.save(WorkspaceMember.builder()
                .workspaceMemberId(new WorkspaceMemberId(workspaceId, adminUser.getId()))
                .workspace(workspace)
                .user(adminUser)
                .role(WorkspaceRole.ADMIN)
                .status(WorkspaceMemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build());

        for (String departmentName : APPLICATION_DEPARTMENT_NAMES) {
            Department department = Department.builder()
                    .name(departmentName)
                    .status(WorkspaceStatus.ACTIVE)
                    .workspace(workspace)
                    .build();
            department = departmentRepository.save(department);

            User manager = createUser(
                    slug(departmentName) + "-manager-" + runId + "@test.local",
                    managerRole,
                    department);
            User member = createUser(
                    slug(departmentName) + "-member-" + runId + "@test.local",
                    memberRole,
                    department);
            User memberB = createUser(
                    slug(departmentName) + "-member-b-" + runId + "@test.local",
                    memberRole,
                    department);

            workspaceMemberRepository.save(memberRow(workspace, manager, WorkspaceRole.MEMBER));
            workspaceMemberRepository.save(memberRow(workspace, member, WorkspaceRole.MEMBER));
            workspaceMemberRepository.save(memberRow(workspace, memberB, WorkspaceRole.MEMBER));

            Project project = Project.builder()
                    .name(departmentName + " Project")
                    .department(department)
                    .status(WorkspaceStatus.ACTIVE)
                    .priority(ProjectPriority.MEDIUM)
                    .build();
            project = projectRepository.save(project);

            Task task = Task.builder()
                    .title(departmentName + " Task")
                    .project(project)
                    .status(TaskStatus.ACTIVE)
                    .assignee(member)
                    .build();
            task = taskRepository.save(task);

            Task unassignedTask = Task.builder()
                    .title(departmentName + " Unassigned Task")
                    .project(project)
                    .status(TaskStatus.ACTIVE)
                    .build();
            unassignedTask = taskRepository.save(unassignedTask);

            departmentsByName.put(departmentName, new DepartmentFixture(
                    department.getId(),
                    departmentName,
                    project.getId(),
                    task.getId(),
                    unassignedTask.getId(),
                    manager,
                    member,
                    memberB,
                    tokenFor(manager),
                    tokenFor(member),
                    tokenFor(memberB)
            ));
        }

        noDepartmentManager = createUser("no-dept-manager-" + runId + "@test.local", managerRole, null);
        workspaceMemberRepository.save(memberRow(workspace, noDepartmentManager, WorkspaceRole.MEMBER));
        noDepartmentManagerToken = tokenFor(noDepartmentManager);

        entityManager.flush();
        entityManager.clear();
    }

    public Collection<DepartmentFixture> allDepartments() {
        return departmentsByName.values();
    }

    public DepartmentFixture department(String name) {
        DepartmentFixture fixture = departmentsByName.get(name);
        if (fixture == null) {
            throw new IllegalArgumentException("Unknown test department: " + name);
        }
        return fixture;
    }

    private WorkspaceMember memberRow(Workspace workspace, User user, WorkspaceRole role) {
        return WorkspaceMember.builder()
                .workspaceMemberId(new WorkspaceMemberId(workspace.getId(), user.getId()))
                .workspace(workspace)
                .user(user)
                .role(role)
                .status(WorkspaceMemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();
    }

    private Role ensureRole(RoleName name, List<String> permissionCodes) {
        Role role = roleRepository.findByName(name).orElseGet(() ->
                roleRepository.save(Role.builder().name(name).description(name.name()).build()));

        for (String code : permissionCodes) {
            Permission permission = permissionRepository.findByCode(code).orElseGet(() ->
                    permissionRepository.save(Permission.builder()
                            .code(code)
                            .displayName(code)
                            .description(code)
                            .build()));

            UUID roleId = role.getId();
            UUID permissionId = permission.getId();
            if (rolePermissionRepository.findById(new RolePermissionId(roleId, permissionId)).isEmpty()) {
                rolePermissionRepository.save(RolePermission.builder()
                        .id(new RolePermissionId(roleId, permissionId))
                        .role(role)
                        .permission(permission)
                        .build());
            }
        }
        entityManager.flush();
        entityManager.clear();
        return roleRepository.findByNameWithPermissions(name).orElseThrow();
    }

    private List<String> projectPermissions(boolean read, boolean create, boolean update, boolean delete) {
        List<String> codes = new ArrayList<>();
        if (read) codes.add("PROJECT_READ");
        if (create) codes.add("PROJECT_CREATE");
        if (update) codes.add("PROJECT_UPDATE");
        if (delete) codes.add("PROJECT_DELETE");
        codes.add("TASK_READ");
        if (create) {
            codes.add("TASK_CREATE");
            codes.add("TASK_ASSIGN");
        }
        if (update) codes.add("TASK_UPDATE");
        if (delete) codes.add("TASK_DELETE");
        codes.add("DOCUMENT_READ");
        if (create) {
            codes.add("DOCUMENT_UPLOAD");
        }
        if (update) codes.add("DOCUMENT_UPDATE");
        if (delete) codes.add("DOCUMENT_DELETE");
        return codes;
    }

    private User createUser(String email, Role role, Department primaryDepartment) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("Test@123456"))
                .firstName("Test")
                .lastName("User")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .enabled(true)
                .primaryDepartment(primaryDepartment)
                .build();
        user = userRepository.save(user);

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(user.getId(), role.getId()))
                .user(user)
                .role(role)
                .build();
        user.getUserRoles().add(userRole);
        user = userRepository.save(user);
        entityManager.flush();
        return user;
    }

    private String tokenFor(User user) {
        entityManager.flush();
        entityManager.clear();
        User loaded = userRepository.findByEmailWithRolesAndPermissions(user.getEmail()).orElseThrow();
        assertFalse(loaded.getUserRoles().isEmpty(), "User must have roles for token generation");
        String token = jwtService.generateAccessToken(loaded);
        assertFalse(
                jwtService.extractPermissions(token).isEmpty(),
                "Token must carry permissions for " + user.getEmail());
        return token;
    }

    private static String slug(String departmentName) {
        return departmentName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    public record DepartmentFixture(
            UUID departmentId,
            String name,
            UUID projectId,
            UUID taskId,
            UUID unassignedTaskId,
            User manager,
            User member,
            User memberB,
            String managerToken,
            String memberToken,
            String memberBToken
    ) {}
}
