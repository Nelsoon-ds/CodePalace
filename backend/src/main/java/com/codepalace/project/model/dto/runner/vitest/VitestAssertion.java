package com.codepalace.project.model.dto.runner.vitest;

import java.util.List;

public record VitestAssertion(String title, String fullName, String status, Double duration, List<String> failureMessages) {
}
