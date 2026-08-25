package com.trio.backend.ai.service.impl;

import com.trio.backend.ai.dto.AIPipelineResult;
import com.trio.backend.ai.dto.request.AIExecutionRequest;
import com.trio.backend.ai.dto.response.AIExecutionResponse;
import com.trio.backend.ai.enums.AIProvider;
import com.trio.backend.ai.enums.AITask;
import com.trio.backend.ai.service.AIOrchestratorService;
import com.trio.backend.ai.service.PipelineExecutor;
import com.trio.backend.ai.service.PromptBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIOrchestratorServiceImpl implements AIOrchestratorService {

    private final PipelineExecutor pipelineExecutor;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // Extract structured analysis from the first provider (GEMINI analysis step)
        Map<String, Object> structuredAnalysis = extractStructuredAnalysis(result, task);

        return AIExecutionResponse.builder()
                .task(task)
                .status(result.getProviderExecutions().stream().allMatch(AIPipelineResult.ProviderExecution::isSuccess)
                        ? "COMPLETED" : "PARTIAL")
                .response(result.getFinalResponse())
                .providerChain(pipeline)
                .executionTime(executionTime)
                .historyIds(historyIds)
                .timestamp(Instant.now())
                .structuredAnalysis(structuredAnalysis)
                .build();
    }

    private Map<String, Object> extractStructuredAnalysis(AIPipelineResult result, AITask task) {
        if (result.getProviderExecutions().isEmpty()) {
            return Map.of();
        }

        // For REPORT_GENERATION, the first provider (GEMINI) returns structured JSON analysis
        if (task == AITask.REPORT_GENERATION) {
            var firstExecution = result.getProviderExecutions().get(0);
            if (firstExecution.isSuccess() && firstExecution.getResponse() != null) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(firstExecution.getResponse());
                    return objectMapper.convertValue(jsonNode, Map.class);
                } catch (Exception e) {
                    log.warn("Failed to parse structured analysis JSON for task {}: {}", task, e.getMessage());
                }
            }
        }
        // For ANALYTICS_SUMMARY, similar approach
        if (task == AITask.ANALYTICS_SUMMARY) {
            var firstExecution = result.getProviderExecutions().get(0);
            if (firstExecution.isSuccess() && firstExecution.getResponse() != null) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(firstExecution.getResponse());
                    return objectMapper.convertValue(jsonNode, Map.class);
                } catch (Exception e) {
                    log.warn("Failed to parse structured analysis JSON for task {}: {}", task, e.getMessage());
                }
            }
        }
        // For HANDOVER_EXECUTIVE_REPORT
        if (task == AITask.HANDOVER_EXECUTIVE_REPORT) {
            var firstExecution = result.getProviderExecutions().get(0);
            if (firstExecution.isSuccess() && firstExecution.getResponse() != null) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(firstExecution.getResponse());
                    return objectMapper.convertValue(jsonNode, Map.class);
                } catch (Exception e) {
                    log.warn("Failed to parse structured analysis JSON for task {}: {}", task, e.getMessage());
                }
            }
        }
        // For KNOWLEDGE_SEARCH
        if (task == AITask.KNOWLEDGE_SEARCH) {
            var firstExecution = result.getProviderExecutions().get(0);
            if (firstExecution.isSuccess() && firstExecution.getResponse() != null) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(firstExecution.getResponse());
                    return objectMapper.convertValue(jsonNode, Map.class);
                } catch (Exception e) {
                    log.warn("Failed to parse structured analysis JSON for task {}: {}", task, e.getMessage());
                }
            }
        }

        return Map.of();
    }
}
