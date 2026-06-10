package com.manu4u.tools.eval.grader;

import com.manu4u.tools.agent.ToolCallTracker;
import com.manu4u.tools.eval.model.EvalRawResult;
import com.manu4u.tools.eval.model.EvalTestCase;

import java.util.*;

public class ToolOrderGrader implements Grader {

    @Override
    public String dimensionName() {
        return "tool_ordering";
    }

    @Override
    public GradeResult grade(EvalTestCase testCase, EvalRawResult result) {
        List<List<String>> requiredOrder = testCase.getExpectations().getToolChain() != null
                ? testCase.getExpectations().getToolChain().getRequiredOrder()
                : Collections.emptyList();

        if (requiredOrder == null || requiredOrder.isEmpty()) {
            return new GradeResult(dimensionName(), true, 1.0, "No ordering constraints");
        }

        List<ToolCallTracker.ToolCallRecord> trace = result.response().getToolTrace();
        if (trace == null || trace.isEmpty()) {
            return new GradeResult(dimensionName(), false, 0.0,
                    "No tool trace available to check ordering");
        }

        // Build a map of toolName -> first sequence number seen
        Map<String, Integer> firstSeq = new HashMap<>();
        for (var record : trace) {
            firstSeq.putIfAbsent(record.getToolName(), record.getSequence());
        }

        int satisfied = 0;
        List<String> violations = new ArrayList<>();

        for (List<String> pair : requiredOrder) {
            if (pair.size() != 2) continue;
            String before = pair.get(0);
            String after = pair.get(1);

            Integer seqBefore = firstSeq.get(before);
            Integer seqAfter = firstSeq.get(after);

            if (seqBefore == null) {
                violations.add(before + " not called (expected before " + after + ")");
            } else if (seqAfter == null) {
                violations.add(after + " not called (expected after " + before + ")");
            } else if (seqBefore < seqAfter) {
                satisfied++;
            } else {
                violations.add(before + "(#" + seqBefore + ") should precede " +
                        after + "(#" + seqAfter + ")");
            }
        }

        double score = (double) satisfied / requiredOrder.size();
        boolean passed = violations.isEmpty();

        return new GradeResult(dimensionName(), passed, score,
                passed ? "All ordering constraints satisfied" : "Violations: " + violations);
    }
}
