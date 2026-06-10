package com.manu4u.tools.eval.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfidenceExpectation {
    private double min = 0.0;
    private double max = 1.0;
}
