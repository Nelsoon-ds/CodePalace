package com.codepalace.project.controller;

import com.codepalace.project.config.JwtService;
import com.codepalace.project.exception.UnauthenticatedException;
import com.codepalace.project.model.dto.client.AuthResponse;
import com.codepalace.project.model.dto.client.RefreshRequest;
import com.codepalace.project.model.dto.client.UserCreation;
import com.codepalace.project.model.dto.client.UserLogin;
import com.codepalace.project.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService,
                          JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<AuthResponse> registerAccount(@Valid @RequestBody UserCreation request) {
        userService.createUser(request.email(), request.password());
        return ResponseEntity.ok(issueTokens(request.email(), request.password()));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> loginAccount(@Valid @RequestBody UserLogin request) {
        return ResponseEntity.ok(issueTokens(request.email(), request.password()));
    }

    /**
     * Exchanges a refresh token for a new access token. The refresh token is accepted in the body
     * or as a bearer token, whichever the client has at hand.
     */
    @PostMapping("/api/auth/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        String token = extractRefreshToken(request, authorization);

        Claims claims = jwtService.parseClaims(token);
        String username = claims.getSubject();

        if (username == null) {
            throw new UnauthenticatedException("The refresh token is invalid.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(claims, userDetails, JwtService.TYPE_REFRESH)) {
            throw new UnauthenticatedException("The refresh token is invalid or has expired.");
        }

        return ResponseEntity.ok(buildResponse(userDetails));
    }

    private String extractRefreshToken(RefreshRequest request, String authorization) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return request.refreshToken().trim();
        }
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        throw new UnauthenticatedException("A refresh token is required.");
    }

    private AuthResponse issueTokens(String email, String rawPassword) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(UserService.normalizeEmail(email), rawPassword)
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return buildResponse(userDetails);
    }

    private AuthResponse buildResponse(UserDetails userDetails) {
        return new AuthResponse(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails),
                new AuthResponse.UserInfo(userDetails.getUsername())
        );
    }
}
