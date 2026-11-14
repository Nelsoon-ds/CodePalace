package com.codepalace.project.model;

import com.codepalace.project.model.dto.server.ValidationStats;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "test_suites")
public class TestSuite {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID kataId;
    private String programmingLanguage;
    private String framework;
    private String code;
    private String suiteHash;
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mutation_strategies", columnDefinition = "jsonb")
    private List<String> mutationStrategies;

    // Mapping a JSON object to a Java Map
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_stats", columnDefinition = "jsonb")
    private ValidationStats validationStats;

    public TestSuite(UUID id, UUID kataId, String programmingLanguage, String framework, String code, String suiteHash, List<String> mutationStrategies, ValidationStats validationStats, Instant createdAt) {
        this.id = id;
        this.kataId = kataId;
        this.programmingLanguage = programmingLanguage;
        this.framework = framework;
        this.code = code;
        this.suiteHash = suiteHash;
        this.mutationStrategies = mutationStrategies;
        this.validationStats = validationStats;
        this.createdAt = createdAt;
    }


    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public TestSuite() {

    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getKataId() {
        return kataId;
    }

    public void setKataId(UUID kataId) {
        this.kataId = kataId;
    }

    public String getProgrammingLanguage() {
        return programmingLanguage;
    }

    public void setProgrammingLanguage(String programmingLanguage) {
        this.programmingLanguage = programmingLanguage;
    }

    public String getFramework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    /**
     * Hidden suite code must never leave the server (see the hidden-suite privacy invariant in
     * docs/05-data-model-api.md). It is excluded from JSON as a safety net.
     */
    @JsonIgnore
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSuiteHash() {
        return suiteHash;
    }

    public void setSuiteHash(String suiteHash) {
        this.suiteHash = suiteHash;
    }

    public List<String> getMutationStrategies() {
        return mutationStrategies;
    }

    public void setMutationStrategies(List<String> mutationStrategies) {
        this.mutationStrategies = mutationStrategies;
    }

    public ValidationStats getValidationStats() {
        return validationStats;
    }

    public void setValidationStats(ValidationStats validationStats) {
        this.validationStats = validationStats;
    }
}
