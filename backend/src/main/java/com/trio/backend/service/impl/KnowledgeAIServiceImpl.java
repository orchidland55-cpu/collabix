package com.trio.backend.service.impl;

import com.trio.backend.ai.dto.request.AIExecutionRequest;
import com.trio.backend.ai.dto.response.AIExecutionResponse;
import com.trio.backend.ai.entity.AIHistory;
import com.trio.backend.ai.enums.AITask;
import com.trio.backend.ai.repository.AIHistoryRepository;
import com.trio.backend.ai.service.AIOrchestratorService;
import com.trio.backend.dto.ai.KnowledgeAIResponse;
import com.trio.backend.dto.ai.KnowledgeSource;
import com.trio.backend.entity.Document;
import com.trio.backend.entity.KnowledgeBase;
import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.repository.DocumentRepository;
import com.trio.backend.repository.KnowledgeBaseRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;
import com.trio.backend.security.ai.AIScopeAuthorization;
import com.trio.backend.security.user.CustomUserDetails;
import com.trio.backend.service.KnowledgeAIService;
import com.trio.backend.service.KnowledgeDataCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class KnowledgeAIServiceImpl implements KnowledgeAIService {

    private final KnowledgeDataCollector knowledgeDataCollector;
    private final AIOrchestratorService orchestratorService;
    private final AIHistoryRepository aiHistoryRepository;
    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AIScopeAuthorization aiScopeAuthorization;

    @Override
    public KnowledgeAIResponse ask(UUID workspaceId, UUID departmentId, UUID projectId, String question) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertCanAccessKnowledge(workspaceId, departmentId, projectId);

        Map<String, Object> collectedData = knowledgeDataCollector.collect(
                workspaceId, departmentId, projectId, question);

        Integer totalFound = (Integer) collectedData.get("totalDocumentsFound");
        if (totalFound == null || totalFound == 0) {
            return KnowledgeAIResponse.builder()
                    .answer("I couldn't find information related to this question in the available company documentation.")
                    .sources(List.of())
                    .confidence("none")
                    .executionTime(0L)
                    .timestamp(Instant.now())
                    .build();
        }

        AIExecutionRequest executionRequest = new AIExecutionRequest();
        executionRequest.setTask(AITask.KNOWLEDGE_SEARCH);
        executionRequest.setInput(question);
        executionRequest.setWorkspaceId(workspaceId);
        executionRequest.setDepartmentId(departmentId);
        executionRequest.setProjectId(projectId);
        executionRequest.setUserId(userId);
        executionRequest.setContext(collectedData);

        long start = System.currentTimeMillis();
        AIExecutionResponse aiResponse = orchestratorService.execute(executionRequest);
        long executionTime = System.currentTimeMillis() - start;

        List<KnowledgeSource> sources = buildSources(collectedData);

        return KnowledgeAIResponse.builder()
                .answer(aiResponse.getResponse())
                .sources(sources)
                .confidence("high")
                .executionTime(executionTime)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeSource> search(UUID workspaceId, UUID departmentId, UUID projectId, String query) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertCanAccessKnowledge(workspaceId, departmentId, projectId);

        List<KnowledgeSource> results = new ArrayList<>();

        List<Document> documents;
        if (projectId != null) {
            documents = documentRepository.searchByTitleInProjectPaginated(
                    projectId, query, PageRequest.of(0, 20)).getContent();
        } else {
            documents = documentRepository.searchByTitleInWorkspacePaginated(
                    workspaceId, query, PageRequest.of(0, 20)).getContent();
        }
        for (Document doc : documents) {
            if (departmentId != null && !doc.getProject().getDepartment().getId().equals(departmentId)) {
                continue;
            }
            results.add(KnowledgeSource.builder()
                    .id(doc.getId())
                    .title(doc.getTitle())
                    .type("DOCUMENT")
                    .category(doc.getCategory())
                    .summary(doc.getDescription())
                    .projectName(doc.getProject().getName())
                    .departmentName(doc.getProject().getDepartment().getName())
                    .lastUpdated(doc.getUpdatedAt())
                    .version(doc.getDocumentVersion())
                    .build());
        }

        List<KnowledgeBase> articles;
        if (projectId != null) {
            articles = knowledgeBaseRepository.searchByContentInProjectPaginated(
                    projectId, query, PageRequest.of(0, 20)).getContent();
        } else {
            articles = knowledgeBaseRepository.searchByContentInWorkspacePaginated(
                    workspaceId, query, PageRequest.of(0, 20)).getContent();
        }
        for (KnowledgeBase kb : articles) {
            if (departmentId != null && !kb.getProject().getDepartment().getId().equals(departmentId)) {
                continue;
            }
            results.add(KnowledgeSource.builder()
                    .id(kb.getId())
                    .title(kb.getTitle())
                    .type("KNOWLEDGE_ARTICLE")
                    .category(kb.getCategory())
                    .summary(kb.getSummary())
                    .projectName(kb.getProject().getName())
                    .departmentName(kb.getProject().getDepartment().getName())
                    .lastUpdated(kb.getUpdatedAt())
                    .version(kb.getArticleVersion())
                    .build());
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<?> getHistory(UUID workspaceId, int page, int size) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertActiveWorkspaceMember(workspaceId, userId);

        return aiScopeAuthorization.resolveReadableDepartmentFilter(workspaceId)
                .map(deptId -> aiHistoryRepository.findByWorkspaceAndDepartmentPaginated(
                        workspaceId, deptId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .orElseGet(() -> aiHistoryRepository.findByWorkspacePaginated(
                        workspaceId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    private List<KnowledgeSource> buildSources(Map<String, Object> collectedData) {
        List<KnowledgeSource> sources = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> documents = (List<Map<String, Object>>) collectedData.get("documents");
        if (documents != null) {
            for (Map<String, Object> doc : documents) {
                sources.add(KnowledgeSource.builder()
                        .id((UUID) doc.get("id"))
                        .title((String) doc.get("title"))
                        .type("DOCUMENT")
                        .category((String) doc.get("category"))
                        .summary((String) doc.get("description"))
                        .projectName((String) doc.get("projectName"))
                        .departmentName((String) doc.get("departmentName"))
                        .version((Integer) doc.get("version"))
                        .build());
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> articles = (List<Map<String, Object>>) collectedData.get("knowledgeArticles");
        if (articles != null) {
            for (Map<String, Object> art : articles) {
                sources.add(KnowledgeSource.builder()
                        .id((UUID) art.get("id"))
                        .title((String) art.get("title"))
                        .type("KNOWLEDGE_ARTICLE")
                        .category((String) art.get("category"))
                        .summary((String) art.get("summary"))
                        .projectName((String) art.get("projectName"))
                        .departmentName((String) art.get("departmentName"))
                        .version((Integer) art.get("version"))
                        .build());
            }
        }

        return sources;
    }

    private UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new BadRequestException("User is not authenticated.");
        }
        return user.getId();
    }

    private void assertActiveWorkspaceMember(UUID workspaceId, UUID userId) {
        WorkspaceMember wm = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace."));
        if (wm.getStatus() != com.trio.backend.enums.WorkspaceMemberStatus.ACTIVE) {
            throw new ForbiddenException("You are not an active member of this workspace.");
        }
    }
}
