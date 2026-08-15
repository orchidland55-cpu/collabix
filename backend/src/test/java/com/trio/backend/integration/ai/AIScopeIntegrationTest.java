package com.trio.backend.integration.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trio.backend.BackendApplication;
import com.trio.backend.ai.dto.response.AIExecutionResponse;
import com.trio.backend.ai.enums.AITask;
import com.trio.backend.ai.service.AIOrchestratorService;
import com.trio.backend.integration.projects.IntegrationTestRateLimitConfig;
import com.trio.backend.integration.projects.ProjectIsolationTestFixtures;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {BackendApplication.class, IntegrationTestRateLimitConfig.class})
@AutoConfigureMockMvc
class AIScopeIntegrationTest {

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

    @MockitoBean
    private AIOrchestratorService orchestratorService;

    private ProjectIsolationTestFixtures fixtures;
    private String adminAiToken;

    @BeforeEach
    void setUp() {
        fixtures = new ProjectIsolationTestFixtures(
                roleRepository, permissionRepository, rolePermissionRepository,
                userRepository, workspaceRepository, workspaceMemberRepository,
                departmentRepository, projectRepository, taskRepository,
                passwordEncoder, jwtService, entityManager, transactionTemplate);
        fixtures.seed();
        grantAiPermissions();
        adminAiToken = jwtService.generateAccessToken(
                userRepository.findByEmailWithRolesAndPermissions(fixtures.getAdminUser().getEmail()).orElseThrow());
        when(orchestratorService.execute(any())).thenReturn(
                AIExecutionResponse.builder()
                        .task(AITask.REPORT_GENERATION)
                        .status("SUCCESS")
                        .response("Scoped AI output")
                        .executionTime(10L)
                        .build());
    }

    private void grantAiPermissions() {
        transactionTemplate.executeWithoutResult(status -> {
            grant("ADMIN", "REPORT_CREATE", "REPORT_READ", "REPORT_UPDATE", "ANALYTICS_VIEW",
                    "HANDOVER_CREATE", "KNOWLEDGE_BASE_READ", "AI_MODEL_READ");
            grant("MANAGER", "REPORT_CREATE", "REPORT_READ", "REPORT_UPDATE", "ANALYTICS_VIEW",
                    "HANDOVER_CREATE", "KNOWLEDGE_BASE_READ", "AI_MODEL_READ");
            grant("MEMBER", "REPORT_READ", "KNOWLEDGE_BASE_READ", "AI_MODEL_READ");
        });
    }

    private void grant(String roleName, String... codes) {
        var role = roleRepository.findByName(com.trio.backend.enums.RoleName.valueOf(roleName)).orElseThrow();
        for (String code : codes) {
            var permission = permissionRepository.findByCode(code).orElseGet(() ->
                    permissionRepository.save(com.trio.backend.entity.Permission.builder()
                            .code(code).displayName(code).description(code).build()));
            var id = new com.trio.backend.entity.ids.RolePermissionId(role.getId(), permission.getId());
            if (rolePermissionRepository.findById(id).isEmpty()) {
                rolePermissionRepository.save(com.trio.backend.entity.RolePermission.builder()
                        .id(id).role(role).permission(permission).build());
            }
        }
    }

    @Test
    @DisplayName("Admin can generate workspace report")
    void adminGeneratesWorkspaceReport() throws Exception {
        Map<String, Object> body = reportBody(null, "WORKSPACE");
        mockMvc.perform(post("/api/reports/ai/generate")
                        .header("Authorization", "Bearer " + adminAiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Manager can generate own department report")
    void managerGeneratesOwnDepartmentReport() throws Exception {
        var marketing = fixtures.department("Marketing");
        Map<String, Object> body = reportBody(marketing.departmentId(), "DEPARTMENT");
        mockMvc.perform(post("/api/reports/ai/generate")
                        .header("Authorization", "Bearer " + marketing.managerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Manager cannot generate other department report")
    void managerBlockedFromOtherDepartment() throws Exception {
        var hrManager = fixtures.department("RH");
        var marketing = fixtures.department("Marketing");
        Map<String, Object> body = reportBody(marketing.departmentId(), "DEPARTMENT");
        mockMvc.perform(post("/api/reports/ai/generate")
                        .header("Authorization", "Bearer " + hrManager.managerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager cannot generate workspace-wide report")
    void managerBlockedFromWorkspaceScope() throws Exception {
        var marketing = fixtures.department("Marketing");
        Map<String, Object> body = reportBody(null, "WORKSPACE");
        mockMvc.perform(post("/api/reports/ai/generate")
                        .header("Authorization", "Bearer " + marketing.managerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Member cannot generate reports")
    void memberCannotGenerateReports() throws Exception {
        var marketing = fixtures.department("Marketing");
        Map<String, Object> body = reportBody(marketing.departmentId(), "DEPARTMENT");
        mockMvc.perform(post("/api/reports/ai/generate")
                        .header("Authorization", "Bearer " + marketing.memberToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Member cannot read other department report by ID")
    void memberCannotReadOtherDepartmentReport() throws Exception {
        var marketing = fixtures.department("Marketing");
        var development = fixtures.department("Development");

        Map<String, Object> body = reportBody(development.departmentId(), "DEPARTMENT");
        String json = mockMvc.perform(post("/api/reports/ai/generate")
                        .header("Authorization", "Bearer " + adminAiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID reportId = UUID.fromString(objectMapper.readTree(json).path("data").path("reportId").asText());

        mockMvc.perform(get("/api/reports/ai/{id}", reportId)
                        .param("workspaceId", fixtures.getWorkspaceId().toString())
                        .header("Authorization", "Bearer " + marketing.memberToken()))
                .andExpect(status().isForbidden());
    }

    private Map<String, Object> reportBody(UUID departmentId, String scope) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("workspaceId", fixtures.getWorkspaceId());
        if (departmentId != null) {
            body.put("departmentId", departmentId);
        }
        body.put("scope", scope);
        body.put("title", "Test Report");
        body.put("reportType", "EXECUTIVE");
        return body;
    }
}
