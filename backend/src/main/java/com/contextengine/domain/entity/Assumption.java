package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.AssumptionId;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Manages engineering hypotheses and external dependencies assumed to be stable.
 */
public class Assumption {
    
    private final AssumptionId id;
    private final ProjectId projectId;
    private final String description;
    private AssumptionState status;

    /**
     * Constructs an Assumption in the UNVERIFIED state.
     *
     * @param id the unique assumption ID
     * @param projectId the parent project ID
     * @param description the description of the hypothesis
     */
    public Assumption(AssumptionId id, ProjectId projectId, String description) {
        this.id = Objects.requireNonNull(id, "AssumptionId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Assumption description must not be null or empty");
        }
        this.description = description.trim();
        this.status = AssumptionState.UNVERIFIED;
    }

    public AssumptionId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public String description() {
        return description;
    }

    public AssumptionState status() {
        return status;
    }

    /**
     * Marks the assumption as VERIFIED.
     */
    public void verify() {
        this.status = AssumptionState.VERIFIED;
    }

    /**
     * Marks the assumption as INVALIDATED.
     */
    public void invalidate() {
        this.status = AssumptionState.INVALIDATED;
    }

    /**
     * Resets the status to UNVERIFIED.
     */
    public void reset() {
        this.status = AssumptionState.UNVERIFIED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Assumption that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
