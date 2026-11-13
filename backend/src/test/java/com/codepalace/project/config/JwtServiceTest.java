package com.codepalace.project.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 60_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 120_000L);

        userDetails = User.withUsername("test@example.com").password("hash").roles("USER").build();
    }

    @Test
    void accessTokenIsOnlyAcceptedAsAnAccessToken() {
        Claims claims = jwtService.parseClaims(jwtService.generateAccessToken(userDetails));

        assertThat(jwtService.isTokenValid(claims, userDetails, JwtService.TYPE_ACCESS)).isTrue();
        assertThat(jwtService.isTokenValid(claims, userDetails, JwtService.TYPE_REFRESH)).isFalse();
    }

    @Test
    void refreshTokenIsOnlyAcceptedAsARefreshToken() {
        Claims claims = jwtService.parseClaims(jwtService.generateRefreshToken(userDetails));

        assertThat(jwtService.isTokenValid(claims, userDetails, JwtService.TYPE_REFRESH)).isTrue();
        assertThat(jwtService.isTokenValid(claims, userDetails, JwtService.TYPE_ACCESS)).isFalse();
    }

    @Test
    void tokenOfAnotherUserIsRejected() {
        UserDetails other = User.withUsername("other@example.com").password("hash").roles("USER").build();
        Claims claims = jwtService.parseClaims(jwtService.generateAccessToken(other));

        assertThat(jwtService.isTokenValid(claims, userDetails, JwtService.TYPE_ACCESS)).isFalse();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(userDetails);

        assertThatThrownBy(() -> jwtService.parseClaims(token + "x"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void garbageTokenIsRejected() {
        assertThatThrownBy(() -> jwtService.parseClaims("not-a-token"))
                .isInstanceOf(JwtException.class);
    }
}
