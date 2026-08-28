package com.trio.backend.dto.Document;

import com.trio.backend.entity.Document.DocumentStatus;
import com.trio.backend.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Document response.
 *
 * <p>Represents the Complete state of a document for API clinkts.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    /**
     * The unique identifier of the document.
     */
    private UUID id;

    /**
     * The ID of the project this document is associated with.
     */
    private UUID projectId;

    /**
     * The ID of the department the document's project belongs to.
     */
    private UUID departmentId;

    /**
     * The ID of the task this document is associated with.
     * Null if the document is at project level.
     */
    private UUID taskId;

    /**
     * The title or name of the document.
     */
    private String title;

    /**
     * The description or summary of the document.
     */
    private String description;

    /**
     * The original file name.
     */
    private String fileName;

    /**
     * The MIME type of the file.
     */
    private String mimeType;

    /**
     * The size of the file in bytes.
     */
    private Long fileSize;

    /**
     * The storage path or identifier for the file.
     */
    private String storagePath;

    /**
     * Functional category of the document.
     */
    private String category;

    /**
     * Comma-separated tags for search and categorization.
     */
    private String tags;

    /**
     * Number of times the document has been viewed.
     */
    private Long viewCount;

    /**
     * The current status of the document (ACTIVE, ARCHIVED, or DELETED).
     */
    private DocumentStatus status;

    /**
     * Current version number of the document.
     * Used for versioning support.
     */
    private Integer version;

    /**
     * Flag indicating whether the document content has been processed by AI.
     */
    private Boolean aiProcessed;

    /**
     * Storage type indicator for cloud storage abstraction.
     * Examples: LOCAL, S3, GCS, AZURE
     */
    private String storageType;

    /**
     * Flag indicating whether PDF export version is available.
     */
    private Boolean pdfExportAvailable;

    /**
     * Timestamp when the document was created.
     */
    private LocalDateTime createdAt;

    /**
     * ID of the user who created the document.
     */
    private UUID createdBy;

    /**
     * Timestamp when the document was last updated.
     */
    private LocalDateTime updatedAt;

    /**
     * ID of the user who last updated the document.
     */
    private UUID updatedBy;

    private ApprovalStatus approvalStatus;
    private UUID approvedBy;
    private Instant approvedAt;
}
