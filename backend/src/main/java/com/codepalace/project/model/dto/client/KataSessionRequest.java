package com.codepalace.project.model.dto.client;

import java.util.UUID;

public record KataSessionRequest(UUID workspaceId, UUID kataId) {
}
