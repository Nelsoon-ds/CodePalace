package com.codepalace.project.repository;

import com.codepalace.project.model.Run;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RunRepository extends JpaRepository<Run, UUID> {

    /**
     * Loads a run only when the submission behind it belongs to a session in a workspace owned by
     * the given user.
     */
    @Query("select r from Run r, Submission s, Session se " +
            "where r.id = :runId and r.submissionId = s.id and s.sessionId = se.id and se.workspace.user.id = :userId")
    Optional<Run> findOwnedRun(@Param("runId") UUID runId, @Param("userId") UUID userId);
}
