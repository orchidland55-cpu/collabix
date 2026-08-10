package com.trio.backend.mapper;

import com.trio.backend.config.MapStructConfig;
import com.trio.backend.dto.organisation.handover.CreateHandoverEntryRequest;
import com.trio.backend.dto.organisation.handover.HandoverEntryResponse;
import com.trio.backend.dto.organisation.handover.UpdateHandoverEntryRequest;
import com.trio.backend.entity.HandoverEntry;
import org.mapstruct.*;

/**
 * Mapper for the HandoverEntry module.
 */
@Mapper(
        config = MapStructConfig.class,
        builder = @Builder(disableBuilder = true),
        uses = UserSummaryMapper.class
)
public interface HandoverEntryMapper {

    @Mapping(target = "workspaceId", source = "workspace.id")
    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "taskId", source = "task.id")
    HandoverEntryResponse toResponse(HandoverEntry handoverEntry);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "workspace", ignore = true),
            @Mapping(target = "department", ignore = true),
            @Mapping(target = "project", ignore = true),
            @Mapping(target = "task", ignore = true),
            @Mapping(target = "sender", ignore = true),
            @Mapping(target = "receiver", ignore = true),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "deleted", ignore = true),
            @Mapping(target = "sentAt", ignore = true),
            @Mapping(target = "submittedAt", ignore = true),
            @Mapping(target = "acceptedAt", ignore = true),
            @Mapping(target = "rejectedAt", ignore = true),
            @Mapping(target = "completedAt", ignore = true),
            @Mapping(target = "archivedAt", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "updatedBy", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    HandoverEntry toEntity(CreateHandoverEntryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "workspace", ignore = true),
            @Mapping(target = "department", ignore = true),
            @Mapping(target = "project", ignore = true),
            @Mapping(target = "task", ignore = true),
            @Mapping(target = "sender", ignore = true),
            @Mapping(target = "receiver", ignore = true),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "deleted", ignore = true),
            @Mapping(target = "sentAt", ignore = true),
            @Mapping(target = "submittedAt", ignore = true),
            @Mapping(target = "acceptedAt", ignore = true),
            @Mapping(target = "rejectedAt", ignore = true),
            @Mapping(target = "completedAt", ignore = true),
            @Mapping(target = "archivedAt", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "createdBy", ignore = true),
            @Mapping(target = "updatedBy", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    void updateHandoverEntry(UpdateHandoverEntryRequest request, @MappingTarget HandoverEntry handoverEntry);
}
