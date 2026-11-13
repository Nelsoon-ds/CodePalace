package com.codepalace.project.model;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID runId;

    private String workType;

    private String status;

    private int attempts;

    private Timestamp claimedAt;

    private Timestamp createdAt;

    public Job(UUID id, UUID runId, String workType, String status, int attempts, Timestamp claimedAt, Timestamp createdAt) {
        this.id = id;
        this.runId = runId;
        this.workType = workType;
        this.status = status;
        this.attempts = attempts;
        this.claimedAt = claimedAt;
        this.createdAt = createdAt;
    }

    public Job() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRunId() {
        return runId;
    }

    public void setRunId(UUID runId) {
        this.runId = runId;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Timestamp getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Timestamp claimedAt) {
        this.claimedAt = claimedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
