package com.manu4u.tools.eval.report;

import com.manu4u.tools.eval.model.EvalCaseResult;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Builder
public class EvalReport {
    private Instant timestamp;
    private int totalCases;
    private int totalPassed;
    private int totalFailed;
    private double overallScore;
    private long avgLatencyMs;
    private Map<String, CategorySummary> byCategory;
    private Map<String, DimensionSummary> byDimension;
    private List<EvalCaseResult> allResults;
    private List<EvalCaseResult> failures;

    @Data
    @Builder
    public static class CategorySummary {
        private String category;
        private int total;
        private int passed;
        private int failed;
        private double score;
    }

    @Data
    @Builder
    public static class DimensionSummary {
        private String dimension;
        private int total;
        private int passed;
        private double score;
    }

    public static EvalReport build(List<EvalCaseResult> results) {
        int passed = (int) results.stream().filter(EvalCaseResult::isOverallPass).count();
        int failed = results.size() - passed;
        long avgLatency = (long) results.stream().mapToLong(EvalCaseResult::getLatencyMs).average().orElse(0);

        // Group by category
        Map<String, CategorySummary> byCategory = new LinkedHashMap<>();
        results.stream()
                .collect(Collectors.groupingBy(r -> r.getCategory() != null ? r.getCategory() : "unknown"))
                .forEach((cat, caseResults) -> {
                    int catPassed = (int) caseResults.stream().filter(EvalCaseResult::isOverallPass).count();
                    byCategory.put(cat, CategorySummary.builder()
                            .category(cat)
                            .total(caseResults.size())
                            .passed(catPassed)
                            .failed(caseResults.size() - catPassed)
                            .score(caseResults.stream().mapToDouble(EvalCaseResult::averageScore).average().orElse(0))
                            .build());
                });

        // Group by dimension
        Map<String, DimensionSummary> byDimension = new LinkedHashMap<>();
        results.stream()
                .flatMap(r -> r.getGrades().stream())
                .collect(Collectors.groupingBy(g -> g.dimension()))
                .forEach((dim, grades) -> {
                    int dimPassed = (int) grades.stream().filter(g -> g.passed()).count();
                    byDimension.put(dim, DimensionSummary.builder()
                            .dimension(dim)
                            .total(grades.size())
                            .passed(dimPassed)
                            .score(grades.stream().mapToDouble(g -> g.score()).average().orElse(0))
                            .build());
                });

        List<EvalCaseResult> failures = results.stream()
                .filter(r -> !r.isOverallPass())
                .toList();

        return EvalReport.builder()
                .timestamp(Instant.now())
                .totalCases(results.size())
                .totalPassed(passed)
                .totalFailed(failed)
                .overallScore(results.stream().mapToDouble(EvalCaseResult::averageScore).average().orElse(0))
                .avgLatencyMs(avgLatency)
                .byCategory(byCategory)
                .byDimension(byDimension)
                .allResults(results)
                .failures(failures)
                .build();
    }
}
