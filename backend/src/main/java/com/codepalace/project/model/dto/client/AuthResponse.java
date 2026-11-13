package com.codepalace.project.model.dto.client;

/**
 * Response of a successful login, registration or token refresh.
 */
public record AuthResponse(String accessToken, String refreshToken, UserInfo user) {

    public record UserInfo(String email) {
    }
}
