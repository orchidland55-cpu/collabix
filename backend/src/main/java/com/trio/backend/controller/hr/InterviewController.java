package com.trio.backend.controller.hr;

import com.trio.backend.common.ApiResponse;
import com.trio.backend.dto.hr.AddParticipantRequest;
import com.trio.backend.dto.hr.CreateInterviewRequest;
import com.trio.backend.dto.hr.InterviewFeedbackRequest;
import com.trio.backend.dto.hr.InterviewFeedbackResponse;
import com.trio.backend.dto.hr.InterviewParticipantResponse;
import com.trio.backend.dto.hr.InterviewResponse;
import com.trio.backend.dto.hr.InterviewStatistics;
import com.trio.backend.dto.hr.UpdateInterviewNotesRequest;
import com.trio.backend.dto.hr.UpdateInterviewRequest;
import com.trio.backend.service.hr.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/departments/{departmentId}/candidates/{candidateId}/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_CREATE')")
    public ApiResponse<InterviewResponse> schedule(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @Valid @RequestBody CreateInterviewRequest request) {
        return ApiResponse.success("Interview scheduled successfully.",
                interviewService.schedule(workspaceId, departmentId, candidateId, request));
    }

    @GetMapping("/{interviewId}")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_READ')")
    public ApiResponse<InterviewResponse> getById(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId) {
        return ApiResponse.success("Interview resorteved successfully.",
                interviewService.getById(workspaceId, departmentId, candidateId, interviewId));
    }

    @GetMapping
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_READ')")
    public ApiResponse<Page<InterviewResponse>> listByCandidate(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success("Interviews resorteved successfully.",
                interviewService.listByCandidate(workspaceId, departmentId, candidateId, pageable));
    }

    @PutMapping("/{interviewId}")
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_UPDATE')")
    public ApiResponse<InterviewResponse> update(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId,
            @Valid @RequestBody UpdateInterviewRequest request) {
        return ApiResponse.success("Interview updated successfully.",
                interviewService.update(workspaceId, departmentId, candidateId, interviewId, request));
    }

    @PutMapping("/{interviewId}/notes")
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_UPDATE')")
    public ApiResponse<InterviewResponse> updateNotes(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId,
            @Valid @RequestBody UpdateInterviewNotesRequest request) {
        return ApiResponse.success("Interview notes saved successfully.",
                interviewService.updateNotes(workspaceId, departmentId, candidateId, interviewId, request));
    }

    @PostMapping("/{interviewId}/cancel")
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_CANCEL')")
    public ApiResponse<InterviewResponse> cancel(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId) {
        interviewService.cancel(workspaceId, departmentId, candidateId, interviewId);
        return ApiResponse.success("Interview cancelled successfully.");
    }

    @PostMapping("/{interviewId}/complete")
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_UPDATE')")
    public ApiResponse<InterviewResponse> complete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId) {
        return ApiResponse.success("Interview completed successfully.",
                interviewService.complete(workspaceId, departmentId, candidateId, interviewId));
    }

    @DeleteMapping("/{interviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_DELETE')")
    public void delete(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId) {
        interviewService.delete(workspaceId, departmentId, candidateId, interviewId);
    }

    @PostMapping("/{interviewId}/participants")
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_CREATE')")
    public ApiResponse<InterviewParticipantResponse> addParticipant(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId,
            @Valid @RequestBody AddParticipantRequest request) {
        return ApiResponse.success("Participant added successfully.",
                interviewService.addParticipant(workspaceId, departmentId, candidateId, interviewId, request));
    }

    @DeleteMapping("/{interviewId}/participants/{participantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_DELETE')")
    public void removeParticipant(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId,
            @PathVariable UUID participantId) {
        interviewService.removeParticipant(workspaceId, departmentId, candidateId, interviewId, participantId);
    }

    @GetMapping("/{interviewId}/participants")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_READ')")
    public ApiResponse<List<InterviewParticipantResponse>> listParticipants(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId) {
        return ApiResponse.success("Participants resorteved successfully.",
                interviewService.listParticipants(workspaceId, departmentId, candidateId, interviewId));
    }

    @PostMapping("/{interviewId}/feedback")
    @PreAuthorize("@workspaceAuth.canManageDepartmentHR(#workspaceId, #departmentId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_CREATE')")
    public ApiResponse<InterviewFeedbackResponse> submitFeedback(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId,
            @Valid @RequestBody InterviewFeedbackRequest request) {
        return ApiResponse.success("Feedback submitted successfully.",
                interviewService.submitFeedback(workspaceId, departmentId, candidateId, interviewId, request));
    }

    @GetMapping("/{interviewId}/feedback")
    @PreAuthorize("@workspaceAuth.canViewWorkspace(#workspaceId, authentication) && @permissionEvaluator.hasPermission(authentication, 'INTERVIEW_READ')")
    public ApiResponse<List<InterviewFeedbackResponse>> getFeedbacks(
            @PathVariable UUID workspaceId,
            @PathVariable UUID departmentId,
            @PathVariable UUID candidateId,
            @PathVariable UUID interviewId) {
        return ApiResponse.success("Feedbacks resorteved successfully.",
                interviewService.getFeedbacks(workspaceId, departmentId, candidateId, interviewId));
    }
}
