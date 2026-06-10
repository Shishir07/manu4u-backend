package com.manu4u.tools.eval.grader;

import com.manu4u.tools.eval.model.EvalRawResult;
import com.manu4u.tools.eval.model.EvalTestCase;

import java.util.ArrayList;
import java.util.List;

public class JsonComplianceGrader implements Grader {

    @Override
    public String dimensionName() {
        return "json_compliance";
    }

    @Override
    public GradeResult grade(EvalTestCase testCase, EvalRawResult result) {
        int totalChecks = 3;
        int passed = 0;
        List<String> failures = new ArrayList<>();

        String rawBody = result.rawBody();

        // Check 1: response is not null/empty
        if (rawBody != null && !rawBody.isBlank()) {
            passed++;
        } else {
            failures.add("Raw body is null or blank");
            return new GradeResult(dimensionName(), false, 0.0, "Raw body is null or blank");
        }

        // Check 2: answer was successfully parsed (not null)
        if (result.response().getAnswer() != null && !result.response().getAnswer().isBlank()) {
            passed++;
        } else {
            failures.add("Answer field is null/blank after parsing");
        }

        // Check 3: sessionId is present
        if (result.response().getSessionId() != null) {
            passed++;
        } else {
            failures.add("SessionId is null");
        }

        double score = (double) passed / totalChecks;
        boolean allPassed = failures.isEmpty();

        return new GradeResult(dimensionName(), allPassed, score,
                allPassed ? "Response parsed correctly" : "Issues: " + failures);
    }
}
