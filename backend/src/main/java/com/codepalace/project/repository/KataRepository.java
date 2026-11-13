package com.codepalace.project.repository;

import com.codepalace.project.model.Kata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KataRepository extends JpaRepository<Kata, UUID> {
}
