package com.codepalace.project.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AgentController {

    @PostMapping(value = "/api/agent/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void postMessage(@RequestBody Map<String, Object> request) {}

    @GetMapping("/api/agent/sessions")
    public void getSessions() {}
}
