package com.codepalace.project.repository;

import com.codepalace.project.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {

    /**
     * Loads a session only when it belongs to a workspace owned by the given user.
     */
    @Query("select s from Session s where s.id = :sessionId and s.workspace.user.id = :userId")
    Optional<Session> findOwnedSession(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId);

    List<Session> findAllByWorkspace_Id(UUID workspaceId);
}
