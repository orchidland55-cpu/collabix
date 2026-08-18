package com.trio.backend.service;

import com.trio.backend.dto.organisation.team.CreateTeamRequest;
import com.trio.backend.dto.organisation.team.TeamDetailsResponse;
import com.trio.backend.dto.organisation.team.TeamResponse;
import com.trio.backend.dto.organisation.team.TeamSummaryResponse;
import com.trio.backend.dto.organisation.team.UpdateTeamRequest;
import com.trio.backend.entity.Department;
import com.trio.backend.entity.Team;
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
import com.trio.backend.mapper.TeamMapper;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.TeamMemberRepository;
import com.trio.backend.repository.TeamRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;
import com.trio.backend.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Implementation of the service management des Team.
 *
 * <p>Team est attachede ÃƒÂ  un {@link Department}, lequel belong ÃƒÂ  un {@link Workspace}.
 * All operations sont bornÃƒÂ©es par {@code workspaceId} pour guarantee l'isolation multi-tenant.
 * </p>
 *
 * <p><strong>RÃƒÂ¨gles mÃƒÂ©tier validatedes :</strong></p>
 * <ul>
 *   <li>Creation : OWNER/ADMIN de workspace active (ou SUPER_ADMIN) + Department existant et non archived.</li>
 *   <li>UnicitÃƒÂ© : {@code name} unique dans un Department, normalized (sortm + case-insensitive ÃƒÂ  la casse).</li>
 *   <li>Modification : OWNER/ADMIN (TEAM LEADER non implÃƒÂ©mentÃƒÂ© pour le MVP si non supportÃƒÂ© par l'entity).</li>
 *   <li>Deletion : soft delete -> ARCHIVED (idempotent). Refusal si ressources actives existent (TODO).</li>
 * </ul>
 *
 * @see TeamService
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMapper teamMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public TeamResponse create(UUID workspaceId, UUID departmentId, CreateTeamRequest request) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);

        Department department = departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));

        if (department.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Department not found.");
        }

        String normalizedName = normalizeName(request.getName());
        request.setName(normalizedName);

        // UnicitÃƒÂ©: UNIQUE(department_id, name) - repo ne proemptydt pas directement the method,
        // on s'appuie sur existsByWorkspace_IdAndName comme placeholder MVP.
        // TODO: ajouter une method repository existsByDepartment_IdAndName quand required.
        if (teamRepository.existsByWorkspace_IdAndDepartment_IdAndName(workspaceId, departmentId, normalizedName)) {
            throw new ConflictException("Team with this name already exists.");
        }

        Team team = teamMapper.toEntity(request);
        team.setWorkspace(department.getWorkspace());
        team.setDepartment(department);
        team.setStatus(WorkspaceStatus.ACTIVE);

        if (request.getManagerId() != null) {
            team.setManager(resolveManager(workspaceId, request.getManagerId()));
        }

        Team saved = teamRepository.save(team);
        return teamMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getById(UUID workspaceId, UUID departmentId, UUID teamId) {

        assertActiveWorkspaceMember(workspaceId, getAuthenticatedUserId());

        Team team = teamRepository.findByIdAndWorkspace_Id(teamId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        if (!team.getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Team not found.");
        }

        if (team.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Team not found.");
        }

        return teamMapper.toResponse(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> listByDepartment(UUID workspaceId, UUID departmentId) {

        assertActiveWorkspaceMember(workspaceId, getAuthenticatedUserId());

        return teamRepository.findAllByWorkspace_IdAndDepartment_IdAndStatus(
                        workspaceId,
                        departmentId,
                        WorkspaceStatus.ACTIVE
                )
                .stream()
                .map(teamMapper::toSummary)
                .toList();

    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> listByWorkspace(UUID workspaceId) {

        assertActiveWorkspaceMember(workspaceId, getAuthenticatedUserId());

        return teamRepository.findAllByWorkspace_Id(workspaceId)
                .stream()
                .map(teamMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeamDetailsResponse getDetails(UUID workspaceId, UUID departmentId, UUID teamId) {

        assertActiveWorkspaceMember(workspaceId, getAuthenticatedUserId());

        Team team = teamRepository.findByIdAndWorkspace_Id(teamId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        if (!team.getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Team not found.");
        }

        if (team.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Team not found.");
        }

        return teamMapper.toDetails(team);
    }

    @Override
    public TeamResponse update(UUID workspaceId, UUID departmentId, UUID teamId, UpdateTeamRequest request) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);

        Team team = teamRepository.findByIdAndWorkspace_Id(teamId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        if (!team.getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Team not found.");
        }

        if (team.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Team not found.");
        }

        if (request.getName() != null) {
            String normalizedName = normalizeName(request.getName());
            request.setName(normalizedName);
        }

        if (Boolean.TRUE.equals(request.getClearManager())) {
            team.setManager(null);
        } else if (request.getManagerId() != null) {
            team.setManager(resolveManager(workspaceId, request.getManagerId()));
        }

        teamMapper.updateTeam(request, team);
        Team saved = teamRepository.save(team);
        return teamMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID workspaceId, UUID departmentId, UUID teamId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId); // OWNER attendu, MVP ÃƒÂ©largit

        Team team = teamRepository.findByIdAndWorkspace_Id(teamId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        if (!team.getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Team not found.");
        }

        if (team.getStatus() != WorkspaceStatus.ACTIVE) {
            return;
        }

        long activeTaskCount = ((Number) entityManager.createQuery(
                "select count(t) from Task t where t.sprint.team.id = :teamId " +
                        "and t.status not in (com.trio.backend.enums.TaskStatus.ARCHIVED, com.trio.backend.enums.TaskStatus.CANCELLED)")
                .setParameter("teamId", teamId)
                .getSingleResult()).longValue();
        if (activeTaskCount > 0) {
            throw new ConflictException("Team cannot be archived while it still has active tasks.");
        }

        long activeSprintCount = ((Number) entityManager.createQuery(
                "select count(s) from Sprint s where s.team.id = :teamId " +
                        "and s.status in (com.trio.backend.enums.SprintStatus.PLANNED, com.trio.backend.enums.SprintStatus.ACTIVE)")
                .setParameter("teamId", teamId)
                .getSingleResult()).longValue();
        if (activeSprintCount > 0) {
            throw new ConflictException("Team cannot be archived while it still has active sprints.");
        }

        team.setStatus(WorkspaceStatus.ARCHIVED);
        teamRepository.save(team);
    }

    @Override
    public void deletePermanently(UUID workspaceId, UUID departmentId, UUID teamId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Team team = teamRepository.findByIdAndWorkspace_Id(teamId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        if (!team.getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Team not found.");
        }

        // Only a Workspace Admin/Owner or the manager of this specific team may
        // permanently delete it. Everything else is rejected (403).
        assertCanPermanentlyDeleteTeam(workspaceId, team, userId);

        // Detach optional team references on unrelated business entities. This
        // follows the existing ON DELETE SET NULL convention instead of blindly
        // cascading the deletion into data that is not owned by the team.
        entityManager.createQuery("update Employee e set e.team = null where e.team.id = :teamId")
                .setParameter("teamId", teamId)
                .executeUpdate();
        entityManager.createQuery("update PerformanceReview p set p.team = null where p.team.id = :teamId")
                .setParameter("teamId", teamId)
                .executeUpdate();
        entityManager.createQuery("update Sprint s set s.team = null where s.team.id = :teamId")
                .setParameter("teamId", teamId)
                .executeUpdate();
        entityManager.createQuery("update SecurityAudit sa set sa.team = null where sa.team.id = :teamId")
                .setParameter("teamId", teamId)
                .executeUpdate();
        entityManager.createQuery("update MarketingCampaign mc set mc.team = null where mc.team.id = :teamId")
                .setParameter("teamId", teamId)
                .executeUpdate();

        // Team memberships are owned by the team (DB FK is ON DELETE CASCADE).
        // Removing them explicitly keeps the persistence context consistent.
        teamMemberRepository.deleteAllByTeamId(teamId);

        teamRepository.delete(team);
    }

    @Override
    public TeamResponse restore(UUID workspaceId, UUID departmentId, UUID teamId) {

        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        assertWorkspaceAdminOrOwner(workspaceId, userId);

        Team team = teamRepository.findByIdAndWorkspace_Id(teamId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found."));

        if (!team.getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Team not found.");
        }

        if (team.getStatus() != WorkspaceStatus.ARCHIVED) {
            return teamMapper.toResponse(team);
        }

        team.setStatus(WorkspaceStatus.ACTIVE);
        Team saved = teamRepository.save(team);
        return teamMapper.toResponse(saved);
    }

    // ============================================================================
    // Helpers
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

    private void assertActiveWorkspaceMember(UUID workspaceId, UUID userId) {
        WorkspaceMember wm = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace."));

        if (wm.getStatus() != WorkspaceMemberStatus.ACTIVE) {
            throw new ForbiddenException("You are not an active member of this workspace.");
        }
    }

    private void assertWorkspaceAdminOrOwner(UUID workspaceId, UUID userId) {
        boolean isAdmin = workspaceMemberRepository.existsWithRole(workspaceId, userId, WorkspaceRole.ADMIN);
        boolean isOwner = workspaceRepository.findById(workspaceId)
                .map(ws -> ws.getOwner().getId().equals(userId))
                .orElse(false);

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("You do not have permission for this operation.");
        }
    }

    /**
     * Permanent deletion is reserved for a Workspace Admin/Owner or the manager
     * of this specific team. All other users are rejected.
     */
    private void assertCanPermanentlyDeleteTeam(UUID workspaceId, Team team, UUID userId) {
        boolean isAdmin = workspaceMemberRepository.existsWithRole(workspaceId, userId, WorkspaceRole.ADMIN);
        boolean isOwner = workspaceRepository.findById(workspaceId)
                .map(ws -> ws.getOwner().getId().equals(userId))
                .orElse(false);
        boolean isTeamManager = team.getManager() != null && team.getManager().getId().equals(userId);

        if (!isAdmin && !isOwner && !isTeamManager) {
            throw new ForbiddenException("You do not have permission to delete this team.");
        }
    }

    /**
     * Resolves and validates a team manager for a workspace.
     *
     * <p>The manager must be an existing user and an active member of the
     * workspace, so that a team can never be tied to an external user.</p>
     */
    private User resolveManager(UUID workspaceId, UUID managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found."));

        WorkspaceMember wm = workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspaceId, managerId)
                .orElseThrow(() -> new ForbiddenException("Manager must be a member of this workspace."));

        if (wm.getStatus() != WorkspaceMemberStatus.ACTIVE) {
            throw new ForbiddenException("Manager must have ACTIVE status in this workspace.");
        }

        return manager;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

