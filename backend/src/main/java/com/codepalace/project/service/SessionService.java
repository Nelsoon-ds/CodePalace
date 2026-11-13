package com.codepalace.project.service;

import com.codepalace.project.model.Kata;
import com.codepalace.project.model.Session;
import com.codepalace.project.model.Workspace;
import com.codepalace.project.repository.KataRepository;
import com.codepalace.project.repository.SessionRepository;
import com.codepalace.project.repository.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private KataRepository kataRepository;

    public Optional<Session> getSession(UUID id) {
        return sessionRepository.findById(id);
    }

    public Session createSession(UUID workspaceId, UUID kataId) {
        Session session = new Session();
        Optional<Kata> kata = kataRepository.findById(kataId);
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        session.setKataId(kataId);
        session.setProgrammingLanguage(kata.get().getProgrammingLanguage());
        session.setStatus("active");
        session.setWorkspace(workspace);
        session.setCreatedAt(Timestamp.from(Instant.now()));
        session.setUpdatedAt(Timestamp.from(Instant.now()));
        sessionRepository.save(session);
        return  session;
    }





}
