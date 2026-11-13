package com.codepalace.project.model.dto.client;

import java.time.Instant;
import java.util.UUID;

/**
 * Compact session view used in listings.
 */
public record SessionSummaryDto(
        UUID id,
        UUID workspaceId,
        UUID kataId,
        String title,
        String language,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
