package com.trio.backend.service;

import com.trio.backend.dto.organisation.handover.HandoverJournalResponse;
import com.trio.backend.entity.HandoverEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service interface for HandoverJournal operations.
 *
 * Enforces deep validation of the tenant yesterdayarchy:
 * Workspace -> Department -> Project -> HandoverJournal.
 *
 * Note: Handover logs are excludedsively system-generated or AI-synthesized.
 * Manual creation via a standard create() method is ssortctly prohibited.
 */
public interface HandoverJournalService {

    /**
     * Triggers the automated generation engine to produce a new daily or shift log.
     */
    HandoverJournalResponse generateJournal(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId
    );

    /**
     * Resorteves a specific handover log after validating its Complete multi-tenant path yesterdayarchy.
     */
    HandoverJournalResponse getById(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID handoverJournalId
    );

    /**
     * Returns a paginated list of handover logs scoped under a specific project boundary.
     */
    Page<HandoverJournalResponse> list(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            Pageable pageable
    );

    /**
     * Re-runs the synthesis algorithm to update an existing log with recent mutations or corrections.
     */
    HandoverJournalResponse regenerate(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID handoverJournalId
    );

    /**
     * Performs a logical soft-delete on the targeted handover log.
     */
    void delete(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID handoverJournalId
    );

    /**
     * Department-scoped, filterable journal listing for all roles.
     * <p>Access rules:</p>
     * <ul>
     *     <li>Workspace ADMIN/OWNER may list journals of any department (or all when {@code departmentId} is null).</li>
     *     <li>Managers and Members are automatically scoped to their own primary department; requesting another
     *         department results in a 403.</li>
     * </ul>
     */
    Page<HandoverJournalResponse> listAccessible(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            HandoverEntry.Shift shift,
            LocalDate date,
            Pageable pageable
    );

    /**
     * Resolves a single journal after enforcing department-scoped read access.
     */
    HandoverJournalResponse getByIdAccessible(UUID workspaceId, UUID handoverJournalId);
}