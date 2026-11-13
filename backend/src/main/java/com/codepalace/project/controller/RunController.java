package com.codepalace.project.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class RunController {

    @GetMapping("/api/runs/{id}")
    public void getRun(@PathVariable("id") UUID id) {}

    @GetMapping(value = "/api/runs/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void getEvents(@PathVariable("id") UUID id) {}
}
