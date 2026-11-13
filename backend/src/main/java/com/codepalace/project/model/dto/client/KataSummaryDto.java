package com.codepalace.project.model.dto.client;

import java.time.Instant;
import java.util.UUID;

/**
 * Public kata view. Deliberately excludes the reference solution (the answer key) and the
 * generation bookkeeping columns.
 */
public record KataSummaryDto(
        UUID id,
        String title,
        String statementMd,
        String difficulty,
        String language,
        String starterCode,
        Instant createdAt
) {
}
