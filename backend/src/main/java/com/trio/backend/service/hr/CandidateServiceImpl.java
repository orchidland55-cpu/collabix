package com.trio.backend.service.hr;

import com.trio.backend.dto.hr.CandidateResponse;
import com.trio.backend.dto.hr.CandidateSearchCriteria;
import com.trio.backend.dto.hr.CandidateStatistics;
import com.trio.backend.dto.hr.CandidateStatusChangeRequest;
import com.trio.backend.dto.hr.CandidateTimelineEntry;
import com.trio.backend.dto.hr.CreateCandidateRequest;
import com.trio.backend.dto.hr.UpdateCandidateRequest;
import com.trio.backend.entity.Candidate;
import com.trio.backend.entity.CandidateAttachment;
import com.trio.backend.entity.CandidateStatusHistory;
import com.trio.backend.entity.Department;
import com.trio.backend.entity.Interview;
import com.trio.backend.entity.RecruiterNote;
import com.trio.backend.enums.CandidateStatus;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ConflictException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.CandidateMapper;
import com.trio.backend.repository.CandidateAttachmentRepository;
import com.trio.backend.repository.CandidateRepository;
import com.trio.backend.repository.CandidateSpecification;
import com.trio.backend.repository.CandidateStatusHistoryRepository;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.InterviewRepository;
import com.trio.backend.repository.RecruiterNoteRepository;
import com.trio.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final CandidateStatusHistoryRepository candidateStatusHistoryRepository;
    private final DepartmentRepository departmentRepository;
    private final RecruiterNoteRepository recruiterNoteRepository;
    private final InterviewRepository interviewRepository;
    private final CandidateAttachmentRepository candidateAttachmentRepository;
    private final CandidateMapper candidateMapper;

    @Override
    public CandidateResponse create(UUID workspaceId, UUID departmentId, CreateCandidateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Department department = findActiveDepartment(workspaceId, departmentId);

        if (candidateRepository.existsByDepartment_IdAndEmailAndArchivedFalse(departmentId, request.getEmail())) {
            throw new ConflictException("A candidate with this email already exists in this department.");
        }

        Optional<Candidate> archivedCandidate = candidateRepository.findByDepartment_IdAndEmail(departmentId, request.getEmail());
        if (archivedCandidate.isPresent()) {
            Candidate revived = archivedCandidate.get();
            revived.setArchived(false);
            revived.setFirstName(request.getFirstName());
            revived.setLastName(request.getLastName());
            revived.setPhone(request.getPhone());
            revived.setPosition(request.getPosition());
            revived.setSource(request.getSource());
            revived.setCurrentStatus(CandidateStatus.APPLIED);
            revived.setRecruiterId(userId);
            Candidate saved = candidateRepository.save(revived);
            log.info("Candidate revived: {} {} (id={}) by user {}", saved.getFirstName(), saved.getLastName(), saved.getId(), userId);
            return candidateMapper.toResponse(saved);
        }

        Candidate candidate = Candidate.builder()
                .department(department)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .position(request.getPosition())
                .source(request.getSource())
                .currentStatus(CandidateStatus.APPLIED)
                .recruiterId(userId)
                .archived(false)
                .build();

        Candidate saved = candidateRepository.save(candidate);
        log.info("Candidate created: {} {} (id={}) by user {}", request.getFirstName(), request.getLastName(), saved.getId(), userId);
        return candidateMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateResponse getById(UUID workspaceId, UUID departmentId, UUID candidateId) {
        SecurityUtils.getCurrentUserId();

        Candidate candidate = findActiveCandidate(workspaceId, departmentId, candidateId);
        return candidateMapper.toResponse(candidate);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateResponse> list(UUID workspaceId, UUID departmentId, CandidateSearchCriteria criteria, Pageable pageable) {
        SecurityUtils.getCurrentUserId();

        findActiveDepartment(workspaceId, departmentId);

        return candidateRepository.findAll(
                        CandidateSpecification.withFilter(departmentId, criteria), pageable)
                .map(candidateMapper::toResponse);
    }

    @Override
    public CandidateResponse update(UUID workspaceId, UUID departmentId, UUID candidateId, UpdateCandidateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Candidate candidate = findActiveCandidate(workspaceId, departmentId, candidateId);

        if (request.getFirstName() != null) {
            candidate.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            candidate.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            String newEmail = request.getEmail();
            if (!newEmail.equals(candidate.getEmail())
                    && candidateRepository.existsByDepartment_IdAndEmailAndArchivedFalse(departmentId, newEmail)) {
                throw new ConflictException("A candidate with this email already exists in this department.");
            }
            candidate.setEmail(newEmail);
        }
        if (request.getPhone() != null) {
            candidate.setPhone(request.getPhone());
        }
        if (request.getPosition() != null) {
            candidate.setPosition(request.getPosition());
        }
        if (request.getSource() != null) {
            candidate.setSource(request.getSource());
        }

        Candidate saved = candidateRepository.save(candidate);
        log.info("Candidate updated: {} {} (id={}) by user {}", saved.getFirstName(), saved.getLastName(), saved.getId(), userId);
        return candidateMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID workspaceId, UUID departmentId, UUID candidateId) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Candidate candidate = findActiveCandidate(workspaceId, departmentId, candidateId);

        candidate.setArchived(true);
        candidateRepository.save(candidate);
        log.info("Candidate archived: {} {} (id={}) by user {}", candidate.getFirstName(), candidate.getLastName(), candidateId, userId);
    }

    @Override
    public CandidateResponse changeStatus(UUID workspaceId, UUID departmentId, UUID candidateId, CandidateStatusChangeRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Candidate candidate = findActiveCandidate(workspaceId, departmentId, candidateId);

        CandidateStatus currentStatus = candidate.getCurrentStatus();
        CandidateStatus newStatus = request.getNewStatus();

        if (currentStatus == newStatus) {
            throw new BadRequestException("Candidate is already in status: " + newStatus);
        }

        CandidateStatusHistory history = CandidateStatusHistory.builder()
                .candidate(candidate)
                .previousStatus(currentStatus)
                .newStatus(newStatus)
                .changedBy(userId)
                .reason(request.getReason())
                .build();

        candidate.setCurrentStatus(newStatus);

        candidate.getStatusHistories().add(history);
        Candidate saved = candidateRepository.save(candidate);
        log.info("Candidate status changed: {} {} (id={}) from {} to {} by user {}",
                saved.getFirstName(), saved.getLastName(), candidateId, currentStatus, newStatus, userId);

        return candidateMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidateTimelineEntry> getTimeline(UUID workspaceId, UUID departmentId, UUID candidateId) {
        SecurityUtils.getCurrentUserId();

        findActiveCandidate(workspaceId, departmentId, candidateId);

        List<CandidateTimelineEntry> ensortes = new ArrayList<>();

        List<CandidateStatusHistory> statusHistories =
                candidateStatusHistoryRepository.findAllByCandidate_IdOrderByCreatedAtDesc(candidateId);
        for (CandidateStatusHistory h : statusHistories) {
            CandidateTimelineEntry entry = new CandidateTimelineEntry();
            entry.setId(h.getId());
            entry.setEventType("STATUS_CHANGE");
            entry.setTitle("Status changed");
            entry.setDescription(h.getPreviousStatus() + " â†’ " + h.getNewStatus()
                    + (h.getReason() != null ? ": " + h.getReason() : ""));
            entry.setOccurredAt(h.getCreatedAt());
            entry.setActorId(h.getChangedBy());
            ensortes.add(entry);
        }

        List<RecruiterNote> notes = recruiterNoteRepository
                .findAllByCandidate_IdOrderByCreatedAtDesc(candidateId, Pageable.unpaged()).getContent();
        for (RecruiterNote n : notes) {
            CandidateTimelineEntry entry = new CandidateTimelineEntry();
            entry.setId(n.getId());
            entry.setEventType(n.getCategory() != null ? "NOTE_" + n.getCategory().name() : "NOTE");
            entry.setTitle(n.getTitle());
            entry.setDescription(n.getContent());
            entry.setOccurredAt(n.getCreatedAt());
            entry.setActorId(n.getCreatedBy());
            ensortes.add(entry);
        }

        List<Interview> interviews = interviewRepository.findAllByCandidate_IdOrderByCreatedAtDesc(candidateId);
        for (Interview i : interviews) {
            CandidateTimelineEntry entry = new CandidateTimelineEntry();
            entry.setId(i.getId());
            entry.setEventType("INTERVIEW");
            entry.setTitle(i.getType().name());
            entry.setDescription(i.getStatus().name() + (i.getScheduledDate() != null ? " on " + i.getScheduledDate() : ""));
            entry.setOccurredAt(i.getCreatedAt());
            entry.setActorId(i.getCreatedBy());
            ensortes.add(entry);
        }

        List<CandidateAttachment> attachments = candidateAttachmentRepository
                .findAllByCandidate_IdOrderByCreatedAtDesc(candidateId, Pageable.unpaged()).getContent();
        for (CandidateAttachment a : attachments) {
            CandidateTimelineEntry entry = new CandidateTimelineEntry();
            entry.setId(a.getId());
            entry.setEventType("ATTACHMENT_" + a.getAttachmentType().name());
            entry.setTitle(a.getOriginalFileName());
            entry.setDescription(a.getDescription() != null ? a.getDescription() : a.getAttachmentType().name());
            entry.setOccurredAt(a.getCreatedAt());
            entry.setActorId(a.getUploadedBy());
            ensortes.add(entry);
        }

        ensortes.sort((a, b) -> b.getOccurredAt().compareTo(a.getOccurredAt()));
        return ensortes;
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateStatistics getStatistics(UUID workspaceId, UUID departmentId) {
        SecurityUtils.getCurrentUserId();

        findActiveDepartment(workspaceId, departmentId);

        CandidateStatistics stats = new CandidateStatistics();
        stats.setTotalCandidates(candidateRepository.countByDepartment_Id(departmentId));
        stats.setHiredCount(candidateRepository.countHiredByDepartmentId(departmentId));
        stats.setRejectedCount(candidateRepository.countExitedByDepartmentId(departmentId));
        stats.setInProgressCount(candidateRepository.countInProgressByDepartmentId(departmentId));

        Map<CandidateStatus, Long> perStatus = EnumSet.allOf(CandidateStatus.class).stream()
                .collect(Collectors.toMap(
                        s -> s,
                        s -> candidateRepository.countByDepartment_IdAndCurrentStatus(departmentId, s)
                ));
        stats.setCandidatesPerStatus(perStatus);

        return stats;
    }

    private Department findActiveDepartment(UUID workspaceId, UUID departmentId) {
        Department department = departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));
        if (department.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Department not found.");
        }
        return department;
    }

    private Candidate findActiveCandidate(UUID workspaceId, UUID departmentId, UUID candidateId) {
        Department department = findActiveDepartment(workspaceId, departmentId);

        Candidate candidate = candidateRepository.findByIdAndDepartmentId(candidateId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found."));

        if (!candidate.getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Candidate not found.");
        }
        if (!candidate.getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Candidate not found.");
        }

        return candidate;
    }
}
