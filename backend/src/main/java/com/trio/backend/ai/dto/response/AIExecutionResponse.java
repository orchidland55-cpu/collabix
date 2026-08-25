package com.trio.backend.ai.dto.response;

import com.trio.backend.ai.enums.AIProvider;
import com.trio.backend.ai.enums.AITask;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class AIExecutionResponse {

    private AITask task;

    private String status;

    private String response;

    private List<AIProvider> providerChain;

    private Long executionTime;

    private List<UUID> historyIds;

    private Instant timestamp;

    private Map<String, Object> structuredAnalysis;
}
