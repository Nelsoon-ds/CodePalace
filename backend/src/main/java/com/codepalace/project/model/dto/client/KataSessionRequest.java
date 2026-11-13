package com.codepalace.project.model.dto.client;

import java.util.UUID;

/**
 * Body of a session creation request. The workspace comes from the path, the kata is optional so
 * a session can start as a blank editor.
 */
public record KataSessionRequest(UUID kataId) {
}
