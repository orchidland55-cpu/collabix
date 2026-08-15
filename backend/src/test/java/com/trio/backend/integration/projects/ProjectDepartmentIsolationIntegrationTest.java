package com.trio.backend.integration.projects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trio.backend.BackendApplication;
import com.trio.backend.repository.*;
import com.trio.backend.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API validation of project department isolation across every application department.
 */
@SpringBootTest(classes = {BackendApplication.class, IntegrationTestRateLimitConfig.class})
@AutoConfigureMockMvc
class ProjectDepartmentIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private ProjectIsolationTestFixtures fixtures;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        fixtures = new ProjectIsolationTestFixtures(
                roleRepository,
                permissionRepository,
                rolePermissionRepository,
                userRepository,
                workspaceRepository,
                workspaceMemberRepository,
                departmentRepository,
                projectRepository,
                taskRepository,
                passwordEncoder,
                jwtService,
                entityManager,
                transactionTemplate);
        fixtures.seed();
    }

    @Test
    @DisplayName("Admin can list and open projects in every department")
    void adminCanAccessAllDepartments() throws Exception {
        for (ProjectIsolationTestFixtures.DepartmentFixture dept : fixtures.allDepartments()) {
            mockMvc.perform(get(projectsPath(dept.departmentId()))
                            .header("Authorization", bearer(fixtures.getAdminToken())))
                    .andExpect(status().isOk());

            mockMvc.perform(get(projectPath(dept.departmentId(), dept.projectId()))
                            .header("Authorization", bearer(fixtures.getAdminToken())))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/workspaces/" + fixtures.getWorkspaceId() + "/projects")
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Cross-department matrix: every Manager and Member")
    void crossDepartmentAccessMatrix() throws Exception {
        List<String> failures = new ArrayList<>();

        for (ProjectIsolationTestFixtures.DepartmentFixture actor : fixtures.allDepartments()) {
            for (ProjectIsolationTestFixtures.DepartmentFixture target : fixtures.allDepartments()) {
                boolean ownDepartment = actor.departmentId().equals(target.departmentId());

                assertAccess(
                        failures,
                        actor.name() + " Manager",
                        target.name(),
                        "list",
                        ownDepartment,
                        get(projectsPath(target.departmentId())).header("Authorization", bearer(actor.managerToken())));

                assertAccess(
                        failures,
                        actor.name() + " Manager",
                        target.name(),
                        "get project",
                        ownDepartment,
                        get(projectPath(target.departmentId(), target.projectId()))
                                .header("Authorization", bearer(actor.managerToken())));

                assertAccess(
                        failures,
                        actor.name() + " Member",
                        target.name(),
                        "list",
                        ownDepartment,
                        get(projectsPath(target.departmentId())).header("Authorization", bearer(actor.memberToken())));

                assertAccess(
                        failures,
                        actor.name() + " Member",
                        target.name(),
                        "get project",
                        ownDepartment,
                        get(projectPath(target.departmentId(), target.projectId()))
                                .header("Authorization", bearer(actor.memberToken())));

                if (!ownDepartment) {
                    assertDenied(
                            failures,
                            actor.name() + " Manager",
                            target.name(),
                            "get task",
                            get(taskPath(target.departmentId(), target.projectId(), target.taskId()))
                                    .header("Authorization", bearer(actor.managerToken())));

                    assertDenied(
                            failures,
                            actor.name() + " Member",
                            target.name(),
                            "get task",
                            get(taskPath(target.departmentId(), target.projectId(), target.taskId()))
                                    .header("Authorization", bearer(actor.memberToken())));
                }
            }
        }

        if (!failures.isEmpty()) {
            fail("Cross-department matrix failures:\n" + String.join("\n", failures));
        }
    }

    @Test
    @DisplayName("Every Manager can create projects only in own department")
    void managerCreateProjectIsolation() throws Exception {
        List<String> failures = new ArrayList<>();

        for (ProjectIsolationTestFixtures.DepartmentFixture actor : fixtures.allDepartments()) {
            String body = "{\"name\":\"manager-created-" + slug(actor.name()) + "\"}";

            for (ProjectIsolationTestFixtures.DepartmentFixture target : fixtures.allDepartments()) {
                boolean ownDepartment = actor.departmentId().equals(target.departmentId());
                int status = mockMvc.perform(post(projectsPath(target.departmentId()))
                                .header("Authorization", bearer(actor.managerToken()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andReturn()
                        .getResponse()
                        .getStatus();

                if (ownDepartment && status != 201) {
                    failures.add(actor.name() + " Manager create own dept expected 201 got " + status);
                }
                if (!ownDepartment && status != 403) {
                    failures.add(actor.name() + " Manager create " + target.name() + " expected 403 got " + status);
                }
            }
        }

        if (!failures.isEmpty()) {
            fail(String.join("\n", failures));
        }
    }

    @Test
    @DisplayName("Every Manager can update own department projects only")
    void managerUpdateProjectIsolation() throws Exception {
        List<String> failures = new ArrayList<>();

        for (ProjectIsolationTestFixtures.DepartmentFixture actor : fixtures.allDepartments()) {
            for (ProjectIsolationTestFixtures.DepartmentFixture target : fixtures.allDepartments()) {
                boolean ownDepartment = actor.departmentId().equals(target.departmentId());
                int status = mockMvc.perform(put(projectPath(target.departmentId(), target.projectId()))
                                .header("Authorization", bearer(actor.managerToken()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"description\":\"updated-by-" + slug(actor.name()) + "\"}"))
                        .andReturn()
                        .getResponse()
                        .getStatus();

                if (ownDepartment && status != 200) {
                    failures.add(actor.name() + " Manager update own expected 200 got " + status);
                }
                if (!ownDepartment && status != 403) {
                    failures.add(actor.name() + " Manager update " + target.name() + " expected 403 got " + status);
                }
            }
        }

        if (!failures.isEmpty()) {
            fail(String.join("\n", failures));
        }
    }

    @Test
    @DisplayName("Members cannot create projects in any department")
    void memberCannotCreateProjects() throws Exception {
        for (ProjectIsolationTestFixtures.DepartmentFixture dept : fixtures.allDepartments()) {
            mockMvc.perform(post(projectsPath(dept.departmentId()))
                            .header("Authorization", bearer(dept.memberToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"member-should-not-create\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Admin can create projects in every department")
    void adminCanCreateInEveryDepartment() throws Exception {
        for (ProjectIsolationTestFixtures.DepartmentFixture dept : fixtures.allDepartments()) {
            mockMvc.perform(post(projectsPath(dept.departmentId()))
                            .header("Authorization", bearer(fixtures.getAdminToken()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"admin-created-" + slug(dept.name()) + "\"}"))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("Manager workspace-wide list is scoped to own department")
    void managerWorkspaceListScopedToOwnDepartment() throws Exception {
        for (ProjectIsolationTestFixtures.DepartmentFixture dept : fixtures.allDepartments()) {
            MvcResult result = mockMvc.perform(get("/api/workspaces/" + fixtures.getWorkspaceId() + "/projects")
                            .header("Authorization", bearer(dept.managerToken())))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("data").path("content");
            assertTrue(content.isArray());

            for (JsonNode project : content) {
                String departmentId = project.path("departmentId").asText();
                assertEquals(dept.departmentId().toString(), departmentId,
                        dept.name() + " Manager must not see projects from other departments in workspace list");
            }
        }
    }

    @Test
    @DisplayName("Search on own department path does not leak other department projects")
    void searchDoesNotLeakOtherDepartments() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture ai = fixtures.department("AI");
        ProjectIsolationTestFixtures.DepartmentFixture development = fixtures.department("Development");

        MvcResult result = mockMvc.perform(get(projectsPath(ai.departmentId()))
                        .param("search", "Development Project")
                        .header("Authorization", bearer(ai.managerToken())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("content");
        for (JsonNode project : content) {
            assertNotEquals(development.projectId().toString(), project.path("id").asText(),
                    "AI Manager search must not return Development project");
        }

        MvcResult adminResult = mockMvc.perform(get(projectsPath(development.departmentId()))
                        .param("search", "Development")
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminContent = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                .path("data").path("content");
        assertTrue(adminContent.size() >= 1, "Admin search should find Development project");
    }

    @Test
    @DisplayName("Direct URL: wrong department path for project is denied")
    void directUrlWrongDepartmentPathReturnsNotFound() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture ai = fixtures.department("AI");
        ProjectIsolationTestFixtures.DepartmentFixture development = fixtures.department("Development");

        int status = mockMvc.perform(get(projectPath(ai.departmentId(), development.projectId()))
                        .header("Authorization", bearer(ai.managerToken())))
                .andReturn()
                .getResponse()
                .getStatus();

        assertTrue(status == 403 || status == 404,
                "Cross-department direct URL must return 403 or 404, got " + status);
    }

    @Test
    @DisplayName("Manager without assigned department is denied everywhere")
    void noDepartmentManagerIsDenied() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture any = fixtures.allDepartments().iterator().next();

        mockMvc.perform(get(projectsPath(any.departmentId()))
                        .header("Authorization", bearer(fixtures.getNoDepartmentManagerToken())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/workspaces/" + fixtures.getWorkspaceId() + "/projects")
                        .header("Authorization", bearer(fixtures.getNoDepartmentManagerToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Member sees only assigned tasks; manager sees all in department")
    void memberTaskVisibilityIsScopedToAssignee() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.allDepartments().iterator().next();

        MvcResult memberList = mockMvc.perform(get(tasksPath(dept.departmentId(), dept.projectId()))
                        .header("Authorization", bearer(dept.memberToken())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode memberContent = objectMapper.readTree(memberList.getResponse().getContentAsString())
                .path("data").path("content");
        assertEquals(1, memberContent.size(), "Member should only see assigned tasks");
        assertEquals(dept.taskId().toString(), memberContent.get(0).path("id").asText());

        mockMvc.perform(get(taskPath(dept.departmentId(), dept.projectId(), dept.unassignedTaskId()))
                        .header("Authorization", bearer(dept.memberToken())))
                .andExpect(status().isForbidden());

        MvcResult managerList = mockMvc.perform(get(tasksPath(dept.departmentId(), dept.projectId()))
                        .header("Authorization", bearer(dept.managerToken())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode managerContent = objectMapper.readTree(managerList.getResponse().getContentAsString())
                .path("data").path("content");
        assertTrue(managerContent.size() >= 2, "Manager should see all department project tasks");
    }

    @Test
    @DisplayName("All application departments are present in test fixture")
    void allDepartmentsAreCovered() {
        assertEquals(
                ProjectIsolationTestFixtures.APPLICATION_DEPARTMENT_NAMES.size(),
                fixtures.allDepartments().size());
        for (String name : ProjectIsolationTestFixtures.APPLICATION_DEPARTMENT_NAMES) {
            assertTrue(fixtures.allDepartments().stream().anyMatch(d -> d.name().equals(name)),
                    "Missing department fixture: " + name);
        }
    }

    private void assertAccess(
            List<String> failures,
            String actor,
            String targetDept,
            String action,
            boolean shouldAllow,
            org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        int status = mockMvc.perform(request).andReturn().getResponse().getStatus();
        if (shouldAllow && status != 200) {
            failures.add(actor + " -> " + targetDept + " " + action + " expected 200 got " + status);
        }
        if (!shouldAllow && status != 403 && status != 404) {
            failures.add(actor + " -> " + targetDept + " " + action + " expected 403/404 got " + status);
        }
    }

    private void assertDenied(
            List<String> failures,
            String actor,
            String targetDept,
            String action,
            org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        int status = mockMvc.perform(request).andReturn().getResponse().getStatus();
        if (status != 403 && status != 404) {
            failures.add(actor + " -> " + targetDept + " " + action + " expected 403/404 got " + status);
        }
    }

    private String projectsPath(UUID departmentId) {
        return "/api/workspaces/" + fixtures.getWorkspaceId() + "/departments/" + departmentId + "/projects";
    }

    private String projectPath(UUID departmentId, UUID projectId) {
        return projectsPath(departmentId) + "/" + projectId;
    }

    private String tasksPath(UUID departmentId, UUID projectId) {
        return projectPath(departmentId, projectId) + "/tasks";
    }

    private String taskPath(UUID departmentId, UUID projectId, UUID taskId) {
        return projectPath(departmentId, projectId) + "/tasks/" + taskId;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String slug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }
}
