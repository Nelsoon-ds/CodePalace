package com.codepalace.project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class GenerationController {

    @GetMapping("/api/generation/kata")
    public void getGenerationStatus(@RequestParam("jobId") UUID jobId) {}

    @PostMapping("/api/generation/kata")
    public void generateKata(@RequestBody Map<String, Object> request) {}
}
