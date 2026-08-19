package com.trio.backend.repository;

import com.trio.backend.entity.InterviewParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewParticipantRepository extends JpaRepository<InterviewParticipant, UUID> {

    List<InterviewParticipant> findAllByInterview_Id(UUID interviewId);

    List<InterviewParticipant> findAllByUser_Id(UUID userId);

    void deleteByUser_Id(UUID userId);

    boolean existsByInterview_IdAndUser_Id(UUID interviewId, UUID userId);
}
