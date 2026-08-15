package com.trio.backend.integration.projects;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {BackendApplication.class, IntegrationTestRateLimitConfig.class})
@AutoConfigureMockMvc
class TaskAssignmentIsolationIntegrationTest {

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
    @Autowired private ObjectMapper objectMapper;

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
    @DisplayName("Admin assigns task to Member A; Member B cannot see or access it")
    void adminAssignsTask_memberIsolation() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("AI");

        MvcResult createResult = mockMvc.perform(post(tasksPath(dept))
                        .header("Authorization", bearer(fixtures.getAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"admin-assigned-task","assigneeId":"%s"}
                                """.formatted(dept.member().getId())))
                .andExpect(status().isCreated())
                .andReturn();

        String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        MvcResult memberAList = mockMvc.perform(get(tasksPath(dept))
                        .header("Authorization", bearer(dept.memberToken())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode memberAContent = objectMapper.readTree(memberAList.getResponse().getContentAsString())
                .path("data").path("content");
        assertTrue(streamContainsTaskId(memberAContent, taskId), "Member A should see assigned task");

        MvcResult memberBList = mockMvc.perform(get(tasksPath(dept))
                        .header("Authorization", bearer(dept.memberBToken())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode memberBContent = objectMapper.readTree(memberBList.getResponse().getContentAsString())
                .path("data").path("content");
        assertFalse(streamContainsTaskId(memberBContent, taskId), "Member B must not see Member A task");

        mockMvc.perform(get(taskPath(dept, UUID.fromString(taskId)))
                        .header("Authorization", bearer(dept.memberBToken())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(taskPath(dept, UUID.fromString(taskId)))
                        .header("Authorization", bearer(dept.memberBToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager assigns task to Member A; manager still sees it; Member B isolated")
    void managerAssignsTask_memberIsolation() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("Development");

        MvcResult createResult = mockMvc.perform(post(tasksPath(dept))
                        .header("Authorization", bearer(dept.managerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"manager-assigned-task","assigneeId":"%s"}
                                """.formatted(dept.member().getId())))
                .andExpect(status().isCreated())
                .andReturn();

        String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        MvcResult managerList = mockMvc.perform(get(tasksPath(dept))
                        .header("Authorization", bearer(dept.managerToken())))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(streamContainsTaskId(
                objectMapper.readTree(managerList.getResponse().getContentAsString()).path("data").path("content"),
                taskId));

        mockMvc.perform(get(taskPath(dept, UUID.fromString(taskId)))
                        .header("Authorization", bearer(dept.memberToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get(taskPath(dept, UUID.fromString(taskId)))
                        .header("Authorization", bearer(dept.memberBToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Member A can update status on own assigned task")
    void memberCanUpdateOwnTaskStatus() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("Marketing");

        mockMvc.perform(put(taskPath(dept, dept.taskId()))
                        .header("Authorization", bearer(dept.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Cross-department assignment is rejected")
    void crossDepartmentAssignmentFails() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture ai = fixtures.department("AI");
        ProjectIsolationTestFixtures.DepartmentFixture dev = fixtures.department("Development");

        mockMvc.perform(post(tasksPath(ai))
                        .header("Authorization", bearer(ai.managerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"cross-dept-assign","assigneeId":"%s"}
                                """.formatted(dev.member().getId())))
                .andExpect(status().isForbidden());
    }

    private String tasksPath(ProjectIsolationTestFixtures.DepartmentFixture dept) {
        return "/api/workspaces/" + fixtures.getWorkspaceId()
                + "/departments/" + dept.departmentId()
                + "/projects/" + dept.projectId()
                + "/tasks";
    }

    private String taskPath(ProjectIsolationTestFixtures.DepartmentFixture dept, UUID taskId) {
        return tasksPath(dept) + "/" + taskId;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static boolean streamContainsTaskId(JsonNode content, String taskId) {
        if (!content.isArray()) return false;
        for (JsonNode node : content) {
            if (taskId.equals(node.path("id").asText())) {
                return true;
            }
        }
        return false;
    }
}
