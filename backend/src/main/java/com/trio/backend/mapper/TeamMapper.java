package com.trio.backend.mapper;

import com.trio.backend.config.InstantToLocalDateTimeMapper;
import com.trio.backend.config.MapStructConfig;
import com.trio.backend.dto.organisation.team.CreateTeamRequest;
import com.trio.backend.dto.organisation.team.TeamDetailsResponse;
import com.trio.backend.dto.organisation.team.TeamResponse;
import com.trio.backend.dto.organisation.team.TeamSummaryResponse;
import com.trio.backend.dto.organisation.team.UpdateTeamRequest;
import com.trio.backend.entity.Team;
import com.trio.backend.enums.WorkspaceMemberStatus;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true),
        config = MapStructConfig.class,
        uses = InstantToLocalDateTimeMapper.class
)
public interface TeamMapper {

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "managerName", expression = "java(team.getManager() != null ? team.getManager().getFirstName() + \" \" + team.getManager().getLastName() : null)")
    TeamResponse toResponse(Team team);

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "managerName", expression = "java(team.getManager() != null ? team.getManager().getFirstName() + \" \" + team.getManager().getLastName() : null)")
    @Mapping(target = "memberCount", expression = "java(countActiveMembers(team))")
    TeamSummaryResponse toSummary(Team team);

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "managerName", expression = "java(team.getManager() != null ? team.getManager().getFirstName() + \" \" + team.getManager().getLastName() : null)")
    @Mapping(target = "memberCount", expression = "java(countActiveMembers(team))")
    TeamDetailsResponse toDetails(Team team);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Team toEntity(CreateTeamRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workspace", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateTeam(UpdateTeamRequest request, @MappingTarget Team team);

    default long countActiveMembers(Team team) {
        if (team.getMembers() == null) {
            return 0L;
        }
        long activeMembers = team.getMembers().stream()
                .filter(m -> m.getStatus() == WorkspaceMemberStatus.ACTIVE)
                .count();
        boolean managerIsMember = team.getManager() != null
                && team.getMembers().stream()
                        .anyMatch(m -> m.getUser() != null
                                && team.getManager().getId() != null
                                && m.getUser().getId().equals(team.getManager().getId()));
        if (team.getManager() != null && !managerIsMember) {
            activeMembers++;
        }
        return activeMembers;
    }
}