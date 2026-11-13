package com.codepalace.project.model.dto.client;

import jakarta.validation.constraints.Size;

public record DraftRequest(
        @Size(max = 200000, message = "Draft is too large")
        String code
) {
}
