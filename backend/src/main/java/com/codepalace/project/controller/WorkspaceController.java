package com.codepalace.project.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class WorkspaceController {

    @GetMapping("/api/workspaces")
    public void getWorkspaces() {}

    @PostMapping("/api/workspaces")
    public void createWorkspace(@RequestBody Map<String, Object> request) {}

    @DeleteMapping("/api/workspaces/{id}")
    public void deleteWorkspace(@PathVariable("id") UUID id) {}
}
