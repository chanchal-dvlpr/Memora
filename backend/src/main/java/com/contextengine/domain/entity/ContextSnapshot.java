package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.ContextSummary;
import com.contextengine.domain.valueobject.EngineeringEvidence;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.SnapshotId;
import com.contextengine.domain.valueobject.Timestamp;
import com.contextengine.domain.valueobject.Version;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds an immutable, compressed representation of a compiled retrieval context.
 * Strictly read-only and immutable.
 */
public class ContextSnapshot {
    
    private final SnapshotId id;
    private final ProjectId projectId;
    private final Version version;
    private final Timestamp createdAt;
    private final ContextSummary summary;
    private final List<EngineeringEvidence> evidences;

    /**
     * Constructs a ContextSnapshot. All fields are final and list fields are made unmodifiable.
     *
     * @param id the unique snapshot ID
     * @param projectId the associated project ID
     * @param version the sequence version of the snapshot
     * @param createdAt the time at which the snapshot was compiled
     * @param summary a metadata summary of the snapshot details
     * @param evidences physical evidence backing the context items
     */
    public ContextSnapshot(SnapshotId id, ProjectId projectId, Version version, Timestamp createdAt, ContextSummary summary, List<EngineeringEvidence> evidences) {
        this.id = Objects.requireNonNull(id, "SnapshotId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.version = Objects.requireNonNull(version, "Version must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt timestamp must not be null");
        this.summary = Objects.requireNonNull(summary, "ContextSummary must not be null");
        Objects.requireNonNull(evidences, "Evidences list must not be null");
        
        this.evidences = List.copyOf(evidences);
    }

    public SnapshotId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public Version version() {
        return version;
    }

    public Timestamp createdAt() {
        return createdAt;
    }

    public ContextSummary summary() {
        return summary;
    }

    public List<EngineeringEvidence> evidences() {
        return evidences;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextSnapshot that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
