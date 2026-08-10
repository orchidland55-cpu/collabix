package com.trio.backend.service.impl;

import com.trio.backend.ai.dto.request.AIExecutionRequest;
import com.trio.backend.ai.dto.response.AIExecutionResponse;
import com.trio.backend.ai.enums.AITask;
import com.trio.backend.ai.service.AIOrchestratorService;
import com.trio.backend.dto.ai.HandoverAIEditRequest;
import com.trio.backend.dto.ai.HandoverAIResponse;
import com.trio.backend.entity.HandoverJournal;
import com.trio.backend.entity.HandoverEntry;
import com.trio.backend.entity.Project;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.HandoverJournalMapper;
import com.trio.backend.repository.HandoverJournalRepository;
import com.trio.backend.repository.ProjectRepository;
import com.trio.backend.service.HandoverAIService;
import com.trio.backend.service.HandoverDataCollector;
import com.trio.backend.service.HandoverSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class HandoverAIServiceImpl implements HandoverAIService {

    private final HandoverDataCollector handoverDataCollector;
    private final AIOrchestratorService orchestratorService;
    private final HandoverJournalRepository handoverJournalRepository;
    private final ProjectRepository projectRepository;
    private final HandoverJournalMapper handoverJournalMapper;
    private final HandoverSupport support;

    @Override
    public HandoverAIResponse generate(UUID workspaceId, UUID departmentId, UUID projectId) {
        return generate(workspaceId, departmentId, projectId, LocalDate.now(), null);
    }

    @Override
    public HandoverAIResponse generate(UUID workspaceId, UUID departmentId, UUID projectId,
                                       LocalDate date, HandoverEntry.Shift shift) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);
        if (!support.isWorkspaceAdminOrOwner(workspaceId, userId)) {
            throw new com.trio.backend.exception.ForbiddenException("You do not have permission for this operation.");
        }

        Map<String, Object> collectedData = handoverDataCollector.collect(workspaceId, departmentId, projectId, date, shift);

        Integer totalHandovers = (Integer) collectedData.get("totalHandovers");
        Integer submittedCount = (Integer) collectedData.get("submittedHandoverEntryCount");
        if ((totalHandovers == null || totalHandovers == 0)
                && (submittedCount == null || submittedCount == 0)) {
            throw new BadRequestException("No handovers found for this project. Cannot generate AI journal.");
        }

        AIExecutionRequest executionRequest = new AIExecutionRequest();
        executionRequest.setTask(AITask.HANDOVER_EXECUTIVE_REPORT);
        executionRequest.setInput("Generate handover journal for project: " + collectedData.get("projectName"));
        executionRequest.setWorkspaceId(workspaceId);
        executionRequest.setDepartmentId(departmentId);
        executionRequest.setProjectId(projectId);
        executionRequest.setUserId(userId);
        executionRequest.setContext(collectedData);

        long start = System.currentTimeMillis();
        AIExecutionResponse aiResponse = orchestratorService.execute(executionRequest);
        long executionTime = System.currentTimeMillis() - start;

        return saveJournal(workspaceId, departmentId, projectId, userId, collectedData, aiResponse, executionTime, date, shift);
    }

    @Override
    public HandoverAIResponse regenerate(UUID workspaceId, UUID departmentId, UUID projectId, UUID journalId) {
        return regenerate(workspaceId, departmentId, projectId, journalId, LocalDate.now(), null);
    }

    @Override
    public HandoverAIResponse regenerate(UUID workspaceId, UUID departmentId, UUID projectId, UUID journalId,
                                         LocalDate date, HandoverEntry.Shift shift) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);
        if (!support.isWorkspaceAdminOrOwner(workspaceId, userId)) {
            throw new com.trio.backend.exception.ForbiddenException("You do not have permission for this operation.");
        }

        handoverJournalRepository.findByIdAndWorkspace(journalId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover journal not found"));

        return generate(workspaceId, departmentId, projectId, date, shift);
    }

    @Override
    public HandoverAIResponse edit(UUID workspaceId, UUID departmentId, UUID projectId, UUID journalId,
                                    HandoverAIEditRequest request) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);
        if (!support.isWorkspaceAdminOrOwner(workspaceId, userId)) {
            throw new com.trio.backend.exception.ForbiddenException("You do not have permission for this operation.");
        }

        HandoverJournal journal = handoverJournalRepository.findByIdAndWorkspace(journalId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover journal not found"));

        if (request.getExecutiveSummary() != null) journal.setGeneratedSummary(request.getExecutiveSummary());
        if (request.getCompletedWork() != null) journal.setMainDoneWork(request.getCompletedWork());
        if (request.getPendingWork() != null) journal.setMainRemainingWork(request.getPendingWork());
        if (request.getBlockedTasks() != null) journal.setBlockers(request.getBlockedTasks());
        if (request.getCriticalRisks() != null) journal.setDifficulties(request.getCriticalRisks());
        if (request.getRecommendations() != null) journal.setRecommendations(request.getRecommendations());

        HandoverJournal saved = handoverJournalRepository.save(journal);
        return toResponse(saved, System.currentTimeMillis());
    }

    @Override
    public HandoverAIResponse approve(UUID workspaceId, UUID departmentId, UUID projectId, UUID journalId) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);
        if (!support.isWorkspaceAdminOrOwner(workspaceId, userId)) {
            throw new com.trio.backend.exception.ForbiddenException("You do not have permission for this operation.");
        }

        HandoverJournal journal = handoverJournalRepository.findByIdAndWorkspace(journalId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover journal not found"));

        journal.setGenerationStatus(HandoverJournal.GenerationStatus.GENERATED);
        HandoverJournal saved = handoverJournalRepository.save(journal);
        return toResponse(saved, System.currentTimeMillis());
    }

    @Override
    public HandoverAIResponse reject(UUID workspaceId, UUID departmentId, UUID projectId, UUID journalId) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);
        if (!support.isWorkspaceAdminOrOwner(workspaceId, userId)) {
            throw new com.trio.backend.exception.ForbiddenException("You do not have permission for this operation.");
        }

        HandoverJournal journal = handoverJournalRepository.findByIdAndWorkspace(journalId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover journal not found"));

        journal.setGenerationStatus(HandoverJournal.GenerationStatus.FAILED);
        HandoverJournal saved = handoverJournalRepository.save(journal);
        return toResponse(saved, System.currentTimeMillis());
    }

    private HandoverAIResponse saveJournal(UUID workspaceId, UUID departmentId, UUID projectId,
                                            UUID userId, Map<String, Object> collectedData,
                                            AIExecutionResponse aiResponse, long executionTime,
                                            LocalDate date, HandoverEntry.Shift shift) {
        Project project = projectRepository.findByIdAndDepartment_Id(projectId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        HandoverJournal journal = handoverJournalRepository
                .findActiveByProjectIdAndJournalDate(
                        projectId, date.atStartOfDay(), date.atStartOfDay().plusDays(1))
                .orElseGet(() -> {
                    HandoverJournal j = new HandoverJournal();
                    j.setWorkspace(project.getDepartment().getWorkspace());
                    j.setDepartment(project.getDepartment());
                    j.setProject(project);
                    j.setJournalDate(date.atStartOfDay());
                    j.setJournalVersion(1);
                    return j;
                });

        if (journal.getJournalVersion() == null) {
            journal.setJournalVersion(1);
        } else if (journal.getId() != null) {
            journal.setJournalVersion(journal.getJournalVersion() + 1);
        }

        journal.setShift(shift);
        journal.setGeneratedBy("Gemini");
        journal.setDepartmentsIncluded(project.getDepartment().getName());
        journal.setEntriesCount(longValue(collectedData, "submittedHandoverEntryCount"));
        journal.setGeneratedSummary(aiResponse.getResponse());
        journal.setMainDoneWork(aiResponse.getResponse());
        journal.setMainRemainingWork(aiResponse.getResponse());
        journal.setBlockers(aiResponse.getResponse());
        journal.setDifficulties(aiResponse.getResponse());
        journal.setRecommendations(aiResponse.getResponse());

        journal.setTotalHandovers(longValue(collectedData, "totalHandovers"));
        journal.setPendingHandovers(longValue(collectedData, "pendingHandovers"));
        journal.setCompletedHandovers(longValue(collectedData, "completedHandovers"));
        journal.setRejectedHandovers(longValue(collectedData, "rejectedHandovers"));
        journal.setUrgentHandovers(longValue(collectedData, "urgentHandovers"));
        journal.setOverdueHandovers(longValue(collectedData, "overdueHandovers"));

        journal.setGenerationStatus(HandoverJournal.GenerationStatus.GENERATED);
        journal.setGenerationDate(LocalDateTime.now());
        journal.setGenerationProcessedBy(userId);

        HandoverJournal saved = handoverJournalRepository.save(journal);
        log.info("AI HandoverJournal generated [ID: {}, Project: {}, ExecutionTime: {}ms]",
                saved.getId(), projectId, executionTime);

        return toResponse(saved, executionTime);
    }

    private long longValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private HandoverAIResponse toResponse(HandoverJournal journal, long executionTime) {
        return HandoverAIResponse.builder()
                .journalId(journal.getId())
                .workspaceId(journal.getWorkspace().getId())
                .departmentId(journal.getDepartment().getId())
                .projectId(journal.getProject().getId())
                .journalDate(journal.getJournalDate())
                .executiveSummary(journal.getGeneratedSummary())
                .completedWork(journal.getMainDoneWork())
                .pendingWork(journal.getMainRemainingWork())
                .criticalRisks(journal.getDifficulties())
                .blockedTasks(journal.getBlockers())
                .recommendations(journal.getRecommendations())
                .generationStatus(journal.getGenerationStatus())
                .generationDate(journal.getGenerationDate())
                .generatedBy(journal.getGenerationProcessedBy())
                .executionTime(executionTime)
                .createdAt(journal.getCreatedAt())
                .updatedAt(journal.getUpdatedAt())
                .build();
    }
}
