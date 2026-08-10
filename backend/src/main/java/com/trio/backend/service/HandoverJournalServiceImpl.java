package com.trio.backend.service;

import com.trio.backend.dto.organisation.handover.HandoverJournalResponse;
import com.trio.backend.entity.HandoverEntry;
import com.trio.backend.entity.HandoverEntry.HandoverStatus;
import com.trio.backend.entity.HandoverJournal;
import com.trio.backend.entity.Project;
import com.trio.backend.enums.CommentStatus;
import com.trio.backend.enums.TaskStatus;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.HandoverJournalMapper;
import com.trio.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation for HandoverJournal generation, aggregating the workflow-based
 * HandoverEntry records by day per project.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class HandoverJournalServiceImpl implements HandoverJournalService {

    private final HandoverJournalRepository handoverJournalRepository;
    private final HandoverEntryRepository handoverEntryRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final ActivityRepository activityRepository;
    private final DocumentRepository documentRepository;
    private final HandoverJournalMapper handoverJournalMapper;
    private final HandoverSupport support;

    @Override
    @Transactional
    public HandoverJournalResponse generateJournal(UUID workspaceId, UUID departmentId, UUID projectId) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);
        if (!support.isWorkspaceAdminOrOwner(workspaceId, userId)) {
            throw new com.trio.backend.exception.ForbiddenException("You do not have permission for this operation.");
        }

        HandoverJournal log = generateJournalInternal(workspaceId, departmentId, projectId, userId);
        return handoverJournalMapper.toResponse(log);
    }

    public HandoverJournal generateJournalInternal(UUID workspaceId, UUID departmentId, UUID projectId, UUID userId) {
        Project project = validateAndGetProject(workspaceId, departmentId, projectId);

        LocalDate today = LocalDate.now();
        HandoverJournal journal = new HandoverJournal();
        journal.setWorkspace(project.getDepartment().getWorkspace());
        journal.setDepartment(project.getDepartment());
        journal.setProject(project);
        journal.setJournalDate(today.atStartOfDay());

        HandoverJournal saved = handoverJournalRepository.save(populateFromSubmitted(workspaceId, departmentId, project, journal, today, userId));
        log.info("HandoverJournal generated [ID: {}, Date: {}]", saved.getId(), today);
        return saved;
    }

    private HandoverJournal populateFromSubmitted(UUID workspaceId, UUID departmentId, Project project,
                                                  HandoverJournal journal, LocalDate date, UUID userId) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

        List<HandoverEntry> entries = handoverEntryRepository
                .findSubmittedByDepartmentIdAndEntryDate(workspaceId, departmentId, date, null);

        journal.setTotalHandovers((long) entries.size());
        journal.setPendingHandovers(countByStatus(entries, HandoverStatus.SUBMITTED));
        journal.setCompletedHandovers(countByStatus(entries, HandoverStatus.COMPLETED));
        journal.setRejectedHandovers(countByStatus(entries, HandoverStatus.REJECTED));
        journal.setUrgentHandovers(entries.stream().filter(e -> e.getPriority() == HandoverEntry.Priority.URGENT).count());
        journal.setOverdueHandovers(entries.stream().filter(HandoverJournalServiceImpl::isOverdue).count());

        journal.setEntriesCount((long) entries.size());
        journal.setDepartmentsIncluded(project.getDepartment().getName());
        journal.setGeneratedBy("Synthesizer");
        if (journal.getJournalVersion() == null) {
            journal.setJournalVersion(1);
        }

        journal.setGeneratedSummary(buildDeterministicSummary(project, dayStart, dayEnd, entries));
        journal.setMainDoneWork(extractFieldConsolidation(entries, "completed"));
        journal.setMainRemainingWork(extractFieldConsolidation(entries, "pending"));
        journal.setBlockers(extractFieldConsolidation(entries, "blockers"));
        journal.setDifficulties(extractFieldConsolidation(entries, "urgent"));
        journal.setRecommendations(extractFieldConsolidation(entries, "rejected"));

        journal.setGenerationStatus(HandoverJournal.GenerationStatus.GENERATED);
        journal.setGenerationDate(LocalDateTime.now());
        journal.setGenerationProcessedBy(userId);
        journal.setStatus(HandoverJournal.HandoverJournalStatus.ACTIVE);
        return journal;
    }

    @Override
    @Transactional(readOnly = true)
    public HandoverJournalResponse getById(UUID workspaceId, UUID departmentId, UUID projectId, UUID handoverJournalId) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        HandoverJournal journal = handoverJournalRepository.findByIdAndWorkspace(handoverJournalId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover log not found."));

        validateJournalHierarchy(journal, departmentId, projectId);
        support.assertCanViewDepartmentJournal(workspaceId, journal.getDepartment().getId());
        return handoverJournalMapper.toResponse(journal);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HandoverJournalResponse> list(UUID workspaceId, UUID departmentId, UUID projectId, Pageable pageable) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        UUID effectiveDepartmentId = support.resolveAccessibleDepartment(workspaceId, departmentId);
        validateAndGetProject(workspaceId, effectiveDepartmentId, projectId);

        return handoverJournalRepository.findByProjectIdPaginated(projectId, pageable)
                .map(handoverJournalMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HandoverJournalResponse> listAccessible(UUID workspaceId, UUID departmentId, UUID projectId,
                                                        HandoverEntry.Shift shift, LocalDate date, Pageable pageable) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        UUID effectiveDepartmentId = support.resolveAccessibleDepartment(workspaceId, departmentId);

        LocalDateTime from = date == null ? null : date.atStartOfDay();
        LocalDateTime to = date == null ? null : date.atStartOfDay().plusDays(1);

        return handoverJournalRepository.findAccessiblePaginated(
                        workspaceId, effectiveDepartmentId, projectId, shift, from, to, pageable)
                .map(handoverJournalMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public HandoverJournalResponse getByIdAccessible(UUID workspaceId, UUID handoverJournalId) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        HandoverJournal journal = handoverJournalRepository.findByIdAndWorkspace(handoverJournalId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover log not found."));

        support.assertCanViewDepartmentJournal(workspaceId, journal.getDepartment().getId());
        return handoverJournalMapper.toResponse(journal);
    }

    @Override
    @Transactional
    public HandoverJournalResponse regenerate(UUID workspaceId, UUID departmentId, UUID projectId, UUID handoverJournalId) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);
        if (!support.isWorkspaceAdminOrOwner(workspaceId, userId)) {
            throw new com.trio.backend.exception.ForbiddenException("You do not have permission for this operation.");
        }

        HandoverJournal journal = handoverJournalRepository.findByIdAndWorkspace(handoverJournalId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover log not found."));

        validateJournalHierarchy(journal, departmentId, projectId);

        LocalDate logDate = journal.getJournalDate().toLocalDate();
        journal.setJournalVersion(journal.getJournalVersion() == null ? 2 : journal.getJournalVersion() + 1);

        populateFromSubmitted(workspaceId, departmentId, journal.getProject(), journal, logDate, userId);

        HandoverJournal updated = handoverJournalRepository.save(journal);
        log.info("HandoverJournal regenerated [ID: {}]", updated.getId());
        return handoverJournalMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(UUID workspaceId, UUID departmentId, UUID projectId, UUID handoverJournalId) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);
        if (!support.isWorkspaceAdminOrOwner(workspaceId, userId)) {
            throw new com.trio.backend.exception.ForbiddenException("You do not have permission for this operation.");
        }

        HandoverJournal journal = handoverJournalRepository.findByIdAndWorkspace(handoverJournalId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover log not found."));

        if (journal.getStatus() == HandoverJournal.HandoverJournalStatus.DELETED) {
            return;
        }

        validateJournalHierarchy(journal, departmentId, projectId);

        journal.setStatus(HandoverJournal.HandoverJournalStatus.DELETED);
        handoverJournalRepository.save(journal);
        log.info("HandoverJournal marked as deleted (Soft Delete) [ID: {}]", handoverJournalId);
    }

    // ============================================================================
    // Aggregation helpers
    // ============================================================================

    private static boolean isOverdue(HandoverEntry e) {
        if (e.getDueDate() == null) {
            return false;
        }
        boolean open = e.getStatus() == HandoverStatus.PENDING || e.getStatus() == HandoverStatus.DRAFT;
        return open && e.getDueDate().isBefore(LocalDateTime.now());
    }

    private long countByStatus(List<HandoverEntry> entries, HandoverStatus status) {
        return entries.stream().filter(e -> e.getStatus() == status).count();
    }

    private String buildDeterministicSummary(Project project, LocalDateTime dayStart, LocalDateTime dayEnd,
                                             List<HandoverEntry> entries) {
        StringBuilder sb = new StringBuilder();

        if (entries.isEmpty()) {
            sb.append("No submitted handover entries for this department on this day.");
        } else {
            sb.append("[V1 Synthesizer] Collected ").append(entries.size())
                    .append(" submitted handover entry(ies) for the day.\n");
            entries.stream()
                    .map(e -> String.format("- [%s] %s",
                            e.getStatus(), entryLabel(e)))
                    .forEach(l -> sb.append(l).append("\n"));
        }

        sb.append("\n--- Project context ---\n");
        if (project != null) {
            sb.append(buildProjectContext(project, dayStart, dayEnd));
        }

        return sb.toString();
    }

    private String entryLabel(HandoverEntry e) {
        String title = e.getTitle() == null || e.getTitle().isBlank()
                ? (e.getShift() == null ? "Entry" : "Entry (" + e.getShift() + ")")
                : e.getTitle();
        String sender = support.userDisplayName(e.getSender());
        if (e.getReceiver() == null) {
            return title + " (by " + sender + ")";
        }
        return title + " (from " + sender + " to " + support.userDisplayName(e.getReceiver()) + ")";
    }

    private String buildProjectContext(Project project, LocalDateTime dayStart, LocalDateTime dayEnd) {
        Instant startInstant = dayStart.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = dayEnd.atZone(ZoneId.systemDefault()).toInstant();
        UUID projectId = project.getId();

        long activeTasks = taskRepository.countActiveByProjectId(projectId);
        long completedTasks = taskRepository.countByProjectIdAndStatusAndUpdatedAtBetween(
                projectId, TaskStatus.COMPLETED, startInstant, endInstant);
        long commentsToday = commentRepository.countByProjectIdAndStatusAndCreatedAtBetween(
                projectId, CommentStatus.ACTIVE, startInstant, endInstant);
        long activitiesToday = activityRepository.countByProjectIdAndStatusAndCreatedAtBetween(
                projectId, com.trio.backend.enums.ActivityStatus.ACTIVE, startInstant, endInstant);
        long documentsToday = documentRepository.countByProjectIdAndCreatedAtBetween(
                projectId, startInstant, endInstant);

        return String.format(
                "- Active tasks: %d\n- Tasks completed (today): %d\n- Comments (today): %d\n- Activities (today): %d\n- Documents (today): %d",
                activeTasks, completedTasks, commentsToday, activitiesToday, documentsToday
        );
    }

    private String extractFieldConsolidation(List<HandoverEntry> entries, String fieldType) {
        String consolidated = entries.stream()
                .filter(e -> switch (fieldType) {
                    case "completed" -> e.getStatus() == HandoverStatus.SUBMITTED;
                    case "pending" -> e.getStatus() == HandoverStatus.SUBMITTED
                            || e.getStatus() == HandoverStatus.PENDING;
                    case "rejected" -> e.getStatus() == HandoverStatus.REJECTED;
                    case "urgent" -> e.getPriority() == HandoverEntry.Priority.URGENT;
                    case "blockers" -> true;
                    default -> false;
                })
                .map(e -> switch (fieldType) {
                    case "blockers" -> formatReportField(e, e.getBlockers(), "No blockers");
                    case "completed" -> formatReportField(e, e.getCompletedTasks(), e.getContent());
                    case "pending" -> formatReportField(e, e.getPendingTasks(), e.getContent());
                    case "rejected" -> formatReportField(e, e.getContent(), e.getContent());
                    case "urgent" -> formatReportField(e, e.getCompletedTasks(), e.getContent());
                    default -> "";
                })
                .filter(content -> content != null && !content.isBlank())
                .collect(Collectors.joining("\n\n"));

        return consolidated.isEmpty() ? "Non renseigne" : consolidated;
    }

    private String formatReportField(HandoverEntry e, String field, String fallback) {
        String text = field == null || field.isBlank() ? fallback : field;
        if (text == null || text.isBlank()) {
            return null;
        }
        return "- " + entryLabel(e) + ": " + text.replace("\n", " ");
    }

    // ============================================================================
    // Validation & Multi-Tenant Isolation Helpers
    // ============================================================================

    private Project validateAndGetProject(UUID workspaceId, UUID departmentId, UUID projectId) {
        Project project = projectRepository.findByIdAndDepartment_Id(projectId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        if (project.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Project not found.");
        }

        if (!project.getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Project not found.");
        }
        return project;
    }

    private void validateJournalHierarchy(HandoverJournal journal, UUID departmentId, UUID projectId) {
        if (!journal.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Handover log not found.");
        }
        if (!journal.getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Handover log not found.");
        }
    }
}
