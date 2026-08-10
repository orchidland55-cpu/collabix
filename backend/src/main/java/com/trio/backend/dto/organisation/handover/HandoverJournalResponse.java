package com.trio.backend.dto.organisation.handover;

import com.trio.backend.entity.HandoverEntry;
import com.trio.backend.entity.HandoverJournal;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response for a HandoverJournal generated automatically.
 */
@Getter
@Setter
public class HandoverJournalResponse {

    private UUID id;

    private UUID workspaceId;

    private UUID departmentId;

    private UUID projectId;

    private LocalDateTime journalDate;

    private HandoverEntry.Shift shift;

    private Integer journalVersion;

    private String generatedBy;

    private String departmentsIncluded;

    private Long entriesCount;

    private String generatedSummary;

    private String mainDoneWork;

    private String mainRemainingWork;

    private String blockers;

    private String difficulties;

    private String recommendations;

    private Long totalHandovers;

    private Long pendingHandovers;

    private Long completedHandovers;

    private Long rejectedHandovers;

    private Long urgentHandovers;

    private Long overdueHandovers;

    private HandoverJournal.GenerationStatus generationStatus;

    private LocalDateTime generationDate;

    private UUID generationProcessedBy;

    private HandoverJournal.HandoverJournalStatus status;

    private Instant createdAt;

    private Instant updatedAt;
}
