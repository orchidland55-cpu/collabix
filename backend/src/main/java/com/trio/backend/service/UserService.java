package com.trio.backend.service;

import com.trio.backend.dto.auth.CreateUserRequest;
import com.trio.backend.dto.user.UpdateProfileRequest;
import com.trio.backend.dto.user.UpdateUserRequest;
import com.trio.backend.dto.user.UserProfileResponse;
import com.trio.backend.dto.user.UserResponse;
import com.trio.backend.dto.user.UserSearchCriteria;
import com.trio.backend.dto.user.UserStatisticsResponse;
import com.trio.backend.enums.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserService {

    UserResponse create(UUID workspaceId, CreateUserRequest request);

    UserResponse update(UUID workspaceId, UUID id, UpdateUserRequest request);

    UserProfileResponse updateProfile(UpdateProfileRequest request);

    UserResponse findById(UUID workspaceId, UUID id);

    Page<UserResponse> search(UUID workspaceId, UserSearchCriteria criteria, Pageable pageable);

    List<UserResponse> findAll(UUID workspaceId);

    void softDelete(UUID workspaceId, UUID id);

    void hardDelete(UUID workspaceId, UUID id);

    UserResponse activate(UUID workspaceId, UUID id);

    UserResponse deactivate(UUID workspaceId, UUID id);

    UserResponse suspend(UUID workspaceId, UUID id);

    UserResponse reactivate(UUID workspaceId, UUID id);

    UserResponse archive(UUID workspaceId, UUID id);

    UserResponse restoreFromArchive(UUID workspaceId, UUID id);

    UserResponse assignRoles(UUID workspaceId, UUID userId, Set<RoleName> roles);

    UserStatisticsResponse getStatistics(UUID workspaceId);

}
