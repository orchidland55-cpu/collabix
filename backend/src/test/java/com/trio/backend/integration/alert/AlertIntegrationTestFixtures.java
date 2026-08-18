package com.trio.backend.integration.alert;

import com.trio.backend.entity.*;
import com.trio.backend.entity.ids.RolePermissionId;
import com.trio.backend.entity.ids.UserRoleId;
import com.trio.backend.entity.ids.WorkspaceMemberId;
import com.trio.backend.enums.*;
import com.trio.backend.repository.*;
import com.trio.backend.security.jwt.JwtService;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Builds a workspace with two users holding ALERT permissions, used to verify
 * the alerts module's REST API and ownership isolation.
 */
@Getter
public class AlertIntegrationTestFixtures {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    private UUID workspaceId;
    private User userA;
    private String userAToken;
    private User userB;
    private String userBToken;

    public AlertIntegrationTestFixtures(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EntityManager entityManager,
            TransactionTemplate transactionTemplate) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }

    public void seed() {
        transactionTemplate.executeWithoutResult(status -> seedInTransaction());
    }

    private void seedInTransaction() {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        Role memberRole = ensureMemberRole();

        userA = createUser("alert-a-" + runId + "@test.local", memberRole);
        userAToken = tokenFor(userA);

        userB = createUser("alert-b-" + runId + "@test.local", memberRole);
        userBToken = tokenFor(userB);

        Workspace workspace = Workspace.builder()
                .name("Alert Test Workspace " + runId)
                .status(WorkspaceStatus.ACTIVE)
                .owner(userA)
                .build();
        workspace = workspaceRepository.save(workspace);
        workspaceId = workspace.getId();

        workspaceMemberRepository.save(memberRow(workspace, userA));
        workspaceMemberRepository.save(memberRow(workspace, userB));

        entityManager.flush();
        entityManager.clear();
    }

    public String alertsPath() {
        return "/api/workspaces/" + workspaceId + "/alerts";
    }

    private Role ensureMemberRole() {
        Role role = roleRepository.findByName(RoleName.MEMBER).orElseGet(() ->
                roleRepository.save(Role.builder().name(RoleName.MEMBER).description(RoleName.MEMBER.name()).build()));

        for (String code : List.of("ALERT_READ", "ALERT_UPDATE", "ALERT_DELETE")) {
            Permission permission = permissionRepository.findByCode(code).orElseGet(() ->
                    permissionRepository.save(Permission.builder()
                            .code(code)
                            .displayName(code)
                            .description(code)
                            .build()));

            UUID roleId = role.getId();
            UUID permissionId = permission.getId();
            if (rolePermissionRepository.findById(new RolePermissionId(roleId, permissionId)).isEmpty()) {
                rolePermissionRepository.save(RolePermission.builder()
                        .id(new RolePermissionId(roleId, permissionId))
                        .role(role)
                        .permission(permission)
                        .build());
            }
        }
        entityManager.flush();
        entityManager.clear();
        return roleRepository.findByNameWithPermissions(RoleName.MEMBER).orElseThrow();
    }

    private User createUser(String email, Role role) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("Test@123456"))
                .firstName("Test")
                .lastName("User")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .enabled(true)
                .build();
        user = userRepository.save(user);

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(user.getId(), role.getId()))
                .user(user)
                .role(role)
                .build();
        user.getUserRoles().add(userRole);
        user = userRepository.save(user);
        entityManager.flush();
        return user;
    }

    private WorkspaceMember memberRow(Workspace workspace, User user) {
        return WorkspaceMember.builder()
                .workspaceMemberId(new WorkspaceMemberId(workspace.getId(), user.getId()))
                .workspace(workspace)
                .user(user)
                .role(WorkspaceRole.MEMBER)
                .status(WorkspaceMemberStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();
    }

    private String tokenFor(User user) {
        entityManager.flush();
        entityManager.clear();
        User loaded = userRepository.findByEmailWithRolesAndPermissions(user.getEmail()).orElseThrow();
        assertFalse(loaded.getUserRoles().isEmpty(), "User must have roles for token generation");
        String token = jwtService.generateAccessToken(loaded);
        assertFalse(
                jwtService.extractPermissions(token).isEmpty(),
                "Token must carry permissions for " + user.getEmail());
        return token;
    }
}