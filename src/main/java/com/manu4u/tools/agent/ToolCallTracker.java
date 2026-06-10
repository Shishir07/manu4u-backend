package com.manu4u.tools.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread-local tracker that captures every tool call during a single request.
 * <p>
 * Usage:
 * <pre>
 *   tracker.startRequest();           // at start of processQuestion
 *   // ... tool calls happen, each calls tracker.record(...)
 *   List&lt;ToolCallRecord&gt; trace = tracker.getTrace();  // after .call() completes
 *   tracker.endRequest();             // cleanup
 * </pre>
 * <p>
 * Each @Tool method in LiveMatchToolsService records its name, arguments,
 * and a summary of the result. This gives full visibility into the
 * tool call chain for prompt evaluation and session history enrichment.
 */
@Slf4j
@Component
public class ToolCallTracker {

    private static final ThreadLocal<List<ToolCallRecord>> CURRENT_TRACE = new ThreadLocal<>();

    /** Call at the start of each request to reset the trace. */
    public void startRequest() {
        CURRENT_TRACE.set(new ArrayList<>());
    }

    /** Record a single tool invocation. Called from @Tool methods. */
    public void record(String toolName, String arguments, String resultSummary) {
        List<ToolCallRecord> trace = CURRENT_TRACE.get();
        if (trace == null) {
            log.warn("ToolCallTracker.record() called outside of a tracked request — call startRequest() first");
            return;
        }
        ToolCallRecord entry = ToolCallRecord.builder()
                .toolName(toolName)
                .arguments(arguments)
                .resultSummary(resultSummary)
                .timestamp(Instant.now())
                .sequence(trace.size() + 1)
                .build();
        trace.add(entry);
        log.info("Tool #{} [{}] args={} → {}", entry.getSequence(), toolName, arguments, resultSummary);
    }

    /** Get the full trace for the current request. */
    public List<ToolCallRecord> getTrace() {
        List<ToolCallRecord> trace = CURRENT_TRACE.get();
        return trace != null ? Collections.unmodifiableList(trace) : Collections.emptyList();
    }

    /** Cleanup — call at the end of each request. */
    public void endRequest() {
        CURRENT_TRACE.remove();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallRecord {
        private int sequence;
        private String toolName;
        private String arguments;
        private String resultSummary;
        private Instant timestamp;
    }
}
