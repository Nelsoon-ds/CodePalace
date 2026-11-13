package com.codepalace.project.model;

import com.codepalace.project.model.dto.server.ValidationStats;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mutation_strategies", columnDefinition = "jsonb")
    private List<String> mutationStrategies;

    // Mapping a JSON object to a Java Map
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_stats", columnDefinition = "jsonb")
    private ValidationStats validationStats;

    public TestSuite(UUID id, UUID kataId, String programmingLanguage, String framework, String code, String suiteHash, List<String> mutationStrategies, ValidationStats validationStats) {
        this.id = id;
        this.kataId = kataId;
        this.programmingLanguage = programmingLanguage;
        this.framework = framework;
        this.code = code;
        this.suiteHash = suiteHash;
        this.mutationStrategies = mutationStrategies;
        this.validationStats = validationStats;
    }
}
