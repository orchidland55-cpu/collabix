package com.trio.backend.dto.organisation.handover;

import com.trio.backend.entity.HandoverEntry;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Update request (partial) of a HandoverEntry.
 *
 * <p>Only the sender may update a handover that is still in DRAFT or REJECTED state.</p>
 */
@Getter
@Setter
public class UpdateHandoverEntryRequest {

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
