package com.manu4u.tools.eval.grader;

import com.manu4u.tools.eval.model.EvalRawResult;
import com.manu4u.tools.eval.model.EvalTestCase;

public interface Grader {
    String dimensionName();
    GradeResult grade(EvalTestCase testCase, EvalRawResult result);
}
