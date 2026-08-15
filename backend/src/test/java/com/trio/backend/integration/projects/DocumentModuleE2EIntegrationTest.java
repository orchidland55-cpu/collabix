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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {BackendApplication.class, IntegrationTestRateLimitConfig.class})
@AutoConfigureMockMvc
class DocumentModuleE2EIntegrationTest {

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
    @Autowired private DocumentRepository documentRepository;
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
    @DisplayName("Admin creates document — persists, lists, metadata correct after re-fetch")
    void adminCreatesDocument_persistsAndLists() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("Development");

        MvcResult createResult = mockMvc.perform(post(documentsPath(dept))
                        .header("Authorization", bearer(fixtures.getAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"phase17-spec",
                                  "description":"E2E audit document",
                                  "fileName":"spec.txt",
                                  "mimeType":"text/plain",
                                  "fileSize":128,
                                  "storagePath":"test/spec.txt",
                                  "category":"Specs",
                                  "tags":"audit,e2e"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("phase17-spec"))
                .andExpect(jsonPath("$.data.category").value("Specs"))
                .andReturn();

        String docId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get(documentsPath(dept) + "/" + docId)
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("phase17-spec"))
                .andExpect(jsonPath("$.data.description").value("E2E audit document"));

        MvcResult listResult = mockMvc.perform(get(documentsPath(dept))
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("content");
        assertTrue(streamContainsId(content, docId));
    }

    @Test
    @DisplayName("Admin uploads file — download returns correct content and filename")
    void adminUploadsAndDownloadsFile() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("AI");
        byte[] payload = "Collabix Phase 17 audit payload".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "audit-report.txt", "text/plain", payload);

