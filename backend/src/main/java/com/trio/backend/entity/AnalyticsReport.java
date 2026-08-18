package com.trio.backend.entity;

import com.trio.backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "analytics_reports",
        indexes = {
                @Index(name = "idx_analytics_reports_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_analytics_reports_department_id", columnList = "department_id"),
                @Index(name = "idx_analytics_reports_project_id", columnList = "project_id"),
                @Index(name = "idx_analytics_reports_report_date", columnList = "report_date"),
                @Index(name = "idx_analytics_reports_status", columnList = "status"),
                @Index(name = "idx_analytics_reports_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReport extends AuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, updatable = false)
    private Workspace workspace;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false, updatable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @NotNull
    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "time_range_start")
    private LocalDate timeRangeStart;

    @Column(name = "time_range_end")
    private LocalDate timeRangeEnd;

    @NotBlank(message = "Executive summary is required")
    @Column(name = "executive_summary", nullable = false, columnDefinition = "TEXT")
    private String executiveSummary;

    @NotBlank(message = "KPI highlights are required")
    @Column(name = "kpi_highlights", nullable = false, columnDefinition = "TEXT")
    private String kpiHighlights;

    @NotBlank(message = "Trends summary is required")
    @Column(name = "trends_summary", nullable = false, columnDefinition = "TEXT")
    private String trendsSummary;

    @NotBlank(message = "Risk assessment is required")
    @Column(name = "risk_assessment", nullable = false, columnDefinition = "TEXT")
    private String riskAssessment;

    @NotBlank(message = "Recommendations are required")
    @Column(name = "recommendations", nullable = false, columnDefinition = "TEXT")
    private String recommendations;

    @NotBlank(message = "Detailed report is required")
    @Column(name = "detailed_report", nullable = false, columnDefinition = "TEXT")
    private String detailedReport;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 30)
    private GenerationStatus generationStatus = GenerationStatus.PENDING;

    @Column(name = "generation_date")
    private LocalDateTime generationDate;

    @Column(name = "generation_processed_by")
    private UUID generationProcessedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.ACTIVE;

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
            status = ReportStatus.ACTIVE;
        }

        Objects.requireNonNull(workspace, "workspace must not be null");
        Objects.requireNonNull(department, "department must not be null");
        if (!Objects.equals(workspace.getId(), department.getWorkspace().getId())) {
            throw new IllegalStateException("AnalyticsReport.workspace must match department.workspace");
        }
        if (project != null) {
            if (!Objects.equals(department.getId(), project.getDepartment().getId())) {
                throw new IllegalStateException("AnalyticsReport.department must match project.department");
            }
            if (!Objects.equals(workspace.getId(), project.getDepartment().getWorkspace().getId())) {
                throw new IllegalStateException("AnalyticsReport.workspace must match project.department.workspace");
            }
        }
    }

    public enum GenerationStatus {
        PENDING,
        COMPLETED,
        FAILED
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum ReportStatus {
        ACTIVE,
        ARCHIVED,
        DELETED
    }
}
