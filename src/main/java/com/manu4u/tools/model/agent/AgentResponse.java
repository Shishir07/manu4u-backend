package com.manu4u.tools.model.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manu4u.tools.agent.ToolCallTracker;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentResponse {
    private String sessionId; // Echo back so client can continue the conversation
    private String answer; // Natural language answer
    private Map<String, Object> evidence; // Key facts extracted
    private List<Source> sources; // Tools used (as reported by the LLM)
    private Double confidence; // 0.0 to 1.0
    private List<ToolCallTracker.ToolCallRecord> toolTrace; // Actual tool calls executed (server-side truth)

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        private String tool; // Tool name
        private String key; // e.g., "fixtureId"
        private Object value; // e.g., 12345
    }
}
