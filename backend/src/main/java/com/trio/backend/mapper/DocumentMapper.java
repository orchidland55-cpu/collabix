package com.trio.backend.mapper;

import com.trio.backend.config.InstantToLocalDateTimeMapper;
import com.trio.backend.config.MapStructConfig;

import com.trio.backend.dto.Document.CreateDocumentRequest;
import com.trio.backend.dto.Document.UpdateDocumentRequest;
import com.trio.backend.dto.Document.DocumentResponse;
import com.trio.backend.entity.Document;
import org.mapstruct.*;

/**
 * Mapper for Document module.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true),
        config = MapStructConfig.class,
uses = InstantToLocalDateTimeMapper.class
)
public interface DocumentMapper {

    @Mapping(source = "documentVersion", target = "version")
    @Mapping(target = "projectId", expression = "java(document.getProject().getId())")
    @Mapping(target = "departmentId", expression = "java(document.getProject().getDepartment().getId())")
    DocumentResponse toResponse(Document document);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "task", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "aiProcessed", ignore = true)
    @Mapping(target = "storageType", ignore = true)
    @Mapping(target = "pdfExportAvailable", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    Document toEntity(CreateDocumentRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "task", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    void updateDocument(UpdateDocumentRequest request, @MappingTarget Document document);
}

