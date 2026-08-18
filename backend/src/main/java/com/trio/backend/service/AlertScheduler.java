package com.trio.backend.service;

import com.trio.backend.dto.alert.CreateAlertCommand;
import com.trio.backend.entity.Alert;
import com.trio.backend.entity.Task;
import com.trio.backend.entity.Workspace;
import com.trio.backend.repository.TaskRepository;
import com.trio.backend.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Generates alerts for task events that require attention: approaching
 * deadlines, overdue tasks, and blocked tasks.
 *
 * <p>Idempotency: each alert carries a {@code dedupKey} derived from the
 * recipient, alert type and task id, so re-running this scheduler never
 * creates duplicate alerts for the same event.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertScheduler {

    /**
     * Window (hours) before a due date that triggers a
     * TASK_DEADLINE_APPROACHING alert.
     */
    private static final long DEADLINE_APPROACHING_WINDOW_HOURS = 24;

    private final WorkspaceRepository workspaceRepository;
    private final TaskRepository taskRepository;
    private final AlertService alertService;

    @Scheduled(cron = "0 */30 * * * *")
    public void generateTaskAlerts() {
        Instant now = Instant.now();

        List<Workspace> activeWorkspaces = workspaceRepository.findAllActive();
        int generated = 0;

        for (Workspace workspace : activeWorkspaces) {
            UUID wsId = workspace.getId();
            generated += generateOverdueAlerts(wsId, now);
            generated += generateDeadlineApproachingAlerts(wsId, now);
            generated += generateBlockedAlerts(wsId);
        }

        if (generated > 0) {
            log.info("Generated {} task alerts across {} active workspaces", generated, activeWorkspaces.size());
        }
    }

    private int generateOverdueAlerts(UUID workspaceId, Instant now) {
        int created = 0;
        List<Task> overdue = taskRepository.findOverdueAssignedTasksByWorkspaceId(workspaceId, now);
        for (Task task : overdue) {
            UUID recipientId = task.getAssignee().getId();
            String dedupKey = dedupKey("TASK_OVERDUE", recipientId, task.getId());

            if (alertService.createInternal(CreateAlertCommand.builder()
                    .workspaceId(workspaceId)
                    .recipientId(recipientId)
                    .departmentId(departmentIdOf(task))
                    .type(Alert.AlertType.TASK_OVERDUE)
                    .severity(Alert.Severity.CRITICAL)
                    .title("Task overdue: " + task.getTitle())
                    .message("This task was due on " + task.getDueAt() + " and is still open. Please update its status.")
                    .resourceType("TASK")
                    .resourceId(task.getId())
                    .dedupKey(dedupKey)
                    .build()) != null) {
                created++;
            }
        }
        return created;
    }

    private int generateDeadlineApproachingAlerts(UUID workspaceId, Instant now) {
        Instant windowEnd = now.plus(DEADLINE_APPROACHING_WINDOW_HOURS, ChronoUnit.HOURS);
        int created = 0;
        List<Task> dueSoon = taskRepository.findDueSoonAssignedTasksByWorkspaceId(workspaceId, now, windowEnd);
        for (Task task : dueSoon) {
            UUID recipientId = task.getAssignee().getId();
            String dedupKey = dedupKey("TASK_DEADLINE_APPROACHING", recipientId, task.getId());

            if (alertService.createInternal(CreateAlertCommand.builder()
                    .workspaceId(workspaceId)
                    .recipientId(recipientId)
                    .departmentId(departmentIdOf(task))
                    .type(Alert.AlertType.TASK_DEADLINE_APPROACHING)
                    .severity(Alert.Severity.WARNING)
                    .title("Deadline approaching: " + task.getTitle())
                    .message("This task is due on " + task.getDueAt() + ". Make sure it is on track.")
                    .resourceType("TASK")
                    .resourceId(task.getId())
                    .dedupKey(dedupKey)
                    .build()) != null) {
                created++;
            }
        }
        return created;
    }

    private int generateBlockedAlerts(UUID workspaceId) {
        int created = 0;
        List<Task> blocked = taskRepository.findBlockedAssignedTasksByWorkspaceId(workspaceId);
        for (Task task : blocked) {
            UUID recipientId = task.getAssignee().getId();
            String dedupKey = dedupKey("TASK_BLOCKED", recipientId, task.getId());

            if (alertService.createInternal(CreateAlertCommand.builder()
                    .workspaceId(workspaceId)
                    .recipientId(recipientId)
                    .departmentId(departmentIdOf(task))
                    .type(Alert.AlertType.TASK_BLOCKED)
                    .severity(Alert.Severity.CRITICAL)
                    .title("Task blocked: " + task.getTitle())
                    .message("This task is blocked. Review it and resolve the blocker.")
                    .resourceType("TASK")
                    .resourceId(task.getId())
                    .dedupKey(dedupKey)
                    .build()) != null) {
                created++;
            }
        }
        return created;
    }

    private UUID departmentIdOf(Task task) {
        if (task.getProject() == null || task.getProject().getDepartment() == null) {
            return null;
        }
        return task.getProject().getDepartment().getId();
    }

    private String dedupKey(String type, UUID recipientId, UUID resourceId) {
        return type + ":" + recipientId + ":" + resourceId;
    }
}
