package com.manu4u.tools.eval.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolChainExpectation {
    private List<String> requiredTools = Collections.emptyList();
    private List<List<String>> requiredOrder = Collections.emptyList(); // [[A, B]] = A before B
    private List<String> forbiddenTools = Collections.emptyList();
    private ToolMatchMode mode = ToolMatchMode.SUBSET;

    public enum ToolMatchMode {
        EXACT,   // agent must call exactly these tools, no more
        SUBSET   // agent must call at least these tools, extras allowed
    }
}
