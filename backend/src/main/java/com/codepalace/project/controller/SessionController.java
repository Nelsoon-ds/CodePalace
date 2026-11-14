package com.codepalace.project.controller;

import com.codepalace.project.model.dto.client.CodeSubmission;
import com.codepalace.project.model.dto.client.SessionDetailDto;
import com.codepalace.project.model.dto.client.DraftRequest;
import com.codepalace.project.model.dto.client.RunResponseDto;
import com.codepalace.project.service.SessionService;
import com.codepalace.project.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class SessionController {

    private final SessionService sessionService;
    private final SubmissionService submissionService;

    public SessionController(SessionService sessionService, SubmissionService submissionService) {
        this.sessionService = sessionService;
        this.submissionService = submissionService;
    }

    @GetMapping("/api/sessions/{id}")
    public SessionDetailDto getSessionById(@PathVariable("id") UUID id) {
        return sessionService.getOwned(id);
    }

    @PutMapping("/api/sessions/{id}/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putDraftSubmission(
            @PathVariable("id") UUID id,
            @Valid @RequestBody DraftRequest draftData) {
        sessionService.replaceDraft(id, draftData.code());
    }

    @PostMapping("/api/sessions/{sessionId}/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RunResponseDto postSubmission(
            @PathVariable("sessionId") UUID sessionId,
            @Valid @RequestBody CodeSubmission submission) {
        return submissionService.createSubmission(sessionId, submission);
    }
}
