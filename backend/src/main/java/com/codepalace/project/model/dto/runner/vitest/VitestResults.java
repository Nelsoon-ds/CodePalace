package com.codepalace.project.model.dto.runner.vitest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties
public record VitestResults(int numTotalTests, int numPassedTests, int numFailedTests, long startTime, boolean success, List<VitestSuiteResult> testResults) {
}
