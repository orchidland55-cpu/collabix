package com.trio.backend.entity;

import com.trio.backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Timeline (journal) event recorded against a {@link HandoverEntry}.
 *
 * <p>Represents the history of a handover: creation, sending, acceptance,
 * rejection, completion, archiving, comments, attachments, reminders.</p>
 */
@Entity
@Table(
        name = "handover_timeline_events",
        indexes = {
                @Index(name = "idx_handover_timeline_entry", columnList = "handover_entry_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandoverTimelineEvent extends AuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handover_entry_id", nullable = false)
    private HandoverEntry handoverEntry;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private TimelineEventType eventType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "actor_id")
    private UUID actorId;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    public enum TimelineEventType {
        CREATED,
        UPDATED,
        SENT,
        SUBMITTED,
        ACCEPTED,
        REJECTED,
        COMPLETED,
        ARCHIVED,
        COMMENTED,
        ATTACHMENT_ADDED,
        ATTACHMENT_REMOVED,
        REMINDER_SENT
    }
}
