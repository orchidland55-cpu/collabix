package com.trio.backend.service;

import com.trio.backend.dto.communication.CreateMessageRequest;
import com.trio.backend.dto.communication.MessageResponse;
import com.trio.backend.dto.communication.UpdateMessageRequest;
import com.trio.backend.entity.*;
import com.trio.backend.entity.ids.ConversationMemberId;
import com.trio.backend.enums.MessageStatusEnum;
import com.trio.backend.enums.MessageType;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.MessageMapper;
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
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final MessageMapper messageMapper;
    private final NotificationService notificationService;

    @Override
    public MessageResponse create(UUID workspaceId, UUID conversationId, CreateMessageRequest request) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Conversation conversation = conversationRepository.findByIdAndWorkspace(conversationId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));

        // Public channels auto-join on first message; private ones require membership.
        ensureConversationMembership(conversation, userId);

        if (conversation.isArchived()) {
            throw new BadRequestException("Cannot send messages to an archived conversation.");
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Message message = messageMapper.toEntity(request);

        if (request.getMessageType() == null) {
            message.setMessageType(MessageType.TEXT);
        }

        message.setConversation(conversation);
        message.setSender(sender);
        message.setStatus(MessageStatusEnum.ACTIVE);

        if (request.getParentMessageId() != null) {
            Message parent = messageRepository.findById(request.getParentMessageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent message not found."));
            message.setParentMessage(parent);
        }

        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(Instant.now());
        String preview = saved.getContent();
        if (preview.length() > 200) {
            preview = preview.substring(0, 200);
        }
        conversation.setLastMessagePreview(preview);
        conversationRepository.save(conversation);

        notifyConversationMembers(workspaceId, conversationId, userId, saved);

        log.info("Message created: id={}, conversation={}, sender={}", saved.getId(), conversationId, userId);
        return messageMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageResponse getById(UUID workspaceId, UUID messageId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found."));

        if (!message.getConversation().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Message not found in this workspace.");
        }

        assertConversationVisible(message.getConversation().getId(), userId);

        return messageMapper.toResponse(message);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> listByConversation(UUID workspaceId, UUID conversationId, UUID cursor, Pageable pageable) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertConversationVisible(conversationId, userId);

        Page<Message> messages;
        if (cursor != null) {
            messages = messageRepository.findByConversationIdBeforeCursor(conversationId, cursor, pageable);
        } else {
            messages = messageRepository.findByConversationId(conversationId, pageable);
        }

        updateLastReadAt(conversationId, userId);

        return messages.map(messageMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> listBySender(UUID workspaceId, Pageable pageable) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        return messageRepository.findBySenderId(userId, workspaceId, pageable)
                .map(messageMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> listPinned(UUID workspaceId, UUID conversationId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertConversationVisible(conversationId, userId);

        return messageRepository.findPinnedMessages(conversationId)
                .stream().map(messageMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> listFilesByConversation(UUID workspaceId, UUID conversationId, Pageable pageable) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertConversationVisible(conversationId, userId);

        return messageRepository.findFilesByConversation(conversationId, pageable)
                .map(messageMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> listFilesByWorkspace(UUID workspaceId, Pageable pageable) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        return messageRepository.findFilesByWorkspace(workspaceId, pageable)
                .map(messageMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> search(UUID workspaceId, UUID conversationId, String query, Pageable pageable) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertConversationVisible(conversationId, userId);

        return messageRepository.searchMessages(conversationId, query, pageable)
                .map(messageMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> searchByWorkspace(UUID workspaceId, String query, Pageable pageable) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        return messageRepository.searchMessagesByWorkspace(workspaceId, query, pageable)
                .map(messageMapper::toResponse);
    }

    @Override
    public MessageResponse update(UUID workspaceId, UUID messageId, UpdateMessageRequest request) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found."));

        if (!message.getConversation().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Message not found in this workspace.");
        }

        if (!message.getSender().getId().equals(userId)) {
            throw new ForbiddenException("You can only edit your own messages.");
        }

        if (message.getStatus() != MessageStatusEnum.ACTIVE) {
            throw new BadRequestException("Cannot edit a deleted message.");
        }

        messageMapper.updateMessage(request, message);
        if (request.getContent() != null) {
            message.setContent(request.getContent());
        }
        message.setStatus(MessageStatusEnum.EDITED);

        Message saved = messageRepository.save(message);
        return messageMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID workspaceId, UUID messageId) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found."));

        if (!message.getConversation().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Message not found in this workspace.");
        }

        if (!message.getSender().getId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own messages.");
        }

        message.setStatus(MessageStatusEnum.DELETED);
        message.setContent("This message has been deleted.");
        messageRepository.save(message);
        log.info("Message deleted: id={}", messageId);
    }

    private void notifyConversationMembers(UUID workspaceId, UUID conversationId, UUID senderId, Message message) {
        try {
            List<ConversationMember> members = conversationMemberRepository.findMembersWithUser(conversationId);
            for (ConversationMember member : members) {
                if (!member.getUser().getId().equals(senderId)) {
                    CreateNotificationRequest notif = new CreateNotificationRequest();
                    notif.setWorkspaceId(workspaceId);
                    notif.setRecipientId(member.getUser().getId());
                    notif.setNotificationType(NotificationType.NEW_MESSAGE);
                    notif.setTitle("New message in " + message.getConversation().getName());
                    String body = message.getContent();
                    if (body.length() > 200) {
                        body = body.substring(0, 200);
                    }
                    notif.setBody(body);
                    notif.setLinkUrl("/communication/chat/" + conversationId);
                    notif.setResourceType("CONVERSATION");
                    notif.setResourceId(conversationId);
                    notificationService.create(workspaceId, notif);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to send notifications for message {}: {}", message.getId(), e.getMessage());
        }
    }

    private void updateLastReadAt(UUID conversationId, UUID userId) {
        conversationMemberRepository.findById_ConversationIdAndId_UserId(conversationId, userId)
                .ifPresent(member -> {
                    member.setLastReadAt(Instant.now());
                    conversationMemberRepository.save(member);
                });
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

    /** Lets any active workspace member send messages in public channels by auto-joining. */
    private void ensureConversationMembership(Conversation conversation, UUID userId) {
        if (conversationMemberRepository.existsById_ConversationIdAndId_UserId(conversation.getId(), userId)) {
            return;
        }
        if (conversation.isPrivate()) {
            throw new ForbiddenException("You are not a member of this conversation.");
        }
        ConversationMember member = ConversationMember.builder()
                .id(new ConversationMemberId(conversation.getId(), userId))
                .conversation(conversation)
                .user(userRepository.getReferenceById(userId))
                .joinedAt(Instant.now())
                .role("MEMBER")
                .build();
        conversationMemberRepository.save(member);
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

        if (wm.getStatus() != WorkspaceMemberStatus.ACTIVE) {
            throw new ForbiddenException("You are not an active member of this workspace.");
        }
    }
}
