package com.codepalace.project.service;

import com.codepalace.project.model.Kata;
import com.codepalace.project.model.Session;
import com.codepalace.project.model.User;
import com.codepalace.project.model.Workspace;
import com.codepalace.project.model.dto.client.WorkspaceSummaryDto;
import com.codepalace.project.model.mapper.WorkspaceMapper;
import com.codepalace.project.repository.KataRepository;
import com.codepalace.project.repository.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final KataRepository kataRepository;
    private final CurrentUserService currentUserService;
    private final WorkspaceMapper workspaceMapper;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            KataRepository kataRepository,
                            CurrentUserService currentUserService,
                            WorkspaceMapper workspaceMapper) {
        this.workspaceRepository = workspaceRepository;
        this.kataRepository = kataRepository;
        this.currentUserService = currentUserService;
        this.workspaceMapper = workspaceMapper;
    }

    public List<WorkspaceSummaryDto> listForCurrentUser() {
        User user = currentUserService.require();
        List<Workspace> workspaces = workspaceRepository.findAllByUser_Id(user.getUuid());
        Map<UUID, String> kataTitles = kataTitles(workspaces);

        return workspaces.stream()
                .map(workspace -> workspaceMapper.toSummary(workspace, kataTitles))
                .toList();
    }

    public WorkspaceSummaryDto getOwned(UUID id) {
        return workspaceMapper.toSummary(requireOwned(id), Map.of());
    }

    @Transactional
    public WorkspaceSummaryDto create(String name) {
        User user = currentUserService.require();
        Workspace workspace = new Workspace(user, name, Instant.now());
        workspaceRepository.save(workspace);
        return workspaceMapper.toSummary(workspace, Map.of());
    }

    @Transactional
    public void delete(UUID id) {
        workspaceRepository.delete(requireOwned(id));
    }

    /**
     * Loads a workspace that belongs to the caller, or fails with 404. Resources owned by somebody
     * else are reported as missing so their existence is not disclosed.
     */
    public Workspace requireOwned(UUID id) {
        User user = currentUserService.require();
        return workspaceRepository.findByIdAndUser_Id(id, user.getUuid())
                .orElseThrow(() -> new EntityNotFoundException("Workspace not found."));
    }

    private Map<UUID, String> kataTitles(List<Workspace> workspaces) {
        List<UUID> kataIds = workspaces.stream()
                .flatMap(workspace -> workspace.getSessions().stream())
                .map(Session::getKataId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (kataIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> titles = new HashMap<>();
        for (Kata kata : kataRepository.findAllById(kataIds)) {
            titles.put(kata.getId(), kata.getTitle());
        }
        return titles;
    }
}
