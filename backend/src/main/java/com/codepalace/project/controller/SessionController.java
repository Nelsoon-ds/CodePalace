package com.codepalace.project.controller;

import com.codepalace.project.model.Session;
import com.codepalace.project.model.dto.client.CodeSubmission;
import com.codepalace.project.service.SessionService;
import com.codepalace.project.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;
import java.util.UUID;

@RestController
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SubmissionService submissionService;

    @GetMapping("/api/sessions/{id}")
    public Optional<Session> getSessionById(@PathVariable("id") UUID id) {
        Optional<Session> session = sessionService.getSession(id);
       return session;
    }

    @PutMapping("/api/sessions/{id}/draft")
    public void putDraftSubmission(
            @PathVariable("id") UUID id,
            @RequestBody String draftData) {
        sessionService.replaceDraft(id, draftData);
    }

    @PostMapping("/api/sessions/{sessionId}/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void postSubmission(
            @PathVariable("sessionId") String sessionId,
            @RequestBody CodeSubmission submission) {
        System.out.println("Client submission:" + submission);
        System.out.println(sessionId);
        UUID id = UUID.fromString(sessionId);
        submissionService.createSubmission(id,submission);

    }
}
