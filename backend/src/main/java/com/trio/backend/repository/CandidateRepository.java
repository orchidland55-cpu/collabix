package com.trio.backend.repository;

import com.trio.backend.entity.Candidate;
import com.trio.backend.enums.CandidateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID>, JpaSpecificationExecutor<Candidate> {

    @Query("SELECT c FROM Candidate c WHERE c.id = :id AND c.department.id = :departmentId AND c.archived = false")
    Optional<Candidate> findByIdAndDepartmentId(@Param("id") UUID id, @Param("departmentId") UUID departmentId);

    @Query("SELECT c FROM Candidate c WHERE c.department.id = :departmentId AND c.archived = false")
    Page<Candidate> findAllByDepartmentId(@Param("departmentId") UUID departmentId, Pageable pageable);

    @Query("SELECT c FROM Candidate c WHERE c.department.id = :departmentId AND c.archived = false")
    List<Candidate> findAllByDepartmentId(@Param("departmentId") UUID departmentId);

    boolean existsByDepartment_IdAndEmail(UUID departmentId, String email);

    boolean existsByDepartment_IdAndEmailAndArchivedFalse(UUID departmentId, String email);

    Optional<Candidate> findByDepartment_IdAndEmail(UUID departmentId, String email);

    long countByDepartment_Id(UUID departmentId);

    long countByDepartment_IdAndCurrentStatus(UUID departmentId, CandidateStatus currentStatus);

    @Query("SELECT COUNT(c) FROM Candidate c WHERE c.department.id = :departmentId AND c.currentStatus = 'HIRED' AND c.archived = false")
    long countHiredByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT COUNT(c) FROM Candidate c WHERE c.department.id = :departmentId AND c.currentStatus IN ('REJECTED', 'WITHDRAWN') AND c.archived = false")
    long countExitedByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT COUNT(c) FROM Candidate c WHERE c.department.id = :departmentId AND c.currentStatus NOT IN ('HIRED', 'REJECTED', 'WITHDRAWN') AND c.archived = false")
    long countInProgressByDepartmentId(@Param("departmentId") UUID departmentId);
}
