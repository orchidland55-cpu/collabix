package com.trio.backend.service;

import com.trio.backend.enums.AdminAnalyticsPeriod;
import com.trio.backend.enums.TaskStatus;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.reporting.analytics.dto.admin.ActivityOverviewPointDto;
import com.trio.backend.reporting.analytics.dto.admin.ActivityOverviewResponse;
import com.trio.backend.reporting.analytics.dto.admin.AdminProjectStatusResponse;
import com.trio.backend.reporting.analytics.dto.admin.ProjectStatusSegmentDto;
import com.trio.backend.repository.ActivityRepository;
import com.trio.backend.repository.CommentRepository;
import com.trio.backend.repository.ProjectRepository;
import com.trio.backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardAnalyticsService {

    private static final Locale LABEL_LOCALE = Locale.ENGLISH;

    private final ActivityRepository activityRepository;
    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public ActivityOverviewResponse getActivityOverview(UUID workspaceId, AdminAnalyticsPeriod period) {
        DateRange range = resolveRange(period);
        Map<LocalDate, Long> totals = new HashMap<>();

        mergeDailyCounts(totals, activityRepository.countActiveByWorkspaceIdGroupedByDay(
                workspaceId, range.fromInstant(), range.toExclusiveInstant()));
        mergeDailyCounts(totals, commentRepository.countActiveByWorkspaceIdGroupedByDay(
                workspaceId, range.fromInstant(), range.toExclusiveInstant()));

        List<ActivityOverviewPointDto> points = new ArrayList<>();
        long total = 0;
        for (LocalDate day = range.start(); !day.isAfter(range.end()); day = day.plusDays(1)) {
            long value = totals.getOrDefault(day, 0L);
            total += value;
            points.add(ActivityOverviewPointDto.builder()
                    .date(day)
                    .label(formatDayLabel(day, period))
                    .value(value)
                    .build());
        }

        return ActivityOverviewResponse.builder()
                .period(period)
                .points(points)
                .total(total)
                .build();
    }

    public AdminProjectStatusResponse getProjectStatus(UUID workspaceId) {
        List<com.trio.backend.entity.Project> activeProjects =
                projectRepository.findAllByWorkspaceIdAndStatus(workspaceId, WorkspaceStatus.ACTIVE);

        Map<UUID, Map<TaskStatus, Long>> tasksByProject = new HashMap<>();
        for (Object[] row : taskRepository.countActiveTasksByProjectAndStatusForWorkspace(workspaceId)) {
            UUID projectId = (UUID) row[0];
            TaskStatus status = (TaskStatus) row[1];
            long count = (Long) row[2];
            tasksByProject.computeIfAbsent(projectId, ignored -> new EnumMap<>(TaskStatus.class))
                    .merge(status, count, Long::sum);
        }

        Map<ProjectWorkflowBucket, Long> bucketCounts = new EnumMap<>(ProjectWorkflowBucket.class);
        for (ProjectWorkflowBucket bucket : ProjectWorkflowBucket.values()) {
            bucketCounts.put(bucket, 0L);
        }

        for (com.trio.backend.entity.Project project : activeProjects) {
            Map<TaskStatus, Long> statusCounts = tasksByProject.getOrDefault(project.getId(), Map.of());
            ProjectWorkflowBucket bucket = classifyProject(statusCounts);
            bucketCounts.merge(bucket, 1L, Long::sum);
        }

        long activeProjectCount = activeProjects.size();
        List<ProjectStatusSegmentDto> segments = new ArrayList<>();
        for (ProjectWorkflowBucket bucket : ProjectWorkflowBucket.values()) {
            long count = bucketCounts.getOrDefault(bucket, 0L);
            double percentage = activeProjectCount == 0 ? 0.0 : (count * 100.0) / activeProjectCount;
            segments.add(ProjectStatusSegmentDto.builder()
                    .status(bucket.name())
                    .label(bucket.label)
                    .count(count)
                    .percentage(Math.round(percentage * 10.0) / 10.0)
                    .build());
        }

        return AdminProjectStatusResponse.builder()
                .activeProjectCount(activeProjectCount)
                .segments(segments)
                .build();
    }

    private void mergeDailyCounts(Map<LocalDate, Long> totals, List<Object[]> rows) {
        for (Object[] row : rows) {
            LocalDate day = toLocalDate(row[0]);
            long count = row[1] instanceof Number number ? number.longValue() : 0L;
            totals.merge(day, count, Long::sum);
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.util.Date utilDate) {
            return utilDate.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        throw new IllegalArgumentException("Unsupported date type: " + value.getClass());
    }

    private DateRange resolveRange(AdminAnalyticsPeriod period) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return switch (period) {
            case THIS_WEEK -> {
                LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new DateRange(start, start.plusDays(6));
            }
            case THIS_MONTH -> new DateRange(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
            case LAST_7_DAYS -> new DateRange(today.minusDays(6), today);
            case LAST_30_DAYS -> new DateRange(today.minusDays(29), today);
        };
    }

    private String formatDayLabel(LocalDate day, AdminAnalyticsPeriod period) {
        if (period == AdminAnalyticsPeriod.THIS_WEEK) {
            return day.getDayOfWeek().getDisplayName(TextStyle.FULL, LABEL_LOCALE);
        }
        return day.getMonth().getDisplayName(TextStyle.SHORT, LABEL_LOCALE) + " " + day.getDayOfMonth();
    }

    private ProjectWorkflowBucket classifyProject(Map<TaskStatus, Long> statusCounts) {
        if (statusCounts.isEmpty()) {
            return ProjectWorkflowBucket.TO_DO;
        }

        long inReview = statusCounts.getOrDefault(TaskStatus.IN_REVIEW, 0L);
        long inProgress = statusCounts.getOrDefault(TaskStatus.IN_PROGRESS, 0L)
                + statusCounts.getOrDefault(TaskStatus.BLOCKED, 0L)
                + statusCounts.getOrDefault(TaskStatus.ACTIVE, 0L);
        long todo = statusCounts.getOrDefault(TaskStatus.TODO, 0L);
        long completed = statusCounts.getOrDefault(TaskStatus.COMPLETED, 0L);

        if (inReview > 0) {
            return ProjectWorkflowBucket.IN_REVIEW;
        }
        if (inProgress > 0) {
            return ProjectWorkflowBucket.IN_PROGRESS;
        }
        if (todo > 0 && completed == 0) {
            return ProjectWorkflowBucket.TO_DO;
        }
        if (todo == 0 && completed > 0) {
            return ProjectWorkflowBucket.DONE;
        }
        if (todo > 0) {
            return ProjectWorkflowBucket.TO_DO;
        }
        return ProjectWorkflowBucket.DONE;
    }

    private record DateRange(LocalDate start, LocalDate end) {
        Instant fromInstant() {
            return start.atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        Instant toExclusiveInstant() {
            return end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
    }

    private enum ProjectWorkflowBucket {
        TO_DO("To Do"),
        IN_PROGRESS("In Progress"),
        IN_REVIEW("Review"),
        DONE("Done");

        private final String label;

        ProjectWorkflowBucket(String label) {
            this.label = label;
        }
    }
}
