package com.codepalace.project.repository;

import com.codepalace.project.model.TestSuite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TestSuiteRepository extends JpaRepository<TestSuite, UUID> {
    // Finds the latest test suite for the given kata and language
    Optional<TestSuite> findFirstByKataIdAndProgrammingLanguageOrderByCreatedAtDesc(
            UUID kataId,
            String programmingLanguage
    );
}
