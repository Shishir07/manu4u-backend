package com.manu4u.tools.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manu4u.tools.model.agent.AgentRequest;
import com.manu4u.tools.model.agent.AgentResponse;
import com.manu4u.tools.service.tools.KnowledgeBaseToolService;
import com.manu4u.tools.service.tools.LiveMatchToolsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;
    @Mock
    private ConversationHistoryService conversationHistoryService;
    @Mock
    private LiveMatchToolsService liveMatchToolsService;
    @Mock
    private KnowledgeBaseToolService knowledgeBaseToolService;

    @InjectMocks
    private AgentOrchestrator orchestrator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void validJsonResponse_parsedIntoAgentResponse() {
        String json = """
                {"answer":"Bruno scored","evidence":{"scorer":"Bruno"},"sources":[],"confidence":1.0}
                """;
        stubChatClient(json.strip());

        AgentResponse result = orchestrator.processQuestion(new AgentRequest("Who scored?", null));

        assertThat(result.getAnswer()).isEqualTo("Bruno scored");
        assertThat(result.getConfidence()).isEqualTo(1.0);
        assertThat(result.getSessionId()).isNotBlank();
    }

    @Test
    void nonJsonResponse_returnedAsRawAnswer() {
        stubChatClient("Sorry, no data available.");

        AgentResponse result = orchestrator.processQuestion(new AgentRequest("Any match?", null));

        assertThat(result.getAnswer()).isEqualTo("Sorry, no data available.");
        assertThat(result.getConfidence()).isEqualTo(0.0);
    }

    @Test
    void providedSessionId_echoedInResponse() {
        String json = """
                {"answer":"They played well","evidence":{},"sources":[],"confidence":1.0}
                """;
        stubChatClient(json.strip());

        AgentResponse result = orchestrator.processQuestion(new AgentRequest("How did they play?", "my-session-123"));

        assertThat(result.getSessionId()).isEqualTo("my-session-123");
    }

    @Test
    void chatClientThrows_returnsErrorResponse() {
        when(chatClient.prompt()).thenThrow(new RuntimeException("OpenAI unreachable"));

        AgentResponse result = orchestrator.processQuestion(new AgentRequest("Who scored?", null));

        assertThat(result.getAnswer()).contains("error");
        assertThat(result.getConfidence()).isEqualTo(0.0);
        assertThat(result.getSessionId()).isNotBlank();
    }

    // --- helpers ---

    private void stubChatClient(String responseText) {
        when(conversationHistoryService.getHistory(anyString())).thenReturn(List.of());
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.messages(any(List.class))).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(responseText);
    }
}
