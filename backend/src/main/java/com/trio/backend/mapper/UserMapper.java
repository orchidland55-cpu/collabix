package com.trio.backend.mapper;

import com.trio.backend.config.InstantToLocalDateTimeMapper;
import com.trio.backend.config.MapStructConfig;
import com.trio.backend.dto.auth.CreateUserRequest;
import com.trio.backend.dto.user.UserProfileResponse;
import com.trio.backend.dto.user.UserResponse;
import com.trio.backend.dto.user.UserSummaryResponse;
import com.trio.backend.entity.TeamMember;
import com.trio.backend.entity.User;
import com.trio.backend.enums.RoleName;
import com.trio.backend.enums.WorkspaceMemberStatus;
import com.trio.backend.dto.user.UpdateProfileRequest;
import com.trio.backend.dto.user.UpdateUserRequest;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true),
        config = MapStructConfig.class,
        uses = InstantToLocalDateTimeMapper.class
)
public interface UserMapper {

    @Mapping(target = "role", expression = "java(extractRole(user))")
    @Mapping(target = "departmentId", expression = "java(extractDepartmentId(user))")
    @Mapping(target = "departmentName", expression = "java(extractDepartmentName(user))")
    @Mapping(target = "teamId", expression = "java(extractTeamId(user))")
    @Mapping(target = "teamName", expression = "java(extractTeamName(user))")
    UserResponse toResponse(User user);

    @Mapping(target = "role", expression = "java(extractRole(user))")
    UserSummaryResponse toSummary(User user);

    @Mapping(target = "role", expression = "java(extractRole(user))")
    UserProfileResponse toProfile(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "memberType", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateProfile(UpdateProfileRequest request,
                       @MappingTarget User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "profilePicture", ignore = true)
    @Mapping(target = "primaryDepartment", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUser(UpdateUserRequest request,
                    @MappingTarget User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "profilePicture", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "primaryDepartment", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    User toEntity(CreateUserRequest request);

    default RoleName extractRole(User user) {
        if (user == null || user.getUserRoles().isEmpty()) {
            return RoleName.MEMBER;
        }
        return user.getUserRoles()
                .stream()
                .findFirst()
                .map(ur -> ur.getRole().getName())
                .orElse(RoleName.MEMBER);
    }

    default UUID extractDepartmentId(User user) {
        if (user == null || user.getPrimaryDepartment() == null) {
            return null;
        }
        return user.getPrimaryDepartment().getId();
    }

    default String extractDepartmentName(User user) {
        if (user == null || user.getPrimaryDepartment() == null) {
            return null;
        }
        return user.getPrimaryDepartment().getName();
    }

    default UUID extractTeamId(User user) {
        if (user == null || user.getTeamMembers() == null || user.getTeamMembers().isEmpty()) {
            return null;
        }
        return user.getTeamMembers()
                .stream()
                .filter(tm -> tm.getStatus() == WorkspaceMemberStatus.ACTIVE)
                .findFirst()
                .map(tm -> tm.getTeam().getId())
                .orElse(null);
    }

    default String extractTeamName(User user) {
        if (user == null || user.getTeamMembers() == null || user.getTeamMembers().isEmpty()) {
            return null;
        }
        return user.getTeamMembers()
                .stream()
                .filter(tm -> tm.getStatus() == WorkspaceMemberStatus.ACTIVE)
                .findFirst()
                .map(tm -> tm.getTeam().getName())
                .orElse(null);
    }
}
