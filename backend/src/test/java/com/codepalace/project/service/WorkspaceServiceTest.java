package com.codepalace.project.service;

import com.codepalace.project.model.User;
import com.codepalace.project.model.Workspace;
import com.codepalace.project.model.mapper.WorkspaceMapper;
import com.codepalace.project.repository.KataRepository;
import com.codepalace.project.repository.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock WorkspaceRepository workspaceRepository;
    @Mock KataRepository kataRepository;
    @Mock CurrentUserService currentUserService;

    WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        workspaceService = new WorkspaceService(workspaceRepository, kataRepository, currentUserService,
                new WorkspaceMapper());
    }

    @Test
    void onlyWorkspacesOfTheCallerAreListed() {
        UUID callerId = UUID.randomUUID();
        User caller = new User("caller@example.com", "hash");
        caller.setUuid(callerId);

        Workspace owned = new Workspace(caller, "Mine", null);
        owned.setId(UUID.randomUUID());

        when(currentUserService.require()).thenReturn(caller);
        when(workspaceRepository.findAllByUser_Id(callerId)).thenReturn(List.of(owned));

        var workspaces = workspaceService.listForCurrentUser();

        assertThat(workspaces).hasSize(1);
        assertThat(workspaces.getFirst().name()).isEqualTo("Mine");
        verify(workspaceRepository).findAllByUser_Id(callerId);
        verify(workspaceRepository, never()).findAll();
    }

    @Test
    void workspaceOfAnotherUserCannotBeDeleted() {
        UUID workspaceId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        User caller = new User("caller@example.com", "hash");
        caller.setUuid(callerId);

        when(currentUserService.require()).thenReturn(caller);
        when(workspaceRepository.findByIdAndUser_Id(workspaceId, callerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceService.delete(workspaceId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(workspaceRepository, never()).delete(any());
    }

    @Test
    void newWorkspaceBelongsToTheCaller() {
        UUID callerId = UUID.randomUUID();
        User caller = new User("caller@example.com", "hash");
        caller.setUuid(callerId);

        when(currentUserService.require()).thenReturn(caller);

        var created = workspaceService.create("Practice");

        assertThat(created.name()).isEqualTo("Practice");
        verify(workspaceRepository).save(any(Workspace.class));
    }
}
