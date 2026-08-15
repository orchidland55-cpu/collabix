package com.trio.backend.dto.hr;

import com.trio.backend.enums.InterviewStatus;
import com.trio.backend.enums.InterviewType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class InterviewResponse {

    private UUID id;
    private UUID candidateId;
    private InterviewType type;
    private InterviewStatus status;
    private String title;
    private String position;
    private String description;
    private Instant scheduledDate;
    private Instant startTime;
    private Instant endTime;
    private String location;
    private String meetingLink;
    private String notes;
    private boolean archived;
    private List<InterviewParticipantResponse> participants;
    private List<InterviewFeedbackResponse> feedbacks;
    private Instant createdAt;
    private Instant updatedAt;
}
