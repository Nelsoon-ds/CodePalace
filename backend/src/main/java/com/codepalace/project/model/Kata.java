package com.codepalace.project.model;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "katas")
public class Kata {

    public Kata(UUID id, String title,
                String statementMd, String difficulty,
                String programmingLanguage, String starterCode,
                String referenceSolution,
                String status, String visibility, String createdBy, Timestamp createdAt) {
        this.id = id;
        this.title = title;
        this.statementMd = statementMd;
        this.difficulty = difficulty;
        this.programmingLanguage = programmingLanguage;
        this.starterCode = starterCode;
        this.referenceSolution = referenceSolution;
        this.status = status;
        this.visibility = visibility;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
    public Kata() {
    }


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    private String statementMd;

    private String difficulty;

    private String programmingLanguage;

    private String starterCode;

    private String referenceSolution;

    private String status;

    private String visibility;

    private String createdBy;

    private Timestamp createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatementMd() {
        return statementMd;
    }

    public void setStatementMd(String statementMd) {
        this.statementMd = statementMd;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getProgrammingLanguage() {
        return programmingLanguage;
    }

    public void setProgrammingLanguage(String programmingLanguage) {
        this.programmingLanguage = programmingLanguage;
    }

    public String getStarterCode() {
        return starterCode;
    }

    public void setStarterCode(String starterCode) {
        this.starterCode = starterCode;
    }

    public String getReferenceSolution() {
        return referenceSolution;
    }

    public void setReferenceSolution(String referenceSolution) {
        this.referenceSolution = referenceSolution;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
