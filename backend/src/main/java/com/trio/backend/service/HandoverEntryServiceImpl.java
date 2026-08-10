package com.trio.backend.service;

import com.trio.backend.dto.organisation.handover.CreateHandoverEntryRequest;
import com.trio.backend.dto.organisation.handover.HandoverEntryResponse;
import com.trio.backend.dto.organisation.handover.HandoverStatusUpdateRequest;
import com.trio.backend.dto.organisation.handover.UpdateHandoverEntryRequest;
import com.trio.backend.entity.*;
import com.trio.backend.entity.HandoverEntry.HandoverStatus;
import com.trio.backend.entity.HandoverEntry.Priority;
import com.trio.backend.entity.HandoverTimelineEvent.TimelineEventType;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.HandoverEntryMapper;
import com.trio.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation for the HandoverEntry workflow.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class HandoverEntryServiceImpl implements HandoverEntryService {

    private final HandoverEntryRepository handoverEntryRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final HandoverEntryMapper handoverEntryMapper;
    private final HandoverSupport support;

    @Override
    @Transactional
    public HandoverEntryResponse create(UUID workspaceId, CreateHandoverEntryRequest request) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        // Admin/owner roles are read-only for handover entries: only managers and members submit daily reports.
        if (support.isWorkspaceAdminOrOwner(workspaceId, userId)) {
            throw new ForbiddenException("Admins cannot create handover entries. Only managers and members can submit daily reports.");
        }
        if (!support.currentUserDepartmentId().equals(request.getDepartmentId())) {
            throw new ForbiddenException("You can only create handover entries in your own department.");
        }

        Project project = validateProject(workspaceId, request.getDepartmentId(), request.getProjectId());

        User receiver = null;
        if (request.getReceiverId() != null) {
            receiver = userRepository.findById(request.getReceiverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Receiver not found."));
            support.assertUserIsActiveMember(workspaceId, receiver.getId(), "Receiver is not an active member of this workspace.");
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Task task = null;
        if (request.getTaskId() != null) {
            task = taskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found."));
            if (!task.getProject().getId().equals(project.getId())) {
                throw new BadRequestException("Task does not belong to the given project.");
            }
        }

        HandoverEntry entry = handoverEntryMapper.toEntity(request);
        entry.setWorkspace(project.getDepartment().getWorkspace());
        entry.setDepartment(project.getDepartment());
        entry.setProject(project);
        entry.setSender(sender);
        entry.setReceiver(receiver);
        entry.setTask(task);
        entry.setStatus(HandoverStatus.DRAFT);

        HandoverEntry saved = handoverEntryRepository.save(entry);
        support.addTimelineEvent(saved, TimelineEventType.CREATED, "Handover created by " + support.userDisplayName(sender), userId);
        return handoverEntryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public HandoverEntryResponse getById(UUID workspaceId, UUID handoverEntryId) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        HandoverEntry entry = findEntry(workspaceId, handoverEntryId);
        assertCanViewEntry(entry, workspaceId, userId);
        return handoverEntryMapper.toResponse(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HandoverEntryResponse> list(
            UUID workspaceId,
            HandoverStatus status,
            Priority priority,
            UUID projectId,
            Pageable pageable
    ) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        if (support.isWorkspaceAdminOrOwner(workspaceId, userId)) {
            return handoverEntryRepository.search(workspaceId, status, priority, projectId, pageable)
                    .map(handoverEntryMapper::toResponse);
        }

        if (support.isWorkspaceManager(workspaceId, userId)) {
            UUID myDepartmentId = support.currentUserDepartmentId();
            return handoverEntryRepository.searchByDepartment(workspaceId, myDepartmentId, status, priority, projectId, pageable)
                    .map(handoverEntryMapper::toResponse);
        }

        // Members only see their own entries (sent by them).
        return handoverEntryRepository.findMine(workspaceId, userId, status, null, null, null, pageable)
                .map(handoverEntryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HandoverEntryResponse> inbox(UUID workspaceId, Pageable pageable) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);
        return handoverEntryRepository.findInboxPaginated(workspaceId, userId, pageable)
                .map(handoverEntryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HandoverEntryResponse> sent(UUID workspaceId, Pageable pageable) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);
        return handoverEntryRepository.findSentPaginated(workspaceId, userId, pageable)
                .map(handoverEntryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HandoverEntryResponse> myEntries(
            UUID workspaceId,
            HandoverStatus status,
            HandoverEntry.Shift shift,
            LocalDate entryDate,
            String search,
            Pageable pageable
    ) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);
        return handoverEntryRepository.findMine(workspaceId, userId, status, shift, entryDate, search, pageable)
                .map(handoverEntryMapper::toResponse);
    }

    @Override
    @Transactional
    public HandoverEntryResponse update(UUID workspaceId, UUID handoverEntryId, UpdateHandoverEntryRequest request) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        HandoverEntry entry = findEntry(workspaceId, handoverEntryId);
        if (!entry.getSender().getId().equals(userId)) {
            throw new ForbiddenException("Only the sender can update this handover.");
        }
        if (entry.getStatus() != HandoverStatus.DRAFT && entry.getStatus() != HandoverStatus.REJECTED) {
            throw new BadRequestException("Only DRAFT or REJECTED handovers can be updated.");
        }

        handoverEntryMapper.updateHandoverEntry(request, entry);

        if (request.getReceiverId() != null) {
            User receiver = userRepository.findById(request.getReceiverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Receiver not found."));
            support.assertUserIsActiveMember(workspaceId, receiver.getId(), "Receiver is not an active member of this workspace.");
            entry.setReceiver(receiver);
        }
        if (request.getTaskId() != null) {
            Task task = taskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found."));
            if (!task.getProject().getId().equals(entry.getProject().getId())) {
                throw new BadRequestException("Task does not belong to the handover project.");
            }
            entry.setTask(task);
        }

        HandoverEntry saved = handoverEntryRepository.save(entry);
        support.addTimelineEvent(saved, TimelineEventType.UPDATED, "Handover updated by " + support.userDisplayName(saved.getSender()), userId);
        return handoverEntryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HandoverEntryResponse send(UUID workspaceId, UUID handoverEntryId, HandoverStatusUpdateRequest request) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        HandoverEntry entry = findEntry(workspaceId, handoverEntryId);
        assertSender(entry, userId);
        if (entry.getStatus() != HandoverStatus.DRAFT && entry.getStatus() != HandoverStatus.REJECTED) {
            throw new BadRequestException("Only DRAFT or REJECTED handovers can be sent.");
        }
        if (entry.getReceiver() == null) {
            throw new BadRequestException("A receiver is required to send this handover. Use Submit for a daily report instead.");
        }

        entry.setStatus(HandoverStatus.PENDING);
        entry.setSentAt(LocalDateTime.now());
        HandoverEntry saved = handoverEntryRepository.save(entry);

        support.addTimelineEvent(saved, TimelineEventType.SENT, "Handover sent to " + support.userDisplayName(saved.getReceiver()), userId);
        support.notifyUser(workspaceId, saved.getReceiver().getId(), Notification.NotificationType.HANDOVER_SENT,
                "New handover: " + saved.getTitle(), saved.getTitle(), saved.getId());

        return handoverEntryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HandoverEntryResponse submit(UUID workspaceId, UUID handoverEntryId, HandoverStatusUpdateRequest request) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        HandoverEntry entry = findEntry(workspaceId, handoverEntryId);
        assertSender(entry, userId);
        if (entry.getStatus() != HandoverStatus.DRAFT && entry.getStatus() != HandoverStatus.REJECTED) {
            throw new BadRequestException("Only DRAFT or REJECTED entries can be submitted.");
        }

        entry.setStatus(HandoverStatus.SUBMITTED);
        entry.setSubmittedAt(LocalDateTime.now());
        HandoverEntry saved = handoverEntryRepository.save(entry);

        support.addTimelineEvent(saved, TimelineEventType.SUBMITTED, "Handover entry submitted for journal generation by "
                + support.userDisplayName(saved.getSender()), userId);

        return handoverEntryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HandoverEntryResponse accept(UUID workspaceId, UUID handoverEntryId, HandoverStatusUpdateRequest request) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        HandoverEntry entry = findEntry(workspaceId, handoverEntryId);
        assertReceiver(entry, userId);
        if (entry.getStatus() != HandoverStatus.PENDING) {
            throw new BadRequestException("Only PENDING handovers can be accepted.");
        }

        entry.setStatus(HandoverStatus.ACCEPTED);
        entry.setAcceptedAt(LocalDateTime.now());
        HandoverEntry saved = handoverEntryRepository.save(entry);

        support.addTimelineEvent(saved, TimelineEventType.ACCEPTED, "Handover accepted by " + support.userDisplayName(saved.getReceiver()), userId);
        support.notifyUser(workspaceId, saved.getSender().getId(), Notification.NotificationType.HANDOVER_ACCEPTED,
                "Handover accepted: " + saved.getTitle(), saved.getTitle(), saved.getId());

        return handoverEntryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HandoverEntryResponse reject(UUID workspaceId, UUID handoverEntryId, HandoverStatusUpdateRequest request) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        HandoverEntry entry = findEntry(workspaceId, handoverEntryId);
        assertReceiver(entry, userId);
        if (entry.getStatus() != HandoverStatus.PENDING) {
            throw new BadRequestException("Only PENDING handovers can be rejected.");
        }

        entry.setStatus(HandoverStatus.REJECTED);
        entry.setRejectedAt(LocalDateTime.now());
        HandoverEntry saved = handoverEntryRepository.save(entry);

        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? ": " + request.getReason()
                : "";
        support.addTimelineEvent(saved, TimelineEventType.REJECTED,
                "Handover rejected by " + support.userDisplayName(saved.getReceiver()) + reason, userId);
        support.notifyUser(workspaceId, saved.getSender().getId(), Notification.NotificationType.HANDOVER_REJECTED,
                "Handover rejected: " + saved.getTitle(), saved.getTitle(), saved.getId());

        return handoverEntryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HandoverEntryResponse complete(UUID workspaceId, UUID handoverEntryId, HandoverStatusUpdateRequest request) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        HandoverEntry entry = findEntry(workspaceId, handoverEntryId);
        boolean participant = entry.getSender().getId().equals(userId)
                || (entry.getReceiver() != null && entry.getReceiver().getId().equals(userId));
        if (!participant) {
            throw new ForbiddenException("Only the sender or receiver can complete this handover.");
        }
        if (entry.getStatus() != HandoverStatus.ACCEPTED) {
            throw new BadRequestException("Only ACCEPTED handovers can be completed.");
        }

        entry.setStatus(HandoverStatus.COMPLETED);
        entry.setCompletedAt(LocalDateTime.now());
        HandoverEntry saved = handoverEntryRepository.save(entry);

        support.addTimelineEvent(saved, TimelineEventType.COMPLETED, "Handover completed by " + support.userDisplayName(saved.getReceiver()), userId);
        UUID otherParty = entry.getSender().getId().equals(userId) ? entry.getReceiver().getId() : entry.getSender().getId();
        support.notifyUser(workspaceId, otherParty, Notification.NotificationType.HANDOVER_COMPLETED,
                "Handover completed: " + saved.getTitle(), saved.getTitle(), saved.getId());

        return handoverEntryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public HandoverEntryResponse archive(UUID workspaceId, UUID handoverEntryId, HandoverStatusUpdateRequest request) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        HandoverEntry entry = findEntry(workspaceId, handoverEntryId);
        boolean participant = entry.getSender().getId().equals(userId)
                || (entry.getReceiver() != null && entry.getReceiver().getId().equals(userId));
        boolean isAdmin = support.isWorkspaceAdminOrOwner(workspaceId, userId);
        if (!participant && !isAdmin) {
            throw new ForbiddenException("You do not have permission to archive this handover.");
        }
        if (entry.getStatus() == HandoverStatus.ARCHIVED) {
            return handoverEntryMapper.toResponse(entry);
        }

        entry.setStatus(HandoverStatus.ARCHIVED);
        entry.setArchivedAt(LocalDateTime.now());
        HandoverEntry saved = handoverEntryRepository.save(entry);

        support.addTimelineEvent(saved, TimelineEventType.ARCHIVED, "Handover archived", userId);
        return handoverEntryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID workspaceId, UUID handoverEntryId) {
        UUID userId = support.currentUserId();
        support.assertActiveWorkspaceMember(workspaceId, userId);

        HandoverEntry entry = findEntry(workspaceId, handoverEntryId);
        boolean isSender = entry.getSender().getId().equals(userId);
        boolean isAdmin = support.isWorkspaceAdminOrOwner(workspaceId, userId);
        if (!isSender && !isAdmin) {
            throw new ForbiddenException("You do not have permission to delete this handover.");
        }

        entry.setDeleted(true);
        handoverEntryRepository.save(entry);
        log.info("Handover soft-deleted [ID: {}] by [User: {}]", handoverEntryId, userId);
    }

    // ============================================================================
    // Helpers
    // ============================================================================

    private HandoverEntry findEntry(UUID workspaceId, UUID handoverEntryId) {
        return handoverEntryRepository.findByIdAndWorkspace(handoverEntryId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover not found."));
    }

    /**
     * Read guard for a single handover entry:
     * sender, receiver, workspace admin/owner, or a manager of the entry's department may view it.
     */
    private void assertCanViewEntry(HandoverEntry entry, UUID workspaceId, UUID userId) {
        boolean isSender = entry.getSender().getId().equals(userId);
        boolean isReceiver = entry.getReceiver() != null && entry.getReceiver().getId().equals(userId);
        if (isSender || isReceiver) {
            return;
        }
        if (support.isWorkspaceAdminOrOwner(workspaceId, userId)) {
            return;
        }
        if (support.isWorkspaceManager(workspaceId, userId)
                && support.currentUserDepartmentId().equals(entry.getDepartment().getId())) {
            return;
        }
        throw new ForbiddenException("You do not have permission to view this handover.");
    }

    private Project validateProject(UUID workspaceId, UUID departmentId, UUID projectId) {
        Project project = projectRepository.findByIdAndDepartment_Id(projectId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
        if (project.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Project not found.");
        }
        if (!project.getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Project not found.");
        }
        return project;
    }

    private void assertSender(HandoverEntry entry, UUID userId) {
        if (!entry.getSender().getId().equals(userId)) {
            throw new ForbiddenException("Only the sender can perform this action.");
        }
    }

    private void assertReceiver(HandoverEntry entry, UUID userId) {
        if (entry.getReceiver() == null) {
            throw new BadRequestException("This handover has no receiver.");
        }
        if (!entry.getReceiver().getId().equals(userId)) {
            throw new ForbiddenException("Only the receiver can perform this action.");
        }
    }
}
