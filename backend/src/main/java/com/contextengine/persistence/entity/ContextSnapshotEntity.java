package com.contextengine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity representing a compiled Context Snapshot record.
 * <p>
 * Bounded Context: Context Assembly
 * Related Subsystem: Persistent Storage
 * </p>
 */
@Entity
@Table(
    name = "context_snapshots",
    indexes = {
        @Index(name = "idx_snapshot_project_created", columnList = "project_id, snapshot_created_at")
    }
)
public class ContextSnapshotEntity extends BasePersistenceEntity {

    @Id
    @Column(name = "snapshot_id", length = 36, nullable = false)
    private String id;

    @Column(name = "project_id", length = 36, nullable = false)
    private String projectId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "snapshot_created_at", nullable = false)
    private Instant snapshotCreatedAt;

    @Column(name = "total_file_count", nullable = false)
    private int totalFileCount;

    @Column(name = "token_footprint", nullable = false)
    private int tokenFootprint;

    @Column(name = "primary_entities", length = 4096, nullable = false)
    private String primaryEntitiesSerialized;

    @jakarta.persistence.Lob
    @Column(name = "evidences", nullable = false)
    private String evidencesSerialized;

    @jakarta.persistence.Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Instant getSnapshotCreatedAt() {
        return snapshotCreatedAt;
    }

    public void setSnapshotCreatedAt(Instant snapshotCreatedAt) {
        this.snapshotCreatedAt = snapshotCreatedAt;
    }

    public int getTotalFileCount() {
        return totalFileCount;
    }

    public void setTotalFileCount(int totalFileCount) {
        this.totalFileCount = totalFileCount;
    }

    public int getTokenFootprint() {
        return tokenFootprint;
    }

    public void setTokenFootprint(int tokenFootprint) {
        this.tokenFootprint = tokenFootprint;
    }

    public String getPrimaryEntitiesSerialized() {
        return primaryEntitiesSerialized;
    }

    public void setPrimaryEntitiesSerialized(String primaryEntitiesSerialized) {
        this.primaryEntitiesSerialized = primaryEntitiesSerialized;
    }

    public String getEvidencesSerialized() {
        return evidencesSerialized;
    }

    public void setEvidencesSerialized(String evidencesSerialized) {
        this.evidencesSerialized = evidencesSerialized;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
