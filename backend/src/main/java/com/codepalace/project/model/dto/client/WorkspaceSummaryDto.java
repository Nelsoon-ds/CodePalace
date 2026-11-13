package com.codepalace.project.model.dto.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Client-facing view of a workspace. Never contains the owning user entity, only its id.
 */
public record WorkspaceSummaryDto(
        UUID id,
        String name,
        Instant createdAt,
        List<SessionSummaryDto> sessions
) {
}
