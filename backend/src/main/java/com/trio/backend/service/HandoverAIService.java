package com.trio.backend.service;

import com.trio.backend.dto.ai.HandoverAIEditRequest;
import com.trio.backend.dto.ai.HandoverAIResponse;
import com.trio.backend.entity.HandoverEntry;

import java.time.LocalDate;
import java.util.UUID;

public interface HandoverAIService {

    HandoverAIResponse generate(UUID workspaceId, UUID departmentId, UUID projectId);

    HandoverAIResponse generate(UUID workspaceId, UUID departmentId, UUID projectId, LocalDate date, HandoverEntry.Shift shift);

    HandoverAIResponse regenerate(UUID workspaceId, UUID departmentId, UUID projectId, UUID journalId);

    HandoverAIResponse regenerate(UUID workspaceId, UUID departmentId, UUID projectId, UUID journalId, LocalDate date, HandoverEntry.Shift shift);

    HandoverAIResponse edit(UUID workspaceId, UUID departmentId, UUID projectId, UUID journalId, HandoverAIEditRequest request);

    HandoverAIResponse approve(UUID workspaceId, UUID departmentId, UUID projectId, UUID journalId);

    HandoverAIResponse reject(UUID workspaceId, UUID departmentId, UUID projectId, UUID journalId);
}
