package com.codepalace.project.model.mapper;

import com.codepalace.project.model.Run;
import com.codepalace.project.model.dto.AssertionDto;
import com.codepalace.project.model.dto.client.RunResponseDto;
import com.codepalace.project.model.dto.runner.vitest.VitestAssertion;
import com.codepalace.project.model.dto.runner.vitest.VitestResults;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class RunMapper {


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
            diff = assertion.failureMessages().get(0);
        }
        return new AssertionDto(
                assertion.title(),
                passed,
                assertion.duration(),
                diff
        );
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
                    run.getOutputTail(),
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
                run.getOutputTail(),
                run.getCreatedAt()
        );
    }
}
