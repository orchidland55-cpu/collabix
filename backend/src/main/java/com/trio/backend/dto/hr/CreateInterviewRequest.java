package com.trio.backend.dto.hr;

import com.trio.backend.enums.InterviewType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CreateInterviewRequest {

    @NotNull
    private InterviewType type;

    @Size(max = 255)
    private String title;

    @Size(max = 150)
    private String position;

    @Size(max = 5000)
    private String description;

    private Instant scheduledDate;

    @NotNull(message = "Start time is required.")
    private Instant startTime;

    @NotNull(message = "End time is required.")
    private Instant endTime;

    @Size(max = 255)
    private String location;

    @Size(max = 500)
    private String meetingLink;
}
