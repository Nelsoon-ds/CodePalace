package com.codepalace.project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class Workspace {

    public Workspace(User user, String workspaceName, Instant createdAt) {
        this.user = user;
        this.workspaceName = workspaceName;
        this.createdAt = createdAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String workspaceName;

    private Instant createdAt;

    @OneToMany(mappedBy = "workspace", fetch = FetchType.LAZY)
    private List<Session> sessions = new ArrayList<>();

    public void setId(UUID id) {
        this.id = id;
    }

    public List<Session> getSessions() {
        return sessions;
    }

    public void setSessions(List<Session> sessions) {
        this.sessions = sessions;
    }

    public Workspace() {
    }

    public UUID getId() {
        return id;
    }

    @JsonProperty("userId")
    public UUID getUserId() {
        return user != null ? user.getUuid() : null;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @JsonProperty("name")
    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
