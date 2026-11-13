package com.codepalace.project.controller;

import com.codepalace.project.config.JwtService;
import com.codepalace.project.model.dto.client.UserCreation;
import com.codepalace.project.model.dto.client.UserLogin;
import com.codepalace.project.repository.UserRepository;
import com.codepalace.project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;

    @PostMapping("/api/auth/register")
    public ResponseEntity<Map<String, Object>> registerAccount(@RequestBody UserCreation request) {

        userService.createUser(request.email(), request.password());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "user", Map.of("email", userDetails.getUsername())
        ));

    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<Map<String, Object>> loginAccount(@RequestBody UserLogin request) {
        System.out.println(request);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "user", Map.of("email", userDetails.getUsername())
        ));

    }}
