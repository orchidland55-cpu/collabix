package com.trio.backend.service;

import com.trio.backend.dto.communication.ConversationMemberResponse;
import com.trio.backend.dto.communication.ConversationResponse;
import com.trio.backend.dto.communication.CreateConversationRequest;
import com.trio.backend.dto.communication.UpdateConversationRequest;
import com.trio.backend.entity.*;
import com.trio.backend.entity.ids.ConversationMemberId;
import com.trio.backend.enums.ConversationType;
import com.trio.backend.enums.MessageStatusEnum;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.ConversationMapper;
import com.trio.backend.repository.*;
import com.trio.backend.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.trio.backend.dto.notification.CreateNotificationRequest;
import com.trio.backend.entity.Notification.NotificationType;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ConversationMapper conversationMapper;
    private final NotificationService notificationService;

    @Override
    public ConversationResponse create(UUID workspaceId, CreateConversationRequest request) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));

        if (request.getType() == ConversationType.DIRECT) {
            return createDirectConversation(workspace, userId, request);
        }

        Conversation conversation = conversationMapper.toEntity(request);
        conversation.setWorkspace(workspace);

        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found."));
            conversation.setDepartment(dept);
        }

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found."));
            conversation.setTeam(team);
        }

        Conversation saved = conversationRepository.save(conversation);

        addMemberToConversation(saved, userId, "ADMIN");

        if (request.getMemberIds() != null) {
            for (UUID memberId : request.getMemberIds()) {
                if (!memberId.equals(userId)) {
                    assertActiveWorkspaceMember(workspaceId, memberId);
                    addMemberToConversation(saved, memberId, "MEMBER");
                }
            }
        }

        log.info("Conversation created: id={}, name={}, type={}, workspace={}", saved.getId(), saved.getName(), request.getType(), workspaceId);
        return mapWithCounts(saved);
    }

    private ConversationResponse createDirectConversation(Workspace workspace, UUID creatorId, CreateConversationRequest request) {
        if (request.getMemberIds() == null || request.getMemberIds().size() != 1) {
            throw new BadRequestException("Direct conversation requires exactly one other member.");
        }

        UUID otherUserId = request.getMemberIds().iterator().next();

        if (otherUserId.equals(creatorId)) {
            throw new BadRequestException("Cannot create a direct conversation with yourself.");
        }

        boolean exists = conversationRepository.findUserDirectConversations(creatorId, workspace.getId())
                .stream().anyMatch(c -> {
                    List<ConversationMember> members = conversationMemberRepository.findById_ConversationId(c.getId());
                    return members.stream().anyMatch(m -> m.getUser().getId().equals(otherUserId));
                });

        if (exists) {
            throw new BadRequestException("A direct conversation with this user already exists.");
        }

        String creatorName = userRepository.findById(creatorId)
                .map(u -> u.getFirstName())
                .orElse("Unknown");
        String otherName = userRepository.findById(otherUserId)
                .map(u -> u.getFirstName())
                .orElse("Unknown");
        String name = creatorName + ", " + otherName;

        Conversation conversation = new Conversation();
        conversation.setWorkspace(workspace);
        conversation.setName(name);
        conversation.setType(ConversationType.DIRECT);
        conversation.setPrivate(true);

        Conversation saved = conversationRepository.save(conversation);

        addMemberToConversation(saved, creatorId, "MEMBER");
        addMemberToConversation(saved, otherUserId, "MEMBER");

        return mapWithCounts(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getById(UUID workspaceId, UUID conversationId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertConversationVisible(conversationId, userId);

        Conversation conversation = conversationRepository.findByIdAndWorkspace(conversationId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        return mapWithCounts(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> listUserConversations(UUID workspaceId, Pageable pageable) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        return conversationRepository.findUserConversations(userId, workspaceId, pageable)
                .map(this::mapWithCounts);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> listByType(UUID workspaceId, String type, Pageable pageable) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        ConversationType conversationType;
        try {
            conversationType = ConversationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid conversation type: " + type);
        }

        return conversationRepository.findByWorkspaceAndType(workspaceId, conversationType, pageable)
                .map(this::mapWithCounts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> listWorkspaceDefaults(UUID workspaceId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        return conversationRepository.findWorkspaceDefaultConversations(workspaceId)
                .stream().map(this::mapWithCounts).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> listDirectConversations(UUID workspaceId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        return conversationRepository.findUserDirectConversations(userId, workspaceId)
                .stream().map(this::mapWithCounts).toList();
    }

    @Override
    public ConversationResponse update(UUID workspaceId, UUID conversationId, UpdateConversationRequest request) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertConversationAdmin(conversationId, userId);

        Conversation conversation = conversationRepository.findByIdAndWorkspace(conversationId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        conversationMapper.updateConversation(request, conversation);
        Conversation saved = conversationRepository.save(conversation);
        return mapWithCounts(saved);
    }

    @Override
    public void archive(UUID workspaceId, UUID conversationId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertConversationAdmin(conversationId, userId);

        Conversation conversation = conversationRepository.findByIdAndWorkspace(conversationId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        if (conversation.isArchived()) {
            return;
        }

        conversation.setArchived(true);
        conversationRepository.save(conversation);
        log.info("Conversation archived: id={}", conversationId);
    }

    @Override
    public void delete(UUID workspaceId, UUID conversationId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Conversation conversation = conversationRepository.findByIdAndWorkspace(conversationId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        conversationRepository.delete(conversation);
        log.info("Conversation deleted: id={}", conversationId);
    }

    @Override
    public ConversationResponse addMember(UUID workspaceId, UUID conversationId, UUID memberUserId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertConversationAdmin(conversationId, userId);
        assertActiveWorkspaceMember(workspaceId, memberUserId);

        Conversation conversation = conversationRepository.findByIdAndWorkspace(conversationId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        if (conversationMemberRepository.existsById_ConversationIdAndId_UserId(conversationId, memberUserId)) {
            throw new BadRequestException("User is already a member of this conversation.");
        }

        addMemberToConversation(conversation, memberUserId, "MEMBER");

        try {
            CreateNotificationRequest notif = new CreateNotificationRequest();
            notif.setWorkspaceId(workspaceId);
            notif.setRecipientId(memberUserId);
            notif.setNotificationType(NotificationType.CHANNEL_INVITE);
            notif.setTitle("You were added to #" + conversation.getName());
            notif.setBody("You have been added to the " + conversation.getType().name().toLowerCase() + " channel.");
            notif.setLinkUrl("/communication/chat/" + conversationId);
            notif.setResourceType("CONVERSATION");
            notif.setResourceId(conversationId);
            notificationService.create(workspaceId, notif);
        } catch (Exception e) {
            log.warn("Failed to send channel invite notification: {}", e.getMessage());
        }

        return mapWithCounts(conversation);
    }

    @Override
    public void removeMember(UUID workspaceId, UUID conversationId, UUID memberUserId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertConversationAdmin(conversationId, userId);

        ConversationMember member = conversationMemberRepository
                .findById_ConversationIdAndId_UserId(conversationId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in conversation."));

        conversationMemberRepository.delete(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationMemberResponse> listMembers(UUID workspaceId, UUID conversationId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertConversationVisible(conversationId, userId);

        return conversationMemberRepository.findMembersWithUser(conversationId)
                .stream().map(conversationMapper::toMemberResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID workspaceId, UUID conversationId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        ConversationMember member = conversationMemberRepository
                .findById_ConversationIdAndId_UserId(conversationId, userId)
                .orElse(null);

        if (member == null) {
            // Public channels are readable without joining; no unread tracking yet.
            assertConversationVisible(conversationId, userId);
            return 0;
        }

        Instant lastReadAt = member.getLastReadAt();
        if (lastReadAt == null) {
            return messageRepository.countByConversationIdAndStatus(conversationId, MessageStatusEnum.ACTIVE);
        }

        return messageRepository.countUnreadSince(conversationId, lastReadAt);
    }

    private ConversationResponse mapWithCounts(Conversation conversation) {
        ConversationResponse response = conversationMapper.toResponse(conversation);
        response.setMemberCount(conversationMemberRepository.countByConversationId(conversation.getId()));
        try {
            UUID currentUserId = getAuthenticatedUserId();
            conversationMemberRepository.findById_ConversationIdAndId_UserId(conversation.getId(), currentUserId)
                    .ifPresent(member -> {
                        Instant lastReadAt = member.getLastReadAt();
                        if (lastReadAt == null) {
                            response.setUnreadCount(messageRepository.countByConversationIdAndStatus(
                                    conversation.getId(), MessageStatusEnum.ACTIVE));
                        } else {
                            response.setUnreadCount(messageRepository.countUnreadSince(
                                    conversation.getId(), lastReadAt));
                        }
                    });
        } catch (Exception e) {
            response.setUnreadCount(0);
        }
        return response;
    }

    private void addMemberToConversation(Conversation conversation, UUID userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        ConversationMemberId memberId = new ConversationMemberId(conversation.getId(), userId);
        ConversationMember member = ConversationMember.builder()
                .id(memberId)
                .conversation(conversation)
                .user(user)
                .joinedAt(Instant.now())
                .role(role)
                .build();
        conversationMemberRepository.save(member);
    }

    private void assertConversationMember(UUID conversationId, UUID userId) {
        if (!conversationMemberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId)) {
            throw new ForbiddenException("You are not a member of this conversation.");
        }
    }

    /**
     * Members always have access. Public (non-private) channels are also readable
     * by every active workspace member, even before joining.
     */
    private void assertConversationVisible(UUID conversationId, UUID userId) {
        if (conversationMemberRepository.existsById_ConversationIdAndId_UserId(conversationId, userId)) {
            return;
        }
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));
        if (conversation.isPrivate()) {
            throw new ForbiddenException("You are not a member of this conversation.");
        }
    }

    private void assertConversationAdmin(UUID conversationId, UUID userId) {
        ConversationMember member = conversationMemberRepository
                .findById_ConversationIdAndId_UserId(conversationId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this conversation."));

        if (!"ADMIN".equals(member.getRole())) {
            throw new ForbiddenException("You are not an admin of this conversation.");
        }
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

    private CustomUserDetails getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails main)) {
            throw new BadRequestException("User is not authenticated.");
        }
        return main;
    }

    private void assertActiveWorkspaceMember(UUID workspaceId, UUID userId) {
        WorkspaceMember wm = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace."));

        if (wm.getStatus() != WorkspaceMemberStatus.ACTIVE) {
            throw new ForbiddenException("You are not an active member of this workspace.");
        }
    }
}
