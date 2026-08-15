package com.trio.backend.service;

import com.trio.backend.dto.ai.AnalyticsAIEditRequest;
import com.trio.backend.dto.ai.AnalyticsAIResponse;
import com.trio.backend.enums.AIScopeType;

import java.time.LocalDate;
import java.util.UUID;

public interface AnalyticsAIService {

    AnalyticsAIResponse generate(UUID workspaceId, UUID departmentId, UUID projectId,
                                  LocalDate startDate, LocalDate endDate);

    AnalyticsAIResponse generate(UUID workspaceId, UUID departmentId, UUID projectId, UUID teamId,
                                  AIScopeType scope, LocalDate startDate, LocalDate endDate);

    AnalyticsAIResponse regenerate(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId);

    AnalyticsAIResponse edit(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId,
                              AnalyticsAIEditRequest request);

    AnalyticsAIResponse approve(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId);

    AnalyticsAIResponse reject(UUID workspaceId, UUID departmentId, UUID projectId, UUID reportId);
}
