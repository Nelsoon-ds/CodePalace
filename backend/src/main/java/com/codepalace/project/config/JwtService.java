package com.codepalace.project.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies the two token types used by the API.
 *
 * <p>Both tokens are signed with the same secret but carry a {@code typ} claim so a refresh token
 * can never be replayed as an access token, and vice versa.
 */
@Service
public class JwtService {

    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserDetails userDetails) {
        return generate(userDetails.getUsername(), TYPE_ACCESS, jwtExpirationMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generate(userDetails.getUsername(), TYPE_REFRESH, refreshExpirationMs);
    }

    private String generate(String subject, String type, long ttlMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TOKEN_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ttlMs)))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parses and verifies the signature of a token.
     *
     * @throws io.jsonwebtoken.JwtException if the token is malformed, unsigned, tampered with or expired
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validates already parsed claims against the expected user and token type.
     *
     * @return true when the claims are unexpired, of the expected type and belong to the given user
     */
    public boolean isTokenValid(Claims claims, UserDetails userDetails, String expectedType) {
        if (claims == null) {
            return false;
        }
        Date expiration = claims.getExpiration();
        if (expiration == null || expiration.before(new Date())) {
            return false;
        }
        if (!expectedType.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            return false;
        }
        String subject = claims.getSubject();
        return subject != null && subject.equals(userDetails.getUsername());
    }
}
