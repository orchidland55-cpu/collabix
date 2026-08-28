package com.trio.backend.repository;

import com.trio.backend.entity.CandidateAttachment;
import com.trio.backend.enums.AttachmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateAttachmentRepository extends JpaRepository<CandidateAttachment, UUID>,
        JpaSpecificationExecutor<CandidateAttachment> {

    Page<CandidateAttachment> findAllByCandidate_IdOrderByCreatedAtDesc(UUID candidateId, Pageable pageable);

    List<CandidateAttachment> findAllByCandidate_Id(UUID candidateId);

    List<CandidateAttachment> findAllByCandidate_IdAndAttachmentType(UUID candidateId, AttachmentType attachmentType);

    Optional<CandidateAttachment> findByIdAndCandidate_Id(UUID id, UUID candidateId);

    long countByCandidate_Id(UUID candidateId);

    long countByCandidate_IdAndAttachmentType(UUID candidateId, AttachmentType attachmentType);

    @Query("SELECT COALESCE(SUM(a.fileSize), 0) FROM CandidateAttachment a WHERE a.candidate.department.id = :departmentId")
    long totalStorageByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT a.attachmentType, COUNT(a) FROM CandidateAttachment a WHERE a.candidate.department.id = :departmentId GROUP BY a.attachmentType")
    List<Object[]> countByTypeGrouped(@Param("departmentId") UUID departmentId);

    boolean existsByCandidate_IdAndAttachmentType(UUID candidateId, AttachmentType attachmentType);

    @Query("SELECT COUNT(a) FROM CandidateAttachment a WHERE a.candidate.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM CandidateAttachment a WHERE a.candidate.department.id = :departmentId AND a.attachmentType = :type")
    boolean existsByDepartmentIdAndAttachmentType(@Param("departmentId") UUID departmentId, @Param("type") AttachmentType type);

    @Query("SELECT COUNT(a) FROM CandidateAttachment a WHERE a.candidate.department.id = :departmentId AND a.attachmentType = :type")
    long countByDepartmentIdAndAttachmentType(@Param("departmentId") UUID departmentId, @Param("type") AttachmentType type);
}
