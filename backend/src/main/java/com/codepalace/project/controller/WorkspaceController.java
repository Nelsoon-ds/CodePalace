package com.codepalace.project.controller;

import com.codepalace.project.model.Session;
import com.codepalace.project.model.Workspace;
import com.codepalace.project.model.dto.client.KataSessionRequest;
import com.codepalace.project.repository.WorkspaceRepository;
import com.codepalace.project.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
public class WorkspaceController {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private SessionService sessionService;

    @GetMapping("/api/workspaces")
    public List<Workspace> getWorkspaces() {
       return workspaceRepository.findAll();

    }

    @PostMapping("/api/workspaces/{workspaceId}/sessions")
    public Optional<Session> createSession(@RequestBody KataSessionRequest request) {
        System.out.println(request);
        Optional<Session> session = Optional.ofNullable(sessionService.createSession(request.workspaceId(), request.kataId()));

        return session;
    }

    @PostMapping("/api/workspaces")
    public void createWorkspace(@RequestBody Map<String, Object> request) {}

    @DeleteMapping("/api/workspaces/{id}")
    public void deleteWorkspace(@PathVariable("id") UUID id) {}
}
