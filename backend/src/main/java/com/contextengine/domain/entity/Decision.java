package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.DecisionId;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Captures architectural choices, trade-offs, design patterns, and design records (ADRs).
 */
public class Decision {
    
    private final DecisionId id;
    private final ProjectId projectId;
    private final String title;
    private final Path markdownPath;
    private DecisionState status;
    private DecisionId supersededBy;

    /**
     * Constructs a Decision in the PROPOSED state.
     *
     * @param id the unique decision ID
     * @param projectId the parent project ID
     * @param title the title of the decision
     * @param markdownPath the path to the markdown document containing the decision
     */
    public Decision(DecisionId id, ProjectId projectId, String title, Path markdownPath) {
        this.id = Objects.requireNonNull(id, "DecisionId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.markdownPath = Objects.requireNonNull(markdownPath, "Markdown path must not be null");
        
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Decision title must not be null or empty");
        }
        this.title = title.trim();
        this.status = DecisionState.PROPOSED;
        this.supersededBy = null;
    }

    public DecisionId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public String title() {
        return title;
    }

    public Path markdownPath() {
        return markdownPath;
    }

    public DecisionState status() {
        return status;
    }

    public DecisionId supersededBy() {
        return supersededBy;
    }

    /**
     * Approves the decision.
     */
    public void approve() {
        if (this.status != DecisionState.PROPOSED) {
            throw new IllegalStateException("Decision must be in PROPOSED state to be approved");
        }
        this.status = DecisionState.APPROVED;
    }

    /**
     * Rejects the decision.
     */
    public void reject() {
        if (this.status != DecisionState.PROPOSED) {
            throw new IllegalStateException("Decision must be in PROPOSED state to be rejected");
        }
        this.status = DecisionState.REJECTED;
    }

    /**
     * Marks the decision as superseded by another decision.
     * Enforces invariant DI-3 (self-supersede prevention).
     *
     * @param supersedingDecisionId the ID of the new decision superseding this one
     */
    public void supersede(DecisionId supersedingDecisionId) {
        Objects.requireNonNull(supersedingDecisionId, "Superseding DecisionId must not be null");
        if (supersedingDecisionId.equals(this.id)) {
            throw new IllegalArgumentException("A decision cannot be superseded by itself (DI-3 Invariant)");
        }
        if (this.status != DecisionState.APPROVED) {
            throw new IllegalStateException("Only APPROVED decisions can be superseded");
        }
        this.status = DecisionState.SUPERSEDED;
        this.supersededBy = supersedingDecisionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Decision decision)) return false;
        return id.equals(decision.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
