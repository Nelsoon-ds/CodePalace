package com.codepalace.project.model.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceCreation(
        @NotBlank(message = "Workspace name is required")
        @Size(max = 120)
        String name
) {
}
