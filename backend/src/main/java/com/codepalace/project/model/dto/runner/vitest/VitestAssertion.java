package com.codepalace.project.model.dto.runner.vitest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public record VitestAssertion(String title, String fullName, String status, Double duration, List<String> failureMessages) {
}
