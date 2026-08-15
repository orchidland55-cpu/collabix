package com.trio.backend.service;

import com.trio.backend.dto.workspace.CreateWorkspaceRequest;
import com.trio.backend.dto.workspace.UpdateWorkspaceRequest;
import com.trio.backend.dto.workspace.WorkspaceResponse;
import com.trio.backend.dto.workspace.WorkspaceSummaryResponse;
import com.trio.backend.entity.User;
import com.trio.backend.entity.Workspace;
import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.entity.ids.WorkspaceMemberId;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ConflictException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.WorkspaceMapper;
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

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of the service management of the Workspaces de Collabix.
 *
 * <p>Cette class centralise alle la logical mÃƒÂ©tier liÃƒÂ©e aux Workspaces,
 * qui constituent le cÃ…â€œur functionnel de la plateforme. Un Workspace
 * reprÃƒÂ©sente the context de travail dans lequel all collaborations
 * s'perform.</p>
 *
 * <p><strong>ResponsabilitÃƒÂ©s :</strong></p>
 * <ul>
 *   <li>CrÃƒÂ©er a new Workspace and initialize le propriÃƒÂ©taire comme first member.</li>
 *   <li>Modify les information of a workspace (name, description).</li>
 *   <li>Supprimer (soft delete) un Workspace and archive alles its resources.</li>
 *   <li>RÃƒÂ©cupÃƒÂ©rer un Workspace spÃƒÂ©cifique avec vÃƒÂ©rification d'accÃƒÂ¨s.</li>
 *   <li>RÃƒÂ©cupÃƒÂ©rer the list of the Workspaces of a user.</li>
 *   <li>Validr les rÃƒÂ¨gles mÃƒÂ©tier (unicitÃƒÂ© du name, permissions, etc.).</li>
 *   <li>Calculer the statistics (namebre de members, ÃƒÂ©quipes).</li>
 *   <li>VÃƒÂ©rifier the permissions d'accÃƒÂ¨s (member, admin, propriÃƒÂ©taire).</li>
 * </ul>
 *
 * <p><strong>Collaborators :</strong></p>
 * <ul>
 *   <li>{@link WorkspaceRepository} pour management of the Workspaces.</li>
 *   <li>{@link WorkspaceMemberRepository} pour management of members.</li>
 *   <li>{@link UserRepository} pour rÃƒÂ©cupÃƒÂ©rer the user authentifiÃƒÂ©.</li>
 *   <li>{@link WorkspaceMapper} pour convertedr les entitÃƒÂ©s en DTOs.</li>
 *   <li>Spring Security pour identifier the currently authentifiÃƒÂ©.</li>
 * </ul>
 *
 * <p>All opÃƒÂ©rations sont exÃƒÂ©cutÃƒÂ©es dans un context transactional
 * in order to guarantee la cohÃƒÂ©rence des donnÃƒÂ©es et isolation des transactions.</p>
 *
 * <p><strong>HiÃƒÂ©rarchie mÃƒÂ©tier :</strong></p>
 * <pre>
 * Workspace
 *   Ã¢â€Å“Ã¢â€â‚¬ WorkspaceMember
 *   Ã¢â€â€š  Ã¢â€â€Ã¢â€â‚¬ User
 *   Ã¢â€Å“Ã¢â€â‚¬ Team
 *   Ã¢â€â€š  Ã¢â€â€Ã¢â€â‚¬ TeamMember
 *   Ã¢â€â€š     Ã¢â€â€Ã¢â€â‚¬ User
 *   Ã¢â€Å“Ã¢â€â‚¬ Tasks
 *   Ã¢â€Å“Ã¢â€â‚¬ Documents
 *   Ã¢â€Å“Ã¢â€â‚¬ Notifications
 *   Ã¢â€â€Ã¢â€â‚¬ Activities
 * </pre>
 *
 * @see WorkspaceService
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    /**
     * Repository pour management des entitÃƒÂ©s {@link Workspace}.
     * Permet les opÃƒÂ©rations CRUD sur les Workspaces.
     */
    private final WorkspaceRepository workspaceRepository;

    /**
     * Repository pour management des associations {@link WorkspaceMember}.
     * Allows to lier les users aux Workspaces et de gÃƒÂ©rer les rÃƒÂ´les/statuss.
     */
    private final WorkspaceMemberRepository workspaceMemberRepository;

    /**
     * Repository pour management des entitÃƒÂ©s {@link User}.
     * Allows to rÃƒÂ©cupÃƒÂ©rer the user authentifiÃƒÂ© et d'others users.
     */
    private final UserRepository userRepository;

    /**
     * Repository pour management des entitÃƒÂ©s {@link com.trio.backend.entity.Team}.
     * Allows to calculer correctment the namebre d'ÃƒÂ©quipes associÃƒÂ©es ÃƒÂ  a workspace.
     */
    private final TeamRepository teamRepository;

    /**
     * Mapper pour convertedr les entitÃƒÂ©s Workspace en DTOs et inversement.
     * Facilite la sÃƒÂ©paration entre the layer de persistance et the layer de prÃƒÂ©sentation.
     */
    private final WorkspaceMapper workspaceMapper;

    private final com.trio.backend.repository.ProjectRepository projectRepository;

    /**
     * Creates a nouveau Workspace.
     *
     * <p>Cette mÃƒÂ©thode performs the opÃƒÂ©rations following :</p>
     * <ol>
     *   <li>RÃƒÂ©cupÃƒÂ¨re the currently authentifiÃƒÂ©.</li>
     *   <li>VÃƒÂ©rifie que the name of the workspace does not exist dÃƒÂ©jÃƒÂ  pour ce propriÃƒÂ©taire.</li>
     *   <li>Convertedt la requÃƒÂªte en entitÃƒÂ© {@link Workspace}.</li>
     *   <li>DÃƒÂ©finit le propriÃƒÂ©taire et the status ÃƒÂ  ACTIVE.</li>
     *   <li>Persiste le Workspace.</li>
     *   <li>Adds le propriÃƒÂ©taire comme first member avec le rÃƒÂ´le OWNER.</li>
     *   <li>Returns the DTO de rÃƒÂ©ponse avec Statistics.</li>
     * </ol>
     *
     * <p><strong>RÃƒÂ¨gles mÃƒÂ©tier :</strong></p>
     * <ul>
     *   <li>The name doit ÃƒÂªtre unique pour ce propriÃƒÂ©taire.</li>
     *   <li>L'user authentifiÃƒÂ© devient propriÃƒÂ©taire of the workspace crÃƒÂ©ÃƒÂ©.</li>
     *   <li>Le Workspace est crÃƒÂ©ÃƒÂ© avec the status ACTIVE.</li>
     *   <li>Le propriÃƒÂ©taire est automaticment ajoutÃƒÂ© comme first member avec le rÃƒÂ´le OWNER.</li>
     * </ul>
     *
     * @param request les donnÃƒÂ©es de crÃƒÂ©ation of the workspace
     * @return la rÃƒÂ©ponse complÃƒÂ¨te of the workspace crÃƒÂ©ÃƒÂ© avec Statistics
     * @throws BadRequestException si aucun user n'est authentifiÃƒÂ©
     * @throws ConflictException si un Workspace avec le mÃƒÂªme name existe dÃƒÂ©jÃƒÂ  pour ce propriÃƒÂ©taire
     */
    @Override
    public WorkspaceResponse create(CreateWorkspaceRequest request) {

        User owner = getAuthenticatedUser();

        if (workspaceRepository.existsByOwner_IdAndName(owner.getId(), request.getName())) {
            throw new ConflictException("Workspace with this name already exists for this owner.");
        }

        Workspace workspace = workspaceMapper.toEntity(request);
        workspace.setOwner(owner);
        workspace.setStatus(WorkspaceStatus.ACTIVE);

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // Addsr le propriÃƒÂ©taire comme first member avec le rÃƒÂ´le OWNER
        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspaceMemberId(new WorkspaceMemberId(
                        savedWorkspace.getId(),
                        owner.getId()
                ))
                .workspace(savedWorkspace)
                .user(owner)
                .role(WorkspaceRole.OWNER)
                .status(WorkspaceMemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        workspaceMemberRepository.save(ownerMember);

        return buildWorkspaceResponse(savedWorkspace);
    }

    /**
     * Met ÃƒÂ  jour les information of a workspace.
     *
     * <p>Seuls the name et la description peuvent ÃƒÂªtre modifiÃƒÂ©s. The fields
     * immutables (propriÃƒÂ©taire, status, members, ÃƒÂ©quipes, timestamps)
     * cannot pas ÃƒÂªtre modifiÃƒÂ©s via cette mÃƒÂ©thode.</p>
     *
     * <p>Cette mÃƒÂ©thode performs the opÃƒÂ©rations following :</p>
     * <ol>
     *   <li>RÃƒÂ©cupÃƒÂ¨re le Workspace par its ID.</li>
     *   <li>VÃƒÂ©rifie que the user authentifiÃƒÂ© a the permissions nÃƒÂ©cessaires.</li>
     *   <li>VÃƒÂ©rifie l'unicitÃƒÂ© du nouveau name si modifiÃƒÂ©.</li>
     *   <li>Applique les modifications (fields null sont ignorÃƒÂ©s).</li>
     *   <li>Persiste les modifications.</li>
     *   <li>Returns the DTO de rÃƒÂ©ponse avec Statistics.</li>
     * </ol>
     *
     * <p><strong>RÃƒÂ¨gles mÃƒÂ©tier :</strong></p>
     * <ul>
     *   <li>Seuls le propriÃƒÂ©taire ou un administrator peuvent modify le Workspace.</li>
     *   <li>The name updated doit rester unique pour ce propriÃƒÂ©taire.</li>
     *   <li>The fields null are not appliquÃƒÂ©s (partial update).</li>
     * </ul>
     *
     * @param workspaceId the ID of the Workspace ÃƒÂ  mettre ÃƒÂ  jour
     * @param request les donnÃƒÂ©es de updated
     * @return la rÃƒÂ©ponse complÃƒÂ¨te of the workspace modifiÃƒÂ© avec Statistics
     * @throws ResourceNotFoundException si le Workspace does not exist
     * @throws ForbiddenException si the user n'a pas the permission de modification
     * @throws ConflictException si the name proposÃƒÂ© existe dÃƒÂ©jÃƒÂ  pour ce propriÃƒÂ©taire
     */
    @Override
    public WorkspaceResponse update(UUID workspaceId, UpdateWorkspaceRequest request) {

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));

        if (!isAdmin(workspaceId)) {
            throw new ForbiddenException("You do not have permission to update this Workspace.");
        }

        // VÃƒÂ©rifier l'unicitÃƒÂ© du name si modifiÃƒÂ©
        if (request.getName() != null
                && !request.getName().equals(workspace.getName())
                && workspaceRepository.existsByOwner_IdAndName(workspace.getOwner().getId(), request.getName())) {

            throw new ConflictException("Workspace with this name already exists for this owner.");
        }

        workspaceMapper.updateWorkspace(request, workspace);

        Workspace updatedWorkspace = workspaceRepository.save(workspace);

        return buildWorkspaceResponse(updatedWorkspace);
    }

    /**
     * RÃƒÂ©cupÃƒÂ¨re un Workspace par its ID.
     *
     * <p>Returns the information complÃƒÂ¨tes of the workspace avec the statistics
     * (namebre de members, namebre d'ÃƒÂ©quipes). L'user authentifiÃƒÂ© doit
     * ÃƒÂªtre member of the workspace pour pouvoir y accÃƒÂ©der.</p>
     *
     * <p>OpÃƒÂ©ration en lecture seule.</p>
     *
     * @param workspaceId the ID of the Workspace
     * @return la rÃƒÂ©ponse complÃƒÂ¨te of the workspace avec Statistics
     * @throws ResourceNotFoundException si le Workspace does not exist
     * @throws ForbiddenException si the user is not member of the workspace
     */
    @Override
    @Transactional(readOnly = true)
    public WorkspaceResponse getById(UUID workspaceId) {

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));

        if (!isMember(workspaceId)) {
            throw new ForbiddenException("You are not a member of this Workspace.");
        }

        return buildWorkspaceResponse(workspace);
    }

    /**
     * RÃƒÂ©cupÃƒÂ¨re the list de all Workspaces actives de the user authentifiÃƒÂ©.
     *
     * <p>Returns uniquement les Workspaces pour lesquels the user est
     * member active (status = ACTIVE). Les Workspaces sont sortÃƒÂ©s by date
     * de crÃƒÂ©ation dÃƒÂ©ascending (plus rÃƒÂ©cents d'abord).</p>
     *
     * <p>OpÃƒÂ©ration en lecture seule.</p>
     *
     * <p><strong>RÃƒÂ¨gles mÃƒÂ©tier :</strong></p>
     * <ul>
     *   <li>Seuls les Workspaces avec status ACTIVE sont retournÃƒÂ©s.</li>
     *   <li>Seuls the members avec status ACTIVE sont considÃƒÂ©rÃƒÂ©s.</li>
     *   <li>Les Workspaces archivÃƒÂ©s are not retournÃƒÂ©s.</li>
     * </ul>
     *
     * @return list of the Workspaces de the user sortÃƒÂ©s by date dÃƒÂ©ascending
     */
    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceSummaryResponse> listByCurrentUser(String search, String sort, String order) {

        User user = getAuthenticatedUser();

        List<WorkspaceMember> members = workspaceMemberRepository.findActiveWorkspacesByUserId(
                user.getId(),
                WorkspaceMemberStatus.ACTIVE
        );

        List<Workspace> workspaces = members.stream()
                .filter(wm -> wm.getWorkspace().getStatus() == WorkspaceStatus.ACTIVE)
                .map(WorkspaceMember::getWorkspace)
                .collect(Collectors.toList());

        if (workspaces.isEmpty()) return List.of();

        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            workspaces = workspaces.stream()
                    .filter(w -> w.getName().toLowerCase().contains(searchLower))
                    .toList();
            if (workspaces.isEmpty()) return List.of();
        }

        Comparator<Workspace> wsComparator;
        if ("name".equals(sort)) {
            wsComparator = Comparator.comparing(Workspace::getName, String.CASE_INSENSITIVE_ORDER);
        } else {
            wsComparator = Comparator.comparing(Workspace::getCreatedAt);
        }
        if (!"asc".equalsIgnoreCase(order)) {
            wsComparator = wsComparator.reversed();
        }
        workspaces = workspaces.stream().sorted(wsComparator).toList();

        List<UUID> workspaceIds = workspaces.stream().map(Workspace::getId).toList();

        Map<UUID, Long> memberCounts = workspaceMemberRepository
                .countByWorkspaceIdsAndStatus(workspaceIds, WorkspaceMemberStatus.ACTIVE)
                .stream().collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));

        Map<UUID, Long> teamCounts = teamRepository
                .countByWorkspaceIdsAndStatus(workspaceIds, WorkspaceStatus.ACTIVE)
                .stream().collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));

        return workspaces.stream()
                .map(w -> {
                    WorkspaceSummaryResponse r = workspaceMapper.toSummary(w);
                    r.setMemberCount(memberCounts.getOrDefault(w.getId(), 0L));
                    r.setTeamCount(teamCounts.getOrDefault(w.getId(), 0L));
                    return r;
                })
                .toList();
    }

    /**
     * RÃƒÂ©cupÃƒÂ¨re the list rÃƒÂ©sumÃƒÂ©e de all Workspaces actives de the user authentifiÃƒÂ©.
     *
     * <p>Similaire ÃƒÂ  {@link #listByCurrentUser()}, mais Returns des rÃƒÂ©ponses
     * rÃƒÂ©sumÃƒÂ©es (WorkspaceSummaryResponse) optimisÃƒÂ©es pour les vues listÃƒÂ©es ou
     * les dashboards.</p>
     *
     * <p>OpÃƒÂ©ration en lecture seule.</p>
     *
     * @return list rÃƒÂ©sumÃƒÂ©e of the Workspaces de the user
     */
    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceSummaryResponse> listSummaryByCurrentUser() {

        return listByCurrentUser();
    }

    /**
     * Supprime (soft delete) un Workspace.
     *
     * <p>Le Workspace is not physiquement deleted, its status passe de ACTIVE
     * ÃƒÂ  ARCHIVED. All ressources associÃƒÂ©es (members, ÃƒÂ©quipes, tÃƒÂ¢ches, etc.)
     * restent intactes mais inaccessible through the requÃƒÂªtes normal.</p>
     *
     * <p>Cette mÃƒÂ©thode performs the opÃƒÂ©rations following :</p>
     * <ol>
     *   <li>RÃƒÂ©cupÃƒÂ¨re le Workspace par its ID.</li>
     *   <li>VÃƒÂ©rifie que the user authentifiÃƒÂ© est propriÃƒÂ©taire or administrator.</li>
     *   <li>Lowcule the status of the workspace ÃƒÂ  ARCHIVED.</li>
     *   <li>Persiste modification.</li>
     * </ol>
     *
     * <p><strong>RÃƒÂ¨gles mÃƒÂ©tier :</strong></p>
     * <ul>
     *   <li>Seul le propriÃƒÂ©taire ou un administrator peuvent supprimer un Workspace.</li>
     *   <li>La deletion est permanent (pas de restauration possible dans cette version).</li>
     *   <li>Un Workspace dÃƒÂ©jÃƒÂ  archivÃƒÂ© peut ÃƒÂªtre deleted ÃƒÂ  nouveau (idempotent).</li>
     *   <li>Les donnÃƒÂ©es are not dÃƒÂ©finitivement deleteds (soft delete).</li>
     * </ul>
     *
     * @param workspaceId the ID of the Workspace ÃƒÂ  supprimer
     * @throws ResourceNotFoundException si le Workspace does not exist
     * @throws ForbiddenException si the user n'a pas the permission de deletion
     */
    @Override
    public void delete(UUID workspaceId) {

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));

        if (!isAdmin(workspaceId)) {
            throw new ForbiddenException("You do not have permission to delete this Workspace.");
        }

        workspace.setStatus(WorkspaceStatus.ARCHIVED);

        workspaceRepository.save(workspace);
    }

    /**
     * VÃƒÂ©rifie si the user authentifiÃƒÂ© is a member of a workspace.
     *
     * <p>Returns true si the user is a member active of the workspace
     * (indÃƒÂ©pendamment de son rÃƒÂ´le).</p>
     *
     * <p>OpÃƒÂ©ration en lecture seule.</p>
     *
     * @param workspaceId the ID of the Workspace
     * @return true si the user is a member active, false sinon
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isMember(UUID workspaceId) {

        User user = getAuthenticatedUser();

        return workspaceMemberRepository.findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(
                workspaceId,
                user.getId()
        )
                .map(wm -> wm.getStatus() == WorkspaceMemberStatus.ACTIVE)
                .orElse(false);
    }

    /**
     * VÃƒÂ©rifie si the user authentifiÃƒÂ© est propriÃƒÂ©taire of a workspace.
     *
     * <p>OpÃƒÂ©ration en lecture seule.</p>
     *
     * @param workspaceId the ID of the Workspace
     * @return true si the user est propriÃƒÂ©taire, false sinon
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isOwner(UUID workspaceId) {

        User user = getAuthenticatedUser();

        return workspaceRepository.findById(workspaceId)
                .map(ws -> ws.getOwner().getId().equals(user.getId()))
                .orElse(false);
    }

    /**
     * VÃƒÂ©rifie si the user authentifiÃƒÂ© est administrator of a workspace.
     *
     * <p>Un administrator est un user avec le rÃƒÂ´le ADMIN ou OWNER.</p>
     *
     * <p>OpÃƒÂ©ration en lecture seule.</p>
     *
     * @param workspaceId the ID of the Workspace
     * @return true si the user est administrator, false sinon
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isAdmin(UUID workspaceId) {

        User user = getAuthenticatedUser();

        return workspaceMemberRepository.existsWithRole(
                workspaceId,
                user.getId(),
                WorkspaceRole.ADMIN
        ) || isOwner(workspaceId);
    }

    /**
     * RÃƒÂ©cupÃƒÂ¨re the currently authentifiÃƒÂ©.
     *
     * <p>MÃƒÂ©thode utilitaire privÃƒÂ©e qui extracted les information de the user
     * depuis the context de sÃƒÂ©curitÃƒÂ© Spring Security.</p>
     *
     * @return l'entitÃƒÂ© {@link User} correspondssing ÃƒÂ  the user authentifiÃƒÂ©
     * @throws BadRequestException si aucun user authentifiÃƒÂ© n'est found
     */
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

    /**
     * Construit une rÃƒÂ©ponse complÃƒÂ¨te {@link WorkspaceResponse} avec Statistics.
     *
     * <p>Calcule the namebre de members actives et the namebre d'ÃƒÂ©quipes of the workspace.</p>
     *
     * @param workspace l'entitÃƒÂ© Workspace
     * @return la rÃƒÂ©ponse complÃƒÂ¨te avec Statistics
     */
    private WorkspaceResponse buildWorkspaceResponse(Workspace workspace) {

        WorkspaceResponse response = workspaceMapper.toResponse(workspace);

        long memberCount = workspaceMemberRepository.countByWorkspace_IdAndStatus(
                workspace.getId(),
                WorkspaceMemberStatus.ACTIVE
        );

        long teamCount = teamRepository.countByWorkspace_IdAndStatus(workspace.getId(), WorkspaceStatus.ACTIVE);

        long projectCount = 0;
        try {
            projectCount = projectRepository.countByWorkspaceIdAndStatus(workspace.getId(), WorkspaceStatus.ACTIVE);
        } catch (Exception e) {
            log.warn("Failed to count projects for workspace {}: {}", workspace.getId(), e.getMessage());
        }

        response.setMemberCount(memberCount);
        response.setTeamCount(teamCount);
        response.setProjectCount(projectCount);

        User user = getAuthenticatedUser();
        workspaceMemberRepository
                .findByWorkspaceMemberId_WorkspaceIdAndWorkspaceMemberId_UserId(workspace.getId(), user.getId())
                .ifPresent(member -> response.setMyRole(member.getRole()));

        return response;
    }

    @Override
    public void archive(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));
        if (!isAdmin(workspaceId)) {
            throw new ForbiddenException("You do not have permission to archive this Workspace.");
        }
        workspace.setStatus(WorkspaceStatus.ARCHIVED);
        workspaceRepository.save(workspace);
    }

    @Override
    public void restore(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));
        if (!isAdmin(workspaceId)) {
            throw new ForbiddenException("You do not have permission to restore this Workspace.");
        }
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspaceRepository.save(workspace);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceSummaryResponse> listArchived() {
        User user = getAuthenticatedUser();
        List<WorkspaceMember> members = workspaceMemberRepository.findActiveWorkspacesByUserId(
                user.getId(),
                WorkspaceMemberStatus.ACTIVE
        );
        List<Workspace> archivedWorkspaces = members.stream()
                .map(WorkspaceMember::getWorkspace)
                .filter(w -> w.getStatus() == WorkspaceStatus.ARCHIVED)
                .toList();
        if (archivedWorkspaces.isEmpty()) return List.of();
        List<UUID> workspaceIds = archivedWorkspaces.stream().map(Workspace::getId).toList();
        Map<UUID, Long> memberCounts = workspaceMemberRepository
                .countByWorkspaceIdsAndStatus(workspaceIds, WorkspaceMemberStatus.ACTIVE)
                .stream().collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
        Map<UUID, Long> teamCounts = teamRepository
                .countByWorkspaceIdsAndStatus(workspaceIds, WorkspaceStatus.ACTIVE)
                .stream().collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
        return archivedWorkspaces.stream()
                .map(w -> {
                    WorkspaceSummaryResponse r = workspaceMapper.toSummary(w);
                    r.setMemberCount(memberCounts.getOrDefault(w.getId(), 0L));
                    r.setTeamCount(teamCounts.getOrDefault(w.getId(), 0L));
                    return r;
                })
                .toList();
    }

}
