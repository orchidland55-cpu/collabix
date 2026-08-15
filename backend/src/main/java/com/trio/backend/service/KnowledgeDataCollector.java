package com.trio.backend.service;

import com.trio.backend.entity.Document;
import com.trio.backend.entity.KnowledgeBase;
import com.trio.backend.entity.Workspace;
import com.trio.backend.repository.DocumentRepository;
import com.trio.backend.repository.KnowledgeBaseRepository;
import com.trio.backend.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeDataCollector {

    private static final int MAX_RESULTS = 20;

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final WorkspaceRepository workspaceRepository;

    public Map<String, Object> collect(UUID workspaceId, UUID departmentId, UUID projectId, String question) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        String searchTerm = extractKeywords(question);

        List<Map<String, Object>> documentResults = searchDocuments(workspaceId, departmentId, projectId, searchTerm);
        List<Map<String, Object>> knowledgeResults = searchKnowledgeBase(workspaceId, departmentId, projectId, searchTerm);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("workspaceName", workspace.getName());
        data.put("workspaceId", workspaceId);
        data.put("departmentId", departmentId);
        data.put("projectId", projectId);
        data.put("question", question);
        data.put("searchTerm", searchTerm);

        data.put("documents", documentResults);
        data.put("documentCount", documentResults.size());

        data.put("knowledgeArticles", knowledgeResults);
        data.put("knowledgeArticleCount", knowledgeResults.size());

        data.put("totalDocumentsFound", documentResults.size() + knowledgeResults.size());

        return data;
    }

    public List<Map<String, Object>> searchDocuments(UUID workspaceId, UUID departmentId, UUID projectId, String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return List.of();
        }

        List<Document> documents;
        if (projectId != null) {
            documents = documentRepository.searchByTitleInProjectPaginated(
                    projectId, searchTerm, PageRequest.of(0, MAX_RESULTS)).getContent();
        } else {
            documents = documentRepository.searchByTitleInWorkspacePaginated(
                    workspaceId, searchTerm, PageRequest.of(0, MAX_RESULTS)).getContent();
        }

        Set<UUID> seenIds = new HashSet<>();
        List<Document> combined = new ArrayList<>(documents);

        List<Document> fileNameMatches;
        if (projectId != null) {
            fileNameMatches = documentRepository.searchByFileNameInProjectPaginated(
                    projectId, searchTerm, PageRequest.of(0, MAX_RESULTS)).getContent();
        } else {
            fileNameMatches = documentRepository.searchByFileNameInWorkspacePaginated(
                    workspaceId, searchTerm, PageRequest.of(0, MAX_RESULTS)).getContent();
        }
        for (Document doc : fileNameMatches) {
            if (seenIds.add(doc.getId())) {
                combined.add(doc);
            }
        }

        return combined.stream()
                .filter(doc -> departmentId == null || doc.getProject().getDepartment().getId().equals(departmentId))
                .limit(MAX_RESULTS)
                .map(this::formatDocument)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> searchKnowledgeBase(UUID workspaceId, UUID departmentId, UUID projectId, String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return List.of();
        }

        List<KnowledgeBase> articles;
        if (projectId != null) {
            articles = knowledgeBaseRepository.searchByContentInProjectPaginated(
                    projectId, searchTerm, PageRequest.of(0, MAX_RESULTS)).getContent();
        } else {
            articles = knowledgeBaseRepository.searchByContentInWorkspacePaginated(
                    workspaceId, searchTerm, PageRequest.of(0, MAX_RESULTS)).getContent();
        }

        return articles.stream()
                .filter(kb -> departmentId == null || kb.getProject().getDepartment().getId().equals(departmentId))
                .limit(MAX_RESULTS)
                .map(this::formatKnowledgeBase)
                .collect(Collectors.toList());
    }

    private Map<String, Object> formatDocument(Document doc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", doc.getId());
        map.put("title", doc.getTitle());
        map.put("description", doc.getDescription());
        map.put("fileName", doc.getFileName());
        map.put("mimeType", doc.getMimeType());
        map.put("category", doc.getCategory());
        map.put("tags", doc.getTags());
        map.put("version", doc.getDocumentVersion());
        map.put("projectName", doc.getProject().getName());
        map.put("projectId", doc.getProject().getId());
        map.put("departmentName", doc.getProject().getDepartment().getName());
        map.put("departmentId", doc.getProject().getDepartment().getId());
        map.put("createdAt", doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null);
        map.put("updatedAt", doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : null);
        return map;
    }

    private Map<String, Object> formatKnowledgeBase(KnowledgeBase kb) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", kb.getId());
        map.put("title", kb.getTitle());
        map.put("content", kb.getContent());
        map.put("summary", kb.getSummary());
        map.put("category", kb.getCategory());
        map.put("tags", kb.getTags());
        map.put("version", kb.getArticleVersion());
        map.put("isPinned", kb.getIsPinned());
        map.put("projectName", kb.getProject().getName());
        map.put("projectId", kb.getProject().getId());
        map.put("departmentName", kb.getProject().getDepartment().getName());
        map.put("departmentId", kb.getProject().getDepartment().getId());
        map.put("createdAt", kb.getCreatedAt() != null ? kb.getCreatedAt().toString() : null);
        map.put("updatedAt", kb.getUpdatedAt() != null ? kb.getUpdatedAt().toString() : null);
        return map;
    }

    private String extractKeywords(String question) {
        if (question == null || question.isBlank()) return "";
        String cleaned = question.toLowerCase()
                .replaceAll("[?.,!;:()]", " ")
                .replaceAll("\\s+", " ").trim();
        List<String> stopWords = List.of("what", "where", "when", "why", "how", "who",
                "which", "the", "a", "an", "is", "are", "was", "were", "do", "does",
                "did", "can", "could", "will", "would", "should", "may", "might",
                "please", "tell", "me", "about", "show", "find", "give", "list",
                "has", "have", "had", "been", "being", "get", "got", "does");
        return Arrays.stream(cleaned.split(" "))
                .filter(w -> w.length() > 2 && !stopWords.contains(w))
                .collect(Collectors.joining(" "));
    }
}
