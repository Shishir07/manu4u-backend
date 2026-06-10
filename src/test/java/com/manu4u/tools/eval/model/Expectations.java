package com.manu4u.tools.eval.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Expectations {
    private ToolChainExpectation toolChain;
    private List<ParameterExpectation> parameters;
    private AnswerExpectation answer;
    private ConfidenceExpectation confidence;
    private long maxLatencyMs = 15000;
}
