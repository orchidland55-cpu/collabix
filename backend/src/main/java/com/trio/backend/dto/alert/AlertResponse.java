package com.trio.backend.dto.alert;

import com.trio.backend.entity.Alert;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an Alert, containing exactly the fields the frontend needs.
 */
@Getter
@Setter
public class AlertResponse {

    private UUID id;

    private UUID workspaceId;

    private UUID recipientId;

    private UUID departmentId;

    private Alert.AlertType type;

    private Alert.Severity severity;

    private Alert.AlertStatus status;

    private String title;

    private String message;

    private String resourceType;

    private UUID resourceId;

    private Instant readAt;

    private Instant createdAt;

    private Instant updatedAt;
}
