package com.codepalace.project.service;

import com.codepalace.project.model.Kata;
import com.codepalace.project.model.Session;
import com.codepalace.project.model.Workspace;
import com.codepalace.project.model.dto.client.SessionDetailDto;
import com.codepalace.project.model.mapper.SessionMapper;
import com.codepalace.project.repository.KataRepository;
import com.codepalace.project.repository.SessionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SessionService {

    private static final String DEFAULT_LANGUAGE = "js";

    private final SessionRepository sessionRepository;
    private final WorkspaceService workspaceService;
    private final KataRepository kataRepository;
    private final CurrentUserService currentUserService;
    private final SessionMapper sessionMapper;

    public SessionService(SessionRepository sessionRepository,
                          WorkspaceService workspaceService,
                          KataRepository kataRepository,
                          CurrentUserService currentUserService,
                          SessionMapper sessionMapper) {
        this.sessionRepository = sessionRepository;
        this.workspaceService = workspaceService;
        this.kataRepository = kataRepository;
        this.currentUserService = currentUserService;
        this.sessionMapper = sessionMapper;
    }

    public SessionDetailDto getOwned(UUID id) {
        Session session = requireOwned(id);
        return sessionMapper.toDetail(session, kataTitle(session.getKataId()));
    }

    @Transactional
    public SessionDetailDto create(UUID workspaceId, UUID kataId) {
        Workspace workspace = workspaceService.requireOwned(workspaceId);

        Kata kata = null;
        if (kataId != null) {
            kata = kataRepository.findById(kataId)
                    .orElseThrow(() -> new EntityNotFoundException("Kata not found."));
        }

        Instant now = Instant.now();
        Session session = new Session();
        session.setWorkspace(workspace);
        session.setKataId(kataId);
        session.setProgrammingLanguage(kata != null ? kata.getProgrammingLanguage() : DEFAULT_LANGUAGE);
        session.setStatus("active");
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionRepository.save(session);

        return sessionMapper.toDetail(session, kata != null ? kata.getTitle() : null);
    }

    @Transactional
    public void replaceDraft(UUID id, String code) {
        Session session = requireOwned(id);
        session.setDraftCode(code);
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
    }

    /**
     * Loads a session owned by the caller, or fails with 404.
     */
    public Session requireOwned(UUID id) {
        UUID userId = currentUserService.requireId();
        return sessionRepository.findOwnedSession(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found."));
    }

    private String kataTitle(UUID kataId) {
        if (kataId == null) {
            return null;
        }
        return kataRepository.findById(kataId).map(Kata::getTitle).orElse(null);
    }
}
