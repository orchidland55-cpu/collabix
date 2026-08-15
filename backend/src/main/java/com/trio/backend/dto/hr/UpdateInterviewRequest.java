package com.trio.backend.dto.hr;

import com.trio.backend.enums.InterviewType;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UpdateInterviewRequest {

    private InterviewType type;

    @Size(max = 255)
    private String title;

    @Size(max = 150)
    private String position;

    @Size(max = 5000)
    private String description;

    private Instant scheduledDate;

    private Instant startTime;

    private Instant endTime;

    @Size(max = 255)
    private String location;

    @Size(max = 500)
    private String meetingLink;
}
