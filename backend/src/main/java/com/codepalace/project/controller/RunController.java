package com.codepalace.project.controller;

import com.codepalace.project.model.Run;
import com.codepalace.project.model.dto.client.RunResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController()
public class RunController {

    @GetMapping("/{id}")
    public ResponseEntity<RunResponseDto> getRun(@PathVariable UUID id) {
        Run run = runService.getRunById(id);
        return ResponseEntity.ok(runMapper.toResponse(run));
    }

}
