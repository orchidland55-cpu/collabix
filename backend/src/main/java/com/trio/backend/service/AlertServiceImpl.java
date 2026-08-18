package com.trio.backend.service;

import com.trio.backend.dto.alert.AlertResponse;
import com.trio.backend.dto.alert.AlertSearchCriteria;
import com.trio.backend.dto.alert.CreateAlertCommand;
import com.trio.backend.entity.Alert;
import com.trio.backend.entity.Alert.AlertStatus;
import com.trio.backend.entity.Department;
import com.trio.backend.entity.User;
import com.trio.backend.entity.Workspace;
import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.AlertMapper;
import com.trio.backend.repository.AlertRepository;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;
import com.trio.backend.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of the Alerts module service.
 *
 * <p>Isolation guarantees:</p>
 * <ul>
 *     <li>Every read/mutation is scoped to the authenticated user
 *         ({@code recipientId == current user}) and the requested workspace.</li>
 *     <li>Alerts are created server-side only through {@link #createInternal},
 *         which runs in a {@code REQUIRES_NEW} transaction so that an alert is
 *         persisted even when the surrounding business transaction rolls back.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AlertMapper alertMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Alert createInternal(CreateAlertCommand command) {
        if (command.getWorkspaceId() == null) {
            throw new BadRequestException("workspaceId is required.");
        }
        if (command.getRecipientId() == null) {
            throw new BadRequestException("recipientId is required.");
        }
        if (command.getType() == null) {
            throw new BadRequestException("type is required.");
        }
        if (command.getSeverity() == null) {
            throw new BadRequestException("severity is required.");
        }
        if (command.getTitle() == null || command.getTitle().isBlank()) {
            throw new BadRequestException("title is required.");
        }

        // Idempotency: skip creation when the dedup key already exists.
        if (command.getDedupKey() != null && alertRepository.existsByDedupKey(command.getDedupKey())) {
            log.debug("Alert skipped (dedup key already exists): {}", command.getDedupKey());
            return null;
        }

        Workspace workspace = workspaceRepository.findById(command.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));
        User recipient = userRepository.findById(command.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found."));

        Alert.AlertBuilder builder = Alert.builder()
                .workspace(workspace)
                .recipient(recipient)
                .type(command.getType())
                .severity(command.getSeverity())
                .title(command.getTitle())
                .message(command.getMessage())
                .resourceType(command.getResourceType())
                .resourceId(command.getResourceId())
                .dedupKey(command.getDedupKey())
                .status(AlertStatus.UNREAD);

        if (command.getDepartmentId() != null) {
            departmentRepository.findByIdAndWorkspace_Id(command.getDepartmentId(), command.getWorkspaceId())
                    .ifPresent(builder::department);
        }

        Alert saved = alertRepository.save(builder.build());
        log.info("Alert created: id={}, type={}, severity={}, recipient={}, workspace={}",
                saved.getId(), saved.getType(), saved.getSeverity(), recipient.getId(), command.getWorkspaceId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public AlertResponse getById(UUID workspaceId, UUID alertId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Alert alert = findOwnedAlert(workspaceId, alertId, userId);
        return alertMapper.toResponse(alert);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlertResponse> list(UUID workspaceId, UUID recipientId, AlertSearchCriteria criteria, Pageable pageable) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        if (!recipientId.equals(userId)) {
            throw new ForbiddenException("You can only list your own alerts.");
        }

        Page<Alert> page;
        if (criteria != null && criteria.getType() != null && criteria.getSeverity() != null) {
            page = alertRepository.findByRecipientIdAndWorkspaceIdAndTypeAndSeverity(
                    recipientId, workspaceId, criteria.getType(), criteria.getSeverity(), pageable);
        } else if (criteria != null && criteria.getType() != null) {
            page = alertRepository.findByRecipientIdAndWorkspaceIdAndType(recipientId, workspaceId, criteria.getType(), pageable);
        } else if (criteria != null && criteria.getSeverity() != null) {
            page = alertRepository.findByRecipientIdAndWorkspaceIdAndSeverity(recipientId, workspaceId, criteria.getSeverity(), pageable);
        } else if (criteria != null && criteria.getStatus() != null) {
            page = alertRepository.findByRecipientIdAndWorkspaceIdAndStatus(recipientId, workspaceId, criteria.getStatus(), pageable);
        } else {
            page = alertRepository.findByRecipientIdAndWorkspaceId(recipientId, workspaceId, pageable);
        }

        return page.map(alertMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID workspaceId, UUID recipientId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        if (!recipientId.equals(userId)) {
            throw new ForbiddenException("You can only count your own alerts.");
        }

        return alertRepository.countUnreadByRecipientIdAndWorkspaceId(recipientId, workspaceId);
    }

    @Override
    public AlertResponse markAsRead(UUID workspaceId, UUID alertId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Alert alert = findOwnedAlert(workspaceId, alertId, userId);

        if (alert.getStatus() == AlertStatus.UNREAD) {
            alert.setStatus(AlertStatus.READ);
            alert.setReadAt(Instant.now());
            alertRepository.save(alert);
            log.info("Alert marked as read: id={}, recipient={}", alertId, userId);
        }

        return alertMapper.toResponse(alert);
    }

    @Override
    public void markAllAsRead(UUID workspaceId, UUID recipientId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        if (!recipientId.equals(userId)) {
            throw new ForbiddenException("You can only mark your own alerts as read.");
        }

        int updated = alertRepository.markAllAsRead(recipientId, workspaceId, Instant.now());
        log.info("All alerts marked as read for recipient={} in workspace={}: count={}", recipientId, workspaceId, updated);
    }

    @Override
    public void dismiss(UUID workspaceId, UUID alertId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Alert alert = findOwnedAlert(workspaceId, alertId, userId);

        if (alert.getStatus() != AlertStatus.ARCHIVED) {
            alert.setStatus(AlertStatus.ARCHIVED);
            alert.setReadAt(Instant.now());
            alertRepository.save(alert);
            log.info("Alert dismissed (archived): id={}, recipient={}", alertId, userId);
        }
    }

    // ============================================================================
    // Helpers
    // ============================================================================

    private Alert findOwnedAlert(UUID workspaceId, UUID alertId, UUID userId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found."));

        if (alert.getStatus() == AlertStatus.ARCHIVED) {
            throw new ResourceNotFoundException("Alert not found.");
        }

        if (!alert.getWorkspace().getId().equals(workspaceId)
                || !alert.getRecipient().getId().equals(userId)) {
            throw new ResourceNotFoundException("Alert not found.");
        }

        return alert;
    }

    private UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails main)) {
            throw new BadRequestException("User is not authenticated.");
        }
        return main.getId();
    }

    private void assertActiveWorkspaceMember(UUID workspaceId, UUID userId) {
        WorkspaceMember wm = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace."));

        if (wm.getStatus() != com.trio.backend.enums.WorkspaceMemberStatus.ACTIVE) {
            throw new ForbiddenException("You are not an active member of this workspace.");
        }
    }
}
