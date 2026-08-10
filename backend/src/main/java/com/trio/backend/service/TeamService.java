package com.trio.backend.service;

import com.trio.backend.dto.organisation.team.CreateTeamRequest;
import com.trio.backend.dto.organisation.team.TeamDetailsResponse;
import com.trio.backend.dto.organisation.team.TeamResponse;
import com.trio.backend.dto.organisation.team.TeamSummaryResponse;
import com.trio.backend.dto.organisation.team.UpdateTeamRequest;

import java.util.List;
import java.util.UUID;

public interface TeamService {

    TeamResponse create(UUID workspaceId, UUID departmentId, CreateTeamRequest request);

    TeamResponse getById(UUID workspaceId, UUID departmentId, UUID teamId);

    List<TeamSummaryResponse> listByDepartment(UUID workspaceId, UUID departmentId);

    List<TeamSummaryResponse> listByWorkspace(UUID workspaceId);

    TeamDetailsResponse getDetails(UUID workspaceId, UUID departmentId, UUID teamId);

    TeamResponse update(UUID workspaceId, UUID departmentId, UUID teamId, UpdateTeamRequest request);

    void delete(UUID workspaceId, UUID departmentId, UUID teamId);

    void deletePermanently(UUID workspaceId, UUID departmentId, UUID teamId);

    TeamResponse restore(UUID workspaceId, UUID departmentId, UUID teamId);
}

