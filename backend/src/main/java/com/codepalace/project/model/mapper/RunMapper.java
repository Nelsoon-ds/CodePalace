package com.codepalace.project.model.mapper;

import com.codepalace.project.model.Run;
import com.codepalace.project.model.dto.AssertionDto;
import com.codepalace.project.model.dto.client.RunResponseDto;
import com.codepalace.project.model.dto.runner.vitest.VitestAssertion;
import com.codepalace.project.model.dto.runner.vitest.VitestResults;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class RunMapper {

    /** Vitest prints the failing line of the test file; that line is hidden suite code. */
    private static final Pattern SOURCE_FRAME = Pattern.compile("^\\s*(\\d+\\s*\\||\\|)");
    private static final Pattern FRAME_POINTER = Pattern.compile("^\\s*[❯>→]");

    private static final int MAX_DIFF_LENGTH = 2_000;
    private static final int MAX_OUTPUT_LENGTH = 8_000;

    public List<AssertionDto> toAssertions (VitestResults results) {
        if (results == null || results.testResults() == null ) {
            return Collections.emptyList();
        }

        return results.testResults().stream()
                .filter(suite -> suite.assertionResults() != null)
                .flatMap(suite -> suite.assertionResults().stream())
                .map(this::toAssertionDto)
                .toList();
    }

    private AssertionDto toAssertionDto(VitestAssertion assertion) {
        boolean passed = "passed".equalsIgnoreCase(assertion.status());
        String diff = null;
        if (!passed && assertion.failureMessages() != null && !assertion.failureMessages().isEmpty()) {
            diff = sanitizeFailureMessage(assertion.failureMessages().get(0));
        }
        return new AssertionDto(
                assertion.title(),
                passed,
                assertion.duration(),
                diff
        );
    }

    /**
     * Removes the code frame Vitest appends to a failure message.
     *
     * <p>The frame contains the source of the hidden test file, which must never reach the
     * browser (hidden-suite privacy invariant in docs/05-data-model-api.md).
     */
    static String sanitizeFailureMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        StringBuilder cleaned = new StringBuilder();

        for (String line : raw.split("\\R")) {
            if (FRAME_POINTER.matcher(line).find()) {
                break;
            }
            if (SOURCE_FRAME.matcher(line).find()) {
                continue;
            }
            if (!cleaned.isEmpty()) {
                cleaned.append('\n');
            }
            cleaned.append(line);
        }

        String result = cleaned.toString().trim();
        if (result.isEmpty()) {
            result = "Assertion failed";
        }
        return truncate(result, MAX_DIFF_LENGTH);
    }

    /**
     * Keeps the tail of the container output and drops anything that looks like a source frame.
     */
    static String sanitizeOutput(String raw) {
        if (raw == null) {
            return null;
        }

        StringBuilder cleaned = new StringBuilder();

        for (String line : raw.split("\\R")) {
            if (SOURCE_FRAME.matcher(line).find()) {
                continue;
            }
            if (!cleaned.isEmpty()) {
                cleaned.append('\n');
            }
            cleaned.append(line);
        }

        return truncate(cleaned.toString().trim(), MAX_OUTPUT_LENGTH);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return "..." + value.substring(value.length() - maxLength);
    }

    public RunResponseDto toResponse (Run run) {
        VitestResults results = run.getResults();

        if (results == null) {
            return new RunResponseDto(
                    run.getId(),
                    run.getSubmissionId(),
                    run.getStatus(),
                    run.getDurationMs(),
                    0,0,0,
                    Collections.emptyList(),
                    sanitizeOutput(run.getOutputTail()),
                    run.getCreatedAt()
            );
        }
        List<AssertionDto> assertions = toAssertions(results);

        return new RunResponseDto(
                run.getId(),
                run.getSubmissionId(),
                run.getStatus(),
                run.getDurationMs(),
                results.numTotalTests(),
                results.numPassedTests(),
                results.numFailedTests(),
                assertions,
                sanitizeOutput(run.getOutputTail()),
                run.getCreatedAt()
        );
    }
}
