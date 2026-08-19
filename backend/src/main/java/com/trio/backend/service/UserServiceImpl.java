package com.trio.backend.service;

import com.trio.backend.dto.auth.CreateUserRequest;
import com.trio.backend.dto.user.UpdateProfileRequest;
import com.trio.backend.dto.user.UpdateUserRequest;
import com.trio.backend.dto.user.UserProfileResponse;
import com.trio.backend.dto.user.UserResponse;
import com.trio.backend.dto.user.UserSearchCriteria;
import com.trio.backend.dto.user.UserStatisticsResponse;
import com.trio.backend.entity.ActivationToken;
import com.trio.backend.entity.Department;
import com.trio.backend.entity.Role;
import com.trio.backend.entity.Team;
import com.trio.backend.entity.TeamMember;
import com.trio.backend.entity.ids.TeamMemberId;
import com.trio.backend.entity.User;
import com.trio.backend.entity.UserHistory;
import com.trio.backend.entity.UserRole;
import com.trio.backend.entity.Workspace;
import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.entity.ids.UserRoleId;
import com.trio.backend.entity.ids.WorkspaceMemberId;
import com.trio.backend.enums.RoleName;
import com.trio.backend.enums.UserStatus;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.event.AccountActivationEmailRequestedEvent;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ConflictException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.UserMapper;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.InterviewParticipantRepository;
import com.trio.backend.repository.MentionRepository;
import com.trio.backend.repository.RoleRepository;
import com.trio.backend.repository.TeamMemberRepository;
import com.trio.backend.repository.TeamRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.repository.UserRoleRepository;
import com.trio.backend.repository.UserSpecification;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;
import com.trio.backend.security.user.CustomUserDetails;
import com.trio.backend.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MentionRepository mentionRepository;
    private final InterviewParticipantRepository interviewParticipantRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserHistoryService userHistoryService;
    private final AccountActivationService accountActivationService;
    private final ApplicationEventPublisher eventPublisher;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.activation.base-url:http://localhost:5173}")
    private String activationBaseUrl;

    @Override
    public UserResponse create(UUID workspaceId, CreateUserRequest request) {

        String email = StringUtils.normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists.");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));

        User user = userMapper.toEntity(request);
        user.setEmail(email);

        String tempPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setEnabled(false);
        user.setStatus(UserStatus.PENDING_ACTIVATION);

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findByIdAndWorkspace_Id(
                    request.getDepartmentId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found in this workspace."));
            user.setPrimaryDepartment(department);
        }

        User savedUser = userRepository.save(user);

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Role not found: " + request.getRole()));

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(savedUser.getId(), role.getId()))
                .user(savedUser)
                .role(role)
                .build();

        // The user role is persisted through the managed user's cascade. Persisting
        // it directly as well makes Hibernate try to manage the same composite key
        // twice when the activation-token query triggers an automatic flush.
        savedUser.getUserRoles().add(userRole);

        WorkspaceMember workspaceMember = WorkspaceMember.builder()
                .workspaceMemberId(new WorkspaceMemberId(workspaceId, savedUser.getId()))
                .workspace(workspace)
                .user(savedUser)
                .role(WorkspaceRole.MEMBER)
                .status(WorkspaceMemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        workspaceMemberRepository.save(workspaceMember);

        if (request.getTeamId() != null) {
            Team team = teamRepository.findByIdAndWorkspace_Id(request.getTeamId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found in this workspace."));
            TeamMember teamMember = TeamMember.builder()
                    .teamMemberId(new TeamMemberId(request.getTeamId(), savedUser.getId()))
                    .team(team)
                    .user(savedUser)
                    .status(WorkspaceMemberStatus.ACTIVE)
                    .build();
            teamMemberRepository.save(teamMember);
            savedUser.getTeamMembers().add(teamMember);
        }

        ActivationToken activationToken = accountActivationService.generateActivationToken(savedUser);

        String activationLink = activationBaseUrl + "/activate?token=" + activationToken.getToken();

        eventPublisher.publishEvent(new AccountActivationEmailRequestedEvent(
                this, savedUser.getId(), activationLink));

        UUID performedBy = getAuthenticatedUserId();
        userHistoryService.record(
                workspaceId, savedUser.getId(), performedBy,
                UserHistory.ACTION_USER_CREATED,
                null, "PENDING_ACTIVATION",
                "User " + savedUser.getFirstName() + " " + savedUser.getLastName() + " created with role: " + request.getRole().name()
        );

        return userMapper.toResponse(savedUser);
    }

    private String generateTemporaryPassword() {
        byte[] randomBytes = new byte[12];
        secureRandom.nextBytes(randomBytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @Override
    public UserResponse assignRoles(UUID workspaceId, UUID userId, Set<RoleName> newRoles) {

        User user = userRepository.findByIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in this workspace."));

        Set<UUID> newRoleIds = newRoles.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName)))
                .map(Role::getId)
                .collect(Collectors.toSet());

        Set<UserRole> toRemove = user.getUserRoles().stream()
                .filter(ur -> !newRoleIds.contains(ur.getRole().getId()))
                .collect(Collectors.toSet());

        Set<UUID> existingRoleIds = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getId())
                .collect(Collectors.toSet());

        for (UserRole ur : toRemove) {
            user.getUserRoles().remove(ur);
            userRoleRepository.delete(ur);
        }

        for (RoleName roleName : newRoles) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

            if (!existingRoleIds.contains(role.getId())) {
                UserRole userRole = UserRole.builder()
                        .id(new UserRoleId(user.getId(), role.getId()))
                        .user(user)
                        .role(role)
                        .build();
                userRoleRepository.save(userRole);
                user.getUserRoles().add(userRole);
            }
        }

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(UUID workspaceId, UUID id) {
        User user = userRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in this workspace."));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> search(UUID workspaceId, UserSearchCriteria criteria, Pageable pageable) {
        return userRepository.findAll(
                        UserSpecification.withCriteria(criteria, workspaceId),
                        pageable
                )
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll(UUID workspaceId) {
        return userRepository.findByWorkspaceId(workspaceId)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse update(UUID workspaceId, UUID id, UpdateUserRequest request) {
        User user = userRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in this workspace."));

        if (user.getStatus() == UserStatus.ARCHIVED && request.getStatus() != null && request.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Archived user cannot be modified. Restore first.");
        }

        UserStatus oldStatus = user.getStatus();

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) {
            String email = request.getEmail().trim().toLowerCase();
            if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
                throw new ConflictException("Email already exists.");
            }
            user.setEmail(email);
        }
        if (request.getMemberType() != null) user.setMemberType(request.getMemberType());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findByIdAndWorkspace_Id(
                    request.getDepartmentId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found in this workspace."));
            user.setPrimaryDepartment(department);
        } else if (request.getDepartmentId() == null && request.getFirstName() == null
                && request.getLastName() == null && request.getEmail() == null
                && request.getMemberType() == null && request.getStatus() == null
                && request.getRole() == null && request.getTeamId() == null) {
        }

        boolean statusChanged = oldStatus != user.getStatus();

        if (statusChanged && !isValidTransition(oldStatus, user.getStatus())) {
            throw new BadRequestException(
                    "Invalid status transition from " + oldStatus + " to " + user.getStatus()
            );
        }

        if (request.getRole() != null) {
            Role newRole = roleRepository.findByName(request.getRole())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));
            userRoleRepository.deleteAllByUser(user);
            user.getUserRoles().clear();
            UserRole userRole = UserRole.builder()
                    .id(new UserRoleId(user.getId(), newRole.getId()))
                    .user(user)
                    .role(newRole)
                    .build();
            userRoleRepository.save(userRole);
            user.getUserRoles().add(userRole);
        }

        if (Boolean.TRUE.equals(request.getRemoveTeam())) {
            teamMemberRepository.deleteAllByUser(user);
            user.getTeamMembers().clear();
        } else if (request.getTeamId() != null) {
            teamMemberRepository.deleteAllByUser(user);
            user.getTeamMembers().clear();
            Team team = teamRepository.findByIdAndWorkspace_Id(request.getTeamId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found in this workspace."));
            TeamMember teamMember = TeamMember.builder()
                    .teamMemberId(new TeamMemberId(request.getTeamId(), user.getId()))
                    .team(team)
                    .user(user)
                    .status(WorkspaceMemberStatus.ACTIVE)
                    .build();
            teamMemberRepository.save(teamMember);
            user.getTeamMembers().add(teamMember);
        }

        User updatedUser = userRepository.save(user);

        UUID performedBy = getAuthenticatedUserId();

        if (statusChanged) {
            String action = switch (user.getStatus()) {
                case ACTIVE -> oldStatus == UserStatus.PENDING_ACTIVATION
                        ? UserHistory.ACTION_ACTIVATED : UserHistory.ACTION_REACTIVATED;
                case INACTIVE -> UserHistory.ACTION_DEACTIVATED;
                case SUSPENDED -> UserHistory.ACTION_SUSPENDED;
                case ARCHIVED -> UserHistory.ACTION_ARCHIVED;
                case SOFT_DELETED -> UserHistory.ACTION_SOFT_DELETED;
                default -> UserHistory.ACTION_PROFILE_UPDATED;
            };
            userHistoryService.record(
                    workspaceId, updatedUser.getId(), performedBy,
                    action,
                    oldStatus.name(), user.getStatus().name(),
                    "User status changed from " + oldStatus + " to " + user.getStatus()
            );
        }

        if (request.getRole() != null) {
            userHistoryService.record(
                    workspaceId, updatedUser.getId(), performedBy,
                    UserHistory.ACTION_ROLE_ASSIGNED,
                    null, request.getRole().name(),
                    "Role changed to " + request.getRole().name()
            );
        }

        return userMapper.toResponse(updatedUser);
    }

    @Override
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = getAuthenticatedUser();

        if (user.getStatus() == UserStatus.ARCHIVED) {
            throw new ForbiddenException("Archived user cannot modify profile.");
        }

        String oldEmail = user.getEmail();
        String oldFirstName = user.getFirstName();
        String oldLastName = user.getLastName();
        String oldProfilePicture = user.getProfilePicture();

        String email = request.getEmail().trim().toLowerCase();
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already exists.");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(email);
        user.setProfilePicture(request.getProfilePicture());

        User updatedUser = userRepository.save(user);

        UUID performedBy = user.getId();

        if (!oldEmail.equals(email)) {
            userHistoryService.record(
                    null, user.getId(), performedBy,
                    UserHistory.ACTION_EMAIL_CHANGED,
                    oldEmail, email,
                    "Email changed from " + oldEmail + " to " + email
            );
        }

        boolean pictureChanged = (oldProfilePicture == null && request.getProfilePicture() != null)
                || (oldProfilePicture != null && !oldProfilePicture.equals(request.getProfilePicture()));
        if (pictureChanged) {
            userHistoryService.record(
                    null, user.getId(), performedBy,
                    UserHistory.ACTION_PROFILE_PICTURE_UPDATED,
                    null, null,
                    "Profile picture updated"
            );
        }

        if (!oldFirstName.equals(request.getFirstName()) || !oldLastName.equals(request.getLastName())) {
            userHistoryService.record(
                    null, user.getId(), performedBy,
                    UserHistory.ACTION_PROFILE_UPDATED,
                    null, null,
                    "Profile updated"
            );
        }

        return userMapper.toProfile(updatedUser);
    }

    @Override
    public void softDelete(UUID workspaceId, UUID id) {
        User user = userRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in this workspace."));

        if (user.getStatus() == UserStatus.SOFT_DELETED) {
            throw new ConflictException("User is already soft deleted.");
        }

        if (user.getStatus() == UserStatus.ARCHIVED) {
            throw new ForbiddenException("Archived user cannot be soft deleted. Restore first.");
        }

        user.setStatus(UserStatus.SOFT_DELETED);
        userRepository.save(user);

        UUID performedBy = getAuthenticatedUserId();
        userHistoryService.record(
                workspaceId, user.getId(), performedBy,
                UserHistory.ACTION_SOFT_DELETED,
                null, UserStatus.SOFT_DELETED.name(),
                "User soft deleted"
        );
    }

    @Override
    public void hardDelete(UUID workspaceId, UUID id) {
        User user = userRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in this workspace."));

        if (id.equals(getAuthenticatedUserId())) {
            throw new ConflictException("You cannot permanently remove your own account.");
        }

        if (!workspaceRepository.findAllByOwner_Id(id).isEmpty()) {
            throw new ConflictException(
                    "User owns a workspace and cannot be permanently removed. Transfer workspace ownership first."
            );
        }

        mentionRepository.deleteByUser_Id(id);
        interviewParticipantRepository.deleteByUser_Id(id);

        userRepository.delete(user);
    }

    @Override
    public UserResponse activate(UUID workspaceId, UUID id) {
        User user = userRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in this workspace."));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ConflictException("User is already active.");
        }

        if (user.getStatus() != UserStatus.PENDING_ACTIVATION) {
            throw new BadRequestException(
                    "Only pending activation users can be activated through this operation."
            );
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
        User activatedUser = userRepository.save(user);

        UUID performedBy = getAuthenticatedUserId();
        userHistoryService.record(
                workspaceId, activatedUser.getId(), performedBy,
                UserHistory.ACTION_ACTIVATED,
                UserStatus.PENDING_ACTIVATION.name(), UserStatus.ACTIVE.name(),
                "User activated"
        );

        return userMapper.toResponse(activatedUser);
    }

    @Override
    public UserResponse deactivate(UUID workspaceId, UUID id) {
        User user = userRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in this workspace."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Only active users can be deactivated.");
        }

        user.setStatus(UserStatus.INACTIVE);
        User deactivatedUser = userRepository.save(user);

        UUID performedBy = getAuthenticatedUserId();
        userHistoryService.record(
                workspaceId, deactivatedUser.getId(), performedBy,
                UserHistory.ACTION_DEACTIVATED,
                UserStatus.ACTIVE.name(), UserStatus.INACTIVE.name(),
                "User deactivated"
        );

        return userMapper.toResponse(deactivatedUser);
    }

    @Override
    public UserResponse suspend(UUID workspaceId, UUID id) {
        User user = userRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in this workspace."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Only active users can be suspended.");
        }

        user.setStatus(UserStatus.SUSPENDED);
        User suspendedUser = userRepository.save(user);

        UUID performedBy = getAuthenticatedUserId();
        userHistoryService.record(
                workspaceId, suspendedUser.getId(), performedBy,
                UserHistory.ACTION_SUSPENDED,
                UserStatus.ACTIVE.name(), UserStatus.SUSPENDED.name(),
                "User suspended"
        );

        return userMapper.toResponse(suspendedUser);
    }

    @Override
    public UserResponse reactivate(UUID workspaceId, UUID id) {
        User user = userRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in this workspace."));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new ConflictException("User is already active.");
        }

        if (user.getStatus() != UserStatus.INACTIVE && user.getStatus() != UserStatus.SUSPENDED) {
            throw new BadRequestException(
                    "Only inactive or suspended users can be reactivated."
            );
        }

        UserStatus oldStatus = user.getStatus();
        user.setStatus(UserStatus.ACTIVE);
        User reactivatedUser = userRepository.save(user);

        UUID performedBy = getAuthenticatedUserId();
        userHistoryService.record(
                workspaceId, reactivatedUser.getId(), performedBy,
                UserHistory.ACTION_REACTIVATED,
                oldStatus.name(), UserStatus.ACTIVE.name(),
                "User reactivated"
        );

        return userMapper.toResponse(reactivatedUser);
    }

    @Override
    public UserResponse archive(UUID workspaceId, UUID id) {
        User user = userRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in this workspace."));

        if (user.getStatus() == UserStatus.ARCHIVED) {
            throw new ConflictException("User is already archived.");
        }

        if (user.getStatus() == UserStatus.SOFT_DELETED) {
            throw new ForbiddenException("Soft deleted user cannot be archived.");
        }

        UserStatus oldStatus = user.getStatus();
        user.setStatus(UserStatus.ARCHIVED);
        user.setArchivedAt(Instant.now());
        User archivedUser = userRepository.save(user);

        UUID performedBy = getAuthenticatedUserId();
        userHistoryService.record(
                workspaceId, archivedUser.getId(), performedBy,
                UserHistory.ACTION_ARCHIVED,
                oldStatus.name(), UserStatus.ARCHIVED.name(),
                "User archived"
        );

        return userMapper.toResponse(archivedUser);
    }

    @Override
    public UserResponse restoreFromArchive(UUID workspaceId, UUID id) {
        User user = userRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in this workspace."));

        if (user.getStatus() != UserStatus.ARCHIVED) {
            throw new ConflictException("User is not archived.");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setArchivedAt(null);
        User restoredUser = userRepository.save(user);

        UUID performedBy = getAuthenticatedUserId();
        userHistoryService.record(
                workspaceId, restoredUser.getId(), performedBy,
                UserHistory.ACTION_RESTORED,
                UserStatus.ARCHIVED.name(), UserStatus.ACTIVE.name(),
                "User restored from archive"
        );

        return userMapper.toResponse(restoredUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatisticsResponse getStatistics(UUID workspaceId) {

        List<UserStatus> allStatuses = List.of(UserStatus.values());
        Map<UserStatus, Long> countByStatus = new HashMap<>();
        for (UserStatus status : allStatuses) {
            countByStatus.put(status, userRepository.countByWorkspaceIdAndStatus(workspaceId, status));
        }

        Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60);
        long recentHires = userRepository.countByWorkspaceIdAndCreatedAtAfter(workspaceId, thirtyDaysAgo);

        Map<String, Long> usersPerDepartment = new HashMap<>();
        for (Object[] row : userRepository.countPerDepartmentByWorkspaceId(workspaceId)) {
            String name = (String) row[0];
            Long count = (Long) row[1];
            usersPerDepartment.put(name != null ? name : "Unassigned", count);
        }

        Map<String, Long> usersPerTeam = new HashMap<>();
        for (Object[] row : userRepository.countPerTeamByWorkspaceId(workspaceId)) {
            usersPerTeam.put((String) row[0], (Long) row[1]);
        }

        Map<String, Long> usersPerRole = new HashMap<>();
        for (Object[] row : userRepository.countPerRoleByWorkspaceId(workspaceId)) {
            usersPerRole.put(((RoleName) row[0]).name(), (Long) row[1]);
        }

        return UserStatisticsResponse.builder()
                .totalUsers(countByStatus.values().stream().mapToLong(Long::longValue).sum())
                .activeUsers(countByStatus.getOrDefault(UserStatus.ACTIVE, 0L))
                .inactiveUsers(countByStatus.getOrDefault(UserStatus.INACTIVE, 0L))
                .suspendedUsers(countByStatus.getOrDefault(UserStatus.SUSPENDED, 0L))
                .archivedUsers(countByStatus.getOrDefault(UserStatus.ARCHIVED, 0L))
                .softDeletedUsers(countByStatus.getOrDefault(UserStatus.SOFT_DELETED, 0L))
                .pendingActivationUsers(countByStatus.getOrDefault(UserStatus.PENDING_ACTIVATION, 0L))
                .lockedUsers(countByStatus.getOrDefault(UserStatus.LOCKED, 0L))
                .usersPerDepartment(usersPerDepartment)
                .usersPerTeam(usersPerTeam)
                .usersPerRole(usersPerRole)
                .recentHires(recentHires)
                .build();
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails main)) {
            throw new ResourceNotFoundException("Authenticated user not found.");
        }

        return userRepository.findByEmail(main.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails main)) {
            return null;
        }

        return main.getId();
    }

    private boolean isValidTransition(UserStatus from, UserStatus to) {
        return switch (from) {
            case PENDING_ACTIVATION -> to == UserStatus.ACTIVE;
            case ACTIVE -> to == UserStatus.INACTIVE
                    || to == UserStatus.SUSPENDED
                    || to == UserStatus.ARCHIVED
                    || to == UserStatus.SOFT_DELETED;
            case INACTIVE -> to == UserStatus.ACTIVE
                    || to == UserStatus.ARCHIVED
                    || to == UserStatus.SOFT_DELETED;
            case SUSPENDED -> to == UserStatus.ACTIVE
                    || to == UserStatus.ARCHIVED
                    || to == UserStatus.SOFT_DELETED;
            case ARCHIVED -> to == UserStatus.ACTIVE;
            case SOFT_DELETED -> to == UserStatus.ACTIVE;
            case LOCKED -> to == UserStatus.ACTIVE;
        };
    }

}
