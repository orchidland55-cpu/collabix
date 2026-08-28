package com.trio.backend.entity;

import com.trio.backend.entity.base.AuditableEntity;
import com.trio.backend.enums.AttachmentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "hr_candidate_attachments",
        indexes = {
                @Index(name = "idx_hr_ca_candidate_id", columnList = "candidate_id"),
                @Index(name = "idx_hr_ca_type", columnList = "attachment_type"),
                @Index(name = "idx_hr_ca_candidate_type", columnList = "candidate_id, attachment_type"),
                @Index(name = "idx_hr_ca_uploaded_by", columnList = "uploaded_by"),
                @Index(name = "idx_hr_ca_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateAttachment extends AuditableEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 50)
    private AttachmentType attachmentType;

    @NotBlank
    @Size(max = 255)
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Size(max = 20)
    @Column(name = "file_extension", length = 20)
    private String fileExtension;

    @NotBlank
    @Size(max = 100)
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @NotNull
    @Positive
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @NotBlank
    @Size(max = 500)
    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "file_version", nullable = false)
    private Integer fileVersion;

    @PrePersist
    private void prePersist() {
        if (fileVersion == null) {
            fileVersion = 1;
        }
    }
}
