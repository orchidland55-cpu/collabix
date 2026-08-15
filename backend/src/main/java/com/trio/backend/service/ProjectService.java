package com.trio.backend.service;

import com.trio.backend.dto.organisation.project.CreateProjectRequest;
import com.trio.backend.dto.organisation.project.ProjectResponse;
import com.trio.backend.dto.organisation.project.UpdateProjectRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProjectService {

    ProjectResponse create(UUID workspaceId, UUID departmentId, CreateProjectRequest request);

    ProjectResponse getById(UUID workspaceId, UUID departmentId, UUID projectId);

    List<ProjectResponse> list(UUID workspaceId, UUID departmentId);

    Page<ProjectResponse> listPaginated(UUID workspaceId, UUID departmentId, String search, Pageable pageable);

    Page<ProjectResponse> listAllPaginated(UUID workspaceId, String search, Pageable pageable);

    List<ProjectResponse> listArchived(UUID workspaceId, UUID departmentId);

    ProjectResponse update(UUID workspaceId, UUID departmentId, UUID projectId, UpdateProjectRequest request);

    void delete(UUID workspaceId, UUID departmentId, UUID projectId);

    ProjectResponse restore(UUID workspaceId, UUID departmentId, UUID projectId);
}
