package com.trio.backend.entity;

import com.trio.backend.entity.base.AuditableEntity;
import com.trio.backend.enums.InterviewStatus;
import com.trio.backend.enums.InterviewType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "hr_interviews",
        indexes = {
                @Index(name = "idx_hr_interviews_candidate_id", columnList = "candidate_id"),
                @Index(name = "idx_hr_interviews_scheduled_date", columnList = "scheduled_date"),
                @Index(name = "idx_hr_interviews_candidate_date", columnList = "candidate_id, scheduled_date"),
                @Index(name = "idx_hr_interviews_status", columnList = "status"),
                @Index(name = "idx_hr_interviews_department_status", columnList = "candidate_id, status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interview extends AuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private InterviewType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InterviewStatus status;

    @Size(max = 255)
    @Column(name = "title", length = 255)
    private String title;

    @Size(max = 150)
    @Column(name = "position", length = 150)
    private String position;

    @Size(max = 5000)
    @Column(name = "description", length = 5000)
    private String description;

    @Column(name = "scheduled_date")
    private Instant scheduledDate;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Size(max = 255)
    @Column(name = "location", length = 255)
    private String location;

    @Size(max = 500)
    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Size(max = 10000)
    @Column(name = "notes", length = 10000)
    private String notes;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Builder.Default
    @OneToMany(mappedBy = "interview", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewParticipant> participants = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "interview", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewFeedback> feedbacks = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (status == null) {
            status = InterviewStatus.SCHEDULED;
        }
    }
}
