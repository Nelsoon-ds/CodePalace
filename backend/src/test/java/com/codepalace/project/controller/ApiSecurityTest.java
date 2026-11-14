package com.codepalace.project.controller;

import com.codepalace.project.config.JwtService;
import com.codepalace.project.config.SecurityConfig;
import com.codepalace.project.model.User;
import com.codepalace.project.repository.UserRepository;
import com.codepalace.project.service.KataService;
import com.codepalace.project.service.RunEventService;
import com.codepalace.project.service.RunService;
import com.codepalace.project.service.SessionService;
import com.codepalace.project.service.SubmissionService;
import com.codepalace.project.service.UserService;
import com.codepalace.project.service.WorkspaceService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the API is actually protected: every route outside the auth endpoints requires a
 * valid access token, and a broken token produces a 401 instead of a 500.
 */
@WebMvcTest(controllers = {WorkspaceController.class, KataController.class, SessionController.class,
        RunController.class, AuthController.class})
@Import({SecurityConfig.class, JwtService.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.jwt.expiration-ms=60000",
        "app.jwt.refresh-expiration-ms=120000"
})
class ApiSecurityTest {

    private static final String EMAIL = "test@example.com";

    @MockitoBean WorkspaceService workspaceService;
    @MockitoBean SessionService sessionService;
    @MockitoBean SubmissionService submissionService;
    @MockitoBean RunService runService;
    @MockitoBean RunEventService runEventService;
    @MockitoBean KataService kataService;
    @MockitoBean UserService userService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean AuthenticationManager authenticationManager;

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        User user = new User(EMAIL, "irrelevant-hash");
        user.setUuid(UUID.randomUUID());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(workspaceService.listForCurrentUser()).thenReturn(List.of());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/workspaces"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void authenticatedRequestIsAccepted() throws Exception {
        mockMvc.perform(get("/api/workspaces").header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void malformedTokenIsRejectedWith401Not500() throws Exception {
        mockMvc.perform(get("/api/workspaces").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejectedWith401Not500() throws Exception {
        mockMvc.perform(get("/api/workspaces").header("Authorization", "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenCannotBeUsedAsAnAccessToken() throws Exception {
        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);

        mockMvc.perform(get("/api/workspaces")
                        .header("Authorization", "Bearer " + jwtService.generateRefreshToken(userDetails)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginIssuesAnAccessAndARefreshToken() throws Exception {
        Authentication authentication = authenticated();
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"correct-horse-battery\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(EMAIL));
    }

    @Test
    void refreshEndpointExchangesARefreshTokenForANewAccessToken() throws Exception {
        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void refreshEndpointRejectsAnAccessToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + accessToken() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownSessionIsReportedAsNotFound() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionService.getOwned(sessionId)).thenThrow(new EntityNotFoundException("Session not found."));

        mockMvc.perform(get("/api/sessions/" + sessionId).header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void invalidLoginBodyIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedUuidInThePathIsRejected() throws Exception {
        mockMvc.perform(get("/api/sessions/not-a-uuid").header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad_request"));
    }

    @Test
    void unknownPathIsReportedAsNotFound() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    private String accessToken() {
        return jwtService.generateAccessToken(userDetailsService.loadUserByUsername(EMAIL));
    }

    private String expiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode("MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="));
        Instant past = Instant.now().minusSeconds(120);
        return Jwts.builder()
                .subject(EMAIL)
                .claim(JwtService.CLAIM_TOKEN_TYPE, JwtService.TYPE_ACCESS)
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plusSeconds(60)))
                .signWith(key)
                .compact();
    }

    private Authentication authenticated() {
        UserDetails userDetails = userDetailsService.loadUserByUsername(EMAIL);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
