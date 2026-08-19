package com.trio.backend.service;

import com.trio.backend.dto.organisation.department.CreateDepartmentRequest;
import com.trio.backend.dto.organisation.department.DepartmentDetailsResponse;
import com.trio.backend.dto.organisation.department.DepartmentResponse;
import com.trio.backend.dto.organisation.department.DepartmentSummaryResponse;
import com.trio.backend.dto.organisation.department.UpdateDepartmentRequest;

import java.util.List;
import java.util.UUID;

public interface DepartmentService {

    DepartmentResponse create(UUID workspaceId, CreateDepartmentRequest request);

    DepartmentResponse getById(UUID workspaceId, UUID departmentId);

    List<DepartmentSummaryResponse> listByWorkspace(UUID workspaceId, boolean includeArchived);

    DepartmentDetailsResponse getDetails(UUID workspaceId, UUID departmentId);

    DepartmentResponse update(UUID workspaceId, UUID departmentId, UpdateDepartmentRequest request);

    void delete(UUID workspaceId, UUID departmentId);

    void deletePermanently(UUID workspaceId, UUID departmentId);

    DepartmentResponse restore(UUID workspaceId, UUID departmentId);
}

