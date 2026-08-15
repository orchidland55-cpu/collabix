package com.trio.backend.service.hr;

import com.trio.backend.dto.hr.AddParticipantRequest;
import com.trio.backend.dto.hr.CreateInterviewRequest;
import com.trio.backend.dto.hr.InterviewFeedbackRequest;
import com.trio.backend.dto.hr.InterviewFeedbackResponse;
import com.trio.backend.dto.hr.InterviewParticipantResponse;
import com.trio.backend.dto.hr.InterviewResponse;
import com.trio.backend.dto.hr.InterviewStatistics;
import com.trio.backend.dto.hr.UpdateInterviewRequest;
import com.trio.backend.dto.hr.UpdateInterviewNotesRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface InterviewService {

    InterviewResponse schedule(UUID workspaceId, UUID departmentId, UUID candidateId, CreateInterviewRequest request);

    InterviewResponse getById(UUID workspaceId, UUID departmentId, UUID candidateId, UUID interviewId);

    Page<InterviewResponse> listByCandidate(UUID workspaceId, UUID departmentId, UUID candidateId, Pageable pageable);

    InterviewResponse update(UUID workspaceId, UUID departmentId, UUID candidateId, UUID interviewId, UpdateInterviewRequest request);

    InterviewResponse updateNotes(UUID workspaceId, UUID departmentId, UUID candidateId, UUID interviewId, UpdateInterviewNotesRequest request);

    void cancel(UUID workspaceId, UUID departmentId, UUID candidateId, UUID interviewId);

    InterviewResponse complete(UUID workspaceId, UUID departmentId, UUID candidateId, UUID interviewId);

    void delete(UUID workspaceId, UUID departmentId, UUID candidateId, UUID interviewId);

    InterviewParticipantResponse addParticipant(UUID workspaceId, UUID departmentId, UUID candidateId, UUID interviewId, AddParticipantRequest request);

    void removeParticipant(UUID workspaceId, UUID departmentId, UUID candidateId, UUID interviewId, UUID participantId);

    List<InterviewParticipantResponse> listParticipants(UUID workspaceId, UUID departmentId, UUID candidateId, UUID interviewId);

    InterviewFeedbackResponse submitFeedback(UUID workspaceId, UUID departmentId, UUID candidateId, UUID interviewId, InterviewFeedbackRequest request);

    List<InterviewFeedbackResponse> getFeedbacks(UUID workspaceId, UUID departmentId, UUID candidateId, UUID interviewId);

    List<InterviewResponse> getTodayInterviews(UUID workspaceId, UUID departmentId);

    List<InterviewResponse> getThisWeekInterviews(UUID workspaceId, UUID departmentId);

    List<InterviewResponse> getUpcomingInterviews(UUID workspaceId, UUID departmentId);

    List<InterviewResponse> getCompletedInterviews(UUID workspaceId, UUID departmentId);

    InterviewStatistics getStatistics(UUID workspaceId, UUID departmentId);
}
