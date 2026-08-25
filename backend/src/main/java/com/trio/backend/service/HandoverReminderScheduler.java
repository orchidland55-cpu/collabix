package com.trio.backend.service;

import com.trio.backend.dto.notification.CreateNotificationRequest;
import com.trio.backend.entity.HandoverEntry;
import com.trio.backend.entity.HandoverTimelineEvent;
import com.trio.backend.entity.HandoverTimelineEvent.TimelineEventType;
import com.trio.backend.entity.Notification;
import com.trio.backend.entity.Workspace;
import com.trio.backend.repository.HandoverEntryRepository;
import com.trio.backend.repository.HandoverTimelineEventRepository;
import com.trio.backend.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Sends reminders to receivers of PENDING handovers that are due soon or overdue.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HandoverReminderScheduler {

    private static final long REMINDER_COOLDOWN_HOURS = 12;

    private final WorkspaceRepository workspaceRepository;
    private final HandoverEntryRepository handoverEntryRepository;
    private final HandoverTimelineEventRepository timelineEventRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void sendDueHandoverReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusHours(2);
        Instant cooldownSince = now.minusHours(REMINDER_COOLDOWN_HOURS).atZone(ZoneId.systemDefault()).toInstant();

        List<Workspace> activeWorkspaces = workspaceRepository.findAllActive();
        int sent = 0;

        for (Workspace workspace : activeWorkspaces) {
            UUID wsId = workspace.getId();
            List<HandoverEntry> dueSoon = handoverEntryRepository.findPendingDueBefore(wsId, soon);

            for (HandoverEntry entry : dueSoon) {
                if (timelineEventRepository.existsReminderSentSince(entry.getId(), cooldownSince)) {
                    continue;
                }
                sendReminder(wsId, entry);
                sent++;
            }
        }

        if (sent > 0) {
            log.info("Sent {} handover due reminders", sent);
        }
    }

    private void sendReminder(UUID workspaceId, HandoverEntry entry) {
        CreateNotificationRequest req = new CreateNotificationRequest();
        req.setWorkspaceId(workspaceId);
        req.setRecipientId(entry.getReceiver().getId());
        req.setNotificationType(Notification.NotificationType.HANDOVER_REMINDER);
        req.setTitle("Handover due: " + entry.getTitle());
        req.setBody("You have a handover due on " + entry.getDueDate() + ". Please review it.");
        req.setLinkUrl("/handovers/" + entry.getId());
        req.setResourceType("HANDOVER");
        req.setResourceId(entry.getId());
        req.setHandoverEntryId(entry.getId());
        notificationService.create(workspaceId, req);

        HandoverTimelineEvent event = HandoverTimelineEvent.builder()
                .handoverEntry(entry)
                .eventType(TimelineEventType.REMINDER_SENT)
                .description("Reminder sent for due handover")
                .build();
        timelineEventRepository.save(event);

        log.debug("Sent due-handover reminder to user {} in workspace {}", entry.getReceiver().getId(), workspaceId);
    }
}
