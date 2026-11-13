package com.codepalace.project.repository;

import com.codepalace.project.model.Run;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RunRepository extends JpaRepository<Run, UUID> {
}
