package com.manu4u.tools.eval.model;

import com.manu4u.tools.model.agent.AgentResponse;

/**
 * Holds both the parsed response and raw HTTP body for grading.
 * The raw body is needed by JsonComplianceGrader to check for markdown fences.
 */
public record EvalRawResult(
        AgentResponse response,
        String rawBody,
        long latencyMs
) {}
