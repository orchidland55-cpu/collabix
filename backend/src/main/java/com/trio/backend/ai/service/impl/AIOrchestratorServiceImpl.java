package com.trio.backend.ai.service.impl;

import com.trio.backend.ai.dto.AIPipelineResult;
import com.trio.backend.ai.dto.request.AIExecutionRequest;
import com.trio.backend.ai.dto.response.AIExecutionResponse;
import com.trio.backend.ai.enums.AIProvider;
import com.trio.backend.ai.enums.AITask;
import com.trio.backend.ai.service.AIOrchestratorService;
import com.trio.backend.ai.service.PipelineExecutor;
import com.trio.backend.ai.service.PromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AIOrchestratorServiceImpl implements AIOrchestratorService {

    private final PipelineExecutor pipelineExecutor;
    private final PromptBuilder promptBuilder;

    @Override
    public AIExecutionResponse execute(AIExecutionRequest request) {
        long start = System.currentTimeMillis();
        AITask task = request.getTask();
        String input = request.getInput();
        UUID userId = request.getUserId();
        UUID workspaceId = request.getWorkspaceId();
        UUID departmentId = request.getDepartmentId();

        List<AIProvider> pipeline = pipelineExecutor.getPipeline(task);

        AIPipelineResult result = pipelineExecutor.execute(
                task, input, request.getContext(), promptBuilder,
                userId, workspaceId, departmentId
        );

        long executionTime = System.currentTimeMillis() - start;

        List<UUID> historyIds = result.getProviderExecutions().stream()
                .map(AIPipelineResult.ProviderExecution::getHistoryId)
                .toList();

        return AIExecutionResponse.builder()
                .task(task)
                .status(result.getProviderExecutions().stream().allMatch(AIPipelineResult.ProviderExecution::isSuccess)
                        ? "COMPLETED" : "PARTIAL")
                .response(result.getFinalResponse())
                .providerChain(pipeline)
                .executionTime(executionTime)
                .historyIds(historyIds)
                .timestamp(Instant.now())
                .build();
    }
}
