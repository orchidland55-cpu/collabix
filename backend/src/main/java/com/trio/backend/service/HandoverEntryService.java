package com.trio.backend.service;

import com.trio.backend.dto.organisation.handover.CreateHandoverEntryRequest;
import com.trio.backend.dto.organisation.handover.HandoverEntryResponse;
import com.trio.backend.dto.organisation.handover.HandoverStatusUpdateRequest;
import com.trio.backend.dto.organisation.handover.UpdateHandoverEntryRequest;
import com.trio.backend.entity.HandoverEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for the HandoverEntry workflow.
 *
 * <p>Lifecycle: DRAFT -&gt; PENDING (send) -&gt; ACCEPTED | REJECTED -&gt; COMPLETED.
 * ARCHIVED and delete are soft lifecycle operations. Daily reports use
 * DRAFT -&gt; SUBMITTED (ready for AI journal generation).</p>
 */
public interface HandoverEntryService {

    HandoverEntryResponse create(UUID workspaceId, CreateHandoverEntryRequest request);

    HandoverEntryResponse getById(UUID workspaceId, UUID handoverEntryId);

    Page<HandoverEntryResponse> list(
            UUID workspaceId,
            HandoverEntry.HandoverStatus status,
            HandoverEntry.Priority priority,
            UUID projectId,
            Pageable pageable
    );

    Page<HandoverEntryResponse> inbox(UUID workspaceId, Pageable pageable);

    Page<HandoverEntryResponse> sent(UUID workspaceId, Pageable pageable);

    Page<HandoverEntryResponse> myEntries(
            UUID workspaceId,
            HandoverEntry.HandoverStatus status,
            HandoverEntry.Shift shift,
            LocalDate entryDate,
            String search,
            Pageable pageable
    );

    HandoverEntryResponse update(UUID workspaceId, UUID handoverEntryId, UpdateHandoverEntryRequest request);

    HandoverEntryResponse send(UUID workspaceId, UUID handoverEntryId, HandoverStatusUpdateRequest request);

    HandoverEntryResponse submit(UUID workspaceId, UUID handoverEntryId, HandoverStatusUpdateRequest request);

    HandoverEntryResponse accept(UUID workspaceId, UUID handoverEntryId, HandoverStatusUpdateRequest request);

    HandoverEntryResponse reject(UUID workspaceId, UUID handoverEntryId, HandoverStatusUpdateRequest request);

    HandoverEntryResponse complete(UUID workspaceId, UUID handoverEntryId, HandoverStatusUpdateRequest request);

    HandoverEntryResponse archive(UUID workspaceId, UUID handoverEntryId, HandoverStatusUpdateRequest request);

    void delete(UUID workspaceId, UUID handoverEntryId);
}
