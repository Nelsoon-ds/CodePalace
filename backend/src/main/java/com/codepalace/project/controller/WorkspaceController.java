package com.codepalace.project.controller;

import com.codepalace.project.model.dto.client.KataSessionRequest;
import com.codepalace.project.model.dto.client.SessionDetailDto;
import com.codepalace.project.model.dto.client.WorkspaceCreation;
import com.codepalace.project.model.dto.client.WorkspaceSummaryDto;
import com.codepalace.project.service.SessionService;
import com.codepalace.project.service.WorkspaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final SessionService sessionService;

    public WorkspaceController(WorkspaceService workspaceService, SessionService sessionService) {
        this.workspaceService = workspaceService;
        this.sessionService = sessionService;
    }

    @GetMapping("/api/workspaces")
    public List<WorkspaceSummaryDto> getWorkspaces() {
        return workspaceService.listForCurrentUser();
    }

    @PostMapping("/api/workspaces")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceSummaryDto createWorkspace(@Valid @RequestBody WorkspaceCreation request) {
        return workspaceService.create(request.name());
    }

    @PostMapping("/api/workspaces/{workspaceId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionDetailDto createSession(@PathVariable("workspaceId") UUID workspaceId,
                                           @RequestBody(required = false) KataSessionRequest request) {
        return sessionService.create(workspaceId, request == null ? null : request.kataId());
    }

    @DeleteMapping("/api/workspaces/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkspace(@PathVariable("id") UUID id) {
        workspaceService.delete(id);
    }
}
