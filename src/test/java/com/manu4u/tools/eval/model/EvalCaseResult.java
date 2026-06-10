package com.manu4u.tools.eval.model;

import com.manu4u.tools.eval.grader.GradeResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EvalCaseResult {
    private String testCaseId;
    private String category;
    private String question;
    private long latencyMs;
    private List<GradeResult> grades;
    private boolean overallPass;

    public double averageScore() {
        if (grades == null || grades.isEmpty()) return 0.0;
        return grades.stream().mapToDouble(GradeResult::score).average().orElse(0.0);
    }
}
