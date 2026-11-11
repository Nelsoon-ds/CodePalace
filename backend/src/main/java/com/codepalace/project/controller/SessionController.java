package com.codepalace.project.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class SessionController {

    @GetMapping("/api/sessions/{id}")
    public void getSessionById(@PathVariable("id") UUID id) {}

    @PutMapping("/api/sessions/{id}/draft")
    public void putDraftSubmission(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> request) {}

    @PatchMapping("/api/sessions/{id}")
    public void patchSession(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> request) {}

    @PostMapping("/api/sessions/{id}/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void postSubmission(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> request) {}
}
