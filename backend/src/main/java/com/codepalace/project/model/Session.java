package com.codepalace.project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session {

    public Session () {}

    public Session(UUID id, UUID workspaceId,
                   UUID kataId, String programmingLanguage,
                   String status,
                   Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.workspace = workspace;
        this.kataId = kataId;
        this.programmingLanguage = programmingLanguage;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    @JsonIgnore
    private Workspace workspace;

    private UUID kataId;

    private String programmingLanguage;

    private String status;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    @Column(name = "draft_code")
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
