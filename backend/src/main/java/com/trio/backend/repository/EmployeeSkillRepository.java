package com.trio.backend.repository;

import com.trio.backend.entity.EmployeeSkill;
import com.trio.backend.enums.SkillCategory;
import com.trio.backend.enums.SkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, UUID>,
        JpaSpecificationExecutor<EmployeeSkill> {

    Optional<EmployeeSkill> findByIdAndEmployee_Id(UUID id, UUID employeeId);

    Optional<EmployeeSkill> findByEmployee_IdAndSkillNameIgnoreCase(UUID employeeId, String skillName);

    List<EmployeeSkill> findAllByEmployee_IdOrderByCreatedAtDesc(UUID employeeId);

    @Query("SELECT COUNT(s) > 0 FROM EmployeeSkill s WHERE s.employee.id = :employeeId AND s.active = true AND LOWER(s.skillName) = LOWER(:skillName)")
    boolean existsActiveByEmployee_IdAndSkillNameIgnoreCase(@Param("employeeId") UUID employeeId, @Param("skillName") String skillName);

    long countByEmployee_Id(UUID employeeId);

    long countByEmployee_IdAndVerified(UUID employeeId, boolean verified);

    long countByEmployee_IdAndCertificationNameIsNotNull(UUID employeeId);

    @Query("SELECT COUNT(DISTINCT s.employee.id) FROM EmployeeSkill s WHERE s.employee.department.id = :departmentId AND s.active = true")
    long countDistinctEmployeeByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT COUNT(s) FROM EmployeeSkill s WHERE s.employee.department.id = :departmentId AND s.active = true")
    long countByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT s.category, COUNT(s) FROM EmployeeSkill s WHERE s.employee.id = :employeeId AND s.active = true GROUP BY s.category")
    List<Object[]> countByCategoryGroupedByEmployeeId(@Param("employeeId") UUID employeeId);

    @Query("SELECT s.proficiencyLevel, COUNT(s) FROM EmployeeSkill s WHERE s.employee.id = :employeeId AND s.active = true GROUP BY s.proficiencyLevel")
    List<Object[]> countByLevelGroupedByEmployeeId(@Param("employeeId") UUID employeeId);

    @Query("SELECT s.category, COUNT(s) FROM EmployeeSkill s WHERE s.employee.department.id = :departmentId AND s.active = true GROUP BY s.category")
    List<Object[]> countByCategoryGroupedByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT s.proficiencyLevel, COUNT(s) FROM EmployeeSkill s WHERE s.employee.department.id = :departmentId AND s.active = true GROUP BY s.proficiencyLevel")
    List<Object[]> countByLevelGroupedByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT s.skillName, s.category, s.proficiencyLevel, COUNT(s) as cnt FROM EmployeeSkill s WHERE s.employee.department.id = :departmentId AND s.active = true GROUP BY s.skillName, s.category, s.proficiencyLevel ORDER BY cnt DESC")
    List<Object[]> findTopSkillsByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT COUNT(s) FROM EmployeeSkill s WHERE s.employee.department.id = :departmentId AND s.active = true AND s.certificationExpiration IS NOT NULL AND s.certificationExpiration > :date AND s.certificationExpiration <= :cutoff")
    long countExpiringCertificationsByDepartmentId(@Param("departmentId") UUID departmentId, @Param("date") LocalDate date, @Param("cutoff") LocalDate cutoff);

    @Query("SELECT s FROM EmployeeSkill s WHERE s.employee.department.id = :departmentId AND s.active = true AND s.certificationExpiration IS NOT NULL AND s.certificationExpiration > :date AND s.certificationExpiration <= :cutoff")
    List<EmployeeSkill> findExpiringCertificationsByDepartmentId(@Param("departmentId") UUID departmentId, @Param("date") LocalDate date, @Param("cutoff") LocalDate cutoff);

    @Query("SELECT COUNT(s) FROM EmployeeSkill s WHERE s.employee.department.id = :departmentId AND s.active = true AND s.certificationName IS NOT NULL")
    long countCertificationsByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT COUNT(s) FROM EmployeeSkill s WHERE s.employee.department.id = :departmentId AND s.active = true AND s.verified = true")
    long countVerifiedByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT COUNT(s) FROM EmployeeSkill s WHERE s.employee.department.id = :departmentId AND s.active = true AND s.verified = false")
    long countUnverifiedByDepartmentId(@Param("departmentId") UUID departmentId);
}
