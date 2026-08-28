package com.trio.backend.service;

import com.trio.backend.dto.Document.CreateDocumentRequest;
import com.trio.backend.dto.Document.UpdateDocumentRequest;
import com.trio.backend.dto.Document.DocumentResponse;
import com.trio.backend.dto.notification.CreateNotificationRequest;
import com.trio.backend.entity.Document;
import com.trio.backend.entity.Notification;
import com.trio.backend.entity.Project;
import com.trio.backend.entity.Task;
import com.trio.backend.entity.WorkspaceMember;
import com.trio.backend.enums.TaskStatus;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.enums.WorkspaceRole;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ForbiddenException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.DocumentMapper;
import com.trio.backend.repository.DocumentRepository;
import com.trio.backend.repository.ProjectRepository;
import com.trio.backend.repository.TaskRepository;
import com.trio.backend.repository.WorkspaceMemberRepository;
import com.trio.backend.repository.WorkspaceRepository;
import com.trio.backend.security.department.DepartmentScopeGuard;
import com.trio.backend.storage.FileValidationService;
import com.trio.backend.security.user.CustomUserDetails;
import com.trio.backend.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.trio.backend.enums.ApprovalStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final DocumentMapper documentMapper;
    private final DepartmentScopeGuard departmentScopeGuard;
    private final FileValidationService fileValidationService;
    private final StorageService storageService;
    private final NotificationService notificationService;
    private final AlertGenerationHelper alertGenerationHelper;

    @Override
    @Transactional
    public DocumentResponse create(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            CreateDocumentRequest request
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Project project = projectRepository.findByIdAndDepartment_Id(projectId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        if (project.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ResourceNotFoundException("Project not found.");
        }

        if (!project.getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Project not found.");
        }

        Document document = documentMapper.toEntity(request);
        document.setProject(project);
        document.setStatus(Document.DocumentStatus.ACTIVE);

        if (request.getTaskId() != null) {
            Task task = taskRepository.findByIdAndProject_Id(request.getTaskId(), projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found."));

            if (task.getStatus().isTerminal()) {
                throw new ResourceNotFoundException("Task not found.");
            }

            document.setTask(task);
        }

        Document saved = documentRepository.save(document);
        DocumentResponse response = documentMapper.toResponse(saved);
        publishDocumentNotification(workspaceId, saved, Notification.NotificationType.DOCUMENT_UPLOADED, "Document \"" + saved.getTitle() + "\" was created.");
        return response;
    }

    @Override
    @Transactional
    public DocumentResponse upload(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID taskId,
            String title,
            String description,
            String category,
            String tags,
            MultipartFile file
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);
        fileValidationService.validate(file);

        Project project = projectRepository.findByIdAndDepartment_Id(projectId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        if (!project.getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Project not found.");
        }

        String storagePath;
        try {
            storagePath = storageService.store(
                    getBytes(file),
                    file.getOriginalFilename(),
                    file.getContentType()
            );
        } catch (RuntimeException ex) {
            alertGenerationHelper.recordDocumentUploadFailure(
                    workspaceId, userId, departmentId, null,
                    "Document upload failed",
                    "The document \"" + (title != null ? title : file.getOriginalFilename())
                            + "\" could not be stored. Please try again.");
            throw ex;
        }

        Document document = new Document();
        document.setProject(project);
        document.setTitle(title != null ? title : file.getOriginalFilename());
        document.setDescription(description);
        document.setCategory(category);
        document.setTags(tags);
        document.setFileName(file.getOriginalFilename());
        document.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        document.setFileSize(file.getSize());
        document.setStoragePath(storagePath);
        document.setStatus(Document.DocumentStatus.ACTIVE);

        if (taskId != null) {
            Task task = taskRepository.findByIdAndProject_Id(taskId, projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found."));
            document.setTask(task);
        }

        Document saved = documentRepository.save(document);
        DocumentResponse response = documentMapper.toResponse(saved);
        publishDocumentNotification(workspaceId, saved, Notification.NotificationType.DOCUMENT_UPLOADED, "Document \"" + saved.getTitle() + "\" was uploaded.");
        return response;
    }

    @Override
    @Transactional
    public DocumentResponse getById(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID documentId
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Document document = documentRepository.findByIdAndWorkspace(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));

        if (document.getStatus() == Document.DocumentStatus.DELETED) {
            throw new ResourceNotFoundException("Document not found.");
        }

        if (!document.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        if (!document.getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        document.setViewCount(document.getViewCount() + 1);
        documentRepository.save(document);

        return documentMapper.toResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> list(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            Pageable pageable
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Project project = projectRepository.findByIdAndDepartment_Id(projectId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        if (!project.getDepartment().getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Project not found.");
        }

        return documentRepository.findByProjectIdPaginated(projectId, pageable)
                .map(documentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> listByWorkspace(
            UUID workspaceId,
            Pageable pageable
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        UUID accessibleDepartmentId = departmentScopeGuard.resolveAccessibleDepartmentId(workspaceId, userId);
        if (accessibleDepartmentId == null) {
            return documentRepository.findByWorkspacePaginated(workspaceId, pageable)
                    .map(documentMapper::toResponse);
        }
        return documentRepository.findByDepartmentIdPaginated(accessibleDepartmentId, pageable)
                .map(documentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> search(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            String query,
            Pageable pageable
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        if (projectId != null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found."));
            departmentScopeGuard.assertDepartmentAccessible(workspaceId, project.getDepartment().getId(), userId);
            return documentRepository.searchByTitleInProjectPaginated(projectId, query, pageable)
                    .map(documentMapper::toResponse);
        }

        UUID accessibleDepartmentId = departmentScopeGuard.resolveAccessibleDepartmentId(workspaceId, userId);
        if (accessibleDepartmentId == null) {
            return documentRepository.searchByTitleInWorkspacePaginated(workspaceId, query, pageable)
                    .map(documentMapper::toResponse);
        }
        return documentRepository.searchByTitleInDepartmentPaginated(accessibleDepartmentId, query, pageable)
                .map(documentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource download(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID documentId
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);

        Document document = documentRepository.findByIdAndWorkspace(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));

        if (document.getStatus() == Document.DocumentStatus.DELETED) {
            throw new ResourceNotFoundException("Document not found.");
        }

        if (!document.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        if (!document.getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        return storageService.loadAsResource(document.getStoragePath());
    }

    @Override
    @Transactional
    public DocumentResponse update(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID documentId,
            UpdateDocumentRequest request
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Document document = documentRepository.findByIdAndWorkspace(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));

        if (document.getStatus() == Document.DocumentStatus.DELETED) {
            throw new ResourceNotFoundException("Document not found.");
        }

        if (!document.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        if (!document.getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        Document snapshot = new Document();
        snapshot.setProject(document.getProject());
        snapshot.setTask(document.getTask());
        snapshot.setTitle(document.getTitle());
        snapshot.setDescription(document.getDescription());
        snapshot.setFileName(document.getFileName());
        snapshot.setMimeType(document.getMimeType());
        snapshot.setFileSize(document.getFileSize());
        snapshot.setStoragePath(document.getStoragePath());
        snapshot.setDocumentVersion(document.getDocumentVersion());
        snapshot.setStatus(Document.DocumentStatus.ARCHIVED);
        snapshot.setAiProcessed(document.getAiProcessed());
        snapshot.setStorageType(document.getStorageType());
        snapshot.setPdfExportAvailable(document.getPdfExportAvailable());
        documentRepository.save(snapshot);

        documentMapper.updateDocument(request, document);
        document.setDocumentVersion(document.getDocumentVersion() + 1);
        Document saved = documentRepository.save(document);
        DocumentResponse response = documentMapper.toResponse(saved);
        publishDocumentNotification(workspaceId, saved, Notification.NotificationType.DOCUMENT_UPLOADED, "Document \"" + saved.getTitle() + "\" was updated.");
        return response;
    }

    @Override
    @Transactional
    public void delete(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID documentId
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Document document = documentRepository.findByIdAndWorkspace(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));

        if (document.getStatus() == Document.DocumentStatus.DELETED) {
            return;
        }

        if (!document.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        if (!document.getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        document.setStatus(Document.DocumentStatus.DELETED);
        documentRepository.save(document);
        publishDocumentNotification(workspaceId, document, Notification.NotificationType.DOCUMENT_UPLOADED, "Document \"" + document.getTitle() + "\" was deleted.");
    }

    @Override
    @Transactional
    public DocumentResponse archive(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID documentId
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Document document = documentRepository.findByIdAndWorkspace(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));

        if (document.getStatus() != Document.DocumentStatus.ACTIVE) {
            throw new BadRequestException("Only active documents can be archived.");
        }

        if (!document.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        if (!document.getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        document.setStatus(Document.DocumentStatus.ARCHIVED);
        Document saved = documentRepository.save(document);
        DocumentResponse response = documentMapper.toResponse(saved);
        publishDocumentNotification(workspaceId, saved, Notification.NotificationType.DOCUMENT_UPLOADED, "Document \"" + saved.getTitle() + "\" was archived.");
        return response;
    }

    @Override
    @Transactional
    public DocumentResponse restore(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID documentId
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Document document = documentRepository.findByIdAndWorkspace(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));

        if (document.getStatus() != Document.DocumentStatus.ARCHIVED) {
            throw new BadRequestException("Only archived documents can be restored.");
        }

        if (!document.getProject().getId().equals(projectId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        if (!document.getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        document.setStatus(Document.DocumentStatus.ACTIVE);
        Document saved = documentRepository.save(document);
        return documentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DocumentResponse submitForApproval(
            UUID workspaceId, UUID departmentId, UUID projectId, UUID documentId
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Document document = documentRepository.findByIdAndWorkspace(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));

        if (!document.getProject().getId().equals(projectId)
                || !document.getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        document.setApprovalStatus(ApprovalStatus.PENDING);
        document.setApprovedBy(null);
        document.setApprovedAt(null);
        Document saved = documentRepository.save(document);
        return documentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DocumentResponse approve(
            UUID workspaceId, UUID departmentId, UUID projectId, UUID documentId
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Document document = documentRepository.findByIdAndWorkspace(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));

        if (!document.getProject().getId().equals(projectId)
                || !document.getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        if (document.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Document is not pending approval.");
        }

        document.setApprovalStatus(ApprovalStatus.APPROVED);
        document.setApprovedBy(userId);
        document.setApprovedAt(Instant.now());
        Document saved = documentRepository.save(document);
        DocumentResponse response = documentMapper.toResponse(saved);
        publishDocumentNotification(workspaceId, saved, Notification.NotificationType.DOCUMENT_UPLOADED, "Document \"" + saved.getTitle() + "\" was approved.");
        return response;
    }

    @Override
    @Transactional
    public DocumentResponse reject(
            UUID workspaceId, UUID departmentId, UUID projectId, UUID documentId
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);

        Document document = documentRepository.findByIdAndWorkspace(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));

        if (!document.getProject().getId().equals(projectId)
                || !document.getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        if (document.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("Document is not pending approval.");
        }

        document.setApprovalStatus(ApprovalStatus.REJECTED);
        document.setApprovedBy(userId);
        document.setApprovedAt(Instant.now());
        Document saved = documentRepository.save(document);
        DocumentResponse response = documentMapper.toResponse(saved);
        publishDocumentNotification(workspaceId, saved, Notification.NotificationType.DOCUMENT_UPLOADED, "Document \"" + saved.getTitle() + "\" was rejected.");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getVersionHistory(
            UUID workspaceId,
            UUID departmentId,
            UUID projectId,
            UUID documentId
    ) {
        UUID userId = getAuthenticatedUserId();
        assertActiveWorkspaceMember(workspaceId, userId);
        departmentScopeGuard.assertDepartmentAccessible(workspaceId, departmentId, userId);
        Document document = documentRepository.findByIdAndWorkspace(documentId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));

        if (!document.getProject().getId().equals(projectId)
                || !document.getProject().getDepartment().getId().equals(departmentId)) {
            throw new ResourceNotFoundException("Document not found.");
        }

        return documentRepository.findAllVersions(projectId, document.getTitle())
                .stream()
                .map(documentMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ============================================================================
    // Helpers
    // ============================================================================

    private void publishDocumentNotification(UUID workspaceId, Document document, Notification.NotificationType type, String message) {
        try {
            CreateNotificationRequest req = new CreateNotificationRequest();
            req.setWorkspaceId(workspaceId);
            req.setRecipientId(getAuthenticatedUserId());
            req.setNotificationType(type);
            req.setTitle(message);
            req.setBody(document.getDescription());
            req.setDocumentId(document.getId());
            req.setProjectId(document.getProject().getId());
            notificationService.create(workspaceId, req);
        } catch (Exception e) {
            log.warn("Failed to publish notification for document {}: {}", document.getId(), e.getMessage());
        }
    }

    private byte[] getBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new BadRequestException("Failed to read file content.");
        }
    }

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
}
