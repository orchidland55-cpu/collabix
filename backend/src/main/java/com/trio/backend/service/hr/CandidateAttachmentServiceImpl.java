package com.trio.backend.service.hr;

import com.trio.backend.dto.hr.AttachmentSearchCriteria;
import com.trio.backend.dto.hr.AttachmentStatistics;
import com.trio.backend.dto.hr.CandidateAttachmentResponse;
import com.trio.backend.entity.Candidate;
import com.trio.backend.entity.CandidateAttachment;
import com.trio.backend.enums.AttachmentType;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.CandidateAttachmentMapper;
import com.trio.backend.repository.AttachmentSpecification;
import com.trio.backend.repository.CandidateAttachmentRepository;
import com.trio.backend.repository.CandidateRepository;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.storage.StorageService;
import com.trio.backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CandidateAttachmentServiceImpl implements CandidateAttachmentService {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg", "gif", "txt", "rtf"
    ));

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024L;

    private final CandidateAttachmentRepository attachmentRepository;
    private final CandidateRepository candidateRepository;
    private final DepartmentRepository departmentRepository;
    private final StorageService storageService;
    private final CandidateAttachmentMapper attachmentMapper;

    @Override
    public CandidateAttachmentResponse upload(UUID workspaceId, UUID departmentId, UUID candidateId,
                                               MultipartFile file, String description, AttachmentType attachmentType) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Candidate candidate = findActiveCandidate(workspaceId, departmentId, candidateId);

        validateFile(file);

        String originalFileName = file.getOriginalFilename();
        String extension = extractExtension(originalFileName);
        validateMimeType(file, extension);

        byte[] content = getFileBytes(file);
        String storagePath = storageService.store(content, originalFileName, file.getContentType());
        String storedFileName = storagePath;

        CandidateAttachment attachment = CandidateAttachment.builder()
                .candidate(candidate)
                .attachmentType(attachmentType)
                .fileName(originalFileName)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .fileExtension(extension)
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .storagePath(storagePath)
                .description(description)
                .uploadedBy(userId)
                .fileVersion(1)
                .build();

        CandidateAttachment saved = attachmentRepository.save(attachment);
        log.info("Attachment uploaded: {} ({}) for candidate {} by user {}",
                originalFileName, attachmentType, candidateId, userId);
        return attachmentMapper.toResponse(saved);
    }

    @Override
    public CandidateAttachmentResponse replace(UUID workspaceId, UUID departmentId, UUID candidateId,
                                                UUID attachmentId, MultipartFile file, String description) {
        UUID userId = SecurityUtils.getCurrentUserId();
        CandidateAttachment existing = findActiveAttachment(workspaceId, departmentId, candidateId, attachmentId);

        validateFile(file);

        storageService.delete(existing.getStoragePath());

        String originalFileName = file.getOriginalFilename();
        String extension = extractExtension(originalFileName);
        validateMimeType(file, extension);
        byte[] content = getFileBytes(file);
        String storagePath = storageService.store(content, originalFileName, file.getContentType());
        String storedFileName = storagePath;

        existing.setFileName(originalFileName);
        existing.setOriginalFileName(originalFileName);
        existing.setStoredFileName(storedFileName);
        existing.setFileExtension(extension);
        existing.setMimeType(file.getContentType());
        existing.setFileSize(file.getSize());
        existing.setStoragePath(storagePath);
        if (description != null) {
            existing.setDescription(description);
        }

        CandidateAttachment saved = attachmentRepository.save(existing);
        log.info("Attachment replaced: {} for candidate {} by user {}",
                originalFileName, candidateId, userId);
        return attachmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateAttachmentResponse getById(UUID workspaceId, UUID departmentId, UUID candidateId, UUID attachmentId) {
        SecurityUtils.getCurrentUserId();
        CandidateAttachment attachment = findActiveAttachment(workspaceId, departmentId, candidateId, attachmentId);
        return attachmentMapper.toResponse(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateAttachmentResponse> listByCandidate(UUID workspaceId, UUID departmentId, UUID candidateId, Pageable pageable) {
        SecurityUtils.getCurrentUserId();
        findActiveCandidate(workspaceId, departmentId, candidateId);
        return attachmentRepository.findAllByCandidate_IdOrderByCreatedAtDesc(candidateId, pageable)
                .map(attachmentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CandidateAttachmentResponse> search(UUID workspaceId, UUID departmentId, AttachmentSearchCriteria criteria, Pageable pageable) {
        SecurityUtils.getCurrentUserId();
        findActiveDepartment(workspaceId, departmentId);
        return attachmentRepository.findAll(
                        AttachmentSpecification.withFilter(departmentId, criteria), pageable)
                .map(attachmentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource download(UUID workspaceId, UUID departmentId, UUID candidateId, UUID attachmentId) {
        SecurityUtils.getCurrentUserId();
        CandidateAttachment attachment = findActiveAttachment(workspaceId, departmentId, candidateId, attachmentId);
        return storageService.loadAsResource(attachment.getStoragePath());
    }

    @Override
    public void delete(UUID workspaceId, UUID departmentId, UUID candidateId, UUID attachmentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        CandidateAttachment attachment = findActiveAttachment(workspaceId, departmentId, candidateId, attachmentId);

        storageService.delete(attachment.getStoragePath());
        attachmentRepository.delete(attachment);
        log.info("Attachment deleted: {} for candidate {} by user {}",
                attachment.getOriginalFileName(), candidateId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentStatistics getStatistics(UUID workspaceId, UUID departmentId) {
        SecurityUtils.getCurrentUserId();
        findActiveDepartment(workspaceId, departmentId);

        AttachmentStatistics stats = new AttachmentStatistics();
        stats.setTotalAttachments(attachmentRepository.countByDepartmentId(departmentId));
        stats.setTotalStorageBytes(attachmentRepository.totalStorageByDepartmentId(departmentId));
        stats.setHasCv(attachmentRepository.existsByDepartmentIdAndAttachmentType(departmentId, AttachmentType.CV));
        stats.setCertificatesCount(attachmentRepository.countByDepartmentIdAndAttachmentType(departmentId, AttachmentType.CERTIFICATE));

        Map<AttachmentType, Long> byType = new EnumMap<>(AttachmentType.class);
        for (Object[] row : attachmentRepository.countByTypeGrouped(departmentId)) {
            byType.put((AttachmentType) row[0], (Long) row[1]);
        }
        stats.setAttachmentsByType(byType);

        return stats;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed size of 20MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("File must have a name.");
        }

        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("File extension '" + extension + "' is not allowed. Allowed: " + ALLOWED_EXTENSIONS);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private void validateMimeType(MultipartFile file, String extension) {
        byte[] bytes = getFileBytes(file);
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("File content is empty.");
        }
        switch (extension) {
            case "pdf":
                if (bytes.length < 4 || !(bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46)) {
                    throw new BadRequestException("Invalid PDF file: header does not match %PDF signature.");
                }
                break;
            case "png":
                if (bytes.length < 8 || bytes[0] != (byte) 0x89 || bytes[1] != 0x50 || bytes[2] != 0x4E || bytes[3] != 0x47
                        || bytes[4] != 0x0D || bytes[5] != 0x0A || bytes[6] != 0x1A || bytes[7] != 0x0A) {
                    throw new BadRequestException("Invalid PNG file: header does not match PNG signature.");
                }
                break;
            case "jpg":
            case "jpeg":
                if (bytes.length < 2 || bytes[0] != (byte) 0xFF || bytes[1] != (byte) 0xD8) {
                    throw new BadRequestException("Invalid JPEG file: header does not match JPEG signature.");
                }
                break;
        }
    }

    private byte[] getFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new BadRequestException("Failed to read uploaded file.");
        }
    }

    private CandidateAttachment findActiveAttachment(UUID workspaceId, UUID departmentId, UUID candidateId, UUID attachmentId) {
        findActiveCandidate(workspaceId, departmentId, candidateId);
        return attachmentRepository.findByIdAndCandidate_Id(attachmentId, candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found."));
    }

    private Candidate findActiveCandidate(UUID workspaceId, UUID departmentId, UUID candidateId) {
        findActiveDepartment(workspaceId, departmentId);
        Candidate candidate = candidateRepository.findByIdAndDepartmentId(candidateId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found."));
        if (candidate.isArchived()) {
            throw new ResourceNotFoundException("Candidate not found.");
        }
        return candidate;
    }

    private void findActiveDepartment(UUID workspaceId, UUID departmentId) {
        departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .filter(dept -> dept.getStatus() == WorkspaceStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));
    }
}
