package com.manu4u.tools.eval.grader;

import com.manu4u.tools.eval.model.AnswerExpectation;
import com.manu4u.tools.eval.model.EvalRawResult;
import com.manu4u.tools.eval.model.EvalTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AnswerQualityGrader implements Grader {

    @Override
    public String dimensionName() {
        return "answer_quality";
    }

    @Override
    public GradeResult grade(EvalTestCase testCase, EvalRawResult result) {
        AnswerExpectation expectation = testCase.getExpectations().getAnswer();
        if (expectation == null) {
            return new GradeResult(dimensionName(), true, 1.0, "No answer expectations");
        }

        String answer = result.response().getAnswer();
        if (answer == null || answer.isBlank()) {
            return new GradeResult(dimensionName(), false, 0.0, "Answer is null or blank");
        }

        int totalChecks = 0;
        int passedChecks = 0;
        List<String> failures = new ArrayList<>();

        // Required patterns — all must match
        for (String pattern : expectation.getRequiredPatterns()) {
            totalChecks++;
            if (Pattern.compile(pattern).matcher(answer).find()) {
                passedChecks++;
            } else {
                failures.add("Required pattern not found: '" + pattern + "'");
            }
        }

        // Forbidden patterns — none should match
        for (String pattern : expectation.getForbiddenPatterns()) {
            totalChecks++;
            if (Pattern.compile(pattern).matcher(answer).find()) {
                failures.add("Forbidden pattern matched: '" + pattern + "'");
            } else {
                passedChecks++;
            }
        }

        double score = totalChecks == 0 ? 1.0 : (double) passedChecks / totalChecks;
        boolean passed = failures.isEmpty();

        return new GradeResult(dimensionName(), passed, score,
                passed ? "Answer matches all patterns" : "Issues: " + failures);
    }
}
