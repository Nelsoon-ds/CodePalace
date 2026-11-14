package com.codepalace.project.service;

import com.codepalace.project.model.Run;
import com.codepalace.project.model.dto.client.RunResponseDto;
import com.codepalace.project.model.mapper.RunMapper;
import com.codepalace.project.repository.RunRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RunService {

    private final RunRepository runRepository;
    private final CurrentUserService currentUserService;
    private final RunMapper runMapper;

    public RunService(RunRepository runRepository,
                      CurrentUserService currentUserService,
                      RunMapper runMapper) {
        this.runRepository = runRepository;
        this.currentUserService = currentUserService;
        this.runMapper = runMapper;
    }

    /**
     * Loads a run owned by the caller, or fails with 404.
     */
    public RunResponseDto getOwned(UUID runId) {
        UUID userId = currentUserService.requireId();
        Run run = runRepository.findOwnedRun(runId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Run not found."));
        return runMapper.toResponse(run);
    }

    /**
     * Loads a run without an ownership check. Only used by the event stream, which has already
     * verified ownership when the subscription was opened.
     */
    public RunResponseDto getRun(UUID runId) {
        Run run = runRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("No run with that id."));
        return runMapper.toResponse(run);
    }
}
