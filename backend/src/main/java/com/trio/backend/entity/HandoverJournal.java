package com.trio.backend.entity;

import com.trio.backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * HandoverJournal represents the automatically generated result built from multiple
 * {@link HandoverEntry} records aggregated by day for a project.
 *
 * <p>Architecture notes:</p>
 * <ul>
 *     <li>HandoverJournal belongs to exactly one Workspace, one Department and one Project.</li>
 *     <li>One journal per project per day (enforced by a unique constraint).</li>
 *     <li>Soft-delete is handled via status.</li>
 * </ul>
 */
@Entity
@Table(
        name = "handover_journals",
        indexes = {
                @Index(name = "idx_handover_journals_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_handover_journals_department_id", columnList = "department_id"),
                @Index(name = "idx_handover_journals_project_id", columnList = "project_id"),
                @Index(name = "idx_handover_journals_date", columnList = "journal_date"),
                @Index(name = "idx_handover_journals_status", columnList = "status"),
                @Index(name = "idx_handover_journals_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_handover_journals_project_date",
                        columnNames = {"project_id", "journal_date"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverJournal extends AuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, updatable = false)
    private Workspace workspace;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false, updatable = false)
    private Department department;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @NotNull
    @Column(name = "journal_date", nullable = false)
    private LocalDateTime journalDate;

    // =========================================================================
    // Generation metadata
    // =========================================================================

    /**
     * Shift this journal covers (MORNING / EVENING). Optional.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "shift", length = 20)
    private HandoverEntry.Shift shift;

    /**
     * Journal version - incremented every regeneration for the same day.
     */
    @NotNull
    @Column(name = "journal_version", nullable = false)
    private Integer journalVersion = 1;

    /**
     * Human-readable generator label (e.g. "Gemini").
     */
    @Column(name = "generated_by", length = 255)
    private String generatedBy;

    /**
     * Comma-separated names of the departments included in this journal.
     */
    @Column(name = "departments_included", length = 500)
    private String departmentsIncluded;

    /**
     * Number of submitted Handover Entries aggregated into this journal.
     */
    @NotNull
    @Column(name = "entries_count", nullable = false)
    private Long entriesCount = 0L;

    // =========================================================================
    // Generated fields
    // =========================================================================

    @NotBlank(message = "Generated summary is required")
    @Column(name = "generated_summary", nullable = false, columnDefinition = "TEXT")
    private String generatedSummary;

    @NotBlank(message = "Main done work is required")
    @Column(name = "main_done_work", nullable = false, columnDefinition = "TEXT")
    private String mainDoneWork;

    @NotBlank(message = "Main remaining work is required")
    @Column(name = "main_remaining_work", nullable = false, columnDefinition = "TEXT")
    private String mainRemainingWork;

    @NotBlank(message = "Blockers are required")
    @Column(name = "blockers", nullable = false, columnDefinition = "TEXT")
    private String blockers;

    @NotBlank(message = "Difficulties are required")
    @Column(name = "difficulties", nullable = false, columnDefinition = "TEXT")
    private String difficulties;

    @NotBlank(message = "Recommendations are required")
    @Column(name = "recommendations", nullable = false, columnDefinition = "TEXT")
    private String recommendations;

    // =========================================================================
    // Aggregated workflow counts
    // =========================================================================

    @NotNull
    @Column(name = "total_handovers", nullable = false)
    private Long totalHandovers = 0L;

    @NotNull
    @Column(name = "pending_handovers", nullable = false)
    private Long pendingHandovers = 0L;

    @NotNull
    @Column(name = "completed_handovers", nullable = false)
    private Long completedHandovers = 0L;

    @NotNull
    @Column(name = "rejected_handovers", nullable = false)
    private Long rejectedHandovers = 0L;

    @NotNull
    @Column(name = "urgent_handovers", nullable = false)
    private Long urgentHandovers = 0L;

    @NotNull
    @Column(name = "overdue_handovers", nullable = false)
    private Long overdueHandovers = 0L;

    // =========================================================================
    // Generation status
    // =========================================================================

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 30)
    private GenerationStatus generationStatus = GenerationStatus.PENDING;

    @Column(name = "generation_date")
    private LocalDateTime generationDate;

    @Column(name = "generation_processed_by")
    private UUID generationProcessedBy;

    // =========================================================================
    // Approval status
    // =========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // =========================================================================
    // Soft delete
    // =========================================================================

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private HandoverJournalStatus status = HandoverJournalStatus.ACTIVE;

    @PrePersist
    @PreUpdate
    private void validateHierarchy() {
        if (generationStatus == null) {
            generationStatus = GenerationStatus.PENDING;
        }
        if (approvalStatus == null) {
            approvalStatus = ApprovalStatus.PENDING;
        }
        if (status == null) {
            status = HandoverJournalStatus.ACTIVE;
        }
        if (totalHandovers == null) totalHandovers = 0L;
        if (pendingHandovers == null) pendingHandovers = 0L;
        if (completedHandovers == null) completedHandovers = 0L;
        if (rejectedHandovers == null) rejectedHandovers = 0L;
        if (urgentHandovers == null) urgentHandovers = 0L;
        if (overdueHandovers == null) overdueHandovers = 0L;
        if (journalVersion == null) journalVersion = 1;
        if (entriesCount == null) entriesCount = 0L;

        Objects.requireNonNull(workspace, "workspace must not be null");
        Objects.requireNonNull(department, "department must not be null");
        Objects.requireNonNull(project, "project must not be null");
        if (!Objects.equals(department.getId(), project.getDepartment().getId())) {
            throw new IllegalStateException("HandoverJournal.department must match project.department");
        }
        if (!Objects.equals(workspace.getId(), project.getDepartment().getWorkspace().getId())) {
            throw new IllegalStateException("HandoverJournal.workspace must match project.department.workspace");
        }
    }

    public enum GenerationStatus {
        PENDING,
        GENERATED,
        FAILED
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum HandoverJournalStatus {
        ACTIVE,
        ARCHIVED,
        DELETED
    }
}
