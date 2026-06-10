package com.manu4u.tools.eval.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvalTestCase {
    private String id;
    private String conversationId;  // null = standalone
    private int turnOrder;          // sequence within conversation
    private String category;
    private List<String> tags;
    private String question;
    private Expectations expectations;
}
