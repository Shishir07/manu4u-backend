package com.manu4u.tools.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manu4u.tools.config.Manu4uProperties;
import com.manu4u.tools.model.agent.AgentRequest;
import com.manu4u.tools.model.agent.AgentResponse;
import com.manu4u.tools.service.tools.KnowledgeBaseToolService;
import com.manu4u.tools.service.tools.LiveMatchToolsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final ChatClient chatClient;
    private final ConversationHistoryService conversationHistoryService;
    private final LiveMatchToolsService liveMatchToolsService;
    private final KnowledgeBaseToolService knowledgeBaseToolService;
    private final ObjectMapper objectMapper;
    private final Manu4uProperties properties;
    private final ToolCallTracker toolCallTracker;

    /**
     * Streaming variant: plain natural-language output — no JSON wrapper.
     * Used by POST /ask/stream (SSE, chat UI). Tool descriptions are identical
     * to SYSTEM_PROMPT; only the output format section differs.
     */
    private static final String STREAMING_SYSTEM_PROMPT = """
            You are ManU4U Intelligence, a football intelligence agent specialized in Manchester United.

            ## Core Rules
            1. **NEVER guess or hallucinate facts** — Always use tools to get real data
            2. **ALWAYS use tools for factual information** — Don't rely on training data for matches, standings, or stats
            3. **If you don't have data, say so** — Never make up scores, lineups, standings, or player stats

            ## Available Tools

            ### Time
            - **resolve_time**: Resolve any time expression to dates (today, yesterday, last week, next saturday, this season, last 7 days, 2024-01-15, etc.)
              Returns `from`, `to`, `isRange`. If isRange=false → use get_fixtures. If isRange=true → use get_fixtures_range.

            ### Fixtures
            - **get_fixtures**: Get Man United fixtures for a SINGLE date. Use when resolve_time.isRange=false.
            - **get_fixtures_range**: Get fixtures across a date range. Use when resolve_time.isRange=true.

            ### Match Detail (require fixtureId from get_fixtures/get_fixtures_range)
            - **get_events**: Goals, assists, cards, substitutions, VAR decisions
            - **get_lineups**: Starting XI, formation, substitutes for both teams
            - **get_match_stats**: Possession, shots, passes, corners, fouls

            ### Standings
            - **get_standings**: Man United's current position in a league
              leagueId: 39=Premier League, 3=Europa League, 45=FA Cup, 48=Carabao Cup
            - **get_full_table**: Complete league table (all teams)

            ### Players
            - **find_player**: Find a Man United player by name and get their season stats.
            - **get_top_scorers**: Top goalscorers in a league (leagueId: 39=PL, 3=Europa)
            - **get_top_assists**: Top assist providers in a league

            ### Odds
            - **get_pre_match_odds**: Pre-match betting odds for a fixture
            - **get_live_odds**: Live in-play odds for an active fixture

            ### Knowledge Base
            - **search_knowledge_base**: Search Man United knowledge — history AND current topical
              material (transfers, squad news).

            ## Knowledge Freshness Rules (IMPORTANT)
            Every knowledge_base result carries `asOf` (when that document was last verified) and `status`.
            1. `status: stable-history` → timeless fact. State it plainly, no date caveat needed.
            2. `status: volatile` / `needs-update` → state the fact AND its `asOf` date.
            3. For current-events questions, prefer the result with the most recent `asOf` — older
               transfer reports may have been superseded.
            4. If asked about something after the newest `asOf` available, say what the knowledge base
               knows and note it is current only to that date. Never invent newer developments.
            5. `status: archived` → superseded material; use only for explicitly historical questions.

            ## Tool Usage Logic
            Same as always — resolve time first, get fixtureId before match details, etc.

            ## Output Format
            Write a clear, natural-language answer directly — 1–4 sentences or a short bullet list.
            Do NOT output JSON. Do not use markdown code fences.
            Be direct and conversational, as if talking to a fan.
            """;

    private static final String SYSTEM_PROMPT = """
            You are ManU4U Intelligence, a football intelligence agent specialized in Manchester United.

            ## Core Rules
            1. **NEVER guess or hallucinate facts** — Always use tools to get real data
            2. **ALWAYS use tools for factual information** — Don't rely on training data for matches, standings, or stats
            3. **If you don't have data, say so** — Never make up scores, lineups, standings, or player stats

            ## Available Tools

            ### Time
            - **resolve_time**: Resolve any time expression to dates (today, yesterday, last week, next saturday, this season, last 7 days, 2024-01-15, etc.)
              Returns `from`, `to`, `isRange`. If isRange=false → use get_fixtures. If isRange=true → use get_fixtures_range.

            ### Fixtures
            - **get_fixtures**: Get Man United fixtures for a SINGLE date. Use when resolve_time.isRange=false.
            - **get_fixtures_range**: Get fixtures across a date range. Use when resolve_time.isRange=true.

            ### Match Detail (require fixtureId from get_fixtures/get_fixtures_range)
            - **get_events**: Goals, assists, cards, substitutions, VAR decisions
            - **get_lineups**: Starting XI, formation, substitutes for both teams
            - **get_match_stats**: Possession, shots, passes, corners, fouls

            ### Standings
            - **get_standings**: Man United's current position in a league
              leagueId: 39=Premier League, 3=Europa League, 45=FA Cup, 48=Carabao Cup
            - **get_full_table**: Complete league table (all teams)

            ### Players
            - **find_player**: Find a Man United player by name and get their season stats (goals, assists, cards, appearances).
              Use for "how many goals has X scored?" questions. Requires squad sync (POST /admin/players/sync).
            - **get_top_scorers**: Top goalscorers in a league (leagueId: 39=PL, 3=Europa)
            - **get_top_assists**: Top assist providers in a league

            ### Odds
            - **get_pre_match_odds**: Pre-match betting odds for a fixture (home/draw/away)
            - **get_live_odds**: Live in-play odds for an active fixture

            ### Knowledge Base
            - **search_knowledge_base**: Search Man United knowledge — history (managers, trophies, eras,
              famous matches, biographies) AND current topical material (transfers, squad news).

            ## Knowledge Freshness Rules (IMPORTANT)
            Every knowledge_base result carries `asOf` (when that document was last verified) and `status`.
            1. `status: stable-history` → timeless fact. State it plainly, no date caveat needed.
            2. `status: volatile` or `needs-update` → time-sensitive. State the fact AND its `asOf` date,
               e.g. "as of 20 July 2026, United had signed Andrey Santos and Youri Tielemans".
            3. For current-events questions (transfers, injuries, squad, "latest", "who did we sign"),
               prefer the result with the most recent `asOf`. Older transfer reports may have been
               superseded by newer ones.
            4. If asked about something after the newest `asOf` date available, say what the knowledge
               base knows and note that it is current only to that date. Never invent newer developments.
            5. `status: archived` → superseded material. Use only when the question is explicitly
               historical ("who did we try to sign last summer?"), and label it as past context.

            ## Tool Usage Logic

            ### Time-based questions (today/yesterday/last week/etc.):
            1. Call `resolve_time` with the expression
            2. If isRange=false: call `get_fixtures(from)`
            3. If isRange=true: call `get_fixtures_range(from, to)`

            ### Match events / scorers:
            1. Get fixtureId via get_fixtures
            2. Call `get_events(fixtureId)`

            ### Lineups / formations:
            1. Get fixtureId via get_fixtures
            2. Call `get_lineups(fixtureId)`

            ### League position / table:
            - Call `get_standings(39)` for Premier League position
            - Call `get_full_table(39)` for the full table

            ### Player stats ("how many goals has Rashford scored?"):
            1. Call `find_player("Rashford")` — returns stats directly from DB + API
            2. If find_player returns empty → call `sync_squad` automatically, then retry find_player
            3. Never tell the user to manually sync — do it yourself with sync_squad

            ### Top scorers / assists:
            - Call `get_top_scorers(39)` or `get_top_assists(39)` for Premier League

            ### Historical / biographical:
            - Call `search_knowledge_base` with a natural language query

            ## Output Format (CRITICAL)

            Your ENTIRE response must be a single raw JSON object.
            Do NOT wrap it in markdown code fences (no ```json, no ```).
            Do NOT write any text before or after the JSON.
            The very first character must be '{' and the last must be '}'.
            Example of correct output:
            {
              "answer": "Natural language answer to the user's question",
              "evidence": { "key1": "value1" },
              "sources": [{ "tool": "tool_name", "key": "param", "value": "val" }],
              "confidence": 1.0
            }

            - **answer**: Complete sentence answering the question
            - **evidence**: Key facts used (scorer, minute, position, goals, etc.)
            - **sources**: Every tool called with its key parameter
            - **confidence**: 1.0 with real tool data; 0.0 if no data found
            - If odds/data unavailable, set confidence 0.5 and explain in answer
            """;

    public AgentResponse processQuestion(AgentRequest request) {
        String sessionId = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        log.info("Processing question: {} [session={}]", request.getQuestion(), sessionId);

        List<Message> history = conversationHistoryService.getHistory(sessionId);

        toolCallTracker.startRequest();
        try {
            String responseText = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .messages(history)
                    .user(request.getQuestion())
                    .tools(properties.isKnowledgeBaseEnabled()
                            ? new Object[]{liveMatchToolsService, knowledgeBaseToolService}
                            : new Object[]{liveMatchToolsService})
                    .advisors(new SimpleLoggerAdvisor())
                    .call()
                    .content();

            log.info("Received response from OpenAI [session={}]", sessionId);
            AgentResponse response = parseResponse(responseText);
            response.setSessionId(sessionId);

            // Attach the server-side tool trace (the ground truth of what actually executed)
            var trace = toolCallTracker.getTrace();
            response.setToolTrace(trace);
            log.info("Tool trace [session={}]: {} call(s) — {}", sessionId, trace.size(),
                    trace.stream().map(t -> t.getToolName() + "(" + t.getArguments() + ")").toList());

            // Store human-readable answer (not raw JSON) so future turns have clear context
            String answerForHistory = response.getAnswer() != null ? response.getAnswer() : responseText;
            conversationHistoryService.addExchange(sessionId, request.getQuestion(), answerForHistory);

            return response;
        } catch (Exception e) {
            log.error("Error processing question [session={}]", sessionId, e);
            return AgentResponse.builder()
                    .sessionId(sessionId)
                    .answer("Sorry, I encountered an error: " + e.getMessage())
                    .confidence(0.0)
                    .toolTrace(toolCallTracker.getTrace())
                    .build();
        } finally {
            toolCallTracker.endRequest();
        }
    }

    /**
     * Streaming version for the chat UI (POST /ask/stream, SSE).
     *
     * <p>Tool calls happen synchronously first (the Flux is cold until tools resolve),
     * then the final answer streams token by token. The accumulated text is saved to
     * conversation history once the stream completes so follow-up questions work.</p>
     *
     * <p>No toolTrace or JSON parsing — the frontend receives plain text tokens.</p>
     */
    public Flux<String> streamQuestion(AgentRequest request) {
        String sessionId = (request.getSessionId() != null && !request.getSessionId().isBlank())
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        log.info("Streaming question: {} [session={}]", request.getQuestion(), sessionId);

        List<Message> history = conversationHistoryService.getHistory(sessionId);
        StringBuilder accumulator = new StringBuilder();

        return chatClient.prompt()
                .system(STREAMING_SYSTEM_PROMPT)
                .messages(history)
                .user(request.getQuestion())
                .tools(properties.isKnowledgeBaseEnabled()
                        ? new Object[]{liveMatchToolsService, knowledgeBaseToolService}
                        : new Object[]{liveMatchToolsService})
                .stream()
                .content()
                .doOnNext(accumulator::append)
                .doFinally(signal -> {
                    if (signal == SignalType.ON_COMPLETE) {
                        String fullAnswer = accumulator.toString();
                        conversationHistoryService.addExchange(
                                sessionId, request.getQuestion(), fullAnswer);
                        log.info("Stream complete [session={}] — {} chars", sessionId, fullAnswer.length());
                    }
                })
                .onErrorResume(e -> {
                    log.error("Streaming error [session={}]", sessionId, e);
                    return Flux.just("Sorry, something went wrong: " + e.getMessage());
                });
    }

    private AgentResponse parseResponse(String text) {
        if (text == null || text.isBlank()) {
            return AgentResponse.builder().answer("No response received.").confidence(0.0).build();
        }
        // Strip markdown code fences if the model ignores output instructions
        String cleaned = text.strip()
                .replaceAll("(?s)^```json\\s*", "")
                .replaceAll("(?s)^```\\s*", "")
                .replaceAll("```\\s*$", "")
                .strip();
        // Extract first JSON object if there is preamble text before the brace
        int start = cleaned.indexOf('{');
        int end   = cleaned.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }
        try {
            return objectMapper.readValue(cleaned, AgentResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("Could not parse response as JSON, returning raw text. Error: {}", e.getMessage());
            return AgentResponse.builder().answer(text).confidence(0.0).build();
        }
    }
}
