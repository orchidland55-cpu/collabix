package com.trio.backend.ai.service.impl;

import com.trio.backend.ai.dto.request.AIHistoryRequest;
import com.trio.backend.ai.dto.response.AIHistoryResponse;
import com.trio.backend.ai.entity.AIHistory;
import com.trio.backend.ai.exception.AIException;
import com.trio.backend.ai.mapper.AIHistoryMapper;
import com.trio.backend.ai.repository.AIHistoryRepository;
import com.trio.backend.ai.service.AIHistoryService;
import com.trio.backend.security.ai.AIScopeAuthorization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AIHistoryServiceImpl implements AIHistoryService {

    private final AIHistoryRepository aiHistoryRepository;
    private final AIHistoryMapper aiHistoryMapper;
    private final AIScopeAuthorization aiScopeAuthorization;

    @Override
    public AIHistoryResponse create(AIHistoryRequest request) {
        aiScopeAuthorization.assertActiveWorkspaceMember(request.getWorkspace(), aiScopeAuthorization.currentUserId());
        aiScopeAuthorization.assertCanReadDepartmentScopedContent(request.getWorkspace(), request.getDepartment());
        AIHistory entity = aiHistoryMapper.toEntity(request);
        AIHistory saved = aiHistoryRepository.save(entity);
        return aiHistoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AIHistoryResponse findById(UUID id) {
        AIHistory entity = aiHistoryRepository.findById(id)
                .orElseThrow(() -> new AIException("AIHistory not found with id: " + id));
        UUID userId = aiScopeAuthorization.currentUserId();
        aiScopeAuthorization.assertActiveWorkspaceMember(entity.getWorkspace(), userId);
        aiScopeAuthorization.assertCanReadDepartmentScopedContent(entity.getWorkspace(), entity.getDepartment());
        if (!entity.getUser().equals(userId)
                && !aiScopeAuthorization.resolveReadableDepartmentFilter(entity.getWorkspace()).isEmpty()) {
            throw new com.trio.backend.exception.ForbiddenException("You do not have permission to access this AI history entry.");
        }
        return aiHistoryMapper.toResponse(entity);
    }
}
