package com.codepalace.project.model.dto.runner.vitest;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VitestSuiteResult(String name, String status, long startTime, long endTime, String message, List<VitestAssertion> assertionResults) {
}
