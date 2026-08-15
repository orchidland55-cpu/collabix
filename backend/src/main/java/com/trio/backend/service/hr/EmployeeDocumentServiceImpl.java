package com.trio.backend.service.hr;

import com.trio.backend.dto.hr.EmployeeDocumentResponse;
import com.trio.backend.dto.hr.EmployeeDocumentSearchCriteria;
import com.trio.backend.dto.hr.EmployeeDocumentStatistics;
import com.trio.backend.entity.Employee;
import com.trio.backend.entity.EmployeeDocument;
import com.trio.backend.entity.EmployeeEventLog;
import com.trio.backend.enums.DocumentStatus;
import com.trio.backend.enums.EmployeeDocumentType;
import com.trio.backend.enums.EmploymentStatus;
import com.trio.backend.enums.WorkspaceStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.exception.ResourceNotFoundException;
import com.trio.backend.mapper.EmployeeDocumentMapper;
import com.trio.backend.repository.DepartmentRepository;
import com.trio.backend.repository.EmployeeDocumentRepository;
import com.trio.backend.repository.EmployeeDocumentSpecification;
import com.trio.backend.repository.EmployeeEventLogRepository;
import com.trio.backend.repository.EmployeeRepository;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmployeeDocumentServiceImpl implements EmployeeDocumentService {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg", "gif", "txt", "rtf"
    ));

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024L;

    private final EmployeeDocumentRepository documentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeEventLogRepository employeeEventLogRepository;
    private final StorageService storageService;
    private final EmployeeDocumentMapper documentMapper;

    @Override
    public EmployeeDocumentResponse upload(UUID workspaceId, UUID departmentId, UUID employeeId,
                                           MultipartFile file, String title, EmployeeDocumentType documentType,
                                           String description, LocalDate expirationDate) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);

        validateFile(file);
        validateExpirationDate(expirationDate);

        String originalFileName = file.getOriginalFilename();
        String extension = extractExtension(originalFileName);
        validateMimeType(file, extension);
        String contentType = resolveContentType(file, extension);

        byte[] content = getFileBytes(file);
        String storagePath = storageService.store(content, originalFileName, contentType);

        EmployeeDocument document = EmployeeDocument.builder()
                .employee(employee)
                .documentType(documentType)
                .title(title != null ? title : originalFileName)
                .originalFileName(originalFileName)
                .storedFileName(storagePath)
                .mimeType(contentType)
                .fileExtension(extension)
                .fileSize(file.getSize())
                .storagePath(storagePath)
                .uploadedBy(userId)
                .fileVersion(1)
                .expirationDate(expirationDate)
                .verified(false)
                .status(DocumentStatus.ACTIVE)
                .description(description)
                .build();

        EmployeeDocument saved = documentRepository.save(document);
        log.info("Employee document uploaded: {} ({}) for employee {} by user {}",
                originalFileName, documentType, employeeId, userId);

        createDocumentEventLog(employee, "DOCUMENT_UPLOADED", null, documentType.name(),
                documentType.name().replace('_', ' ') + " uploaded: " + originalFileName);

        return documentMapper.toResponse(saved);
    }

    @Override
    public EmployeeDocumentResponse replace(UUID workspaceId, UUID departmentId, UUID employeeId,
                                            UUID documentId, MultipartFile file, String title,
                                            String description, LocalDate expirationDate) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);

        EmployeeDocument existing = findActiveDocument(documentId, employeeId);
        validateFile(file);
        validateExpirationDate(expirationDate);

        storageService.delete(existing.getStoragePath());

        String originalFileName = file.getOriginalFilename();
        String extension = extractExtension(originalFileName);
        validateMimeType(file, extension);
        String contentType = resolveContentType(file, extension);
        byte[] content = getFileBytes(file);
        String storagePath = storageService.store(content, originalFileName, contentType);

        EmployeeDocumentType oldType = existing.getDocumentType();

        existing.setOriginalFileName(originalFileName);
        existing.setStoredFileName(storagePath);
        existing.setFileExtension(extension);
        existing.setMimeType(contentType);
        existing.setFileSize(file.getSize());
        existing.setStoragePath(storagePath);
        existing.setChecksum(null);
        existing.setVerified(false);
        existing.setVerifiedBy(null);
        existing.setVerifiedAt(null);
        if (title != null) {
            existing.setTitle(title);
        }
        if (description != null) {
            existing.setDescription(description);
        }
        if (expirationDate != null) {
            existing.setExpirationDate(expirationDate);
        }

        EmployeeDocument saved = documentRepository.save(existing);
        log.info("Employee document replaced: {} for employee {} by user {}",
                originalFileName, employeeId, userId);

        createDocumentEventLog(employee, "DOCUMENT_REPLACED", oldType.name(), saved.getDocumentType().name(),
                saved.getDocumentType().name().replace('_', ' ') + " replaced: " + originalFileName);

        return documentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDocumentResponse getById(UUID workspaceId, UUID departmentId, UUID employeeId, UUID documentId) {
        SecurityUtils.getCurrentUserId();
        findActiveEmployee(workspaceId, departmentId, employeeId);
        EmployeeDocument document = findActiveDocument(documentId, employeeId);
        return documentMapper.toResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDocumentResponse> listByEmployee(UUID workspaceId, UUID departmentId, UUID employeeId, Pageable pageable) {
        SecurityUtils.getCurrentUserId();
        findActiveEmployee(workspaceId, departmentId, employeeId);
        return documentRepository
                .findAllByEmployee_IdAndStatusOrderByCreatedAtDesc(employeeId, DocumentStatus.ACTIVE, pageable)
                .map(documentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDocumentResponse> search(UUID workspaceId, UUID departmentId,
                                                  EmployeeDocumentSearchCriteria criteria, Pageable pageable) {
        SecurityUtils.getCurrentUserId();
        findActiveDepartment(workspaceId, departmentId);

        if (criteria != null && criteria.getEmployeeId() != null) {
            findActiveEmployee(workspaceId, departmentId, criteria.getEmployeeId());
        }

        return documentRepository.findAll(
                        EmployeeDocumentSpecification.withFilter(criteria), pageable)
                .map(documentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource download(UUID workspaceId, UUID departmentId, UUID employeeId, UUID documentId) {
        SecurityUtils.getCurrentUserId();
        findActiveEmployee(workspaceId, departmentId, employeeId);
        EmployeeDocument document = findActiveDocument(documentId, employeeId);
        return storageService.loadAsResource(document.getStoragePath());
    }

    @Override
    public EmployeeDocumentResponse verify(UUID workspaceId, UUID departmentId, UUID employeeId, UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);
        EmployeeDocument document = findActiveDocument(documentId, employeeId);

        document.setVerified(true);
        document.setVerifiedBy(userId);
        document.setVerifiedAt(java.time.Instant.now());

        EmployeeDocument saved = documentRepository.save(document);
        log.info("Employee document verified: {} for employee {} by user {}",
                document.getOriginalFileName(), employeeId, userId);

        createDocumentEventLog(employee, "DOCUMENT_VERIFIED", null, document.getDocumentType().name(),
                document.getDocumentType().name().replace('_', ' ') + " verified: " + document.getOriginalFileName());

        return documentMapper.toResponse(saved);
    }

    @Override
    public EmployeeDocumentResponse unverify(UUID workspaceId, UUID departmentId, UUID employeeId, UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);
        EmployeeDocument document = findActiveDocument(documentId, employeeId);

        document.setVerified(false);
        document.setVerifiedBy(null);
        document.setVerifiedAt(null);

        EmployeeDocument saved = documentRepository.save(document);
        log.info("Employee document unverified: {} for employee {} by user {}",
                document.getOriginalFileName(), employeeId, userId);

        createDocumentEventLog(employee, "DOCUMENT_UNVERIFIED", null, document.getDocumentType().name(),
                document.getDocumentType().name().replace('_', ' ') + " unverified: " + document.getOriginalFileName());

        return documentMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID workspaceId, UUID departmentId, UUID employeeId, UUID documentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Employee employee = findActiveEmployee(workspaceId, departmentId, employeeId);
        EmployeeDocument document = findActiveDocument(documentId, employeeId);

        storageService.delete(document.getStoragePath());
        document.setStatus(DocumentStatus.DELETED);
        documentRepository.save(document);

        log.info("Employee document deleted: {} for employee {} by user {}",
                document.getOriginalFileName(), employeeId, userId);

        createDocumentEventLog(employee, "DOCUMENT_DELETED", null, document.getDocumentType().name(),
                document.getDocumentType().name().replace('_', ' ') + " deleted: " + document.getOriginalFileName());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDocumentStatistics getStatistics(UUID workspaceId, UUID departmentId, UUID employeeId) {
        SecurityUtils.getCurrentUserId();
        findActiveEmployee(workspaceId, departmentId, employeeId);

        EmployeeDocumentStatistics stats = new EmployeeDocumentStatistics();
        long total = documentRepository.countByEmployee_IdAndStatus(employeeId, DocumentStatus.ACTIVE);
        long verified = documentRepository.countByEmployee_IdAndStatusAndVerified(employeeId, DocumentStatus.ACTIVE, true);
        long unverified = total - verified;

        stats.setTotalDocuments(total);
        stats.setVerifiedCount(verified);
        stats.setUnverifiedCount(unverified);

        LocalDate now = LocalDate.now();
        long expiressd = documentRepository.countExpiredByEmployeeId(employeeId, now);
        long expiring = documentRepository.countExpiringByEmployeeId(employeeId, now, now.plusDays(30));

        stats.setExpiredCount(expiressd);
        stats.setExpiringCount(expiring);

        long totalBytes = documentRepository.totalStorageByEmployeeId(employeeId);
        stats.setTotalStorageBytes(totalBytes);

        Map<EmployeeDocumentType, Long> byType = new EnumMap<>(EmployeeDocumentType.class);
        for (Object[] row : documentRepository.countByTypeGroupedByEmployeeId(employeeId)) {
            byType.put((EmployeeDocumentType) row[0], (Long) row[1]);
        }
        stats.setDocumentsByType(byType);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDocumentResponse> getExpiringDocuments(UUID workspaceId, UUID departmentId, int withinDays) {
        SecurityUtils.getCurrentUserId();
        findActiveDepartment(workspaceId, departmentId);

        LocalDate now = LocalDate.now();
        LocalDate warningDate = now.plusDays(withinDays);

        return documentRepository.findExpiringByDepartmentId(departmentId, now, warningDate)
                .stream()
                .map(documentMapper::toResponse)
                .collect(Collectors.toList());
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

    private void validateExpirationDate(LocalDate expirationDate) {
        if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Expiration date cannot be in the past.");
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

    private String resolveContentType(MultipartFile file, String extension) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            return contentType;
        }
        return switch (extension.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "txt" -> "text/plain";
            case "rtf" -> "application/rtf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }

    private EmployeeDocument findActiveDocument(UUID documentId, UUID employeeId) {
        return documentRepository.findByIdAndEmployee_IdAndStatus(documentId, employeeId, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));
    }

    private Employee findActiveEmployee(UUID workspaceId, UUID departmentId, UUID employeeId) {
        findActiveDepartment(workspaceId, departmentId);
        Employee employee = employeeRepository.findByIdAndDepartment_Id(employeeId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));
        if (employee.getEmploymentStatus() == EmploymentStatus.TERMINATED
                || employee.getEmploymentStatus() == EmploymentStatus.RESIGNED
                || employee.getEmploymentStatus() == EmploymentStatus.RETIRED) {
            throw new ResourceNotFoundException("Employee not found.");
        }
        return employee;
    }

    private void findActiveDepartment(UUID workspaceId, UUID departmentId) {
        departmentRepository.findByIdAndWorkspace_Id(departmentId, workspaceId)
                .filter(dept -> dept.getStatus() == WorkspaceStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found."));
    }

    private void createDocumentEventLog(Employee employee, String eventType, String previousValue,
                                        String newValue, String description) {
        EmployeeEventLog log = EmployeeEventLog.builder()
                .employee(employee)
                .eventType(eventType)
                .previousValue(previousValue)
                .newValue(newValue)
                .description(description)
                .build();
        employeeEventLogRepository.save(log);
    }
}
