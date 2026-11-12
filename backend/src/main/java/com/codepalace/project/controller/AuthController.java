package com.codepalace.project.controller;

import com.codepalace.project.model.dto.client.UserCreation;
import com.codepalace.project.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/api/auth/register")
    public void registerAccount(@RequestBody UserCreation request) {
        userService.createUser(request.username(), request.password());
        System.out.println(request);

    }

    @PostMapping("/api/auth/login")
    public void loginAccount(@RequestBody Map<String, Object> request) {
    }
}
