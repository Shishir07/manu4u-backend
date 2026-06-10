package com.manu4u.tools.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory per-session conversation history.
 * Stores human-readable user/assistant pairs (not raw JSON) so the model
 * receives natural-language context on follow-up questions.
 */
@Slf4j
@Service
public class ConversationHistoryService {

    private static final int MAX_MESSAGES_PER_SESSION = 20; // 10 exchanges

    private final ConcurrentHashMap<String, List<Message>> sessions = new ConcurrentHashMap<>();

    public List<Message> getHistory(String sessionId) {
        return Collections.unmodifiableList(
                sessions.getOrDefault(sessionId, Collections.emptyList()));
    }

    /**
     * Persist one exchange. The {@code assistantAnswer} should be the extracted
     * natural-language answer, NOT the raw JSON blob.
     */
    public void addExchange(String sessionId, String userQuestion, String assistantAnswer) {
        List<Message> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
        history.add(new UserMessage(userQuestion));
        history.add(new AssistantMessage(assistantAnswer));

        // Trim to window — drop oldest pair when over limit
        while (history.size() > MAX_MESSAGES_PER_SESSION) {
            history.remove(0);
            history.remove(0);
        }

        log.debug("Session [{}] history size: {} messages", sessionId, history.size());
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("Cleared session [{}]", sessionId);
    }
}
