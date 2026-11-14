package com.codepalace.project.model.dto.client;

/**
 * Body of a refresh request. The refresh token may also be sent as a bearer token.
 */
public record RefreshRequest(String refreshToken) {
}
