package com.codepalace.project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session {

    public Session () {}

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    @JsonIgnore
    private Workspace workspace;

    private UUID kataId;

    private String programmingLanguage;

    private String status;

    private Instant createdAt;

    private Instant updatedAt;

    @Column(name = "draft_code", columnDefinition = "text")
    private String draftCode;

    public Workspace getWorkspace() {
        return workspace;
    }

    public String getDraftCode() {
        return draftCode;
    }

    public void setDraftCode(String draftCode) {
        this.draftCode = draftCode;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @JsonProperty("workspaceId")
    public UUID getWorkspaceId() {
        return workspace != null ? workspace.getId() : null;
    }

    public void setWorkspace(Workspace workspace){
        this.workspace = workspace;
    }

    public UUID getKataId() {
        return kataId;
    }

    public void setKataId(UUID kataId) {
        this.kataId = kataId;
    }

    @JsonProperty("language")
    public String getProgrammingLanguage() {
        return programmingLanguage;
    }

    public void setProgrammingLanguage(String programmingLanguage) {
        this.programmingLanguage = programmingLanguage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
