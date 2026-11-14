package com.codepalace.project.repository;

import com.codepalace.project.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    List<Workspace> findAllByUser_Id(UUID userId);

    Optional<Workspace> findByIdAndUser_Id(UUID id, UUID userId);
}
