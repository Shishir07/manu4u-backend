package com.manu4u.tools.eval;

import com.manu4u.tools.eval.grader.*;
import com.manu4u.tools.eval.model.EvalCaseResult;
import com.manu4u.tools.eval.model.EvalRawResult;
import com.manu4u.tools.eval.model.EvalTestCase;
import com.manu4u.tools.eval.report.EvalReport;
import com.manu4u.tools.eval.report.EvalReportPrinter;
import com.manu4u.tools.eval.report.EvalReportWriter;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Eval runner that executes test cases against a live ManU4U instance.
 *
 * <p>Usage: {@code mvn test -Peval} or {@code mvn test -Peval -Deval.base-url=http://host:port}
 *
 * <p>Requires the app to be running separately (not @SpringBootTest).
 */
@Tag("eval")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EvalRunner {

    private static EvalHttpClient client;
    private static List<EvalTestCase> allCases;
    private static final List<EvalCaseResult> ALL_RESULTS = new CopyOnWriteArrayList<>();

    private static final List<Grader> GRADERS = List.of(
            new ToolSelectionGrader(),
            new ToolOrderGrader(),
            new ParameterAccuracyGrader(),
            new AnswerQualityGrader(),
            new ConfidenceCalibrationGrader(),
            new JsonComplianceGrader(),
            new LatencyGrader()
    );

    @BeforeAll
    static void setup() {
        String baseUrl = System.getProperty("eval.base-url", "http://localhost:8080");
        System.out.println("Eval target: " + baseUrl);
        client = new EvalHttpClient(baseUrl);
        allCases = EvalDatasetLoader.load();
        assertThat(allCases).as("Eval dataset must not be empty").isNotEmpty();
    }

    // ── Standalone cases ─────────────────────────────────────────────────────

    @Order(1)
    @TestFactory
    @DisplayName("Standalone eval cases")
    Stream<DynamicTest> evalStandaloneCases() {
        List<EvalTestCase> standalone = allCases.stream()
                .filter(tc -> tc.getConversationId() == null)
                .toList();

        return standalone.stream().map(tc ->
                DynamicTest.dynamicTest(tc.getId() + ": " + tc.getQuestion(), () -> {
                    EvalCaseResult result = executeAndGrade(tc, null);
                    ALL_RESULTS.add(result);
                }));
    }

    // ── Multi-turn conversations ─────────────────────────────────────────────

    @Order(2)
    @TestFactory
    @DisplayName("Multi-turn eval conversations")
    Stream<DynamicTest> evalMultiTurnConversations() {
        Map<String, List<EvalTestCase>> conversations = allCases.stream()
                .filter(tc -> tc.getConversationId() != null)
                .collect(Collectors.groupingBy(EvalTestCase::getConversationId));

        List<DynamicTest> tests = new ArrayList<>();

        conversations.forEach((convId, turns) -> {
            turns.sort(Comparator.comparingInt(EvalTestCase::getTurnOrder));
            String sessionId = UUID.randomUUID().toString();

            for (EvalTestCase turn : turns) {
                tests.add(DynamicTest.dynamicTest(
                        turn.getId() + " [conv=" + convId + " turn=" + turn.getTurnOrder() + "]",
                        () -> {
                            EvalCaseResult result = executeAndGrade(turn, sessionId);
                            ALL_RESULTS.add(result);
                        }));
            }
        });

        return tests.stream();
    }

    // ── Report ───────────────────────────────────────────────────────────────

    @AfterAll
    static void printReport() {
        if (ALL_RESULTS.isEmpty()) {
            System.out.println("No eval results to report.");
            return;
        }

        EvalReport report = EvalReport.build(ALL_RESULTS);
        EvalReportPrinter.print(report);
        EvalReportWriter.write(report);
    }

    // ── Core execution ───────────────────────────────────────────────────────

    private EvalCaseResult executeAndGrade(EvalTestCase testCase, String sessionId) {
        System.out.printf("  Running: [%s] %s%n", testCase.getId(), testCase.getQuestion());

        EvalRawResult rawResult = client.ask(testCase.getQuestion(), sessionId);

        List<GradeResult> grades = GRADERS.stream()
                .map(grader -> grader.grade(testCase, rawResult))
                .toList();

        boolean overallPass = grades.stream().allMatch(GradeResult::passed);

        EvalCaseResult result = EvalCaseResult.builder()
                .testCaseId(testCase.getId())
                .category(testCase.getCategory())
                .question(testCase.getQuestion())
                .latencyMs(rawResult.latencyMs())
                .grades(grades)
                .overallPass(overallPass)
                .build();

        // Log failures immediately
        if (!overallPass) {
            grades.stream()
                    .filter(g -> !g.passed())
                    .forEach(g -> System.out.printf("    FAIL [%s] %.2f — %s%n",
                            g.dimension(), g.score(), g.explanation()));
        }

        return result;
    }
}
