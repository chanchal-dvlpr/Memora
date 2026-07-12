package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.ConstraintId;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Enforces absolute limits on design, performance, regulatory compliance, or libraries.
 */
public class Constraint {
    
    private final ConstraintId id;
    private final ProjectId projectId;
    private final String constraintType;
    private Metadata rules;

    /**
     * Constructs a Constraint.
     *
     * @param id the unique constraint ID
     * @param projectId the parent project ID
     * @param constraintType the type of the constraint (e.g. "dependency-license")
     * @param rules the metadata rules governing the constraint execution
     */
    public Constraint(ConstraintId id, ProjectId projectId, String constraintType, Metadata rules) {
        this.id = Objects.requireNonNull(id, "ConstraintId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.rules = Objects.requireNonNull(rules, "Rules must not be null");
        
        if (constraintType == null || constraintType.trim().isEmpty()) {
            throw new IllegalArgumentException("Constraint type must not be null or empty");
        }
        this.constraintType = constraintType.trim();
    }

    public ConstraintId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public String constraintType() {
        return constraintType;
    }

    public Metadata rules() {
        return rules;
    }

    /**
     * Updates the rules configuration.
     *
     * @param newRules the new rules metadata
     */
    public void updateRules(Metadata newRules) {
        this.rules = Objects.requireNonNull(newRules, "Rules must not be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Constraint that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
