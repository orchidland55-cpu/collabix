package com.trio.backend.integration.alert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trio.backend.BackendApplication;
import com.trio.backend.dto.alert.CreateAlertCommand;
import com.trio.backend.entity.Alert;
import com.trio.backend.integration.projects.IntegrationTestRateLimitConfig;
import com.trio.backend.repository.*;
import com.trio.backend.security.jwt.JwtService;
import com.trio.backend.service.AlertService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {BackendApplication.class, IntegrationTestRateLimitConfig.class})
@AutoConfigureMockMvc
class AlertModuleIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AlertService alertService;

    private AlertIntegrationTestFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures = new AlertIntegrationTestFixtures(
                roleRepository, permissionRepository, rolePermissionRepository,
                userRepository, workspaceRepository, workspaceMemberRepository,
                passwordEncoder, jwtService, entityManager, transactionTemplate);
        fixtures.seed();
    }

    @Test
    @DisplayName("User A can list alerts and count unread alerts")
    void listAndCount() throws Exception {
        createAlertForUserA();

        MvcResult listResult = mockMvc.perform(get(fixtures.alertsPath())
                        .header("Authorization", bearer(fixtures.getUserAToken())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("content");
        assertTrue(content.isArray() && content.size() >= 1, "User A should see own alerts");

        MvcResult countResult = mockMvc.perform(get(fixtures.alertsPath() + "/unread/count")
                        .header("Authorization", bearer(fixtures.getUserAToken())))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(1L, objectMapper.readTree(countResult.getResponse().getContentAsString())
                .path("data").asLong());
    }

    @Test
    @DisplayName("User B cannot see User A's alerts (ownership isolation)")
    void ownershipIsolation() throws Exception {
        UUID alertId = createAlertForUserA();

        MvcResult listB = mockMvc.perform(get(fixtures.alertsPath())
                        .header("Authorization", bearer(fixtures.getUserBToken())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode contentB = objectMapper.readTree(listB.getResponse().getContentAsString())
                .path("data").path("content");
        assertTrue(contentB.isArray() && contentB.isEmpty(), "User B must not see User A alerts");

        mockMvc.perform(get(fixtures.alertsPath() + "/" + alertId)
                        .header("Authorization", bearer(fixtures.getUserBToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User A can mark an alert as read and dismiss it")
    void markReadAndDismiss() throws Exception {
        UUID alertId = createAlertForUserA();

        mockMvc.perform(put(fixtures.alertsPath() + "/" + alertId + "/read")
                        .header("Authorization", bearer(fixtures.getUserAToken())))
                .andExpect(status().isOk());

        MvcResult countResult = mockMvc.perform(get(fixtures.alertsPath() + "/unread/count")
                        .header("Authorization", bearer(fixtures.getUserAToken())))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(0L, objectMapper.readTree(countResult.getResponse().getContentAsString())
                .path("data").asLong());

        mockMvc.perform(delete(fixtures.alertsPath() + "/" + alertId)
                        .header("Authorization", bearer(fixtures.getUserAToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(fixtures.alertsPath() + "/" + alertId)
                        .header("Authorization", bearer(fixtures.getUserAToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Idempotent alert creation via dedup key only creates one alert")
    void dedupKeyIdempotency() {
        CreateAlertCommand command = CreateAlertCommand.builder()
                .workspaceId(fixtures.getWorkspaceId())
                .recipientId(fixtures.getUserA().getId())
                .type(Alert.AlertType.TASK_OVERDUE)
                .severity(Alert.Severity.CRITICAL)
                .title("Duplicate test")
                .resourceType("TASK")
                .resourceId(UUID.randomUUID())
                .dedupKey("TASK_OVERDUE:" + fixtures.getUserA().getId() + ":dup")
                .build();

        Alert first = alertService.createInternal(command);
        Alert second = alertService.createInternal(command);

        assertTrue(first != null, "First creation should persist the alert");
        assertEquals(null, second, "Duplicate creation should be skipped by dedup key");
    }

    private UUID createAlertForUserA() {
        CreateAlertCommand command = CreateAlertCommand.builder()
                .workspaceId(fixtures.getWorkspaceId())
                .recipientId(fixtures.getUserA().getId())
                .type(Alert.AlertType.TASK_DEADLINE_APPROACHING)
                .severity(Alert.Severity.WARNING)
                .title("Deadline approaching: Fix the build")
                .message("This task is due soon.")
                .resourceType("TASK")
                .resourceId(UUID.randomUUID())
                .dedupKey("TASK_DEADLINE_APPROACHING:" + fixtures.getUserA().getId() + ":task1")
                .build();
        Alert saved = alertService.createInternal(command);
        return saved.getId();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}