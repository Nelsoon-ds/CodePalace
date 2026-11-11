package com.codepalace.project.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    @PostMapping("/api/auth/register")
    public void registerAccount(@RequestBody Map<String, Object> request) {}

    @PostMapping("/api/auth/login")
    public void loginAccount(@RequestBody Map<String, Object> request) {}
}
