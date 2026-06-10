package com.manu4u.tools.eval.report;

import com.manu4u.tools.eval.grader.GradeResult;
import com.manu4u.tools.eval.model.EvalCaseResult;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class EvalReportPrinter {

    private static final String SEPARATOR = "=".repeat(80);
    private static final String THIN_SEP = "-".repeat(80);

    public static void print(EvalReport report) {
        System.out.println();
        System.out.println(SEPARATOR);
        System.out.printf("  ManU4U Eval Report  |  %s  |  %d cases%n",
                report.getTimestamp().atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                report.getTotalCases());
        System.out.println(SEPARATOR);

        // Category summary
        System.out.println();
        System.out.println("CATEGORY SUMMARY");
        System.out.printf("  %-25s %-8s %-8s %-8s%n", "Category", "Pass", "Fail", "Score");
        System.out.println("  " + THIN_SEP.substring(0, 50));
        report.getByCategory().forEach((cat, summary) ->
                System.out.printf("  %-25s %d/%-5d %d/%-5d %.1f%%%n",
                        cat,
                        summary.getPassed(), summary.getTotal(),
                        summary.getFailed(), summary.getTotal(),
                        summary.getScore() * 100));

        // Dimension summary
        System.out.println();
        System.out.println("DIMENSION SUMMARY");
        System.out.printf("  %-28s %-10s %-8s%n", "Dimension", "Pass", "Score");
        System.out.println("  " + THIN_SEP.substring(0, 48));
        report.getByDimension().forEach((dim, summary) ->
                System.out.printf("  %-28s %d/%-7d %.1f%%%n",
                        dim,
                        summary.getPassed(), summary.getTotal(),
                        summary.getScore() * 100));

        // Overall
        System.out.println();
        System.out.printf("OVERALL: %d/%d passed (%.1f%%)  |  Avg latency: %dms%n",
                report.getTotalPassed(), report.getTotalCases(),
                (double) report.getTotalPassed() / report.getTotalCases() * 100,
                report.getAvgLatencyMs());

        // Failures
        if (!report.getFailures().isEmpty()) {
            System.out.println();
            System.out.println("FAILURES:");
            for (EvalCaseResult failure : report.getFailures()) {
                System.out.printf("  [FAIL] %s — \"%s\"%n", failure.getTestCaseId(),
                        truncate(failure.getQuestion(), 60));
                for (GradeResult grade : failure.getGrades()) {
                    if (!grade.passed()) {
                        System.out.printf("    - %s: FAIL (%.2f) — %s%n",
                                grade.dimension(), grade.score(),
                                truncate(grade.explanation(), 100));
                    }
                }
            }
        }

        System.out.println();
        System.out.println(SEPARATOR);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}
