package com.trio.backend.service.impl;

import com.trio.backend.ai.dto.request.AIExecutionRequest;
import com.trio.backend.ai.dto.response.AIExecutionResponse;
import com.trio.backend.ai.enums.AITask;
import com.trio.backend.ai.service.AIOrchestratorService;
import com.trio.backend.dto.ai.ReportingEditRequest;
import com.trio.backend.dto.ai.ReportingGenerateRequest;
import com.trio.backend.dto.ai.ReportingResponse;
import com.trio.backend.entity.*;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.repository.*;
import com.trio.backend.enums.AIScopeType;
import com.trio.backend.security.ai.AIScopeAuthorization;
import com.trio.backend.util.AIScopeUtils;
import com.trio.backend.security.user.CustomUserDetails;
import com.trio.backend.service.ReportingAIService;
import com.trio.backend.service.ReportingDataCollector;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReportingAIServiceImpl implements ReportingAIService {

    private final ReportingDataCollector reportingDataCollector;
    private final AIOrchestratorService orchestratorService;
    private final ExecutiveReportRepository executiveReportRepository;
    private final WorkspaceRepository workspaceRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AIScopeAuthorization aiScopeAuthorization;
    private final AlertGenerationHelper alertGenerationHelper;

    @Override
    public ReportingResponse generate(ReportingGenerateRequest request) {
        UUID userId = getAuthenticatedUserId();
        UUID workspaceId = request.getWorkspaceId();
        UUID departmentId = request.getDepartmentId();
        UUID projectId = request.getProjectId();
        UUID teamId = request.getTeamId();
        AIScopeType scope = AIScopeUtils.resolveScope(request.getScope(), projectId, teamId);

        aiScopeAuthorization.assertCanGenerate(workspaceId, scope, departmentId, projectId, teamId);
        departmentId = resolvePersistedDepartmentId(workspaceId, scope, departmentId);
        request.setDepartmentId(departmentId);

        LocalDate periodStart = request.getPeriodStart();
        LocalDate periodEnd = request.getPeriodEnd();

        if (periodStart != null && periodEnd != null && periodStart.isAfter(periodEnd)) {
            throw new BadRequestException("Period start must be before period end.");
        }

        Map<String, Object> collectedData = reportingDataCollector.collect(
                workspaceId, departmentId, projectId, teamId, scope, periodStart, periodEnd);

        AIExecutionRequest executionRequest = new AIExecutionRequest();
        executionRequest.setTask(AITask.REPORT_GENERATION);
        executionRequest.setInput("Generate executive report: " + request.getTitle());
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
                    workspaceId, userId, departmentId, "EXECUTIVE_REPORT", null,
                    "AI report generation failed",
                    "The AI could not generate the executive report. Please try again.");
            throw ex;
        }
        long executionTime = System.currentTimeMillis() - start;

        return saveReport(request, userId, aiResponse, executionTime);
    }

    @Override
    public ReportingResponse regenerate(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertActiveWorkspaceMember(workspaceId, userId);

        ExecutiveReport existing = executiveReportRepository.findByIdAndWorkspace(reportId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Executive report not found"));
        assertCanAccessReport(existing);

        ReportingGenerateRequest request = new ReportingGenerateRequest();
        request.setWorkspaceId(workspaceId);
        request.setDepartmentId(existing.getDepartment() != null ? existing.getDepartment().getId() : departmentId);
        request.setProjectId(existing.getProject() != null ? existing.getProject().getId() : projectId);
        request.setTitle(existing.getTitle());
        request.setReportType(existing.getReportType());
        request.setPeriodStart(existing.getPeriodStart());
        request.setPeriodEnd(existing.getPeriodEnd());

        ReportingResponse response = generate(request);

        if (response.getReportId() != null) {
            ExecutiveReport newReport = executiveReportRepository.findByIdAndWorkspace(
                    response.getReportId(), workspaceId).orElse(null);
            if (newReport != null) {
                newReport.setReportVersion(existing.getReportVersion() + 1);
                executiveReportRepository.save(newReport);
                response = toResponse(newReport, response.getExecutionTime());
            }
        }

        return response;
    }

    @Override
    public ReportingResponse edit(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId,
                                   ReportingEditRequest request) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertActiveWorkspaceMember(workspaceId, userId);

        ExecutiveReport report = executiveReportRepository.findByIdAndWorkspace(reportId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Executive report not found"));
        assertCanMutateReport(report);

        if (request.getTitle() != null) report.setTitle(request.getTitle());
        if (request.getExecutiveSummary() != null) report.setExecutiveSummary(request.getExecutiveSummary());
        if (request.getMajorHighlights() != null) report.setMajorHighlights(request.getMajorHighlights());
        if (request.getBusinessHealth() != null) report.setBusinessHealth(request.getBusinessHealth());
        if (request.getProductivityReview() != null) report.setProductivityReview(request.getProductivityReview());
        if (request.getCriticalRisks() != null) report.setCriticalRisks(request.getCriticalRisks());
        if (request.getAchievements() != null) report.setAchievements(request.getAchievements());
        if (request.getChallenges() != null) report.setChallenges(request.getChallenges());
        if (request.getRecommendations() != null) report.setRecommendations(request.getRecommendations());
        if (request.getStrategicPriorities() != null) report.setStrategicPriorities(request.getStrategicPriorities());
        if (request.getNextActions() != null) report.setNextActions(request.getNextActions());
        if (request.getFinalReport() != null) report.setFinalReport(request.getFinalReport());

        ExecutiveReport saved = executiveReportRepository.save(report);
        return toResponse(saved, System.currentTimeMillis());
    }

    @Override
    public ReportingResponse approve(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertActiveWorkspaceMember(workspaceId, userId);

        ExecutiveReport report = executiveReportRepository.findByIdAndWorkspace(reportId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Executive report not found"));
        assertCanMutateReport(report);

        report.setApprovalStatus(ExecutiveReport.ApprovalStatus.APPROVED);
        report.setApprovedBy(userId);
        report.setApprovedAt(LocalDateTime.now());
        ExecutiveReport saved = executiveReportRepository.save(report);
        return toResponse(saved, System.currentTimeMillis());
    }

    @Override
    public ReportingResponse reject(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertActiveWorkspaceMember(workspaceId, userId);

        ExecutiveReport report = executiveReportRepository.findByIdAndWorkspace(reportId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Executive report not found"));
        assertCanMutateReport(report);

        report.setApprovalStatus(ExecutiveReport.ApprovalStatus.REJECTED);
        report.setApprovedBy(userId);
        report.setApprovedAt(LocalDateTime.now());
        ExecutiveReport saved = executiveReportRepository.save(report);
        return toResponse(saved, System.currentTimeMillis());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportingResponse> getHistory(UUID workspaceId, int page, int size) {
        UUID userId = getAuthenticatedUserId();
        aiScopeAuthorization.assertActiveWorkspaceMember(workspaceId, userId);

        return aiScopeAuthorization.resolveReadableDepartmentFilter(workspaceId)
                .map(deptId -> executiveReportRepository.findByWorkspaceAndDepartmentPaginated(
                        workspaceId, deptId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .orElseGet(() -> executiveReportRepository.findByWorkspacePaginated(
                        workspaceId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .map(report -> toResponse(report, null));
    }

    @Override
    @Transactional(readOnly = true)
    public ReportingResponse getById(UUID workspaceId, UUID reportId) {
        aiScopeAuthorization.assertActiveWorkspaceMember(workspaceId, aiScopeAuthorization.currentUserId());
        ExecutiveReport report = executiveReportRepository.findByIdAndWorkspace(reportId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Executive report not found"));
        assertCanAccessReport(report);
        return toResponse(report, null);
    }

    private void assertCanAccessReport(ExecutiveReport report) {
        UUID workspaceId = report.getWorkspace().getId();
        UUID deptId = report.getDepartment() != null ? report.getDepartment().getId() : null;
        aiScopeAuthorization.assertCanReadDepartmentScopedContent(workspaceId, deptId);
    }

    private void assertCanMutateReport(ExecutiveReport report) {
        UUID workspaceId = report.getWorkspace().getId();
        UUID deptId = report.getDepartment() != null ? report.getDepartment().getId() : null;
        UUID projectId = report.getProject() != null ? report.getProject().getId() : null;
        AIScopeType scope = projectId != null
                ? AIScopeType.PROJECT
                : (deptId != null ? AIScopeType.DEPARTMENT : AIScopeType.WORKSPACE);
        aiScopeAuthorization.assertCanGenerate(workspaceId, scope, deptId, projectId, null);
    }

    private UUID resolvePersistedDepartmentId(UUID workspaceId, AIScopeType scope, UUID departmentId) {
        if (scope == AIScopeType.WORKSPACE) {
            return null;
        }
        if (departmentId != null) {
            return departmentId;
        }
        throw new BadRequestException("departmentId is required for this scope.");
    }

    private ReportingResponse saveReport(ReportingGenerateRequest request, UUID userId,
                                          AIExecutionResponse aiResponse, long executionTime) {
        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        Department department = request.getDepartmentId() != null
                ? departmentRepository.findById(request.getDepartmentId()).orElse(null)
                : null;
        Project project = request.getProjectId() != null && request.getDepartmentId() != null
                ? projectRepository.findByIdAndDepartment_Id(request.getProjectId(), request.getDepartmentId()).orElse(null)
                : (request.getProjectId() != null
                    ? projectRepository.findById(request.getProjectId()).orElse(null)
                    : null);

        ExecutiveReport report = new ExecutiveReport();
        report.setWorkspace(workspace);
        report.setDepartment(department);
        report.setProject(project);
        report.setTitle(request.getTitle());
        report.setReportType(request.getReportType());
        report.setPeriodStart(request.getPeriodStart());
        report.setPeriodEnd(request.getPeriodEnd());
        report.setReportVersion(1);
        report.setExecutiveSummary(aiResponse.getResponse());
        report.setMajorHighlights(aiResponse.getResponse());
        report.setBusinessHealth(aiResponse.getResponse());
        report.setProductivityReview(aiResponse.getResponse());
        report.setCriticalRisks(aiResponse.getResponse());
        report.setAchievements(aiResponse.getResponse());
        report.setChallenges(aiResponse.getResponse());
        report.setRecommendations(aiResponse.getResponse());
        report.setStrategicPriorities(aiResponse.getResponse());
        report.setNextActions(aiResponse.getResponse());
        report.setFinalReport(aiResponse.getResponse());
        report.setGenerationStatus(ExecutiveReport.GenerationStatus.COMPLETED);
        report.setGenerationDate(LocalDateTime.now());
        report.setGenerationProcessedBy(userId);

        ExecutiveReport saved = executiveReportRepository.save(report);
        log.info("AI ExecutiveReport generated [ID: {}, Title: {}, Workspace: {}, ExecutionTime: {}ms]",
                saved.getId(), request.getTitle(), request.getWorkspaceId(), executionTime);

        return toResponse(saved, executionTime);
    }

    private ReportingResponse toResponse(ExecutiveReport report, Long executionTime) {
        return ReportingResponse.builder()
                .reportId(report.getId())
                .workspaceId(report.getWorkspace().getId())
                .departmentId(report.getDepartment() != null ? report.getDepartment().getId() : null)
                .projectId(report.getProject() != null ? report.getProject().getId() : null)
                .title(report.getTitle())
                .reportType(report.getReportType())
                .periodStart(report.getPeriodStart())
                .periodEnd(report.getPeriodEnd())
                .reportVersion(report.getReportVersion())
                .executiveSummary(report.getExecutiveSummary())
                .majorHighlights(report.getMajorHighlights())
                .businessHealth(report.getBusinessHealth())
                .productivityReview(report.getProductivityReview())
                .criticalRisks(report.getCriticalRisks())
                .achievements(report.getAchievements())
                .challenges(report.getChallenges())
                .recommendations(report.getRecommendations())
                .strategicPriorities(report.getStrategicPriorities())
                .nextActions(report.getNextActions())
                .finalReport(report.getFinalReport())
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
