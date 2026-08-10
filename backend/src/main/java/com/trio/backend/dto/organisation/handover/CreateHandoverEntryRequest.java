package com.trio.backend.dto.organisation.handover;

import com.trio.backend.entity.HandoverEntry;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Create request of a HandoverEntry.
 *
 * <p>An entry can be either a classic sender-&gt;receiver handover
 * (receiverId, title, content) or a daily work report
 * (shift, entryDate, completedTasks, currentProgress, pendingTasks, ...).
 * Both are created as DRAFT and later SENT or SUBMITTED respectively.</p>
 */
@Getter
@Setter
public class CreateHandoverEntryRequest {

    @NotNull(message = "Department is required")
    private UUID departmentId;

    @NotNull(message = "Project is required")
    private UUID projectId;

    private UUID taskId;

    private UUID receiverId;

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String content;

    private HandoverEntry.Priority priority;

    private LocalDateTime dueDate;

    // ==================== Daily report fields ====================

    private HandoverEntry.Shift shift;

    private LocalDate entryDate;

    private String completedTasks;

    private String currentProgress;

    private String pendingTasks;

    private String blockers;

    private String importantNotes;

    private String estimatedRemainingWork;

    private String mood;
}
