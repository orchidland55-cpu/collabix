package com.trio.backend.entity;

import com.trio.backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * HandoverEntry represents a handover handed over by a sender to a receiver.
 *
 * <p>Architecture notes:</p>
 * <ul>
 *     <li>HandoverEntry belongs to exactly one Workspace and exactly one Department and exactly one Project.</li>
 *     <li>Task is optional and can be associated for task-level handover context.</li>
 *     <li>An entry can be a <b>daily report</b> (filled by the {@code sender} with tasks completed, progress,
 *     pending tasks, blockers etc.) or a classic sender-&gt;receiver handover.</li>
 *     <li>Lifecycle: DRAFT -&gt; PENDING (sent) -&gt; ACCEPTED | REJECTED -&gt; COMPLETED; ARCHIVED for soft archiving.
 *     Daily reports additionally use SUBMITTED (ready for journal generation).</li>
 *     <li>Soft-delete is handled via the {@code deleted} flag.</li>
 * </ul>
 */
@Entity
@Table(
        name = "handover_entries",
        indexes = {
                @Index(name = "idx_handover_entries_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_handover_entries_department_id", columnList = "department_id"),
                @Index(name = "idx_handover_entries_project_id", columnList = "project_id"),
                @Index(name = "idx_handover_entries_sender_id", columnList = "sender_id"),
                @Index(name = "idx_handover_entries_receiver_id", columnList = "receiver_id"),
                @Index(name = "idx_handover_entries_task_id", columnList = "task_id"),
                @Index(name = "idx_handover_entries_status", columnList = "status"),
                @Index(name = "idx_handover_entries_priority", columnList = "priority"),
                @Index(name = "idx_handover_entries_due_date", columnList = "due_date"),
                @Index(name = "idx_handover_entries_sent_at", columnList = "sent_at"),
                @Index(name = "idx_handover_entries_shift", columnList = "shift"),
                @Index(name = "idx_handover_entries_entry_date", columnList = "entry_date"),
                @Index(name = "idx_handover_entries_deleted", columnList = "deleted"),
                @Index(name = "idx_handover_entries_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@BatchSize(size = 20)
public class HandoverEntry extends AuditableEntity {

    /**
     * Workspace owning this handover. Required.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, updatable = false)
    private Workspace workspace;

    /**
     * Department owning this handover. Required.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false, updatable = false)
    private Department department;

    /**
     * Project context for this handover. Required.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    /**
     * Optional task context for this handover.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "task_id", nullable = true, updatable = false)
    private Task task;

    /**
     * Sender (author of the handover). Required.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false, updatable = false)
    private User sender;

    /**
     * Receiver (person the handover is handed to).
     * Optional: for daily-report entries there is no explicit receiver.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "receiver_id", nullable = true)
    private User receiver;

    // =========================================================================
    // Workflow fields
    // =========================================================================

    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private HandoverStatus status = HandoverStatus.DRAFT;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    // =========================================================================
    // Daily report fields
    // =========================================================================

    /**
     * Shift the report covers (MORNING / EVENING). Optional for classic handovers.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "shift", length = 20)
    private Shift shift;

    /**
     * The date this daily report covers (defaults to today for new entries).
     */
    @Column(name = "entry_date")
    private LocalDate entryDate;

    @Column(name = "completed_tasks", columnDefinition = "TEXT")
    private String completedTasks;

    @Column(name = "current_progress", columnDefinition = "TEXT")
    private String currentProgress;

    @Column(name = "pending_tasks", columnDefinition = "TEXT")
    private String pendingTasks;

    @Column(name = "blockers", columnDefinition = "TEXT")
    private String blockers;

    @Column(name = "important_notes", columnDefinition = "TEXT")
    private String importantNotes;

    @Column(name = "estimated_remaining_work", length = 255)
    private String estimatedRemainingWork;

    @Column(name = "mood", length = 50)
    private String mood;

    // =========================================================================
    // Lifecycle timestamps
    // =========================================================================

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /**
     * Soft delete flag.
     */
    @NotNull
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @PrePersist
    @PreUpdate
    private void validateHierarchy() {
        if (priority == null) {
            priority = Priority.MEDIUM;
        }
        if (status == null) {
            status = HandoverStatus.DRAFT;
        }
        if (deleted == null) {
            deleted = false;
        }
        if (entryDate == null && shift != null) {
            entryDate = LocalDate.now();
        }

        Objects.requireNonNull(workspace, "workspace must not be null");
        Objects.requireNonNull(department, "department must not be null");
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(sender, "sender must not be null");
        if (!Objects.equals(department.getId(), project.getDepartment().getId())) {
            throw new IllegalStateException("HandoverEntry.department must match project.department");
        }
        if (!Objects.equals(workspace.getId(), project.getDepartment().getWorkspace().getId())) {
            throw new IllegalStateException("HandoverEntry.workspace must match project.department.workspace");
        }
        if (receiver != null && Objects.equals(sender.getId(), receiver.getId())) {
            throw new IllegalStateException("HandoverEntry.sender and receiver must be different users");
        }
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    public enum Shift {
        MORNING,
        EVENING
    }

    public enum HandoverStatus {
        DRAFT,
        SUBMITTED,
        PENDING,
        ACCEPTED,
        REJECTED,
        COMPLETED,
        ARCHIVED
    }
}
