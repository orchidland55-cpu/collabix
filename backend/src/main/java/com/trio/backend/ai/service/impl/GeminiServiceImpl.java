package com.trio.backend.ai.service.impl;

import com.trio.backend.ai.configuration.AIConfiguration;
import com.trio.backend.ai.dto.request.AIHistoryRequest;
import com.trio.backend.ai.dto.request.AITextRequest;
import com.trio.backend.ai.dto.response.AITextResponse;
import com.trio.backend.ai.enums.AIProvider;
import com.trio.backend.ai.exception.AIConnectionException;
import com.trio.backend.ai.exception.AIConfigurationException;
import com.trio.backend.ai.exception.AIProviderException;
import com.trio.backend.ai.exception.AIResponseException;
import com.trio.backend.ai.service.AIHistoryService;
import com.trio.backend.ai.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GeminiServiceImpl implements GeminiService {

    private final RestClient restClient;
    private final AIConfiguration configuration;
    private final AIHistoryService aiHistoryService;

    @Override
    public AITextResponse generateText(AITextRequest request, UUID userId, UUID workspaceId, UUID departmentId) {
        String apiKey = configuration.getGemini().getApiKey();
        String model = configuration.getGemini().getModel();
        String baseUrl = configuration.getGemini().getUrl();

        if (apiKey == null || apiKey.isBlank()) {
            throw new AIConfigurationException("Gemini API key is not configured");
        }
        if (model == null || model.isBlank()) {
            throw new AIConfigurationException("Gemini model is not configured");
        }

        String url = baseUrl + "/v1/models/" + model + ":generateContent";

        Map<String, Object> part = Map.of("text", request.getPrompt());
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("contents", List.of(content));

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            Map<String, Object> systemPart = Map.of("text", request.getSystemPrompt());
            requestBody.put("systemInstruction", Map.of("parts", List.of(systemPart)));
        }

        Map<String, Object> generationConfig = new java.util.HashMap<>();
        if (request.getTemperature() != null) {
            generationConfig.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            generationConfig.put("maxOutputTokens", request.getMaxTokens());
        }
        if (!generationConfig.isEmpty()) {
            requestBody.put("generationConfig", generationConfig);
        }

        long start = System.currentTimeMillis();
        try {
            Map response = restClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            long executionTime = System.currentTimeMillis() - start;

            if (response == null) {
                throw new AIResponseException("Gemini returned null response");
            }

            if (response.containsKey("error")) {
                Map error = (Map) response.get("error");
                String message = error != null ? (String) error.get("message") : "Unknown error";
                throw new AIProviderException("Gemini API error: " + message);
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new AIResponseException("Gemini returned no candidates");
            }

            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> contentResponse = (Map<String, Object>) firstCandidate.get("content");
            if (contentResponse == null) {
                throw new AIResponseException("Gemini response missing content");
            }
            List<Map<String, Object>> parts = (List<Map<String, Object>>) contentResponse.get("parts");
            if (parts == null || parts.isEmpty()) {
                throw new AIResponseException("Gemini response missing parts");
            }
            String text = (String) parts.get(0).get("text");
            if (text == null) {
                throw new AIResponseException("Gemini response missing text");
            }

            Integer tokenCount = null;
            Map<String, Object> usageMetadata = (Map<String, Object>) response.get("usageMetadata");
            if (usageMetadata != null) {
                Object totalTokens = usageMetadata.get("totalTokenCount");
                if (totalTokens instanceof Number) {
                    tokenCount = ((Number) totalTokens).intValue();
                }
            }

            recordHistory(model, request.getPrompt(), text, executionTime, tokenCount, true, userId, workspaceId, departmentId);

            return AITextResponse.builder()
                    .provider(AIProvider.GEMINI)
                    .model(model)
                    .response(text)
                    .executionTime(executionTime)
                    .tokenUsage(tokenCount)
                    .success(true)
                    .build();

        } catch (AIConfigurationException | AIProviderException | AIResponseException e) {
            throw e;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - start;
            recordHistory(model, request.getPrompt(), e.getMessage(), executionTime, null, false, userId, workspaceId, departmentId);
            throw new AIConnectionException("Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    private void recordHistory(String model, String prompt, String response, long executionTime,
                                Integer tokenCount, boolean success, UUID userId, UUID workspaceId, UUID departmentId) {
        AIHistoryRequest historyRequest = new AIHistoryRequest();
        historyRequest.setUser(userId);
        historyRequest.setWorkspace(workspaceId);
        historyRequest.setDepartment(departmentId);
        historyRequest.setProvider(AIProvider.GEMINI);
        historyRequest.setModel(model);
        historyRequest.setPrompt(prompt);
        historyRequest.setResponse(response);
        historyRequest.setExecutionTime(executionTime);
        historyRequest.setTokenCount(tokenCount);
        historyRequest.setSuccess(success);
        aiHistoryService.create(historyRequest);
    }
}
