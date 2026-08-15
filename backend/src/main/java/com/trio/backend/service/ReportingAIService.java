package com.trio.backend.service;

import com.trio.backend.dto.ai.ReportingEditRequest;
import com.trio.backend.dto.ai.ReportingGenerateRequest;
import com.trio.backend.dto.ai.ReportingResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ReportingAIService {

    ReportingResponse generate(ReportingGenerateRequest request);

    ReportingResponse regenerate(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId);

    ReportingResponse edit(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId,
                            ReportingEditRequest request);

    ReportingResponse approve(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId);

    ReportingResponse reject(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId);

    ReportingResponse getById(UUID workspaceId, UUID reportId);

    Page<ReportingResponse> getHistory(UUID workspaceId, int page, int size);
}
