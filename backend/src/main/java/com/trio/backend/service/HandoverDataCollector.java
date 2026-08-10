package com.trio.backend.service;

import com.trio.backend.entity.*;
import com.trio.backend.entity.HandoverEntry.HandoverStatus;
import com.trio.backend.entity.HandoverEntry.Priority;
import com.trio.backend.enums.CommentStatus;
import com.trio.backend.enums.TaskStatus;
import com.trio.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collects workflow-based handover data for a project to feed the AI journal generation.
 */
@Service
@RequiredArgsConstructor
public class HandoverDataCollector {

    private final HandoverEntryRepository handoverEntryRepository;
    private final HandoverJournalRepository handoverJournalRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final ProjectRepository projectRepository;
    private final HandoverSupport support;

    public Map<String, Object> collect(UUID workspaceId, UUID departmentId, UUID projectId) {
        return collect(workspaceId, departmentId, projectId, LocalDate.now(), null);
    }

    public Map<String, Object> collect(UUID workspaceId, UUID departmentId, UUID projectId,
                                       LocalDate reportDate, HandoverEntry.Shift shift) {
        Project project = projectRepository.findByIdAndDepartment_Id(projectId, departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        LocalDateTime dayStart = reportDate.atStartOfDay();
        LocalDateTime dayEnd = reportDate.atTime(LocalTime.MAX);
        Instant startInstant = dayStart.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = dayEnd.atZone(ZoneId.systemDefault()).toInstant();

        List<HandoverEntry> currentEntries = handoverEntryRepository
                .findByProjectIdAndCreatedAtBetween(projectId, startInstant, endInstant);

        List<HandoverEntry> submittedEntries = handoverEntryRepository
                .findSubmittedByDepartmentIdAndEntryDate(workspaceId, departmentId, reportDate, shift);

        List<HandoverEntry> pending = filterStatus(currentEntries, HandoverStatus.PENDING);
        List<HandoverEntry> completed = filterStatus(currentEntries, HandoverStatus.COMPLETED);
        List<HandoverEntry> rejected = filterStatus(currentEntries, HandoverStatus.REJECTED);
        List<HandoverEntry> urgent = currentEntries.stream()
                .filter(e -> e.getPriority() == Priority.URGENT)
                .collect(Collectors.toList());
        List<HandoverEntry> overdue = currentEntries.stream()
                .filter(HandoverDataCollector::isOverdue)
                .collect(Collectors.toList());

        List<Task> pendingTasks = taskRepository.findAllActiveByProjectId(projectId);
        List<Task> completedTasks = taskRepository.findAllByProject_IdAndStatus(projectId, TaskStatus.COMPLETED);

        List<Comment> recentComments = commentRepository.findAllByProjectIdAndStatus(projectId, CommentStatus.ACTIVE)
                .stream()
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(startInstant))
                .toList();

        LocalDate previousDay = reportDate.minusDays(1);
        Optional<HandoverJournal> previousJournal = handoverJournalRepository
                .findByProjectIdAndJournalDateBetween(
                        projectId, previousDay.atStartOfDay(), previousDay.atStartOfDay().plusDays(1))
                .stream().findFirst();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("projectName", project.getName());
        data.put("projectDescription", project.getDescription());
        data.put("workspaceId", workspaceId);
        data.put("departmentId", departmentId);
        data.put("projectId", projectId);
        data.put("reportDate", reportDate.toString());
        data.put("shift", shift == null ? null : shift.name());

        data.put("totalHandovers", currentEntries.size());
        data.put("pendingHandovers", pending.size());
        data.put("completedHandovers", completed.size());
        data.put("rejectedHandovers", rejected.size());
        data.put("urgentHandovers", urgent.size());
        data.put("overdueHandovers", overdue.size());

        data.put("handoverSummary", formatEntries(currentEntries));
        data.put("pendingHandoverDetails", formatEntries(pending));
        data.put("completedHandoverDetails", formatEntries(completed));
        data.put("rejectedHandoverDetails", formatEntries(rejected));
        data.put("urgentHandoverDetails", formatEntries(urgent));
        data.put("overdueHandoverDetails", formatEntries(overdue));

        data.put("submittedHandoverEntries", submittedEntries.size());
        data.put("submittedHandoverEntryDetails", formatSubmittedEntries(submittedEntries));
        data.put("submittedHandoverEntryCount", submittedEntries.size());

        data.put("pendingTasks", formatTasks(pendingTasks));
        data.put("completedTasks", formatTasks(completedTasks));
        data.put("pendingTaskCount", pendingTasks.size());
        data.put("completedTaskCount", completedTasks.size());

        data.put("recentComments", formatComments(recentComments));
        data.put("recentCommentCount", recentComments.size());

        previousJournal.ifPresent(journal -> data.put("previousJournal", formatPreviousJournal(journal)));

        return data;
    }

