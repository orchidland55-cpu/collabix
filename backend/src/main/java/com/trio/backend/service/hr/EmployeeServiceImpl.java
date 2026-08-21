package com.trio.backend.service.hr;

import com.trio.backend.dto.hr.CreateEmployeeRequest;
import com.trio.backend.dto.hr.EmployeeResponse;
import com.trio.backend.dto.hr.EmployeeSearchCriteria;
import com.trio.backend.dto.hr.EmployeeStatistics;
import com.trio.backend.dto.hr.EmployeeTimelineEntry;
import com.trio.backend.dto.hr.UpdateEmployeeRequest;
import com.trio.backend.dto.notification.CreateNotificationRequest;
import com.trio.backend.entity.Candidate;
import com.trio.backend.entity.Notification;
import com.trio.backend.entity.Department;
import com.trio.backend.entity.Employee;
import com.trio.backend.entity.EmployeeEventLog;
import com.trio.backend.entity.Team;
import com.trio.backend.enums.EmploymentStatus;
import com.trio.backend.enums.ContractType;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ConflictException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.EmployeeMapper;
import com.trio.backend.repository.CandidateRepository;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.EmployeeEventLogRepository;
import com.trio.backend.repository.EmployeeRepository;
import com.trio.backend.repository.EmployeeSpecification;
import com.trio.backend.repository.TeamRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.service.NotificationService;
import com.trio.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeEventLogRepository employeeEventLogRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmployeeMapper employeeMapper;

    @Override
    public EmployeeResponse create(UUID workspaceId, UUID departmentId, CreateEmployeeRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Department department = findActiveDepartment(workspaceId, departmentId);

        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("An employee with this email already exists.");
        }

        if (request.getStartDate().isAfter(LocalDate.now().plusMonths(1))) {
            throw new BadRequestException("Start date cannot be more than one month in the future.");
        }

        Candidate candidate = null;
        if (request.getCandidateId() != null) {
            candidate = candidateRepository.findByIdAndDepartmentId(request.getCandidateId(), departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Candidate not found."));
            if (candidate.getCurrentStatus() != com.trio.backend.enums.CandidateStatus.HIRED) {
                throw new BadRequestException("Candidate has not been hired.");
            }
            if (employeeRepository.existsByEmail(candidate.getEmail())) {
                throw new ConflictException("A candidate with this email is already an employee.");
            }
        }

        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found."));
            if (!team.getDepartment().getId().equals(departmentId)) {
                throw new BadRequestException("Team does not belong to the specified department.");
            }
            if (team.getStatus() != WorkspaceStatus.ACTIVE) {
                throw new ResourceNotFoundException("Team not found.");
            }
        }

        Employee manager = null;
        if (request.getManagerId() != null) {
            manager = employeeRepository.findByIdAndDepartment_Id(request.getManagerId(), departmentId)
                    .orElseThrow(() -> new BadRequestException(
                            "Manager not found. Select an existing employee from this department as manager."));
            if (manager.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
                throw new BadRequestException("Manager is not active.");
            }
        }

        String employeeNumber = generateEmployeeNumber();

        Employee.EmployeeBuilder builder = Employee.builder()
                .employeeNumber(employeeNumber)
                .firstName(candidate != null ? candidate.getFirstName() : request.getFirstName())
                .lastName(candidate != null ? candidate.getLastName() : request.getLastName())
                .email(candidate != null ? candidate.getEmail() : request.getEmail())
                .phone(candidate != null ? candidate.getPhone() : request.getPhone())
                .position(candidate != null ? candidate.getPosition() : request.getPosition())
                .address(request.getAddress())
                .dateOfBirth(request.getDateOfBirth())
                .nationality(request.getNationality())
                .emergencyContact(request.getEmergencyContact())
                .department(department)
                .team(team)
                .manager(manager)
                .employmentType(request.getEmploymentType())
                .employmentStatus(EmploymentStatus.ONBOARDING)
                .startDate(request.getStartDate())
                .candidate(candidate);

        Employee employee = builder.build();
        Employee saved = employeeRepository.save(employee);
        log.info("Employee created: {} {} (emp={}) by user {}", saved.getFirstName(), saved.getLastName(),
                saved.getEmployeeNumber(), userId);

        createEventLog(saved, "EMPLOYEE_CREATED", null, null,
                "Employee " + saved.getFirstName() + " " + saved.getLastName() + " created.");

        if (candidate != null) {
            createEventLog(saved, "CONVERTED_FROM_CANDIDATE", null, candidate.getId().toString(),
                    "Converted from candidate " + candidate.getFirstName() + " " + candidate.getLastName());
        }

        notifyEmployeeCreated(workspaceId, saved);

        return employeeMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID workspaceId, UUID departmentId, UUID employeeId) {
        SecurityUtils.getCurrentUserId();

        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(UUID workspaceId, UUID departmentId, EmployeeSearchCriteria criteria, Pageable pageable) {
        SecurityUtils.getCurrentUserId();

        findActiveDepartment(workspaceId, departmentId);

        if (criteria != null) {
            criteria.setDepartmentId(departmentId);
        }

        return employeeRepository.findAll(
                        EmployeeSpecification.withFilter(departmentId, criteria), pageable)
                .map(employeeMapper::toResponse);
    }

    @Override
    public EmployeeResponse update(UUID workspaceId, UUID departmentId, UUID employeeId, UpdateEmployeeRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);

        if (request.getEmail() != null && !request.getEmail().equals(employee.getEmail())) {
            if (employeeRepository.existsByEmailAndIdNot(request.getEmail(), employeeId)) {
                throw new ConflictException("Another employee with this email already exists.");
            }
            employee.setEmail(request.getEmail());
        }

        String oldDepartment = employee.getDepartment().getName();
        String oldTeam = employee.getTeam() != null ? employee.getTeam().getName() : null;
        String oldManager = employee.getManager() != null ? employee.getManager().getFirstName() + " " + employee.getManager().getLastName() : null;
        String oldPosition = employee.getPosition();
        EmploymentStatus oldStatus = employee.getEmploymentStatus();

        if (request.getFirstName() != null) {
            employee.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            employee.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            employee.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            employee.setAddress(request.getAddress());
        }
        if (request.getDateOfBirth() != null) {
            employee.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getNationality() != null) {
            employee.setNationality(request.getNationality());
        }
        if (request.getEmergencyContact() != null) {
            employee.setEmergencyContact(request.getEmergencyContact());
        }
        if (request.getPosition() != null) {
            employee.setPosition(request.getPosition());
        }
        if (request.getEmploymentType() != null) {
            employee.setEmploymentType(request.getEmploymentType());
        }
        if (request.getEmploymentStatus() != null) {
            employee.setEmploymentStatus(request.getEmploymentStatus());
        }
        if (request.getStartDate() != null) {
            employee.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            employee.setEndDate(request.getEndDate());
        }
        if (request.getProfilePicturePath() != null) {
            employee.setProfilePicturePath(request.getProfilePicturePath());
        }

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found."));
            if (!team.getDepartment().getId().equals(departmentId)) {
                throw new BadRequestException("Team does not belong to the specified department.");
            }
            if (team.getStatus() != WorkspaceStatus.ACTIVE) {
                throw new ResourceNotFoundException("Team not found.");
            }
            employee.setTeam(team);
        }

        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findByIdAndDepartment_Id(request.getManagerId(), departmentId)
                    .orElseThrow(() -> new BadRequestException(
                            "Manager not found. Select an existing employee from this department as manager."));
            if (manager.getEmploymentStatus() != EmploymentStatus.ACTIVE) {
                throw new BadRequestException("Manager is not active.");
            }
            employee.setManager(manager);
        }

        Employee saved = employeeRepository.save(employee);
        log.info("Employee updated: {} {} (emp={}) by user {}", saved.getFirstName(), saved.getLastName(),
                saved.getEmployeeNumber(), userId);

        trackChanges(employee, oldDepartment, oldTeam, oldManager, oldPosition, oldStatus);

        return employeeMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID workspaceId, UUID departmentId, UUID employeeId) {
        UUID userId = SecurityUtils.getCurrentUserId();

        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);

        employee.setEmploymentStatus(EmploymentStatus.TERMINATED);
        employee.setEndDate(LocalDate.now());
        employeeRepository.save(employee);

        createEventLog(employee, "TERMINATED", null, null,
                "Employee " + employee.getFirstName() + " " + employee.getLastName() + " terminated.");

        log.info("Employee terminated: {} {} (emp={}) by user {}", employee.getFirstName(), employee.getLastName(),
                employee.getEmployeeNumber(), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeTimelineEntry> getTimeline(UUID workspaceId, UUID departmentId, UUID employeeId) {
        SecurityUtils.getCurrentUserId();

        findActiveEmployee(workspaceId, departmentId, employeeId);

        List<EmployeeEventLog> logs = employeeEventLogRepository
                .findAllByEmployee_IdOrderByCreatedAtDesc(employeeId);

        return logs.stream()
                .map(employeeMapper::toTimelineEntry)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeStatistics getStatistics(UUID workspaceId, UUID departmentId) {
        SecurityUtils.getCurrentUserId();

        findActiveDepartment(workspaceId, departmentId);

        EmployeeStatistics stats = new EmployeeStatistics();

        List<Employee> allEmployees = employeeRepository.findAll(
                EmployeeSpecification.withFilter(departmentId, null), Pageable.unpaged()).getContent();

        long total = allEmployees.size();
        long active = allEmployees.stream().filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE).count();
        long onLeave = allEmployees.stream().filter(e -> e.getEmploymentStatus() == EmploymentStatus.ON_LEAVE).count();
        long probation = allEmployees.stream().filter(e -> e.getEmploymentStatus() == EmploymentStatus.PROBATION).count();

        stats.setTotalEmployees(total);
        stats.setActiveEmployees(active);
        stats.setOnLeaveCount(onLeave);
        stats.setProbationCount(probation);

        Map<String, Long> byDepartment = new HashMap<>();
        for (Object[] row : employeeRepository.countByDepartmentAcrossWorkspace(workspaceId)) {
            byDepartment.put((String) row[0], (Long) row[1]);
        }
        stats.setEmployeesByDepartment(byDepartment);

        Map<String, Long> byTeam = new HashMap<>();
        for (Employee e : allEmployees) {
            if (e.getTeam() != null) {
                byTeam.merge(e.getTeam().getName(), 1L, Long::sum);
            }
        }
        stats.setEmployeesByTeam(byTeam);

        Map<ContractType, Long> byType = new HashMap<>();
        for (Employee e : allEmployees) {
            byType.merge(e.getEmploymentType(), 1L, Long::sum);
        }
        stats.setEmployeesByEmploymentType(byType);

        Map<EmploymentStatus, Long> byStatus = new HashMap<>();
        for (Employee e : allEmployees) {
            byStatus.merge(e.getEmploymentStatus(), 1L, Long::sum);
        }
        stats.setEmployeesByStatus(byStatus);

        long newHires = employeeRepository.countNewHiresThisMonth(departmentId);
        stats.setNewHiresThisMonth(newHires);

        return stats;
    }

    private Employee findActiveEmployee(UUID workspaceId, UUID departmentId, UUID employeeId) {
        Employee employee = employeeRepository.findByIdAndDepartment_Id(employeeId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));
        if (employee.getEmploymentStatus() == EmploymentStatus.TERMINATED
                || employee.getEmploymentStatus() == EmploymentStatus.RESIGNED
                || employee.getEmploymentStatus() == EmploymentStatus.RETIRED) {
            throw new ResourceNotFoundException("Employee not found.");
        }
        return employee;
    }

    private Department findActiveDepartment(UUID workspaceId, UUID departmentId) {
        Department department = departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));
        if (department.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Department not found.");
        }
        return department;
    }

    private String generateEmployeeNumber() {
        String prefix = "EMP-";
        String uuidSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String number = prefix + uuidSuffix;
        while (employeeRepository.existsByEmployeeNumber(number)) {
            uuidSuffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            number = prefix + uuidSuffix;
        }
        return number;
    }

    private void createEventLog(Employee employee, String eventType, String previousValue, String newValue, String description) {
        EmployeeEventLog log = EmployeeEventLog.builder()
                .employee(employee)
                .eventType(eventType)
                .previousValue(previousValue)
                .newValue(newValue)
                .description(description)
                .build();
        employeeEventLogRepository.save(log);
    }

    private void notifyEmployeeCreated(UUID workspaceId, Employee employee) {
        userRepository.findByEmail(employee.getEmail()).ifPresent(user -> {
            CreateNotificationRequest notifReq = new CreateNotificationRequest();
            notifReq.setRecipientId(user.getId());
            notifReq.setNotificationType(Notification.NotificationType.EMPLOYEE_CREATED);
            notifReq.setTitle("Welcome aboard");
            notifReq.setBody("You have been added as " + employee.getPosition() + " starting " + employee.getStartDate());
            notificationService.create(workspaceId, notifReq);
        });
    }

    private void trackChanges(Employee employee, String oldDepartment, String oldTeam,
                              String oldManager, String oldPosition, EmploymentStatus oldStatus) {
        String newDepartment = employee.getDepartment().getName();
        if (!newDepartment.equals(oldDepartment)) {
            createEventLog(employee, "DEPARTMENT_CHANGED", oldDepartment, newDepartment,
                    "Department changed from " + oldDepartment + " to " + newDepartment);
        }

        String newTeam = employee.getTeam() != null ? employee.getTeam().getName() : null;
        if ((oldTeam == null && newTeam != null) || (oldTeam != null && !oldTeam.equals(newTeam))) {
            createEventLog(employee, "TEAM_CHANGED", oldTeam, newTeam,
                    "Team changed from " + oldTeam + " to " + newTeam);
        }

        String newManager = employee.getManager() != null ? employee.getManager().getFirstName() + " " + employee.getManager().getLastName() : null;
        if ((oldManager == null && newManager != null) || (oldManager != null && !oldManager.equals(newManager))) {
            createEventLog(employee, "MANAGER_CHANGED", oldManager, newManager,
                    "Manager changed from " + oldManager + " to " + newManager);
        }

        if (!oldPosition.equals(employee.getPosition())) {
            createEventLog(employee, "POSITION_CHANGED", oldPosition, employee.getPosition(),
                    "Position changed from " + oldPosition + " to " + employee.getPosition());
        }

        if (oldStatus != employee.getEmploymentStatus()) {
            createEventLog(employee, "EMPLOYMENT_STATUS_CHANGED", oldStatus.name(), employee.getEmploymentStatus().name(),
                    "Employment status changed from " + oldStatus + " to " + employee.getEmploymentStatus());
        }
    }
}
