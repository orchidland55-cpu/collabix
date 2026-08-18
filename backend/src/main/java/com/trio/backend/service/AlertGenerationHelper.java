package com.trio.backend.service;

import com.trio.backend.dto.alert.CreateAlertCommand;
import com.trio.backend.entity.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Helper that records an alert defensively: any failure while creating the
 * alert is logged and swallowed, so it never masks the original business
 * exception (e.g. an {@code AIProviderException}) that triggered the alert.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertGenerationHelper {

    private final AlertService alertService;

    /**
     * Attempts to create the given alert, logging and swallowing any error so
     * the caller's own exception is preserved.
     *
     * @param command the alert creation command (must be fully populated)
     */
    public void tryCreate(CreateAlertCommand command) {
        try {
            alertService.createInternal(command);
        } catch (Exception ex) {
            log.warn("Failed to record alert type={} workspace={} recipient={}: {}",
                    command.getType(), command.getWorkspaceId(), command.getRecipientId(), ex.getMessage());
        }
    }

    /**
     * Records an {@code AI_GENERATION_FAILED} alert for the given user,
     * swallowing any error so the original AI exception is preserved.
     */
    public void recordAiFailure(UUID workspaceId, UUID recipientId, UUID departmentId,
                                String resourceType, UUID resourceId, String title, String message) {
        tryCreate(CreateAlertCommand.builder()
                .workspaceId(workspaceId)
                .recipientId(recipientId)
                .departmentId(departmentId)
                .type(Alert.AlertType.AI_GENERATION_FAILED)
                .severity(Alert.Severity.CRITICAL)
                .title(title)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .dedupKey(null)
                .build());
    }

    /**
     * Records an {@code AI_GENERATION_REQUIRES_ATTENTION} alert for the given
     * user, swallowing any error so the original exception is preserved.
     */
    public void recordAiNeedsAttention(UUID workspaceId, UUID recipientId, UUID departmentId,
                                       String resourceType, UUID resourceId, String title, String message) {
        tryCreate(CreateAlertCommand.builder()
                .workspaceId(workspaceId)
                .recipientId(recipientId)
                .departmentId(departmentId)
                .type(Alert.AlertType.AI_GENERATION_REQUIRES_ATTENTION)
                .severity(Alert.Severity.WARNING)
                .title(title)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .dedupKey(null)
                .build());
    }

    /**
     * Records a {@code DOCUMENT_UPLOAD_FAILED} alert for the given user,
     * swallowing any error so the original exception is preserved.
     */
    public void recordDocumentUploadFailure(UUID workspaceId, UUID recipientId, UUID departmentId,
                                            UUID documentId, String title, String message) {
        tryCreate(CreateAlertCommand.builder()
                .workspaceId(workspaceId)
                .recipientId(recipientId)
                .departmentId(departmentId)
                .type(Alert.AlertType.DOCUMENT_UPLOAD_FAILED)
                .severity(Alert.Severity.WARNING)
                .title(title)
                .message(message)
                .resourceType("DOCUMENT")
                .resourceId(documentId)
                .dedupKey(null)
                .build());
    }
}
