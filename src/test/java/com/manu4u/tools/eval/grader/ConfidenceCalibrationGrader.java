package com.manu4u.tools.eval.grader;

import com.manu4u.tools.eval.model.ConfidenceExpectation;
import com.manu4u.tools.eval.model.EvalRawResult;
import com.manu4u.tools.eval.model.EvalTestCase;

public class ConfidenceCalibrationGrader implements Grader {

    @Override
    public String dimensionName() {
        return "confidence";
    }

    @Override
    public GradeResult grade(EvalTestCase testCase, EvalRawResult result) {
        ConfidenceExpectation expectation = testCase.getExpectations().getConfidence();
        if (expectation == null) {
            return new GradeResult(dimensionName(), true, 1.0, "No confidence expectations");
        }

        Double confidence = result.response().getConfidence();
        if (confidence == null) {
            return new GradeResult(dimensionName(), false, 0.0, "Confidence is null");
        }

        boolean passed = confidence >= expectation.getMin() && confidence <= expectation.getMax();

        return new GradeResult(dimensionName(), passed, passed ? 1.0 : 0.0,
                passed
                        ? String.format("Confidence %.2f within [%.2f, %.2f]",
                            confidence, expectation.getMin(), expectation.getMax())
                        : String.format("Confidence %.2f outside expected range [%.2f, %.2f]",
                            confidence, expectation.getMin(), expectation.getMax()));
    }
}
