package com.trio.backend.service;

import com.trio.backend.ai.entity.AIHistory;
import com.trio.backend.ai.enums.AIProvider;
import com.trio.backend.ai.repository.AIHistoryRepository;
import com.trio.backend.enums.AIScopeType;
import com.trio.backend.reporting.analytics.dto.metrics.WorkspaceAnalyticsResponse;
import com.trio.backend.entity.*;
import com.trio.backend.repository.*;
import com.trio.backend.service.AnalyticsDataCollector;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingDataCollector {

    private final AnalyticsService analyticsService;
    private final DashboardService dashboardService;
    private final WorkspaceRepository workspaceRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;
    private final AnalyticsReportRepository analyticsReportRepository;
    private final HandoverJournalRepository handoverJournalRepository;
    private final AIHistoryRepository aiHistoryRepository;
    private final AnalyticsDataCollector analyticsDataCollector;

    public Map<String, Object> collect(UUID workspaceId, UUID departmentId, UUID projectId,
                                        LocalDate periodStart, LocalDate periodEnd) {
        return collect(workspaceId, departmentId, projectId, null, AIScopeType.DEPARTMENT, periodStart, periodEnd);
    }

    public Map<String, Object> collect(UUID workspaceId, UUID departmentId, UUID projectId, UUID teamId,
                                        AIScopeType scope, LocalDate periodStart, LocalDate periodEnd) {
        AIScopeType effectiveScope = scope != null ? scope : AIScopeType.DEPARTMENT;
        Map<String, Object> scopedData = analyticsDataCollector.collect(
                workspaceId, departmentId, projectId, teamId, effectiveScope, periodStart, periodEnd);

        Map<String, Object> data = new LinkedHashMap<>(scopedData);
        data.put("periodStart", periodStart != null ? periodStart.toString() : null);
        data.put("periodEnd", periodEnd != null ? periodEnd.toString() : null);

        if (effectiveScope == AIScopeType.WORKSPACE) {
            data.put("analyticsReport", collectLatestAnalyticsReport(workspaceId));
            data.put("handoverJournals", collectRecentHandoverJournals(workspaceId));
        } else if (departmentId != null) {
            data.put("handoverJournals", collectRecentHandoverJournalsForDepartment(workspaceId, departmentId));
        }

        data.put("summary", buildSummary(data));
        return data;
    }

    private List<Map<String, Object>> collectRecentHandoverJournalsForDepartment(UUID workspaceId, UUID departmentId) {
        var journals = handoverJournalRepository.findByDepartmentIdPaginated(departmentId, PageRequest.of(0, 5));
        return journals.getContent().stream().map(this::mapHandoverJournal).collect(Collectors.toList());
    }

    private Map<String, Object> mapHandoverJournal(HandoverJournal hj) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", hj.getId());
        map.put("projectName", hj.getProject().getName());
        map.put("journalDate", hj.getJournalDate() != null ? hj.getJournalDate().toString() : null);
        map.put("totalHandovers", hj.getTotalHandovers());
        map.put("executiveSummary", hj.getGeneratedSummary());
        map.put("generatedAt", hj.getGenerationDate() != null ? hj.getGenerationDate().toString() : null);
        return map;
    }

    private Map<String, Object> collectLatestAnalyticsReport(UUID workspaceId) {
        var reports = analyticsReportRepository.findByWorkspacePaginated(workspaceId, PageRequest.of(0, 1));
        if (reports.isEmpty()) return Collections.emptyMap();

        AnalyticsReport ar = reports.getContent().get(0);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", ar.getId());
        map.put("executiveSummary", ar.getExecutiveSummary());
        map.put("kpiHighlights", ar.getKpiHighlights());
        map.put("trendsSummary", ar.getTrendsSummary());
        map.put("riskAssessment", ar.getRiskAssessment());
        map.put("recommendations", ar.getRecommendations());
        map.put("generatedAt", ar.getGenerationDate() != null ? ar.getGenerationDate().toString() : null);
        map.put("reportDate", ar.getReportDate() != null ? ar.getReportDate().toString() : null);
        return map;
    }

    private List<Map<String, Object>> collectRecentHandoverJournals(UUID workspaceId) {
        var journals = handoverJournalRepository.findByWorkspaceIdPaginated(workspaceId, PageRequest.of(0, 5));
        return journals.getContent().stream().map(this::mapHandoverJournal).collect(Collectors.toList());
    }

    private List<Map<String, Object>> collectRecentKnowledgeHistory(UUID workspaceId) {
        var history = aiHistoryRepository.findByWorkspacePaginated(workspaceId, PageRequest.of(0, 10));
        return history.getContent().stream()
                .filter(h -> h.getProvider() == AIProvider.GROQ)
                .map(h -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", h.getId());
                    map.put("model", h.getModel());
                    map.put("prompt", truncate(h.getPrompt(), 200));
                    map.put("response", truncate(h.getResponse(), 500));
                    map.put("executionTime", h.getExecutionTime());
                    map.put("createdAt", h.getCreatedAt() != null ? h.getCreatedAt().toString() : null);
                    return map;
                }).collect(Collectors.toList());
    }

    private Map<String, Object> buildKpiOverview(WorkspaceAnalyticsResponse analytics) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (analytics.getTasks() != null) {
            map.put("activeTasks", analytics.getTasks().getActiveCount());
            map.put("archivedTasks", analytics.getTasks().getArchivedCount());
            map.put("overdueTasks", analytics.getTasks().getOverdueCount());
            map.put("completionRate", analytics.getTasks().getCompletionRate());
        }
        if (analytics.getActivities() != null) {
            map.put("totalActivities", analytics.getActivities().getTotalCount());
        }
        if (analytics.getDocuments() != null) {
            map.put("totalDocuments", analytics.getDocuments().getDocumentCount());
        }
        if (analytics.getNotifications() != null) {
            map.put("totalNotifications", analytics.getNotifications().getTotalCount());
            map.put("unreadNotifications", analytics.getNotifications().getUnreadCount());
        }
        map.put("commentCount", analytics.getCommentCount());
        map.put("memberCount", analytics.getMemberCount());
        map.put("projectCount", analytics.getProjectCount());
        return map;
    }

    private Map<String, Object> buildWorkspaceOverview(
            com.trio.backend.dto.Dashboard.scope.WorkspaceDashboardResponse dashboard) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (dashboard.getWorkspaceSummary() != null) {
            map.put("departmentCount", dashboard.getWorkspaceSummary().getDepartmentCount());
            map.put("teamCount", dashboard.getWorkspaceSummary().getTeamCount());
        }
        if (dashboard.getMemberSummary() != null) {
            map.put("totalMembers", dashboard.getMemberSummary().getTotalMembers());
            map.put("activeMembers", dashboard.getMemberSummary().getActiveMembers());
        }
        if (dashboard.getProjectSummary() != null) {
            map.put("totalProjects", dashboard.getProjectSummary().getTotalProjects());
            map.put("activeProjects", dashboard.getProjectSummary().getActiveProjects());
        }
        if (dashboard.getTaskSummary() != null) {
            map.put("totalTasks", dashboard.getTaskSummary().getTotalTasks());
            map.put("overdueTasks", dashboard.getTaskSummary().getOverdueTasks());
        }
        return map;
    }

    private String buildSummary(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("This report aggregates data from Analytics AI, Handover AI, and Knowledge AI.\n");
        @SuppressWarnings("unchecked")
        Map<String, Object> analyticsReport = (Map<String, Object>) data.get("analyticsReport");
        if (analyticsReport != null && !analyticsReport.isEmpty()) {
            sb.append("- Analytics AI Report available: ").append(analyticsReport.get("reportDate")).append("\n");
        } else {
            sb.append("- No Analytics AI report found for this period.\n");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> handovers = (List<Map<String, Object>>) data.get("handoverJournals");
        sb.append("- Handover Journals: ").append(handovers != null ? handovers.size() : 0).append(" found.\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> knowledge = (List<Map<String, Object>>) data.get("knowledgeHistory");
        sb.append("- Knowledge AI interactions: ").append(knowledge != null ? knowledge.size() : 0).append(" found.\n");
        return sb.toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
