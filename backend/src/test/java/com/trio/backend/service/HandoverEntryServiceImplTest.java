package com.trio.backend.service;

import com.trio.backend.dto.organisation.handover.CreateHandoverEntryRequest;
import com.trio.backend.dto.organisation.handover.HandoverEntryResponse;
import com.trio.backend.dto.organisation.handover.HandoverStatusUpdateRequest;
import com.trio.backend.dto.organisation.handover.UpdateHandoverEntryRequest;
import com.trio.backend.entity.*;
import com.trio.backend.entity.HandoverEntry.HandoverStatus;
import com.trio.backend.enums.*;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.HandoverEntryMapper;
import com.trio.backend.repository.HandoverEntryRepository;
import com.trio.backend.repository.ProjectRepository;
import com.trio.backend.repository.TaskRepository;
import com.trio.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandoverEntryServiceImplTest {

    @Mock
    private HandoverEntryRepository handoverEntryRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HandoverEntryMapper handoverEntryMapper;
    @Mock
    private HandoverSupport support;

    @InjectMocks
    private HandoverEntryServiceImpl handoverEntryService;

    private User actor;
    private User receiver;
    private Workspace workspace;
    private Department department;
    private Project project;
    private HandoverEntry exampleEntry;
    private HandoverEntryResponse exampleResponse;
    private UUID wsId;
    private UUID deptId;
    private UUID projId;
    private UUID entryId;
    private UUID userId;
    private UUID receiverId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        receiverId = UUID.randomUUID();
        wsId = UUID.randomUUID();
        deptId = UUID.randomUUID();
        projId = UUID.randomUUID();
        entryId = UUID.randomUUID();

        actor = User.builder()
                .email("user@example.com")
                .password("secret")
                .firstName("Test")
                .lastName("User")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(actor, "id", userId);

        receiver = User.builder()
                .email("receiver@example.com")
                .password("secret")
                .firstName("Rece")
                .lastName("iver")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(receiver, "id", receiverId);

        workspace = Workspace.builder()
                .name("TestWorkspace")
                .status(WorkspaceStatus.ACTIVE)
                .owner(actor)
                .build();
        ReflectionTestUtils.setField(workspace, "id", wsId);

        department = Department.builder()
                .name("TestDept")
                .status(WorkspaceStatus.ACTIVE)
                .workspace(workspace)
                .build();
        ReflectionTestUtils.setField(department, "id", deptId);

        project = Project.builder()
                .name("TestProject")
                .status(WorkspaceStatus.ACTIVE)
                .department(department)
                .build();
        ReflectionTestUtils.setField(project, "id", projId);

        exampleEntry = HandoverEntry.builder()
                .workspace(workspace)
                .department(department)
                .project(project)
                .sender(actor)
                .receiver(receiver)
                .title("Handover title")
                .content("Handover content")
                .priority(HandoverEntry.Priority.MEDIUM)
                .status(HandoverStatus.DRAFT)
                .build();
        ReflectionTestUtils.setField(exampleEntry, "id", entryId);

        exampleResponse = new HandoverEntryResponse();

        lenient().when(support.currentUserId()).thenReturn(userId);
        lenient().when(support.currentUserDepartmentId()).thenReturn(deptId);
        lenient().when(support.userDisplayName(any())).thenReturn("Test User");
    }

    @Test
    void createShouldSucceed() {
        CreateHandoverEntryRequest request = new CreateHandoverEntryRequest();
        ReflectionTestUtils.setField(request, "departmentId", deptId);
        ReflectionTestUtils.setField(request, "projectId", projId);
        ReflectionTestUtils.setField(request, "receiverId", receiverId);

        when(projectRepository.findByIdAndDepartment_Id(projId, deptId)).thenReturn(Optional.of(project));
        when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));
        when(handoverEntryMapper.toEntity(request)).thenReturn(new HandoverEntry());
        when(handoverEntryRepository.save(any(HandoverEntry.class))).thenReturn(exampleEntry);
        when(handoverEntryMapper.toResponse(exampleEntry)).thenReturn(exampleResponse);

        HandoverEntryResponse result = handoverEntryService.create(wsId, request);

        assertNotNull(result);
        verify(handoverEntryRepository).save(any(HandoverEntry.class));
        verify(support).addTimelineEvent(any(HandoverEntry.class), any(), anyString(), any(UUID.class));
    }

    @Test
    void createShouldThrowWhenProjectNotFound() {
        CreateHandoverEntryRequest request = new CreateHandoverEntryRequest();
        ReflectionTestUtils.setField(request, "departmentId", deptId);
        ReflectionTestUtils.setField(request, "projectId", projId);
        ReflectionTestUtils.setField(request, "receiverId", receiverId);
        when(projectRepository.findByIdAndDepartment_Id(projId, deptId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> handoverEntryService.create(wsId, request));
    }

    @Test
    void createShouldThrowWhenProjectInactive() {
        project.setStatus(WorkspaceStatus.ARCHIVED);
        CreateHandoverEntryRequest request = new CreateHandoverEntryRequest();
        ReflectionTestUtils.setField(request, "departmentId", deptId);
        ReflectionTestUtils.setField(request, "projectId", projId);
        ReflectionTestUtils.setField(request, "receiverId", receiverId);
        when(projectRepository.findByIdAndDepartment_Id(projId, deptId)).thenReturn(Optional.of(project));

        assertThrows(ResourceNotFoundException.class,
                () -> handoverEntryService.create(wsId, request));
    }

    @Test
    void createShouldThrowWhenUserIsWorkspaceAdmin() {
        CreateHandoverEntryRequest request = new CreateHandoverEntryRequest();
        ReflectionTestUtils.setField(request, "departmentId", deptId);
        ReflectionTestUtils.setField(request, "projectId", projId);
        when(support.isWorkspaceAdminOrOwner(wsId, userId)).thenReturn(true);

        assertThrows(ForbiddenException.class,
                () -> handoverEntryService.create(wsId, request));
    }

    @Test
    void createShouldThrowWhenCrossDepartment() {
        CreateHandoverEntryRequest request = new CreateHandoverEntryRequest();
        ReflectionTestUtils.setField(request, "departmentId", UUID.randomUUID());
        ReflectionTestUtils.setField(request, "projectId", projId);

        assertThrows(ForbiddenException.class,
                () -> handoverEntryService.create(wsId, request));
    }

    @Test
    void createShouldThrowWhenReceiverNotFound() {
        CreateHandoverEntryRequest request = new CreateHandoverEntryRequest();
        ReflectionTestUtils.setField(request, "departmentId", deptId);
        ReflectionTestUtils.setField(request, "projectId", projId);
        ReflectionTestUtils.setField(request, "receiverId", receiverId);
        when(projectRepository.findByIdAndDepartment_Id(projId, deptId)).thenReturn(Optional.of(project));
        when(userRepository.findById(receiverId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> handoverEntryService.create(wsId, request));
    }

    @Test
    void createShouldThrowForNonMember() {
        UUID otherUserId = UUID.randomUUID();
        when(support.currentUserId()).thenReturn(otherUserId);
        doThrow(new ForbiddenException("You are not a member of this workspace."))
                .when(support).assertActiveWorkspaceMember(eq(wsId), eq(otherUserId));

        assertThrows(ForbiddenException.class,
                () -> handoverEntryService.create(wsId, new CreateHandoverEntryRequest()));
    }

    @Test
    void getByIdShouldReturnEntry() {
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));
        when(handoverEntryMapper.toResponse(exampleEntry)).thenReturn(exampleResponse);

        HandoverEntryResponse result = handoverEntryService.getById(wsId, entryId);

        assertNotNull(result);
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> handoverEntryService.getById(wsId, entryId));
    }

    @Test
    void getByIdShouldThrowWhenDeleted() {
        exampleEntry.setDeleted(true);
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> handoverEntryService.getById(wsId, entryId));
    }

    @Test
    void listShouldReturnPaginatedResults() {
        when(support.isWorkspaceAdminOrOwner(wsId, userId)).thenReturn(true);
        Page<HandoverEntry> page = new PageImpl<>(List.of(exampleEntry));
        when(handoverEntryRepository.search(eq(wsId), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(page);
        when(handoverEntryMapper.toResponse(exampleEntry)).thenReturn(exampleResponse);

        Page<HandoverEntryResponse> result = handoverEntryService.list(wsId, null, null, null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void inboxShouldReturnReceiverEntries() {
        Page<HandoverEntry> page = new PageImpl<>(List.of(exampleEntry));
        when(handoverEntryRepository.findInboxPaginated(eq(wsId), eq(userId), any(PageRequest.class))).thenReturn(page);
        when(handoverEntryMapper.toResponse(exampleEntry)).thenReturn(exampleResponse);

        Page<HandoverEntryResponse> result = handoverEntryService.inbox(wsId, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateShouldSucceedForSender() {
        UpdateHandoverEntryRequest request = new UpdateHandoverEntryRequest();
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));
        when(handoverEntryRepository.save(any(HandoverEntry.class))).thenReturn(exampleEntry);
        when(handoverEntryMapper.toResponse(exampleEntry)).thenReturn(exampleResponse);

        HandoverEntryResponse result = handoverEntryService.update(wsId, entryId, request);

        assertNotNull(result);
        verify(handoverEntryMapper).updateHandoverEntry(request, exampleEntry);
    }

    @Test
    void updateShouldThrowForNonSender() {
        User otherUser = User.builder().firstName("Other").build();
        ReflectionTestUtils.setField(otherUser, "id", UUID.randomUUID());
        exampleEntry.setSender(otherUser);
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));

        assertThrows(ForbiddenException.class,
                () -> handoverEntryService.update(wsId, entryId, new UpdateHandoverEntryRequest()));
    }

    @Test
    void updateShouldThrowWhenNotDraftOrRejected() {
        exampleEntry.setStatus(HandoverStatus.PENDING);
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));

        assertThrows(BadRequestException.class,
                () -> handoverEntryService.update(wsId, entryId, new UpdateHandoverEntryRequest()));
    }

    @Test
    void sendShouldSucceed() {
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));
        when(handoverEntryRepository.save(any(HandoverEntry.class))).thenReturn(exampleEntry);
        when(handoverEntryMapper.toResponse(exampleEntry)).thenReturn(exampleResponse);

        HandoverEntryResponse result = handoverEntryService.send(wsId, entryId, new HandoverStatusUpdateRequest());

        assertNotNull(result);
        assertEquals(HandoverStatus.PENDING, exampleEntry.getStatus());
        assertNotNull(exampleEntry.getSentAt());
        verify(support).notifyUser(eq(wsId), eq(receiverId), any(), anyString(), anyString(), eq(entryId));
    }

    @Test
    void sendShouldThrowForNonSender() {
        User otherUser = User.builder().firstName("Other").build();
        ReflectionTestUtils.setField(otherUser, "id", UUID.randomUUID());
        exampleEntry.setSender(otherUser);
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));

        assertThrows(ForbiddenException.class,
                () -> handoverEntryService.send(wsId, entryId, new HandoverStatusUpdateRequest()));
    }

    @Test
    void acceptShouldSucceed() {
        when(support.currentUserId()).thenReturn(receiverId);
        exampleEntry.setStatus(HandoverStatus.PENDING);
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));
        when(handoverEntryRepository.save(any(HandoverEntry.class))).thenReturn(exampleEntry);
        when(handoverEntryMapper.toResponse(exampleEntry)).thenReturn(exampleResponse);

        HandoverEntryResponse result = handoverEntryService.accept(wsId, entryId, new HandoverStatusUpdateRequest());

        assertNotNull(result);
        assertEquals(HandoverStatus.ACCEPTED, exampleEntry.getStatus());
        assertNotNull(exampleEntry.getAcceptedAt());
    }

    @Test
    void acceptShouldThrowForNonReceiver() {
        User otherUser = User.builder().firstName("Other").build();
        ReflectionTestUtils.setField(otherUser, "id", UUID.randomUUID());
        exampleEntry.setStatus(HandoverStatus.PENDING);
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));

        assertThrows(ForbiddenException.class,
                () -> handoverEntryService.accept(wsId, entryId, new HandoverStatusUpdateRequest()));
    }

    @Test
    void rejectShouldSucceed() {
        when(support.currentUserId()).thenReturn(receiverId);
        exampleEntry.setStatus(HandoverStatus.PENDING);
        HandoverStatusUpdateRequest request = new HandoverStatusUpdateRequest();
        request.setReason("Not ready");
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));
        when(handoverEntryRepository.save(any(HandoverEntry.class))).thenReturn(exampleEntry);
        when(handoverEntryMapper.toResponse(exampleEntry)).thenReturn(exampleResponse);

        HandoverEntryResponse result = handoverEntryService.reject(wsId, entryId, request);

        assertNotNull(result);
        assertEquals(HandoverStatus.REJECTED, exampleEntry.getStatus());
        assertNotNull(exampleEntry.getRejectedAt());
        verify(support).addTimelineEvent(eq(exampleEntry), any(), contains("Not ready"), eq(receiverId));
    }

    @Test
    void completeShouldSucceed() {
        exampleEntry.setStatus(HandoverStatus.ACCEPTED);
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));
        when(handoverEntryRepository.save(any(HandoverEntry.class))).thenReturn(exampleEntry);
        when(handoverEntryMapper.toResponse(exampleEntry)).thenReturn(exampleResponse);

        HandoverEntryResponse result = handoverEntryService.complete(wsId, entryId, new HandoverStatusUpdateRequest());

        assertNotNull(result);
        assertEquals(HandoverStatus.COMPLETED, exampleEntry.getStatus());
        assertNotNull(exampleEntry.getCompletedAt());
    }

    @Test
    void completeShouldThrowWhenNotAccepted() {
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));

        assertThrows(BadRequestException.class,
                () -> handoverEntryService.complete(wsId, entryId, new HandoverStatusUpdateRequest()));
    }

    @Test
    void archiveShouldSucceed() {
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));
        when(handoverEntryRepository.save(any(HandoverEntry.class))).thenReturn(exampleEntry);
        when(handoverEntryMapper.toResponse(exampleEntry)).thenReturn(exampleResponse);

        HandoverEntryResponse result = handoverEntryService.archive(wsId, entryId, new HandoverStatusUpdateRequest());

        assertNotNull(result);
        assertEquals(HandoverStatus.ARCHIVED, exampleEntry.getStatus());
        assertNotNull(exampleEntry.getArchivedAt());
    }

    @Test
    void deleteShouldSoftDelete() {
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));

        handoverEntryService.delete(wsId, entryId);

        assertTrue(exampleEntry.getDeleted());
        verify(handoverEntryRepository).save(exampleEntry);
    }

    @Test
    void deleteShouldThrowWhenNotSenderOrAdmin() {
        User otherUser = User.builder().firstName("Other").build();
        ReflectionTestUtils.setField(otherUser, "id", UUID.randomUUID());
        exampleEntry.setSender(otherUser);
        when(support.isWorkspaceAdminOrOwner(wsId, userId)).thenReturn(false);
        when(handoverEntryRepository.findByIdAndWorkspace(entryId, wsId)).thenReturn(Optional.of(exampleEntry));

        assertThrows(ForbiddenException.class,
                () -> handoverEntryService.delete(wsId, entryId));
    }
}
