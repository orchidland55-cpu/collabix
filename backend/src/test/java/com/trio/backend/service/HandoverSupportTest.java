package com.trio.backend.service;

import com.trio.backend.entity.Department;
import com.trio.backend.entity.User;
import com.trio.backend.entity.Workspace;
import com.trio.backend.enums.MemberType;
import com.trio.backend.enums.UserStatus;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.repository.HandoverTimelineEventRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;
import com.trio.backend.security.user.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandoverSupportTest {

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private HandoverTimelineEventRepository timelineEventRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HandoverSupport support;

    private UUID userId;
    private UUID wsId;
    private UUID myDeptId;
    private UUID otherDeptId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        wsId = UUID.randomUUID();
        myDeptId = UUID.randomUUID();
        otherDeptId = UUID.randomUUID();

        Department myDept = new Department();
        ReflectionTestUtils.setField(myDept, "id", myDeptId);

        user = User.builder()
                .email("member@example.com")
                .password("secret")
                .firstName("Member")
                .lastName("User")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        user.setPrimaryDepartment(myDept);

        CustomUserDetails principal = new CustomUserDetails(user, List.of());
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubAsWorkspaceAdmin() {
        when(workspaceMemberRepository.existsWithRole(eq(wsId), eq(userId), eq(WorkspaceRole.ADMIN))).thenReturn(true);
    }

    private void stubAsWorkspaceMember() {
        when(workspaceMemberRepository.existsWithRole(eq(wsId), eq(userId), eq(WorkspaceRole.ADMIN))).thenReturn(false);
        when(workspaceRepository.findById(wsId)).thenReturn(Optional.empty());
    }

    @Test
    void assertCanViewDepartmentJournalAllowsAdminForAnyDepartment() {
        stubAsWorkspaceAdmin();

        assertDoesNotThrow(() -> support.assertCanViewDepartmentJournal(wsId, otherDeptId));
    }

    @Test
    void assertCanViewDepartmentJournalAllowsMemberForOwnDepartment() {
        stubAsWorkspaceMember();

        assertDoesNotThrow(() -> support.assertCanViewDepartmentJournal(wsId, myDeptId));
    }

    @Test
    void assertCanViewDepartmentJournalRejectsMemberForOtherDepartment() {
        stubAsWorkspaceMember();

        assertThrows(ForbiddenException.class, () -> support.assertCanViewDepartmentJournal(wsId, otherDeptId));
    }

    @Test
    void resolveAccessibleDepartmentLocksMemberToOwnDepartment() {
        stubAsWorkspaceMember();

        assertEquals(myDeptId, support.resolveAccessibleDepartment(wsId, null));
        assertEquals(myDeptId, support.resolveAccessibleDepartment(wsId, myDeptId));
    }

    @Test
    void resolveAccessibleDepartmentRejectsMemberRequestingOtherDepartment() {
        stubAsWorkspaceMember();

        assertThrows(ForbiddenException.class, () -> support.resolveAccessibleDepartment(wsId, otherDeptId));
    }

    @Test
    void resolveAccessibleDepartmentAllowsAdminToPickAnyDepartment() {
        stubAsWorkspaceAdmin();

        assertEquals(otherDeptId, support.resolveAccessibleDepartment(wsId, otherDeptId));
        assertNull(support.resolveAccessibleDepartment(wsId, null));
    }

    @Test
    void currentUserDepartmentIdRejectsUserWithoutDepartment() {
        user.setPrimaryDepartment(null);

        assertThrows(ForbiddenException.class, () -> support.currentUserDepartmentId());
    }

    @Test
    void isWorkspaceManagerDetectsManagerRole() {
        when(workspaceMemberRepository.existsWithRole(eq(wsId), eq(userId), eq(WorkspaceRole.MANAGER))).thenReturn(true);

        assertTrue(support.isWorkspaceManager(wsId, userId));
    }

    @Test
    void isWorkspaceManagerReturnsFalseWhenNotManager() {
        when(workspaceMemberRepository.existsWithRole(eq(wsId), eq(userId), eq(WorkspaceRole.MANAGER))).thenReturn(false);

        assertFalse(support.isWorkspaceManager(wsId, userId));
    }

    @Test
    void isWorkspaceAdminOrOwnerDetectsOwner() {
        when(workspaceMemberRepository.existsWithRole(eq(wsId), eq(userId), eq(WorkspaceRole.ADMIN))).thenReturn(false);
        Workspace ws = new Workspace();
        ws.setOwner(user);
        ReflectionTestUtils.setField(ws, "id", wsId);
        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(ws));

        assertTrue(support.isWorkspaceAdminOrOwner(wsId, userId));
    }
}
