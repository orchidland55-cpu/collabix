package com.trio.backend.service;

import com.trio.backend.dto.organisation.handover.HandoverJournalResponse;
import com.trio.backend.entity.*;
import com.trio.backend.entity.HandoverEntry.HandoverStatus;
import com.trio.backend.entity.HandoverEntry.Priority;
import com.trio.backend.entity.HandoverEntry.Shift;
import com.trio.backend.enums.*;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.HandoverJournalMapper;
import com.trio.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandoverJournalServiceImplTest {

    @Mock
    private HandoverJournalRepository handoverJournalRepository;
    @Mock
    private HandoverEntryRepository handoverEntryRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private HandoverJournalMapper handoverJournalMapper;
    @Mock
    private HandoverSupport support;

    @InjectMocks
    private HandoverJournalServiceImpl handoverJournalService;

    private User actor;
    private Workspace workspace;
    private Department department;
    private Project project;
    private HandoverJournal exampleJournal;
    private HandoverJournalResponse exampleResponse;
    private UUID wsId;
    private UUID deptId;
    private UUID projId;
    private UUID journalId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        wsId = UUID.randomUUID();
        deptId = UUID.randomUUID();
        projId = UUID.randomUUID();
        journalId = UUID.randomUUID();

        actor = User.builder()
                .email("admin@example.com")
                .password("secret")
                .firstName("Admin")
                .lastName("User")
                .memberType(MemberType.EMPLOYEE)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(actor, "id", userId);

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

        exampleJournal = HandoverJournal.builder()
                .workspace(workspace)
                .department(department)
                .project(project)
                .journalDate(LocalDate.now().atStartOfDay())
                .generatedSummary("Test summary")
                .mainDoneWork("Done")
                .mainRemainingWork("Remaining")
                .blockers("None")
                .difficulties("None")
                .recommendations("None")
                .totalHandovers(0L)
                .pendingHandovers(0L)
                .completedHandovers(0L)
                .rejectedHandovers(0L)
                .urgentHandovers(0L)
                .overdueHandovers(0L)
                .generationStatus(HandoverJournal.GenerationStatus.GENERATED)
                .generationDate(LocalDateTime.now())
                .generationProcessedBy(userId)
                .status(HandoverJournal.HandoverJournalStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(exampleJournal, "id", journalId);

        exampleResponse = new HandoverJournalResponse();

        lenient().when(support.currentUserId()).thenReturn(userId);
        lenient().when(support.isWorkspaceAdminOrOwner(wsId, userId)).thenReturn(true);
        lenient().when(support.currentUserDepartmentId()).thenReturn(deptId);
        lenient().when(support.resolveAccessibleDepartment(wsId, deptId)).thenReturn(deptId);
        lenient().when(support.userDisplayName(any())).thenReturn("Admin User");
    }

    @Test
    void generateJournalShouldSucceed() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);
        Instant startInstant = dayStart.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = dayEnd.atZone(ZoneId.systemDefault()).toInstant();

        when(projectRepository.findByIdAndDepartment_Id(projId, deptId)).thenReturn(Optional.of(project));
        when(handoverEntryRepository.findSubmittedByDepartmentIdAndEntryDate(wsId, deptId, LocalDate.now(), null))
                .thenReturn(List.of());
        when(handoverJournalRepository.save(any(HandoverJournal.class))).thenReturn(exampleJournal);
        when(handoverJournalMapper.toResponse(exampleJournal)).thenReturn(exampleResponse);

        HandoverJournalResponse result = handoverJournalService.generateJournal(wsId, deptId, projId);

        assertNotNull(result);
        verify(handoverJournalRepository).save(any(HandoverJournal.class));
        verify(taskRepository).countActiveByProjectId(projId);
        verify(taskRepository).countByProjectIdAndStatusAndUpdatedAtBetween(
                projId, TaskStatus.COMPLETED, startInstant, endInstant);
        verify(commentRepository).countByProjectIdAndStatusAndCreatedAtBetween(
                projId, CommentStatus.ACTIVE, startInstant, endInstant);
        verify(activityRepository).countByProjectIdAndStatusAndCreatedAtBetween(
                projId, ActivityStatus.ACTIVE, startInstant, endInstant);
        verify(documentRepository).countByProjectIdAndCreatedAtBetween(
                projId, startInstant, endInstant);
    }

    @Test
    void generateJournalShouldThrowWhenProjectNotFound() {
        when(projectRepository.findByIdAndDepartment_Id(projId, deptId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> handoverJournalService.generateJournal(wsId, deptId, projId));
    }

    @Test
    void generateJournalShouldThrowWhenProjectInactive() {
        project.setStatus(WorkspaceStatus.ARCHIVED);
        when(projectRepository.findByIdAndDepartment_Id(projId, deptId)).thenReturn(Optional.of(project));

        assertThrows(ResourceNotFoundException.class,
                () -> handoverJournalService.generateJournal(wsId, deptId, projId));
    }

    @Test
    void getByIdShouldReturnJournal() {
        when(handoverJournalRepository.findByIdAndWorkspace(journalId, wsId)).thenReturn(Optional.of(exampleJournal));
        when(handoverJournalMapper.toResponse(exampleJournal)).thenReturn(exampleResponse);

        HandoverJournalResponse result = handoverJournalService.getById(wsId, deptId, projId, journalId);

        assertNotNull(result);
    }

    @Test
    void getByIdShouldThrowWhenNotFound() {
        when(handoverJournalRepository.findByIdAndWorkspace(journalId, wsId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> handoverJournalService.getById(wsId, deptId, projId, journalId));
    }

    @Test
    void getByIdShouldThrowWhenProjectMismatch() {
        Project otherProject = Project.builder().build();
        ReflectionTestUtils.setField(otherProject, "id", UUID.randomUUID());
        exampleJournal.setProject(otherProject);
        when(handoverJournalRepository.findByIdAndWorkspace(journalId, wsId)).thenReturn(Optional.of(exampleJournal));

        assertThrows(ResourceNotFoundException.class,
                () -> handoverJournalService.getById(wsId, deptId, projId, journalId));
    }

    @Test
    void getByIdShouldThrowWhenMemberOfDifferentDepartment() {
        when(handoverJournalRepository.findByIdAndWorkspace(journalId, wsId)).thenReturn(Optional.of(exampleJournal));
        doThrow(ForbiddenException.class).when(support).assertCanViewDepartmentJournal(eq(wsId), any());

        assertThrows(ForbiddenException.class,
                () -> handoverJournalService.getById(wsId, deptId, projId, journalId));
    }

    @Test
    void getByIdAccessibleShouldReturnForAdmin() {
        when(handoverJournalRepository.findByIdAndWorkspace(journalId, wsId)).thenReturn(Optional.of(exampleJournal));
        when(handoverJournalMapper.toResponse(exampleJournal)).thenReturn(exampleResponse);

        HandoverJournalResponse result = handoverJournalService.getByIdAccessible(wsId, journalId);

        assertNotNull(result);
    }

    @Test
    void getByIdAccessibleShouldThrowWhenMemberOfDifferentDepartment() {
        when(handoverJournalRepository.findByIdAndWorkspace(journalId, wsId)).thenReturn(Optional.of(exampleJournal));
        doThrow(ForbiddenException.class).when(support).assertCanViewDepartmentJournal(eq(wsId), any());

        assertThrows(ForbiddenException.class,
                () -> handoverJournalService.getByIdAccessible(wsId, journalId));
    }

    @Test
    void listAccessibleShouldReturnPaginatedResults() {
        Page<HandoverJournal> page = new PageImpl<>(List.of(exampleJournal));
        when(handoverJournalRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(page);
        when(handoverJournalMapper.toResponse(exampleJournal)).thenReturn(exampleResponse);

        Page<HandoverJournalResponse> result = handoverJournalService.listAccessible(
                wsId, deptId, null, null, null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listAccessibleShouldThrowWhenRequestingOtherDepartment() {
        UUID otherDept = UUID.randomUUID();
        doThrow(ForbiddenException.class).when(support).resolveAccessibleDepartment(eq(wsId), eq(otherDept));

        assertThrows(ForbiddenException.class,
                () -> handoverJournalService.listAccessible(wsId, otherDept, null, null, null, PageRequest.of(0, 10)));
    }

    @Test
    void listShouldReturnPaginatedResults() {
        Page<HandoverJournal> page = new PageImpl<>(List.of(exampleJournal));
        when(projectRepository.findByIdAndDepartment_Id(projId, deptId)).thenReturn(Optional.of(project));
        when(handoverJournalRepository.findByProjectIdPaginated(eq(projId), any(PageRequest.class))).thenReturn(page);
        when(handoverJournalMapper.toResponse(exampleJournal)).thenReturn(exampleResponse);

        Page<HandoverJournalResponse> result = handoverJournalService.list(wsId, deptId, projId, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void regenerateShouldUpdateExistingJournal() {
        LocalDate journalDate = exampleJournal.getJournalDate().toLocalDate();

        when(handoverJournalRepository.findByIdAndWorkspace(journalId, wsId)).thenReturn(Optional.of(exampleJournal));
        when(handoverEntryRepository.findSubmittedByDepartmentIdAndEntryDate(wsId, deptId, journalDate, null))
                .thenReturn(List.of());
        when(handoverJournalRepository.save(any(HandoverJournal.class))).thenReturn(exampleJournal);
        when(handoverJournalMapper.toResponse(exampleJournal)).thenReturn(exampleResponse);

        HandoverJournalResponse result = handoverJournalService.regenerate(wsId, deptId, projId, journalId);

        assertNotNull(result);
        verify(handoverJournalRepository).save(exampleJournal);
    }

    @Test
    void regenerateShouldThrowWhenJournalNotFound() {
        when(handoverJournalRepository.findByIdAndWorkspace(journalId, wsId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> handoverJournalService.regenerate(wsId, deptId, projId, journalId));
    }

    @Test
    void deleteShouldSoftDelete() {
        when(handoverJournalRepository.findByIdAndWorkspace(journalId, wsId)).thenReturn(Optional.of(exampleJournal));

        handoverJournalService.delete(wsId, deptId, projId, journalId);

        assertEquals(HandoverJournal.HandoverJournalStatus.DELETED, exampleJournal.getStatus());
        verify(handoverJournalRepository).save(exampleJournal);
    }

    @Test
    void deleteShouldBeIdempotentWhenAlreadyDeleted() {
        exampleJournal.setStatus(HandoverJournal.HandoverJournalStatus.DELETED);
        when(handoverJournalRepository.findByIdAndWorkspace(journalId, wsId)).thenReturn(Optional.of(exampleJournal));

        handoverJournalService.delete(wsId, deptId, projId, journalId);

        verify(handoverJournalRepository, never()).save(any());
    }

    @Test
    void generateJournalInternalShouldReturnSavedEntity() {
        LocalDate today = LocalDate.now();

        when(projectRepository.findByIdAndDepartment_Id(projId, deptId)).thenReturn(Optional.of(project));
        when(handoverEntryRepository.findSubmittedByDepartmentIdAndEntryDate(wsId, deptId, today, null))
                .thenReturn(List.of());
        when(handoverJournalRepository.save(any(HandoverJournal.class))).thenReturn(exampleJournal);

        HandoverJournal result = handoverJournalService.generateJournalInternal(wsId, deptId, projId, userId);

        assertNotNull(result);
        assertEquals(exampleJournal, result);
    }

    @Test
    void generateJournalInternalShouldAggregateEntries() {
        LocalDate today = LocalDate.now();

        User otherUser = User.builder().firstName("Other").build();
        ReflectionTestUtils.setField(otherUser, "id", UUID.randomUUID());

        HandoverEntry entry = HandoverEntry.builder()
                .workspace(workspace)
                .department(department)
                .project(project)
                .sender(otherUser)
                .title("Bug fix")
                .completedTasks("Fixed bug")
                .priority(Priority.HIGH)
                .status(HandoverStatus.SUBMITTED)
                .build();
        ReflectionTestUtils.setField(entry, "id", UUID.randomUUID());

        when(projectRepository.findByIdAndDepartment_Id(projId, deptId)).thenReturn(Optional.of(project));
        when(handoverEntryRepository.findSubmittedByDepartmentIdAndEntryDate(wsId, deptId, today, null))
                .thenReturn(List.of(entry));
        when(handoverJournalRepository.save(any(HandoverJournal.class))).thenAnswer(invocation -> {
            HandoverJournal saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });

        HandoverJournal result = handoverJournalService.generateJournalInternal(wsId, deptId, projId, userId);

        assertNotNull(result);
        assertEquals(1L, result.getTotalHandovers());
        assertTrue(result.getGeneratedSummary().contains("[V1 Synthesizer] Collected 1 submitted handover entry(ies) for the day."));
        assertTrue(result.getGeneratedSummary().contains("--- Project context ---"));
        assertTrue(result.getMainDoneWork().contains("Fixed bug"));
        verify(handoverEntryRepository).findSubmittedByDepartmentIdAndEntryDate(wsId, deptId, today, null);
    }
}
