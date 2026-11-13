package com.codepalace.project.service;

import com.codepalace.project.model.Kata;
import com.codepalace.project.model.Session;
import com.codepalace.project.model.Workspace;
import com.codepalace.project.model.mapper.SessionMapper;
import com.codepalace.project.repository.KataRepository;
import com.codepalace.project.repository.SessionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionOwnershipTest {

    @Mock SessionRepository sessionRepository;
    @Mock WorkspaceService workspaceService;
    @Mock KataRepository kataRepository;
    @Mock CurrentUserService currentUserService;

    SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionRepository, workspaceService, kataRepository,
                currentUserService, new SessionMapper());
    }

    @Test
    void sessionOfAnotherUserIsNotFound() {
        UUID sessionId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        when(currentUserService.requireId()).thenReturn(callerId);
        when(sessionRepository.findOwnedSession(sessionId, callerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getOwned(sessionId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Session not found.");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void draftOfAnotherUserIsNotOverwritten() {
        UUID sessionId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        when(currentUserService.requireId()).thenReturn(callerId);
        when(sessionRepository.findOwnedSession(sessionId, callerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.replaceDraft(sessionId, "console.log('pwned')"))
                .isInstanceOf(EntityNotFoundException.class);

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void draftOfTheCallersSessionIsSaved() {
        UUID sessionId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        Session session = new Session();
        session.setId(sessionId);
        session.setProgrammingLanguage("js");
        session.setStatus("active");

        when(currentUserService.requireId()).thenReturn(callerId);
        when(sessionRepository.findOwnedSession(sessionId, callerId)).thenReturn(Optional.of(session));

        sessionService.replaceDraft(sessionId, "export const sum = (a, b) => a + b;");

        assertThat(session.getDraftCode()).isEqualTo("export const sum = (a, b) => a + b;");
        assertThat(session.getUpdatedAt()).isNotNull();
        verify(sessionRepository).save(session);
    }

    @Test
    void sessionIsCreatedInsideAWorkspaceOfTheCaller() {
        UUID workspaceId = UUID.randomUUID();
        UUID kataId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();

        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        Kata kata = new Kata();
        kata.setId(kataId);
        kata.setTitle("Sum of Two Numbers");
        kata.setProgrammingLanguage("js");

        when(workspaceService.requireOwned(workspaceId)).thenReturn(workspace);
        when(kataRepository.findById(kataId)).thenReturn(Optional.of(kata));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var dto = sessionService.create(workspaceId, kataId);

        assertThat(dto.id()).isNotNull();
        assertThat(dto.kataId()).isEqualTo(kataId);
        assertThat(dto.title()).isEqualTo("Sum of Two Numbers");
        assertThat(dto.language()).isEqualTo("js");
        verify(sessionRepository).save(any(Session.class));
    }
}
