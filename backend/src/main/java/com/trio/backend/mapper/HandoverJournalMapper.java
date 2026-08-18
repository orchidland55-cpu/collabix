package com.trio.backend.mapper;

import com.trio.backend.config.MapStructConfig;
import com.trio.backend.dto.organisation.handover.HandoverJournalResponse;
import com.trio.backend.entity.HandoverJournal;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class, builder = @Builder(disableBuilder = true))
public interface HandoverJournalMapper {

    @Mapping(source = "workspace.id", target = "workspaceId")
    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "project.id", target = "projectId")
    HandoverJournalResponse toResponse(HandoverJournal handoverJournal);

    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    HandoverJournal toEntity(HandoverJournalResponse handoverJournalResponse);
}
