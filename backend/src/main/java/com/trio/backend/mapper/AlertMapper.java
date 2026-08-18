package com.trio.backend.mapper;

import com.trio.backend.config.InstantToLocalDateTimeMapper;
import com.trio.backend.config.MapStructConfig;
import com.trio.backend.dto.alert.AlertResponse;
import com.trio.backend.entity.Alert;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the Alert module.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true),
        config = MapStructConfig.class,
        uses = InstantToLocalDateTimeMapper.class
)
public interface AlertMapper {

    @Mappings({
            @Mapping(target = "workspaceId", source = "alert.workspace.id"),
            @Mapping(target = "recipientId", source = "alert.recipient.id"),
            @Mapping(target = "departmentId", source = "alert.department.id")
    })
    AlertResponse toResponse(Alert alert);
}
