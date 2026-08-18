package com.trio.backend.dto.ai;

import com.trio.backend.entity.AnalyticsReport;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AnalyticsAIResponse {

    private UUID reportId;

    private UUID workspaceId;

    private UUID departmentId;

    private UUID projectId;

    private LocalDate reportDate;

    private LocalDate timeRangeStart;

    private LocalDate timeRangeEnd;

    private String executiveSummary;

    private String kpiHighlights;

    private String trendsSummary;

    private String riskAssessment;

    private String recommendations;

    private String detailedReport;

    private AnalyticsReport.GenerationStatus generationStatus;

    private AnalyticsReport.ApprovalStatus approvalStatus;

    private LocalDateTime generationDate;

    private UUID generatedBy;

    private Long executionTime;

    private Instant createdAt;

    private Instant updatedAt;
}
