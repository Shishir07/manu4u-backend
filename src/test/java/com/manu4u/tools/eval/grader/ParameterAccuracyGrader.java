package com.manu4u.tools.eval.grader;

import com.manu4u.tools.agent.ToolCallTracker;
import com.manu4u.tools.eval.model.EvalRawResult;
import com.manu4u.tools.eval.model.EvalTestCase;
import com.manu4u.tools.eval.model.ParameterExpectation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ParameterAccuracyGrader implements Grader {

    @Override
    public String dimensionName() {
        return "parameter_accuracy";
    }

    @Override
    public GradeResult grade(EvalTestCase testCase, EvalRawResult result) {
        List<ParameterExpectation> paramExpectations = testCase.getExpectations().getParameters();
        if (paramExpectations == null || paramExpectations.isEmpty()) {
            return new GradeResult(dimensionName(), true, 1.0, "No parameter expectations");
        }

        List<ToolCallTracker.ToolCallRecord> trace = result.response().getToolTrace() != null
                ? result.response().getToolTrace()
                : Collections.emptyList();

        int totalChecks = 0;
        int passedChecks = 0;
        List<String> failures = new ArrayList<>();

        for (ParameterExpectation pe : paramExpectations) {
            // Find all trace entries for this tool
            List<ToolCallTracker.ToolCallRecord> toolCalls = trace.stream()
                    .filter(t -> t.getToolName().equals(pe.getToolName()))
                    .toList();

            if (toolCalls.isEmpty()) {
                totalChecks += pe.getArgPatterns().size();
                failures.add(pe.getToolName() + " was not called");
                continue;
            }

            // Concatenate all arguments from all calls to this tool
            String allArgs = toolCalls.stream()
                    .map(ToolCallTracker.ToolCallRecord::getArguments)
                    .reduce("", (a, b) -> a + " " + b);

            for (String pattern : pe.getArgPatterns()) {
                totalChecks++;
                if (Pattern.compile(pattern).matcher(allArgs).find()) {
                    passedChecks++;
                } else {
                    failures.add(pe.getToolName() + ": pattern '" + pattern +
                            "' not found in args '" + allArgs.trim() + "'");
                }
            }
        }

        double score = totalChecks == 0 ? 1.0 : (double) passedChecks / totalChecks;
        boolean passed = failures.isEmpty();

        return new GradeResult(dimensionName(), passed, score,
                passed ? "All parameter checks passed" : "Failures: " + failures);
    }
}
