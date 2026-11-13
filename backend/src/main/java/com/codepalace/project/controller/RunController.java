package com.codepalace.project.controller;

import com.codepalace.project.model.dto.client.RunResponseDto;
import com.codepalace.project.service.RunEventService;
import com.codepalace.project.service.RunService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
public class RunController {

    private final RunService runService;
    private final RunEventService runEventService;

    public RunController(RunService runService, RunEventService runEventService) {
        this.runService = runService;
        this.runEventService = runEventService;
    }

    @GetMapping("/api/runs/{id}")
    public RunResponseDto getRun(@PathVariable("id") UUID id) {
        return runService.getOwned(id);
    }

    @GetMapping(value = "/api/runs/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getEvents(@PathVariable("id") UUID id) {
        return runEventService.stream(id);
    }
}
