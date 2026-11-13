package com.codepalace.project.model.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeSubmission(
        @NotBlank(message = "Code is required")
        @Size(max = 200000, message = "Submission is too large")
        String code
) {
}