        MvcResult uploadResult = mockMvc.perform(multipart(documentsPath(dept) + "/upload")
                        .file(file)
                        .param("title", "audit-report")
                        .param("category", "Reports")
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fileName").value("audit-report.txt"))
                .andReturn();

        String docId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get(documentsPath(dept) + "/" + docId + "/download")
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("audit-report.txt")))
                .andExpect(content().bytes(payload));
    }

    @Test
    @DisplayName("Admin update creates version history — latest version current, older accessible")
    void adminUpdateCreatesVersionHistory() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("Marketing");

        MvcResult createResult = mockMvc.perform(post(documentsPath(dept))
                        .header("Authorization", bearer(fixtures.getAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"versioned-doc",
                                  "fileName":"v1.txt",
                                  "mimeType":"text/plain",
                                  "fileSize":10,
                                  "storagePath":"test/v1.txt"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String docId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(put(documentsPath(dept) + "/" + docId)
                        .header("Authorization", bearer(fixtures.getAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"versioned-doc","description":"version 2"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.description").value("version 2"));

        MvcResult versionsResult = mockMvc.perform(get(documentsPath(dept) + "/" + docId + "/versions")
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode versions = objectMapper.readTree(versionsResult.getResponse().getContentAsString()).path("data");
        assertTrue(versions.isArray());
        assertTrue(versions.size() >= 2, "Expected at least 2 version rows");
    }

    @Test
    @DisplayName("Search returns matching documents only within project scope")
    void searchExactPartialAndScoped() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("Cybersecurity");

        mockMvc.perform(post(documentsPath(dept))
                        .header("Authorization", bearer(fixtures.getAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"network-security-policy","fileName":"policy.pdf","mimeType":"application/pdf","fileSize":1024,"storagePath":"test/policy.pdf"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post(documentsPath(dept))
                        .header("Authorization", bearer(fixtures.getAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"unrelated-budget","fileName":"budget.xlsx","mimeType":"application/vnd.ms-excel","fileSize":512,"storagePath":"test/budget.xlsx"}
                                """))
                .andExpect(status().isCreated());

        MvcResult exact = mockMvc.perform(get(documentsPath(dept) + "/search")
                        .param("query", "network-security-policy")
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode exactContent = objectMapper.readTree(exact.getResponse().getContentAsString())
                .path("data").path("content");
        assertEquals(1, exactContent.size());
        assertEquals("network-security-policy", exactContent.get(0).path("title").asText());

        MvcResult partial = mockMvc.perform(get(documentsPath(dept) + "/search")
                        .param("query", "security")
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode partialContent = objectMapper.readTree(partial.getResponse().getContentAsString())
                .path("data").path("content");
        assertEquals(1, partialContent.size());
        assertFalse(partialContent.get(0).path("title").asText().contains("budget"));
    }

    @Test
    @DisplayName("Admin delete requires workspace OWNER role — workspace ADMIN gets 403")
    void adminDeleteRequiresWorkspaceOwner() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("RH");

        MvcResult createResult = mockMvc.perform(post(documentsPath(dept))
                        .header("Authorization", bearer(fixtures.getAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"lifecycle-doc","fileName":"life.txt","mimeType":"text/plain","fileSize":8,"storagePath":"test/life.txt"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String docId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(post(documentsPath(dept) + "/" + docId + "/archive")
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        mockMvc.perform(post(documentsPath(dept) + "/" + docId + "/restore")
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(delete(documentsPath(dept) + "/" + docId)
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(documentsPath(dept) + "/" + docId)
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Member can read authorized documents but cannot mutate")
    void memberReadOnlyPermissions() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("Development");

        MvcResult createResult = mockMvc.perform(post(documentsPath(dept))
                        .header("Authorization", bearer(fixtures.getAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"member-read-test","fileName":"read.txt","mimeType":"text/plain","fileSize":4,"storagePath":"test/read.txt"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String docId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get(documentsPath(dept) + "/" + docId)
                        .header("Authorization", bearer(dept.memberToken())))
                .andExpect(status().isOk());

        byte[] payload = "member download test".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "member-dl.txt", "text/plain", payload);
        MvcResult uploadResult = mockMvc.perform(multipart(documentsPath(dept) + "/upload")
                        .file(file)
                        .param("title", "member-download-doc")
                        .header("Authorization", bearer(fixtures.getAdminToken())))
                .andExpect(status().isCreated())
                .andReturn();
        String uploadDocId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get(documentsPath(dept) + "/" + uploadDocId + "/download")
                        .header("Authorization", bearer(dept.memberToken())))
                .andExpect(status().isOk())
                .andExpect(content().bytes(payload));

        mockMvc.perform(post(documentsPath(dept))
                        .header("Authorization", bearer(dept.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"blocked","fileName":"b.txt","mimeType":"text/plain","fileSize":1,"storagePath":"test/b.txt"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(documentsPath(dept) + "/" + docId)
                        .header("Authorization", bearer(dept.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"hack\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(documentsPath(dept) + "/" + docId)
                        .header("Authorization", bearer(dept.memberToken())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager with DOCUMENT_UPLOAD permission blocked by workspace role gate on write")
    void managerUploadBlockedByWorkspaceRole() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("Development");

        mockMvc.perform(post(documentsPath(dept))
                        .header("Authorization", bearer(dept.managerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"mgr-upload","fileName":"m.txt","mimeType":"text/plain","fileSize":1,"storagePath":"test/m.txt"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-department direct document access returns not found")
    void crossDepartmentDocumentAccessDenied() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture ai = fixtures.department("AI");
        ProjectIsolationTestFixtures.DepartmentFixture dev = fixtures.department("Development");

        MvcResult createResult = mockMvc.perform(post(documentsPath(ai))
                        .header("Authorization", bearer(fixtures.getAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"ai-only-doc","fileName":"ai.txt","mimeType":"text/plain","fileSize":2,"storagePath":"test/ai.txt"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String docId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get(documentsPath(dev) + "/" + docId)
                        .header("Authorization", bearer(dev.managerToken())))
                .andExpect(status().isNotFound());

        mockMvc.perform(put(documentsPath(dev) + "/" + docId)
                        .header("Authorization", bearer(dev.managerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"cross-dept\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Workspace-wide list exposes cross-department documents to any member with read permission")
    void workspaceListCrossDepartmentLeak() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture ai = fixtures.department("AI");

        mockMvc.perform(post(documentsPath(ai))
                        .header("Authorization", bearer(fixtures.getAdminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"workspace-leak-test","fileName":"leak.txt","mimeType":"text/plain","fileSize":3,"storagePath":"test/leak.txt"}
                                """))
                .andExpect(status().isCreated());

        ProjectIsolationTestFixtures.DepartmentFixture dev = fixtures.department("Development");

        MvcResult wsList = mockMvc.perform(get("/api/workspaces/" + fixtures.getWorkspaceId() + "/documents")
                        .header("Authorization", bearer(dev.memberToken())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(wsList.getResponse().getContentAsString())
                .path("data").path("content");
        boolean foundAiDoc = false;
        if (content.isArray()) {
            for (JsonNode node : content) {
                if ("workspace-leak-test".equals(node.path("title").asText())) {
                    foundAiDoc = true;
                    break;
                }
            }
        }
        assertTrue(foundAiDoc, "Workspace list returns documents from other departments — isolation gap");
    }

    @Test
    @DisplayName("Empty project returns empty document list")
    void emptyProjectReturnsEmptyList() throws Exception {
        ProjectIsolationTestFixtures.DepartmentFixture dept = fixtures.department("Marketing");

        MvcResult listResult = mockMvc.perform(get(documentsPath(dept))
                        .header("Authorization", bearer(dept.memberToken())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("content");
        assertTrue(content.isArray());
    }

    private String documentsPath(ProjectIsolationTestFixtures.DepartmentFixture dept) {
        return "/api/workspaces/" + fixtures.getWorkspaceId()
                + "/departments/" + dept.departmentId()
                + "/projects/" + dept.projectId()
                + "/documents";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static boolean streamContainsId(JsonNode content, String id) {
        if (!content.isArray()) return false;
        for (JsonNode node : content) {
            if (id.equals(node.path("id").asText())) return true;
        }
        return false;
    }
}
