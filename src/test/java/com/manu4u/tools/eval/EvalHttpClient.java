package com.manu4u.tools.eval;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.manu4u.tools.eval.model.EvalRawResult;
import com.manu4u.tools.model.agent.AgentResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Lightweight HTTP client for eval — uses java.net.http.HttpClient
 * so we don't need Spring context in tests.
 */
public class EvalHttpClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EvalHttpClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public EvalRawResult ask(String question, String sessionId) {
        try {
            Map<String, String> body = sessionId != null
                    ? Map.of("question", question, "sessionId", sessionId)
                    : Map.of("question", question);

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/ask"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            long start = System.nanoTime();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = (System.nanoTime() - start) / 1_000_000;

            String rawBody = httpResponse.body();
            AgentResponse response = objectMapper.readValue(rawBody, AgentResponse.class);

            return new EvalRawResult(response, rawBody, latencyMs);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            AgentResponse errorResponse = AgentResponse.builder()
                    .answer("HTTP ERROR: " + e.getMessage())
                    .confidence(0.0)
                    .build();
            return new EvalRawResult(errorResponse, "", 0);
        }
    }
}
