package com.trio.backend.dto.alert;

import com.trio.backend.entity.Alert;
import lombok.*;

import java.util.UUID;

/**
 * Internal command used by the scheduler and business services (tasks,
 * documents, AI, handover) to generate an alert for a specific recipient.
 *
 * <p>This command is <strong>not</strong> exposed through any REST endpoint.
 * Alerts are generated server-side only, never by clients.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlertCommand {

    /**
     * Workspace the alert belongs to (tenant isolation).
     */
    private UUID workspaceId;

    /**
     * Recipient user of the alert (mandatory).
     */
    private UUID recipientId;

    /**
     * Optional department the alert is scoped to.
     */
    private UUID departmentId;

    /**
     * Business event that triggered the alert.
     */
    private Alert.AlertType type;

    /**
     * Severity of the alert.
     */
    private Alert.Severity severity;

    /**
     * Short user-facing title.
     */
    private String title;

    /**
     * Optional detailed message.
     */
    private String message;

    /**
     * Generic resource type (TASK, PROJECT, DOCUMENT, REPORT, HANDOVER, ...).
     */
    private String resourceType;

    /**
     * Generic resource identifier.
     */
    private UUID resourceId;

    /**
     * Idempotency key; when provided, creation is skipped if an alert with
     * the same key already exists.
     */
    private String dedupKey;
}
