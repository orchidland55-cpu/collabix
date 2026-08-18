package com.trio.backend.entity;

import com.trio.backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.UUID;

/**
 * Alert represents an event that requires attention, addressed to a specific
 * user within a Workspace.
 *
 * <p>Architecture notes:</p>
 * <ul>
 *     <li>Alerts are a separate module from {@link Notification}s:
 *         notifications communicate normal events, alerts communicate events
 *         that require attention (deadlines, failures, blockages).</li>
 *     <li>An Alert belongs to exactly one {@link Workspace} for tenant isolation.</li>
 *     <li>An Alert is always addressed to exactly one recipient {@link User}.</li>
 *     <li>An Alert may optionally reference one business resource via the generic
 *         {@code resourceType} + {@code resourceId} pattern.</li>
 *     <li>Idempotency is enforced through a nullable {@link #dedupKey} backed by
 *         a partial unique index, so scheduled generation never duplicates alerts.</li>
 *     <li>Read state is tracked via {@link #readAt} and the {@link AlertStatus}.</li>
 * </ul>
 */
@Entity
@Table(
        name = "alerts",
        indexes = {
                @Index(name = "idx_alerts_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_alerts_recipient_id", columnList = "recipient_id"),
                @Index(name = "idx_alerts_recipient_status", columnList = "recipient_id, status"),
                @Index(name = "idx_alerts_recipient_created", columnList = "recipient_id, created_at"),
                @Index(name = "idx_alerts_recipient_unread", columnList = "recipient_id, status, read_at"),
                @Index(name = "idx_alerts_type", columnList = "type"),
                @Index(name = "idx_alerts_severity", columnList = "severity"),
                @Index(name = "idx_alerts_department_id", columnList = "department_id"),
                @Index(name = "idx_alerts_resource_type_id", columnList = "resource_type, resource_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@BatchSize(size = 20)
public class Alert extends AuditableEntity {

    // =========================================================================
    // Mandatory context
    // =========================================================================

    /**
     * The Workspace this alert belongs to.
     * Required for tenant isolation and multi-tenant compatibility.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, updatable = false)
    private Workspace workspace;

    /**
     * The recipient User of this alert.
     * Required. Always addressed to exactly one user.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false, updatable = false)
    private User recipient;

    /**
     * Optional department this alert is scoped to (used for isolation and
     * department filtering by managers).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "department_id", nullable = true, updatable = false)
    private Department department;

    // =========================================================================
    // Alert classification
    // =========================================================================

    /**
     * The type of alert (business event that triggered it).
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private AlertType type;

    /**
     * The severity of the alert.
     * <ul>
     *     <li>{@link Severity#INFO} — informational, no urgency</li>
     *     <li>{@link Severity#WARNING} — requires attention soon</li>
     *     <li>{@link Severity#CRITICAL} — requires immediate attention</li>
     * </ul>
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    // =========================================================================
    // Alert content
    // =========================================================================

    /**
     * The title of the alert.
     * Required. Short, user-facing summary.
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * The body/content of the alert.
     * Optional. Detailed message for the recipient.
     */
    @Size(max = 2000)
    @Column(name = "message", length = 2000)
    private String message;

    /**
     * Generic resource type (e.g. TASK, PROJECT, DOCUMENT, REPORT, HANDOVER).
     */
    @Size(max = 50)
    @Column(name = "resource_type", length = 50)
    private String resourceType;

    /**
     * Generic resource identifier for {@link #resourceType}.
     */
    @Column(name = "resource_id")
    private UUID resourceId;

    /**
     * Idempotency key that guarantees the same event (type + resource +
     * recipient) only ever produces a single alert. Nullable: one-off system
     * alerts may repeat without a key. Backed by a partial unique index.
     */
    @Size(max = 255)
    @Column(name = "dedup_key", length = 255)
    private String dedupKey;

    // =========================================================================
    // Read tracking & lifecycle
    // =========================================================================

    /**
     * Timestamp indicating when the alert was read by the recipient.
     * Null until the alert is marked as read.
     */
    @Column(name = "read_at")
    private Instant readAt;

    /**
     * The status of the alert.
     * <ul>
     *     <li>{@link AlertStatus#UNREAD} — alert has not been seen</li>
     *     <li>{@link AlertStatus#READ} — alert has been read</li>
     *     <li>{@link AlertStatus#ARCHIVED} — alert dismissed (soft-delete)</li>
     * </ul>
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status = AlertStatus.UNREAD;

    @PrePersist
    private void prePersist() {
        if (status == null) {
            status = AlertStatus.UNREAD;
        }
    }

    // =========================================================================
    // Enums
    // =========================================================================

    /**
     * Status values for the Alert lifecycle.
     * Supports soft-delete via ARCHIVED status.
     */
    public enum AlertStatus {
        UNREAD,
        READ,
        ARCHIVED
    }

    /**
     * Types of alerts supported by the platform.
     * Each value corresponds to a business event that requires attention.
     */
    public enum AlertType {
        TASK_DEADLINE_APPROACHING,
        TASK_OVERDUE,
        TASK_BLOCKED,
        DOCUMENT_UPLOAD_FAILED,
        AI_GENERATION_FAILED,
        AI_GENERATION_REQUIRES_ATTENTION,
        HANDOVER_GENERATION_FAILED,
        PERMISSION_DENIED,
        SYSTEM_ERROR
    }

    /**
     * Severity levels for alerts.
     * <ul>
     *     <li>{@code INFO} — informational, no urgency</li>
     *     <li>{@code WARNING} — requires attention soon</li>
     *     <li>{@code CRITICAL} — requires immediate attention</li>
     * </ul>
     */
    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }
}
