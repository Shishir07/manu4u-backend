package com.manu4u.tools.eval.grader;

import com.manu4u.tools.agent.ToolCallTracker;
import com.manu4u.tools.eval.model.EvalRawResult;
import com.manu4u.tools.eval.model.EvalTestCase;
import com.manu4u.tools.eval.model.ToolChainExpectation;

import java.util.*;
import java.util.stream.Collectors;

public class ToolSelectionGrader implements Grader {

    @Override
    public String dimensionName() {
        return "tool_selection";
    }

    @Override
    public GradeResult grade(EvalTestCase testCase, EvalRawResult result) {
        ToolChainExpectation expectation = testCase.getExpectations().getToolChain();
        if (expectation == null || expectation.getRequiredTools().isEmpty()) {
            return new GradeResult(dimensionName(), true, 1.0, "No tool chain expectations defined");
        }

        Set<String> expected = new LinkedHashSet<>(expectation.getRequiredTools());
        Set<String> actual = result.response().getToolTrace() != null
                ? result.response().getToolTrace().stream()
                    .map(ToolCallTracker.ToolCallRecord::getToolName)
                    .collect(Collectors.toCollection(LinkedHashSet::new))
                : Collections.emptySet();
        Set<String> forbidden = new HashSet<>(expectation.getForbiddenTools());

        // Check forbidden tools
        Set<String> forbiddenUsed = actual.stream().filter(forbidden::contains).collect(Collectors.toSet());
        if (!forbiddenUsed.isEmpty()) {
            return new GradeResult(dimensionName(), false, 0.0,
                    "Forbidden tools called: " + forbiddenUsed);
        }

        // Check required tools present
        Set<String> missing = new LinkedHashSet<>(expected);
        missing.removeAll(actual);

        Set<String> extra = new LinkedHashSet<>(actual);
        extra.removeAll(expected);
        extra.removeAll(forbidden); // already checked

        boolean passed;
        if (expectation.getMode() == ToolChainExpectation.ToolMatchMode.EXACT) {
            passed = missing.isEmpty() && extra.isEmpty();
        } else {
            // SUBSET: extras are allowed
            passed = missing.isEmpty();
        }

        // Jaccard similarity for scoring
        Set<String> union = new LinkedHashSet<>(expected);
        union.addAll(actual);
        Set<String> intersection = new LinkedHashSet<>(expected);
        intersection.retainAll(actual);
        double score = union.isEmpty() ? 1.0 : (double) intersection.size() / union.size();

        String explanation;
        if (passed) {
            explanation = "OK — tools: " + actual;
        } else {
            List<String> issues = new ArrayList<>();
            if (!missing.isEmpty()) issues.add("Missing: " + missing);
            if (!extra.isEmpty() && expectation.getMode() == ToolChainExpectation.ToolMatchMode.EXACT)
                issues.add("Extra: " + extra);
            explanation = "Expected " + expected + ", got " + actual + ". " + String.join(", ", issues);
        }

        return new GradeResult(dimensionName(), passed, score, explanation);
    }
}
