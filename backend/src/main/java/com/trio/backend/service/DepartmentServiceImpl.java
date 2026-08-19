package com.trio.backend.service;

import com.trio.backend.dto.organisation.department.CreateDepartmentRequest;
import com.trio.backend.dto.organisation.department.DepartmentDetailsResponse;
import com.trio.backend.dto.organisation.department.DepartmentResponse;
import com.trio.backend.dto.organisation.department.DepartmentSummaryResponse;
import com.trio.backend.dto.organisation.department.UpdateDepartmentRequest;
import com.trio.backend.entity.Department;
import com.trio.backend.entity.User;
import com.trio.backend.entity.Workspace;
import com.trio.backend.entity.WorkspaceMember;

import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ConflictException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.DepartmentMapper;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.TeamRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;

import com.trio.backend.security.user.CustomUserDetails;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of the service management des Department.
 *
 * <p>Department est une ressource organizationnelle attachede ÃƒÂ  un {@link Workspace}.
 * Les operations respectsnt l'isolation multi-tenant through {@code workspaceId}.
 * </p>
 *
 * <p><strong>RÃƒÂ¨gles mÃƒÂ©tier validatedes :</strong></p>
 * <ul>
 *   <li>Creation : authorizede si the user est {@code WorkspaceMember.ACTIVE} in the workspace
 *       et has of a role {@code OWNER}/{@code ADMIN} (ou {@code SUPER_ADMIN}).</li>
 *   <li>Modification : authorizede si the user est {@code OWNER}/{@code ADMIN} (ou {@code SUPER_ADMIN}).</li>
 *   <li>Deletion : soft delete ; le Department passe ÃƒÂ  {@code ARCHIVED}.
 *       Interdite s'il still contains active teams (pour le MVP, verification sera faite
 *       par the method TeamRepository lorsque ce module sera branchÃƒÂ©).</li>
 *   <li>UnicitÃƒÂ© : {@code name} unique in a workspace, normalized (sortm + case-insensitive ÃƒÂ  la casse).</li>
 * </ul>
 *
 * @see DepartmentService
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final DepartmentMapper departmentMapper;
    private final EntityManager entityManager;


    /**
     * Creates a Department in the workspace.
     *
     * @param workspaceId the ID of the workspace
     * @param request the request de creation
     * @return le Department created
     */
    @Override
    public DepartmentResponse create(UUID workspaceId, CreateDepartmentRequest request) {

        UUID userId = getAuthenticatedUserId();
        User actor = getAuthenticatedUser();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);

        String normalizedName = normalizeName(request.getName());

        if (departmentRepository.existsByWorkspace_IdAndName(workspaceId, normalizedName)) {
            throw new ConflictException("Department with this name already exists in this workspace.");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));

        Department department = departmentMapper.toEntity(request);
        department.setWorkspace(workspace);
        department.setStatus(WorkspaceStatus.ACTIVE);

        Department saved = departmentRepository.save(department);

        DepartmentResponse response = departmentMapper.toResponse(saved);
        response.setTeamCount(teamRepository.countByDepartment_IdAndStatus(saved.getId(), WorkspaceStatus.ACTIVE));
        return response;
    }

    /**
     * Resorteves a Department by ID.
     *
     * @param workspaceId the ID of the workspace
     * @param departmentId l'identifiant du department
     * @return le Department
     */
    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getById(UUID workspaceId, UUID departmentId) {

        assertActiveWorkspaceMember(workspaceId, getAuthenticatedUserId());

        Department department = departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        if (department.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Department not found.");
        }

        DepartmentResponse response = departmentMapper.toResponse(department);
        response.setTeamCount(teamRepository.countByDepartment_IdAndStatus(departmentId, WorkspaceStatus.ACTIVE));
        return response;
    }

    /**
     * Liste les Departments of a workspace.
     *
     * @param workspaceId the ID of the workspace
     * @return the list summarye
     */
    @Override
    @Transactional(readOnly = true)
    public List<DepartmentSummaryResponse> listByWorkspace(UUID workspaceId, boolean includeArchived) {

        assertActiveWorkspaceMember(workspaceId, getAuthenticatedUserId());

        List<Department> departments = includeArchived
                ? departmentRepository.findAllByWorkspace_Id(workspaceId)
                : departmentRepository.findAllByWorkspace_IdAndStatus(workspaceId, WorkspaceStatus.ACTIVE);

        return departments.stream()
                .map(d -> {
                    DepartmentSummaryResponse r = departmentMapper.toSummary(d);
                    r.setTeamCount(teamRepository.countByDepartment_IdAndStatus(d.getId(), WorkspaceStatus.ACTIVE));
                    return r;
                })
                .toList();
    }

    /**
     * Resorteves the variante details d'un Department.
     *
     * @param workspaceId the ID of the workspace
     * @param departmentId l'identifiant du department
     * @return le DepartmentDetailsResponse
     */
    @Override
    @Transactional(readOnly = true)
    public DepartmentDetailsResponse getDetails(UUID workspaceId, UUID departmentId) {

        assertActiveWorkspaceMember(workspaceId, getAuthenticatedUserId());

        Department department = departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        if (department.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Department not found.");
        }

        DepartmentDetailsResponse response = departmentMapper.toDetails(department);
        response.setTeamCount(teamRepository.countByDepartment_IdAndStatus(departmentId, WorkspaceStatus.ACTIVE));
        return response;
    }

    /**
     * Updates a Department.
     *
     * <p>Partial update : the fields null are not appliquÃƒÂ©s.</p>
     *
     * @param workspaceId the ID of the workspace
     * @param departmentId l'identifiant du department
     * @param request the request de updated
     * @return le Department updated
     */
    @Override
    public DepartmentResponse update(UUID workspaceId, UUID departmentId, UpdateDepartmentRequest request) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);

        Department department = departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        if (department.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Department not found.");
        }

        if (request.getName() != null) {
            String normalizedName = normalizeName(request.getName());
            if (!normalizedName.equals(normalizeName(department.getName()))
                    && departmentRepository.existsByWorkspace_IdAndName(workspaceId, normalizedName)) {
                throw new ConflictException("Department with this name already exists in this workspace.");
            }
            request.setName(normalizedName);
        }

        departmentMapper.updateDepartment(request, department);
        Department saved = departmentRepository.save(department);

        DepartmentResponse response = departmentMapper.toResponse(saved);
        response.setTeamCount(teamRepository.countByDepartment_IdAndStatus(saved.getId(), WorkspaceStatus.ACTIVE));
        return response;
    }

    /**
     * Restores an archived department back to ACTIVE status.
     */
    @Override
    public DepartmentResponse restore(UUID workspaceId, UUID departmentId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);

        Department department = departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        if (department.getStatus() != WorkspaceStatus.ARCHIVED) {
            throw new BadRequestException("Department is not archived.");
        }

        department.setStatus(WorkspaceStatus.ACTIVE);
        Department saved = departmentRepository.save(department);

        DepartmentResponse response = departmentMapper.toResponse(saved);
        response.setTeamCount(teamRepository.countByDepartment_IdAndStatus(saved.getId(), WorkspaceStatus.ACTIVE));
        return response;
    }

    /**
     * Soft delete : passe le Department en ARCHIVED.
     *
     * <p>Le delete est Refusaled if the department contains active teams (verification non cÃƒÂ¢blÃƒÂ©e
     * tant que le TeamRepository is not used ici).</p>
     *
     * @param workspaceId the ID of the workspace
     * @param departmentId l'identifiant du department
     */
    @Override
    public void delete(UUID workspaceId, UUID departmentId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);

        Department department = departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        if (department.getStatus() != WorkspaceStatus.ACTIVE) {
            // idempotent : si dÃƒÂ©jÃƒÂ  archived => aucune error
            return;
        }

        // RÃƒÂ¨gle mÃƒÂ©tier validatede : deletion refusalede si le Department still contains active teams
        if (teamRepository.existsByDepartment_IdAndStatus(departmentId, WorkspaceStatus.ACTIVE)) {
            throw new ConflictException("Cannot delete department: it still contains active teams.");
        }

        department.setStatus(WorkspaceStatus.ARCHIVED);
        departmentRepository.save(department);

    }

    /**
     * Hard delete : supprime physiquement le Department de la base de donnÃƒÂ©es.
     *
     * <p>Cette opÃƒÂ©ration est irrÃƒÂ©versible. Elle respecte les contraintes FK
     * existantes : aucune cascade aveugle. Si des enregistrements mÃƒÂ©tier dÃƒÂ©pendent
     * encore du Department (teams, projets, ressources HR, sprints, campagnes,
     * audits, rapports, handovers, modÃƒÂ¨les IA...), la suppression est refusÃƒÂ©e.</p>
     *
     * <p>Les rÃƒÂ©fÃƒÂ©rences optionnelles (users.primary_department_id, announcements,
     * conversations, alerts, ai_history) sont gÃƒÂ©rÃƒÂ©es par la base via ON DELETE SET NULL.</p>
     *
     * @param workspaceId the ID of the workspace
     * @param departmentId l'identifiant du department
     */
    @Override
    public void deletePermanently(UUID workspaceId, UUID departmentId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);

        Department department = departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        Map<String, Long> dependents = countDependents(departmentId);

        if (!dependents.isEmpty()) {
            String labels = String.join(", ", dependents.keySet());
            throw new ConflictException("Cannot permanently delete department: it still contains " + labels
                    + ". Archive the department or move its data first.");
        }

        departmentRepository.delete(department);
    }

    /**
     * Counts business records that would block a hard delete of the department.
     *
     * <p>Only records backed by a non-null FK to departments(id) with NO ACTION
     * (or an ON DELETE CASCADE that would silently erase owned data) are guarded.
     * Optional references with ON DELETE SET NULL (users, announcements,
     * conversations, alerts, ai_history) are intentionally not guarded: the
     * database detaches them automatically.</p>
     *
     * @param departmentId the department id
     * @return map of friendly label to record count, empty when safe to delete
     */
    private Map<String, Long> countDependents(UUID departmentId) {
        Map<String, Long> result = new LinkedHashMap<>();
        countDependents(result, departmentId, "teams", "Team");
        countDependents(result, departmentId, "projects", "Project");
        countDependents(result, departmentId, "employees", "Employee");
        countDependents(result, departmentId, "candidates", "Candidate");
        countDependents(result, departmentId, "sprints", "Sprint");
        countDependents(result, departmentId, "marketing campaigns", "MarketingCampaign");
        countDependents(result, departmentId, "security audits", "SecurityAudit");
        countDependents(result, departmentId, "analytics reports", "AnalyticsReport");
        countDependents(result, departmentId, "executive reports", "ExecutiveReport");
        countDependents(result, departmentId, "handover journals", "HandoverJournal");
        countDependents(result, departmentId, "handover entries", "HandoverEntry");
        countDependents(result, departmentId, "AI models", "AIModel");
        return result;
    }

    private void countDependents(Map<String, Long> result, UUID departmentId, String label, String entityName) {
        Long count = (Long) entityManager.createQuery(
                        "select count(e) from " + entityName + " e where e.department.id = :deptId")
                .setParameter("deptId", departmentId)
                .getSingleResult();
        if (count != null && count > 0) {
            result.put(label, count);
        }
    }

    // ============================================================================
    // PRIVATE HELPERS
    // ============================================================================

    private UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails main)) {
            throw new BadRequestException("User is not authenticated.");
        }
        return main.getId();
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails main)) {
            throw new BadRequestException("User is not authenticated.");
        }

        return userRepository.findByEmail(main.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private void assertActiveWorkspaceMember(UUID workspaceId, UUID userId) {
        // Utilise la same logical que WorkspaceAuthorization / WorkspaceServiceImpl
        WorkspaceMember wm = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace."));

        if (wm.getStatus() != WorkspaceMemberStatus.ACTIVE) {
            throw new ForbiddenException("You are not an active member of this workspace.");
        }
    }

    private void assertWorkspaceAdminOrOwner(UUID workspaceId, UUID userId) {
        if (!workspaceMemberRepository.existsWithRole(workspaceId, userId, WorkspaceRole.ADMIN)
                && !isOwner(workspaceId, userId)) {
            throw new ForbiddenException("You do not have permission for this operation.");
        }
    }

    private void assertWorkspaceOwner(UUID workspaceId, UUID userId) {
        if (!isOwner(workspaceId, userId)) {
            throw new ForbiddenException("Only OWNER can perform this operation.");
        }
    }

    private boolean isOwner(UUID workspaceId, UUID userId) {
        return workspaceRepository.findById(workspaceId)
                .map(ws -> ws.getOwner().getId().equals(userId))
                .orElse(false);
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

