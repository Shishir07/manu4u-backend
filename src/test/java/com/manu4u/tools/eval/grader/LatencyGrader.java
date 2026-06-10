package com.manu4u.tools.eval.grader;

import com.manu4u.tools.eval.model.EvalRawResult;
import com.manu4u.tools.eval.model.EvalTestCase;

public class LatencyGrader implements Grader {

    @Override
    public String dimensionName() {
        return "latency";
    }

    @Override
    public GradeResult grade(EvalTestCase testCase, EvalRawResult result) {
        long maxMs = testCase.getExpectations().getMaxLatencyMs();
        long actualMs = result.latencyMs();
        boolean passed = actualMs <= maxMs;

        return new GradeResult(dimensionName(), passed, passed ? 1.0 : 0.0,
                String.format("%dms %s %dms limit",
                        actualMs, passed ? "<=" : "EXCEEDS", maxMs));
    }
}
