package com.codepalace.project.service;

import com.codepalace.project.model.Run;
import com.codepalace.project.model.dto.client.RunResponseDto;
import com.codepalace.project.model.mapper.RunMapper;
import com.codepalace.project.repository.RunRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Service
public class RunService {

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private RunMapper runMapper;

    public RunResponseDto getRun(UUID runId) {
        Run run = runRepository.findById(runId).orElseThrow(() -> new EntityNotFoundException("No run with that id."));

        return runMapper.toResponse(run);
    }


}
