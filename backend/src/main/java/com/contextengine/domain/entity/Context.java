package com.contextengine.domain.entity;

import com.contextengine.domain.aggregate.AggregateRoot;
import com.contextengine.domain.valueobject.ProjectId;
import com.contextengine.domain.valueobject.TokenBudget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Resolves, compiles, prioritizes, and structures optimal project context chunks.
 * Serves as the boundary coordinator for the Context Aggregate.
 */
public class Context implements AggregateRoot<ProjectId> {
    
    private final ProjectId projectId;
    private final TokenBudget budget;
    private final List<ContextSnapshot> snapshots;

    /**
     * Constructs a Context.
     *
     * @param projectId the parent project ID
     * @param budget the token budget configured for this context
     */
    public Context(ProjectId projectId, TokenBudget budget) {
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.budget = Objects.requireNonNull(budget, "TokenBudget must not be null");
        this.snapshots = new ArrayList<>();
    }

    @Override
    public ProjectId id() {
        return projectId;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public TokenBudget budget() {
        return budget;
    }

    public List<ContextSnapshot> snapshots() {
        return Collections.unmodifiableList(snapshots);
    }

    /**
     * Compiles and adds an immutable ContextSnapshot.
     * Enforces the consistency rule: "The cumulative token count of elements packaged inside a Context Snapshot MUST NOT exceed the configured TokenBudget boundary."
     *
     * @param snapshot the snapshot to add
     */
    public void addSnapshot(ContextSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "ContextSnapshot must not be null");
        
        // Enforce boundary constraint: tokenFootprint <= budget
        if (snapshot.summary().tokenFootprint() > budget.value()) {
            throw new IllegalArgumentException(
                "Snapshot token footprint (" + snapshot.summary().tokenFootprint() + 
                ") exceeds the allowed token budget (" + budget.value() + ")"
            );
        }
        
        if (snapshots.stream().anyMatch(s -> s.id().equals(snapshot.id()))) {
            throw new IllegalArgumentException("Snapshot with ID '" + snapshot.id() + "' already exists in this context");
        }
        
        snapshots.add(snapshot);
    }

    /**
     * Reconstructs an existing Context from persistence data.
     *
     * @param projectId the parent project ID
     * @param budget the token budget
     * @param snapshots list of snapshots
     * @return reconstructed Context
     */
    public static Context reconstruct(ProjectId projectId, TokenBudget budget, List<ContextSnapshot> snapshots) {
        Context context = new Context(projectId, budget);
        if (snapshots != null) {
            context.snapshots.addAll(snapshots);
        }
        return context;
    }
}
