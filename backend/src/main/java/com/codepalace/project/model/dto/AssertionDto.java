package com.codepalace.project.model.dto;

public record AssertionDto(String name, boolean passed, Double ms, String diff) {
}
