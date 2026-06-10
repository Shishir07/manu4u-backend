package com.manu4u.tools.eval.grader;

public record GradeResult(
        String dimension,
        boolean passed,
        double score,       // 0.0 to 1.0
        String explanation  // human-readable detail on failure
) {}
