package com.trio.backend.controller;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.organisation.handover.HandoverJournalResponse;
import com.trio.backend.entity.HandoverEntry;
import com.trio.backend.service.HandoverJournalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Department-scoped Handover Journal endpoints available to every workspace role.
 *
 * <p>Access rules (enforced in {@code HandoverJournalServiceImpl} via {@code HandoverSupport}):</p>
 * <ul>
 *     <li>Workspace ADMIN/OWNER can list/view journals of any department.</li>
 *     <li>Managers and Members can only list/view journals of their own primary department;
 *         any cross-department request returns 403.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/handover-journals")
@RequiredArgsConstructor
@Tag(name = "Handover Journals (Accessible)", description = "Department-scoped Handover Journal read API")
@SecurityRequirement(name = "bearer")
public class HandoverJournalAccessController {

    private final HandoverJournalService handoverJournalService;

    @Operation(
            summary = "List handover journals accessible to the caller",
            description = "Admin/Owner may scope by department; Managers and Members are auto-scoped to their own department."
    )
    @GetMapping
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_READ')")
    public ApiResponse<Page<HandoverJournalResponse>> listJournals(
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) HandoverEntry.Shift shift,
            @RequestParam(required = false) LocalDate date,
            Pageable pageable) {
        return ApiResponse.success("Handover journals retrieved successfully.",
                handoverJournalService.listAccessible(workspaceId, departmentId, projectId, shift, date, pageable));
    }

    @Operation(
            summary = "Get a handover journal by id (department-scoped)",
            description = "Returns the journal only if the caller is an admin/owner or belongs to the journal's department."
    )
    @GetMapping("/{handoverJournalId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'HANDOVER_READ')")
    public ApiResponse<HandoverJournalResponse> getJournalById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID handoverJournalId) {
        return ApiResponse.success("Handover journal retrieved successfully.",
                handoverJournalService.getByIdAccessible(workspaceId, handoverJournalId));
    }
}
