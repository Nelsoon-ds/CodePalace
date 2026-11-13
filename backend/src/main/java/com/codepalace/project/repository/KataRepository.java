package com.codepalace.project.repository;

import com.codepalace.project.model.Kata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KataRepository extends JpaRepository<Kata, UUID> {

    List<Kata> findAllByStatusAndVisibilityOrderByCreatedAtDesc(String status, String visibility);

    List<Kata> findAllByStatusAndVisibilityAndDifficultyOrderByCreatedAtDesc(String status, String visibility, String difficulty);

    @Query("select k from Kata k where k.status = 'published' and k.visibility = 'public' " +
            "and (lower(k.title) like :term or lower(k.statementMd) like :term) " +
            "order by k.createdAt desc")
    List<Kata> searchPublishedPublic(@Param("term") String term);
}
