package com.trio.backend.integration.projects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trio.backend.BackendApplication;
import com.trio.backend.repository.*;
import com.trio.backend.security.jwt.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {BackendApplication.class, IntegrationTestRateLimitConfig.class})
@AutoConfigureMockMvc
class TaskKanbanWorkflowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;

    private ProjectIsolationTestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new ProjectIsolationTestFixtures(
                roleRepository, permissionRepository, rolePermissionRepository,
                userRepository, workspaceRepository, workspaceMemberRepository,
                departmentRepository, projectRepository, taskRepository,
                passwordEncoder, jwtService, entityManager, transactionTemplate);
        fixtures.seed();
    }

    @Test
    @DisplayName("Assignee can move task through Kanban workflow and change persists")
    void assigneeCanUpdateWorkflowStatus() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("Development");

        mockMvc.perform(put(taskPath(dept, dept.taskId()))
                        .header("Authorization", bearer(dept.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(get(taskPath(dept, dept.taskId()))
                        .header("Authorization", bearer(dept.memberToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Non-assignee member cannot move another member's task")
    void nonAssigneeMemberCannotUpdateWorkflowStatus() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("AI");

        mockMvc.perform(put(taskPath(dept, dept.taskId()))
                        .header("Authorization", bearer(dept.memberBToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager can move department tasks through Kanban workflow")
    void managerCanMoveDepartmentTask() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("Marketing");

        mockMvc.perform(get(taskPath(dept, dept.taskId()))
                        .header("Authorization", bearer(dept.managerToken())))
                .andExpect(status().isOk());

        mockMvc.perform(put(taskPath(dept, dept.taskId()))
                        .header("Authorization", bearer(dept.managerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Manager can still update non-status fields on department tasks")
    void managerCanUpdateNonStatusFields() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("Cybersecurity");

        mockMvc.perform(put(taskPath(dept, dept.taskId()))
                        .header("Authorization", bearer(dept.managerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"manager-updated-title\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("manager-updated-title"));
    }

    @Test
    @DisplayName("Manager can delete a department task")
    void managerCanDeleteDepartmentTask() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("Development");
        UUID taskId = dept.taskId();

        mockMvc.perform(delete(taskPath(dept, taskId))
                        .header("Authorization", bearer(dept.managerToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(taskPath(dept, taskId))
                        .header("Authorization", bearer(dept.managerToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Member cannot delete another member's task")
    void memberCannotDeleteTask() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("AI");

        mockMvc.perform(delete(taskPath(dept, dept.taskId()))
                        .header("Authorization", bearer(dept.memberToken())))
                .andExpect(status().isForbidden());
    }

    private String taskPath(ProjectIsolationTestFixtures.DepartmentFixture dept, UUID taskId) {
        return "/api/workspaces/" + fixtures.getWorkspaceId()
                + "/departments/" + dept.departmentId()
                + "/projects/" + dept.projectId()
                + "/tasks/" + taskId;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
