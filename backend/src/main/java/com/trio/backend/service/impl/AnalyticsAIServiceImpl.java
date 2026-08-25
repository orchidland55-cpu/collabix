package com.trio.backend.service.impl;

import com.trio.backend.ai.dto.request.AIExecutionRequest;
import com.trio.backend.ai.dto.response.AIExecutionResponse;
import com.trio.backend.ai.enums.AITask;
import com.trio.backend.ai.service.AIOrchestratorService;
import com.trio.backend.dto.ai.AnalyticsAIEditRequest;
import com.trio.backend.dto.ai.AnalyticsAIResponse;
import com.trio.backend.entity.AnalyticsReport;
import com.trio.backend.entity.Project;
import com.trio.backend.entity.Workspace;
import com.trio.backend.security.user.CustomUserDetails;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.entity.Department;
import com.trio.backend.repository.AnalyticsReportRepository;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.ProjectRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;
import com.trio.backend.enums.AIScopeType;
import com.trio.backend.security.ai.AIScopeAuthorization;
import com.trio.backend.util.AIScopeUtils;
import com.trio.backend.service.AnalyticsAIService;
import com.trio.backend.service.AnalyticsDataCollector;
import com.trio.backend.service.AlertGenerationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AnalyticsAIServiceImpl implements AnalyticsAIService {

    private final AnalyticsDataCollector analyticsDataCollector;
    private final AIOrchestratorService orchestratorService;
    private final AnalyticsReportRepository analyticsReportRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AIScopeAuthorization aiScopeAuthorization;
    private final AlertGenerationHelper alertGenerationHelper;

    @Override
    public AnalyticsAIResponse generate(UUID workspaceId, UUID departmentId, UUID projectId,
                                         LocalDate startDate, LocalDate endDate) {
        return generate(workspaceId, departmentId, projectId, null, null, startDate, endDate);
    }

    @Override
    public AnalyticsAIResponse generate(UUID workspaceId, UUID departmentId, UUID projectId, UUID teamId,
                                         AIScopeType scope, LocalDate startDate, LocalDate endDate) {
        UUID userId = getAuthenticatedUserId();
        AIScopeType effectiveScope = AIScopeUtils.resolveScope(scope, projectId, teamId);
        aiScopeAuthorization.assertCanGenerate(workspaceId, effectiveScope, departmentId, projectId, teamId);

        Map<String, Object> collectedData = analyticsDataCollector.collect(
                workspaceId, departmentId, projectId, teamId, effectiveScope, startDate, endDate);

        AIExecutionRequest executionRequest = new AIExecutionRequest();
        executionRequest.setTask(AITask.ANALYTICS_SUMMARY);
        executionRequest.setInput("Generate analytics executive report for workspace: "
                + collectedData.get("workspaceName"));
        executionRequest.setWorkspaceId(workspaceId);
        executionRequest.setDepartmentId(departmentId);
        executionRequest.setProjectId(projectId);
        executionRequest.setUserId(userId);
        executionRequest.setContext(collectedData);

        long start = System.currentTimeMillis();
        AIExecutionResponse aiResponse;
        try {
            aiResponse = orchestratorService.execute(executionRequest);
        } catch (RuntimeException ex) {
            alertGenerationHelper.recordAiFailure(
                    workspaceId, userId, departmentId, "ANALYTICS_REPORT", null,
                    "AI analytics generation failed",
                    "The AI could not generate the analytics report. Please try again.");
            throw ex;
        }
        long executionTime = System.currentTimeMillis() - start;

        return saveReport(workspaceId, departmentId, projectId, userId, startDate, endDate,
                aiResponse, executionTime);
    }

    @Override
    public AnalyticsAIResponse regenerate(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertCanGenerate(workspaceId, AIScopeType.DEPARTMENT, departmentId, projectId, null);

        AnalyticsReport report = analyticsReportRepository.findByIdAndWorkspace(reportId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Analytics report not found"));

        return generate(workspaceId, departmentId, projectId,
                report.getTimeRangeStart(), report.getTimeRangeEnd());
    }

    @Override
    public AnalyticsAIResponse edit(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId,
                                     AnalyticsAIEditRequest request) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertCanGenerate(workspaceId, AIScopeType.DEPARTMENT, departmentId, projectId, null);

        AnalyticsReport report = analyticsReportRepository.findByIdAndWorkspace(reportId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Analytics report not found"));

        if (request.getExecutiveSummary() != null) report.setExecutiveSummary(request.getExecutiveSummary());
        if (request.getKpiHighlights() != null) report.setKpiHighlights(request.getKpiHighlights());
        if (request.getTrendsSummary() != null) report.setTrendsSummary(request.getTrendsSummary());
        if (request.getRiskAssessment() != null) report.setRiskAssessment(request.getRiskAssessment());
        if (request.getRecommendations() != null) report.setRecommendations(request.getRecommendations());
        if (request.getDetailedReport() != null) report.setDetailedReport(request.getDetailedReport());

        AnalyticsReport saved = analyticsReportRepository.save(report);
        return toResponse(saved, System.currentTimeMillis());
    }

    @Override
    public AnalyticsAIResponse approve(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertCanGenerate(workspaceId, AIScopeType.DEPARTMENT, departmentId, projectId, null);

        AnalyticsReport report = analyticsReportRepository.findByIdAndWorkspace(reportId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Analytics report not found"));

        report.setApprovalStatus(AnalyticsReport.ApprovalStatus.APPROVED);
        report.setApprovedBy(userId);
        report.setApprovedAt(LocalDateTime.now());
        AnalyticsReport saved = analyticsReportRepository.save(report);
        return toResponse(saved, System.currentTimeMillis());
    }

    @Override
    public AnalyticsAIResponse reject(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertCanGenerate(workspaceId, AIScopeType.DEPARTMENT, departmentId, projectId, null);

        AnalyticsReport report = analyticsReportRepository.findByIdAndWorkspace(reportId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Analytics report not found"));

        report.setApprovalStatus(AnalyticsReport.ApprovalStatus.REJECTED);
        report.setApprovedBy(userId);
        report.setApprovedAt(LocalDateTime.now());
        AnalyticsReport saved = analyticsReportRepository.save(report);
        return toResponse(saved, System.currentTimeMillis());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnalyticsAIResponse> getHistory(UUID workspaceId, int page, int size) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertActiveWorkspaceMember(workspaceId, userId);

        return aiScopeAuthorization.resolveReadableDepartmentFilter(workspaceId)
                .map(deptId -> analyticsReportRepository.findByWorkspaceAndDepartmentPaginated(
                        workspaceId, deptId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .orElseGet(() -> analyticsReportRepository.findByWorkspacePaginated(
                        workspaceId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .map(report -> toResponse(report, 0L));
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsAIResponse getById(UUID workspaceId, UUID reportId) {
        aiScopeAuthorization.assertActiveWorkspaceMember(workspaceId, aiScopeAuthorization.currentUserId());
        AnalyticsReport report = analyticsReportRepository.findByIdAndWorkspace(reportId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Analytics report not found"));

        UUID deptId = report.getDepartment() != null ? report.getDepartment().getId() : null;
        aiScopeAuthorization.assertCanReadDepartmentScopedContent(workspaceId, deptId);
        return toResponse(report, 0L);
    }

    private AnalyticsAIResponse saveReport(UUID workspaceId, UUID departmentId, UUID projectId,
                                            UUID userId, LocalDate startDate, LocalDate endDate,
                                            AIExecutionResponse aiResponse, long executionTime) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        Department department = departmentId != null
                ? departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"))
                : null;

        Project project = projectId != null
                ? projectRepository.findByIdAndDepartment_Id(projectId, departmentId).orElse(null)
                : null;

        Map<String, Object> analysis = aiResponse.getStructuredAnalysis();
        String finalReport = aiResponse.getResponse();

        AnalyticsReport report = new AnalyticsReport();
        report.setWorkspace(workspace);
        report.setDepartment(department);
        report.setProject(project);
        report.setReportDate(LocalDate.now());
        report.setTimeRangeStart(startDate);
        report.setTimeRangeEnd(endDate);

        // Populate structured fields from analysis JSON
        report.setExecutiveSummary(getStringOrDefault(analysis, "executiveSummary", "Executive summary not available."));
        report.setKpiHighlights(getStringOrDefault(analysis, "kpiHighlights", "KPI highlights not available."));
        report.setTrendsSummary(getStringOrDefault(analysis, "trendsSummary", "Trends summary not available."));
        report.setRiskAssessment(formatListOrDefault(analysis, "riskAssessment", "No risks identified."));
        report.setRecommendations(formatListOrDefault(analysis, "recommendations", "No recommendations available."));
        report.setDetailedReport(finalReport != null ? finalReport : "Report generation incomplete.");

        report.setGenerationStatus(AnalyticsReport.GenerationStatus.COMPLETED);
        report.setGenerationDate(LocalDateTime.now());
        report.setGenerationProcessedBy(userId);

        AnalyticsReport saved = analyticsReportRepository.save(report);
        log.info("AI AnalyticsReport generated [ID: {}, Workspace: {}, ExecutionTime: {}ms]",
                saved.getId(), workspaceId, executionTime);

        return toResponse(saved, executionTime);
    }

    private String getStringOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        return value.toString();
    }

    private String formatListOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining("\n• ", "• ", ""));
        }
        return value.toString();
    }

    private AnalyticsAIResponse toResponse(AnalyticsReport report, long executionTime) {
        return AnalyticsAIResponse.builder()
                .reportId(report.getId())
                .workspaceId(report.getWorkspace().getId())
                .departmentId(report.getDepartment() != null ? report.getDepartment().getId() : null)
                .projectId(report.getProject() != null ? report.getProject().getId() : null)
                .reportDate(report.getReportDate())
                .timeRangeStart(report.getTimeRangeStart())
                .timeRangeEnd(report.getTimeRangeEnd())
                .executiveSummary(report.getExecutiveSummary())
                .kpiHighlights(report.getKpiHighlights())
                .trendsSummary(report.getTrendsSummary())
                .riskAssessment(report.getRiskAssessment())
                .recommendations(report.getRecommendations())
                .detailedReport(report.getDetailedReport())
                .generationStatus(report.getGenerationStatus())
                .approvalStatus(report.getApprovalStatus())
                .generationDate(report.getGenerationDate())
                .generatedBy(report.getGenerationProcessedBy())
                .executionTime(executionTime)
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new BadRequestException("User is not authenticated.");
        }
        return user.getId();
    }
}
