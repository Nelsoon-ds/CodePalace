package com.codepalace.project.model;

import com.codepalace.project.model.dto.runner.vitest.VitestResults;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "runs")
public class Run {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "suite_id", nullable = false)
    private UUID suiteId;

    @Column(nullable = false)
    private String status; // QUEUED, RUNNING, COMPLETED, FAILED, TIMED_OUT, OOM_KILLED, INTERNAL_ERROR

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "duration_ms")
    private Double durationMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "results_jsonb", columnDefinition = "jsonb")
    private VitestResults results;

    @Column(name = "output_tail", columnDefinition = "text")
    private String outputTail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(UUID submissionId) {
        this.submissionId = submissionId;
    }

    public UUID getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(UUID suiteId) {
        this.suiteId = suiteId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public Double getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Double durationMs) {
        this.durationMs = durationMs;
    }

    public VitestResults getResults() {
        return results;
    }

    public void setResults(VitestResults results) {
        this.results = results;
    }

    public String getOutputTail() {
        return outputTail;
    }

    public void setOutputTail(String outputTail) {
        this.outputTail = outputTail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
};
