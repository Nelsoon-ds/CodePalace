package com.codepalace.project.model.mapper;

import com.codepalace.project.model.Kata;
import com.codepalace.project.model.Run;
import com.codepalace.project.model.dto.runner.vitest.VitestAssertion;
import com.codepalace.project.model.dto.runner.vitest.VitestResults;
import com.codepalace.project.model.dto.runner.vitest.VitestSuiteResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The hidden test suite must never reach the browser: not through the failure diff, not through
 * the container output, and not through a serialised kata.
 */
class RunMapperTest {

    private static final String VITEST_FAILURE_MESSAGE = """
            AssertionError: expected 1 to be 2 // Object.is equality

            - Expected
            + Received

            - 2
            + 1

             ❯ test/katas/sum.test.mjs:12:24
                10|   it('adds', () => {
                11|     expect(sum(1, 1)).toBe(3)
                12|     expect(sum(2, 2)).toBe(4)
                   |                     ^
            """;

    private final RunMapper runMapper = new RunMapper();

    @Test
    void failureDiffDoesNotContainTheHiddenSuiteSource() {
        String diff = RunMapper.sanitizeFailureMessage(VITEST_FAILURE_MESSAGE);

        assertThat(diff).isNotNull();
        assertThat(diff).contains("expected 1 to be 2");
        assertThat(diff).doesNotContain("sum.test.mjs");
        assertThat(diff).doesNotContain("expect(sum(2, 2)).toBe(4)");
        assertThat(diff).doesNotContain("|");
    }

    @Test
    void containerOutputDoesNotContainTheHiddenSuiteSource() {
        String output = RunMapper.sanitizeOutput("""
                stderr | test/katas/sum.test.mjs
                  11|     expect(sum(1, 1)).toBe(3)
                stdout | done
                """);

        assertThat(output).isNotNull();
        assertThat(output).contains("done");
        assertThat(output).doesNotContain("expect(sum(1, 1)).toBe(3)");
    }

    @Test
    void assertionsAreMappedWithoutSuiteCode() {
        VitestResults results = new VitestResults(1, 0, 1, 0L, false,
                List.of(new VitestSuiteResult("sum.test.mjs", "failed", 0L, 1L, "",
                        List.of(new VitestAssertion("adds", "adds", "failed", 1.5,
                                List.of(VITEST_FAILURE_MESSAGE))))));

        Run run = new Run();
        run.setStatus("FAILED");
        run.setResults(results);

        var dto = runMapper.toResponse(run);

        assertThat(dto.assertions()).hasSize(1);
        assertThat(dto.assertions().getFirst().passed()).isFalse();
        assertThat(dto.assertions().getFirst().diff()).doesNotContain("sum.test.mjs");
    }

    @Test
    void serialisedKataNeverContainsTheReferenceSolution() throws Exception {
        Kata kata = new Kata();
        kata.setTitle("Sum of Two Numbers");
        kata.setReferenceSolution("export function sum(a, b) { return a + b; }");

        String json = new ObjectMapper().writeValueAsString(kata);

        assertThat(json).doesNotContain("referenceSolution");
        assertThat(json).doesNotContain("return a + b");
    }
}
