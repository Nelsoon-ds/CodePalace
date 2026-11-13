package com.codepalace.project.model.dto.client;

import com.codepalace.project.model.dto.AssertionDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Client response for get /api/runs/{id}
public record RunResponseDto(UUID id, UUID submissionId, String status, Double durationMs, int totalTests, int passedTests, int failedTests,
                             List<AssertionDto> assertions, String outputTail, Instant createdAt) {
}
