package com.trio.backend.service.hr;

import com.trio.backend.dto.hr.CreateEmployeeSkillRequest;
import com.trio.backend.dto.hr.EmployeeSkillResponse;
import com.trio.backend.dto.hr.EmployeeSkillSearchCriteria;
import com.trio.backend.dto.hr.EmployeeSkillStatistics;
import com.trio.backend.dto.hr.SkillSummary;
import com.trio.backend.dto.hr.UpdateEmployeeSkillRequest;
import com.trio.backend.entity.Employee;
import com.trio.backend.entity.EmployeeEventLog;
import com.trio.backend.entity.EmployeeSkill;
import com.trio.backend.enums.EmploymentStatus;
import com.trio.backend.enums.SkillCategory;
import com.trio.backend.enums.SkillLevel;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.EmployeeSkillMapper;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.EmployeeEventLogRepository;
import com.trio.backend.repository.EmployeeRepository;
import com.trio.backend.repository.EmployeeSkillRepository;
import com.trio.backend.repository.EmployeeSkillSpecification;
import com.trio.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmployeeSkillServiceImpl implements EmployeeSkillService {

    private final EmployeeSkillRepository skillRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeEventLogRepository employeeEventLogRepository;
    private final EmployeeSkillMapper skillMapper;

    @Override
    public EmployeeSkillResponse create(UUID workspaceId, UUID departmentId, UUID employeeId, CreateEmployeeSkillRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);

        if (skillRepository.existsActiveByEmployee_IdAndSkillNameIgnoreCase(employeeId, request.getSkillName())) {
            throw new BadRequestException("Skill '" + request.getSkillName() + "' already exists for this employee.");
        }

        validateCertificationDates(request.getCertificationDate(), request.getCertificationExpiration());

        EmployeeSkill saved = skillRepository.findByEmployee_IdAndSkillNameIgnoreCase(employeeId, request.getSkillName())
                .filter(s -> !s.isActive())
                .map(s -> {
                    s.setActive(true);
                    s.setCategory(request.getCategory());
                    s.setProficiencyLevel(request.getProficiencyLevel());
                    s.setYearsOfExperience(request.getYearsOfExperience());
                    s.setLastUsedDate(request.getLastUsedDate());
                    s.setCertificationName(request.getCertificationName());
                    s.setCertificationIssuer(request.getCertificationIssuer());
                    s.setCertificationDate(request.getCertificationDate());
                    s.setCertificationExpiration(request.getCertificationExpiration());
                    s.setVerified(false);
                    s.setNotes(request.getNotes());
                    return skillRepository.save(s);
                })
                .orElseGet(() -> {
                    EmployeeSkill skill = EmployeeSkill.builder()
                            .employee(employee)
                            .skillName(request.getSkillName())
                            .category(request.getCategory())
                            .proficiencyLevel(request.getProficiencyLevel())
                            .yearsOfExperience(request.getYearsOfExperience())
                            .lastUsedDate(request.getLastUsedDate())
                            .certificationName(request.getCertificationName())
                            .certificationIssuer(request.getCertificationIssuer())
                            .certificationDate(request.getCertificationDate())
                            .certificationExpiration(request.getCertificationExpiration())
                            .verified(false)
                            .active(true)
                            .notes(request.getNotes())
                            .build();
                    return skillRepository.save(skill);
                });
        log.info("Employee skill created: {} ({}) for employee {} by user {}",
                saved.getSkillName(), saved.getCategory(), employeeId, userId);

        createEventLog(employee, "SKILL_ADDED", null, saved.getSkillName(),
                "Skill " + saved.getSkillName() + " added (" + saved.getCategory() + ")");

        return skillMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeSkillResponse getById(UUID workspaceId, UUID departmentId, UUID employeeId, UUID skillId) {
        SecurityUtils.getCurrentUserId();
        findActiveEmployee(workspaceId, departmentId, employeeId);
        EmployeeSkill skill = findSkill(skillId, employeeId);
        return skillMapper.toResponse(skill);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeSkillResponse> listByEmployee(UUID workspaceId, UUID departmentId, UUID employeeId, Pageable pageable) {
        SecurityUtils.getCurrentUserId();
        findActiveEmployee(workspaceId, departmentId, employeeId);

        EmployeeSkillSearchCriteria criteria = new EmployeeSkillSearchCriteria();
        criteria.setEmployeeId(employeeId);
        criteria.setActive(true);

        return skillRepository.findAll(EmployeeSkillSpecification.withFilter(criteria), pageable)
                .map(skillMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeSkillResponse> search(UUID workspaceId, UUID departmentId, EmployeeSkillSearchCriteria criteria, Pageable pageable) {
        SecurityUtils.getCurrentUserId();
        findActiveDepartment(workspaceId, departmentId);

        if (criteria != null && criteria.getEmployeeId() != null) {
            findActiveEmployee(workspaceId, departmentId, criteria.getEmployeeId());
        }

        return skillRepository.findAll(EmployeeSkillSpecification.withFilter(criteria), pageable)
                .map(skillMapper::toResponse);
    }

    @Override
    public EmployeeSkillResponse update(UUID workspaceId, UUID departmentId, UUID employeeId, UUID skillId, UpdateEmployeeSkillRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);
        EmployeeSkill skill = findSkill(skillId, employeeId);

        if (request.getSkillName() != null && !request.getSkillName().equalsIgnoreCase(skill.getSkillName())) {
            if (skillRepository.existsActiveByEmployee_IdAndSkillNameIgnoreCase(employeeId, request.getSkillName())) {
                throw new BadRequestException("Skill '" + request.getSkillName() + "' already exists for this employee.");
            }
        }

        String oldName = skill.getSkillName();

        if (request.getSkillName() != null) {
            skill.setSkillName(request.getSkillName());
        }
        if (request.getCategory() != null) {
            skill.setCategory(request.getCategory());
        }
        if (request.getProficiencyLevel() != null) {
            skill.setProficiencyLevel(request.getProficiencyLevel());
        }
        if (request.getYearsOfExperience() != null) {
            skill.setYearsOfExperience(request.getYearsOfExperience());
        }
        if (request.getLastUsedDate() != null) {
            skill.setLastUsedDate(request.getLastUsedDate());
        }
        if (request.getCertificationName() != null) {
            skill.setCertificationName(request.getCertificationName());
        }
        if (request.getCertificationIssuer() != null) {
            skill.setCertificationIssuer(request.getCertificationIssuer());
        }
        if (request.getCertificationDate() != null) {
            skill.setCertificationDate(request.getCertificationDate());
        }
        if (request.getCertificationExpiration() != null) {
            skill.setCertificationExpiration(request.getCertificationExpiration());
        }
        if (request.getNotes() != null) {
            skill.setNotes(request.getNotes());
        }
        if (request.getActive() != null) {
            skill.setActive(request.getActive());
        }

        validateCertificationDates(skill.getCertificationDate(), skill.getCertificationExpiration());

        EmployeeSkill saved = skillRepository.save(skill);
        log.info("Employee skill updated: {} for employee {} by user {}",
                saved.getSkillName(), employeeId, userId);

        if (!oldName.equals(saved.getSkillName())) {
            createEventLog(employee, "SKILL_UPDATED", oldName, saved.getSkillName(),
                    "Skill renamed from " + oldName + " to " + saved.getSkillName());
        }

        return skillMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID workspaceId, UUID departmentId, UUID employeeId, UUID skillId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);
        EmployeeSkill skill = findSkill(skillId, employeeId);

        skill.setActive(false);
        skillRepository.save(skill);
        log.info("Employee skill deactivated: {} for employee {} by user {}",
                skill.getSkillName(), employeeId, userId);

        createEventLog(employee, "SKILL_REMOVED", skill.getSkillName(), null,
                "Skill " + skill.getSkillName() + " removed");
    }

    @Override
    public EmployeeSkillResponse verify(UUID workspaceId, UUID departmentId, UUID employeeId, UUID skillId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);
        EmployeeSkill skill = findSkill(skillId, employeeId);

        skill.setVerified(true);
        skill.setVerifiedBy(userId);
        skill.setVerifiedAt(java.time.Instant.now());

        EmployeeSkill saved = skillRepository.save(skill);
        log.info("Employee skill verified: {} for employee {} by user {}",
                saved.getSkillName(), employeeId, userId);

        createEventLog(employee, "SKILL_VERIFIED", null, saved.getSkillName(),
                "Skill " + saved.getSkillName() + " verified");

        return skillMapper.toResponse(saved);
    }

    @Override
    public EmployeeSkillResponse unverify(UUID workspaceId, UUID departmentId, UUID employeeId, UUID skillId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);
        EmployeeSkill skill = findSkill(skillId, employeeId);

        skill.setVerified(false);
        skill.setVerifiedBy(null);
        skill.setVerifiedAt(null);

        EmployeeSkill saved = skillRepository.save(skill);
        log.info("Employee skill unverified: {} for employee {} by user {}",
                saved.getSkillName(), employeeId, userId);

        createEventLog(employee, "SKILL_UNVERIFIED", null, saved.getSkillName(),
                "Skill " + saved.getSkillName() + " unverified");

        return skillMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeSkillStatistics getStatistics(UUID workspaceId, UUID departmentId) {
        SecurityUtils.getCurrentUserId();
        findActiveDepartment(workspaceId, departmentId);

        EmployeeSkillStatistics stats = new EmployeeSkillStatistics();

        long totalSkills = skillRepository.countByDepartmentId(departmentId);
        long distinctEmployees = skillRepository.countDistinctEmployeeByDepartmentId(departmentId);

        stats.setTotalSkills(totalSkills);
        stats.setAverageSkillsPerEmployee(distinctEmployees > 0 ? (double) totalSkills / distinctEmployees : 0);

        Map<SkillCategory, Long> byCategory = new EnumMap<>(SkillCategory.class);
        for (Object[] row : skillRepository.countByCategoryGroupedByDepartmentId(departmentId)) {
            byCategory.put((SkillCategory) row[0], (Long) row[1]);
        }
        stats.setSkillsByCategory(byCategory);

        Map<SkillLevel, Long> byLevel = new EnumMap<>(SkillLevel.class);
        for (Object[] row : skillRepository.countByLevelGroupedByDepartmentId(departmentId)) {
            byLevel.put((SkillLevel) row[0], (Long) row[1]);
        }
        stats.setSkillsByLevel(byLevel);

        long certCount = skillRepository.countCertificationsByDepartmentId(departmentId);
        stats.setCertificationCount(certCount);

        long expiring = skillRepository.countExpiringCertificationsByDepartmentId(departmentId, LocalDate.now(), LocalDate.now().plusDays(30));
        stats.setExpiringCertificationCount(expiring);

        List<SkillSummary> topSkills = new ArrayList<>();
        List<Object[]> topRows = skillRepository.findTopSkillsByDepartmentId(departmentId);
        int limit = Math.min(topRows.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = topRows.get(i);
            topSkills.add(new SkillSummary(
                    (String) row[0],
                    (SkillCategory) row[1],
                    (SkillLevel) row[2],
                    (Long) row[3]
            ));
        }
        stats.setTopSkills(topSkills);

        stats.setVerifiedCount(skillRepository.countVerifiedByDepartmentId(departmentId));
        stats.setUnverifiedCount(skillRepository.countUnverifiedByDepartmentId(departmentId));

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSkillResponse> getExpiringCertifications(UUID workspaceId, UUID departmentId, int withinDays) {
        SecurityUtils.getCurrentUserId();
        findActiveDepartment(workspaceId, departmentId);

        LocalDate now = LocalDate.now();
        LocalDate cutoff = now.plusDays(withinDays);
        return skillRepository.findExpiringCertificationsByDepartmentId(departmentId, now, cutoff)
                .stream()
                .map(skillMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void validateCertificationDates(LocalDate certDate, LocalDate certExpiration) {
        if (certDate != null && certExpiration != null && certExpiration.isBefore(certDate)) {
            throw new BadRequestException("Certification expiration date cannot be before certification date.");
        }
    }

    private EmployeeSkill findSkill(UUID skillId, UUID employeeId) {
        return skillRepository.findByIdAndEmployee_Id(skillId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found."));
    }

    private Employee findActiveEmployee(UUID workspaceId, UUID departmentId, UUID employeeId) {
        findActiveDepartment(workspaceId, departmentId);
        Employee employee = employeeRepository.findByIdAndDepartment_Id(employeeId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));
        if (employee.getEmploymentStatus() == EmploymentStatus.TERMINATED
                || employee.getEmploymentStatus() == EmploymentStatus.RESIGNED
                || employee.getEmploymentStatus() == EmploymentStatus.RETIRED) {
            throw new ResourceNotFoundException("Employee not found.");
        }
        return employee;
    }

    private void findActiveDepartment(UUID workspaceId, UUID departmentId) {
        departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .filter(dept -> dept.getStatus() == WorkspaceStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));
    }

    private void createEventLog(Employee employee, String eventType, String previousValue,
                                String newValue, String description) {
        EmployeeEventLog log = EmployeeEventLog.builder()
                .employee(employee)
                .eventType(eventType)
                .previousValue(previousValue)
                .newValue(newValue)
                .description(description)
                .build();
        employeeEventLogRepository.save(log);
    }
}