    private static boolean isOverdue(HandoverEntry e) {
        if (e.getDueDate() == null) {
            return false;
        }
        boolean open = e.getStatus() == HandoverStatus.PENDING || e.getStatus() == HandoverStatus.DRAFT;
        return open && e.getDueDate().isBefore(LocalDateTime.now());
    }

    private List<HandoverEntry> filterStatus(List<HandoverEntry> entries, HandoverStatus status) {
        return entries.stream().filter(e -> e.getStatus() == status).collect(Collectors.toList());
    }

    private String formatEntries(List<HandoverEntry> entries) {
        return entries.stream()
                .map(e -> String.format("- [%s] %s (priority %s, due %s) from %s to %s: %s",
                        e.getStatus(), e.getTitle(), e.getPriority(),
                        e.getDueDate() != null ? e.getDueDate().toLocalDate() : "n/a",
                        support.userDisplayName(e.getSender()),
                        e.getReceiver() == null ? "n/a" : support.userDisplayName(e.getReceiver()),
                        e.getContent() == null ? "" : e.getContent().replace("\n", " ")))
                .collect(Collectors.joining("\n"));
    }

    private String formatSubmittedEntries(List<HandoverEntry> entries) {
        if (entries.isEmpty()) {
            return "No submitted daily report entries for this department on this day.";
        }
        return entries.stream()
                .map(e -> String.format(
                        "- [%s] %s (by %s)%s\n  Tasks completed: %s\n  Current progress: %s\n  Pending tasks: %s\n  Blockers: %s\n  Notes: %s\n  Estimate remaining: %s\n  Mood: %s",
                        e.getStatus(),
                        e.getShift() == null ? "Daily report" : "Daily report (" + e.getShift() + ")",
                        support.userDisplayName(e.getSender()),
                        e.getPriority() == null ? "" : " | priority " + e.getPriority(),
                        e.getCompletedTasks() == null ? "" : e.getCompletedTasks().replace("\n", " "),
                        e.getCurrentProgress() == null ? "" : e.getCurrentProgress().replace("\n", " "),
                        e.getPendingTasks() == null ? "" : e.getPendingTasks().replace("\n", " "),
                        e.getBlockers() == null ? "" : e.getBlockers().replace("\n", " "),
                        e.getImportantNotes() == null ? "" : e.getImportantNotes().replace("\n", " "),
                        e.getEstimatedRemainingWork() == null ? "" : e.getEstimatedRemainingWork(),
                        e.getMood() == null ? "" : e.getMood()))
                .collect(Collectors.joining("\n"));
    }

    private String formatTasks(List<Task> tasks) {
        return tasks.stream()
                .map(t -> String.format("- %s (status %s, due %s)",
                        t.getTitle(), t.getStatus(),
                        t.getDueAt() != null ? t.getDueAt().atZone(ZoneId.systemDefault()).toLocalDate() : "n/a"))
                .collect(Collectors.joining("\n"));
    }

    private String formatComments(List<Comment> comments) {
        return comments.stream()
                .map(c -> String.format("- %s (author %s)", c.getContent(), c.getCreatedBy()))
                .collect(Collectors.joining("\n"));
    }

    private String formatPreviousJournal(HandoverJournal journal) {
        return String.format("Summary: %s | Generated: %s",
                journal.getGeneratedSummary() != null ? journal.getGeneratedSummary() : "",
                journal.getGenerationDate() != null ? journal.getGenerationDate().toString() : "");
    }
}
