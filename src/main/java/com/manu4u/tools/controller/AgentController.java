package com.manu4u.tools.controller;

import com.manu4u.tools.agent.AgentOrchestrator;
import com.manu4u.tools.model.agent.AgentRequest;
import com.manu4u.tools.model.agent.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/ask")
@RequiredArgsConstructor
@Tag(name = "Agent", description = "Natural language Q&A about Manchester United")
public class AgentController {

    private final AgentOrchestrator agentOrchestrator;

    @Operation(summary = "Ask a question about Manchester United",
               description = "Runs a ReAct agent loop with tool calling. Returns a structured JSON response " +
                             "including answer, evidence, sources, confidence, and toolTrace. " +
                             "Use POST /ask/stream for the chat UI (SSE token streaming).")
    @PostMapping
    public ResponseEntity<AgentResponse> ask(@Valid @RequestBody AgentRequest request) {
        log.info("Received question: {}", request.getQuestion());
        AgentResponse response = agentOrchestrator.processQuestion(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Ask a question — streaming (SSE)",
               description = "Same agent as POST /ask but streams the answer token-by-token as Server-Sent Events. " +
                             "Intended for the chat UI frontend. Returns plain text (no JSON wrapper). " +
                             "Tool calls resolve before streaming begins, then the answer flows in real time. " +
                             "Conversation history is saved on stream completion so follow-up questions work.")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@Valid @RequestBody AgentRequest request) {
        log.info("Received streaming question: {}", request.getQuestion());
        return agentOrchestrator.streamQuestion(request);
    }
}
