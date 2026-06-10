package com.manu4u.tools.eval.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnswerExpectation {
    private List<String> requiredPatterns = Collections.emptyList();   // all must match
    private List<String> forbiddenPatterns = Collections.emptyList();  // none must match
}
