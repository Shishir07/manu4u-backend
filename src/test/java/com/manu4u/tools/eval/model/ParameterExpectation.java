package com.manu4u.tools.eval.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParameterExpectation {
    private String toolName;
    private List<String> argPatterns = Collections.emptyList(); // regex patterns
}
