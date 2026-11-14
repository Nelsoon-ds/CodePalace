package com.codepalace.project.model.dto.client;

import java.time.Instant;
import java.util.UUID;

/**
 * Full session view for the editor. Carries the autosaved draft, never the hidden test suite.
 */
public record SessionDetailDto(
        UUID id,
        UUID workspaceId,
        UUID kataId,
        String title,
        String language,
        String status,
        String draftCode,
        Instant createdAt,
        Instant updatedAt
) {
}
