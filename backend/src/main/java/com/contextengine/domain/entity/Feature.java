package com.contextengine.domain.entity;

import com.contextengine.domain.valueobject.FeatureId;
import com.contextengine.domain.valueobject.Priority;
import com.contextengine.domain.valueobject.ProjectId;
import java.util.Objects;

/**
 * Models a logical product roadmap objective or functional development block.
 */
public class Feature {
    
    private final FeatureId id;
    private final ProjectId projectId;
    private final String title;
    private final Priority priority;
    private FeatureState status;
    private double progressPercentage;

    /**
     * Constructs a Feature in the BACKLOG state with 0% progress.
     *
     * @param id the unique feature ID
     * @param projectId the parent project ID
     * @param title the title of the feature
     * @param priority the feature priority
     */
    public Feature(FeatureId id, ProjectId projectId, String title, Priority priority) {
        this.id = Objects.requireNonNull(id, "FeatureId must not be null");
        this.projectId = Objects.requireNonNull(projectId, "ProjectId must not be null");
        this.priority = Objects.requireNonNull(priority, "Priority must not be null");
        
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Feature title must not be null or empty");
        }
        this.title = title.trim();
        this.status = FeatureState.BACKLOG;
        this.progressPercentage = 0.0;
    }

    public FeatureId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public String title() {
        return title;
    }

    public Priority priority() {
        return priority;
    }

    public FeatureState status() {
        return status;
    }

    public double progressPercentage() {
        return progressPercentage;
    }

    /**
     * Updates the progress percentage based on associated tasks.
     *
     * @param progress the calculated progress percentage (0.0 to 100.0)
     */
    public void updateProgress(double progress) {
        if (progress < 0.0 || progress > 100.0) {
            throw new IllegalArgumentException("Progress percentage must be between 0.0 and 100.0: " + progress);
        }
        this.progressPercentage = progress;
        
        // Auto-activate to IN_PROGRESS if backlog and progress starts
        if (this.status == FeatureState.BACKLOG && progress > 0.0) {
            this.status = FeatureState.IN_PROGRESS;
        }
    }

    /**
     * Activates the feature from BACKLOG to IN_PROGRESS.
     */
    public void startProgress() {
        if (this.status != FeatureState.BACKLOG) {
            throw new IllegalStateException("Feature is already active or completed: " + this.status);
        }
        this.status = FeatureState.IN_PROGRESS;
    }

    /**
     * Submits the feature for review.
     */
    public void sendToReview() {
        if (this.status != FeatureState.IN_PROGRESS) {
            throw new IllegalStateException("Feature must be in IN_PROGRESS state to submit for review");
        }
        this.status = FeatureState.UNDER_REVIEW;
    }

    /**
     * Rejects the review and transitions the feature back to IN_PROGRESS.
     */
    public void rejectReview() {
        if (this.status != FeatureState.UNDER_REVIEW) {
            throw new IllegalStateException("Feature must be in UNDER_REVIEW state to reject");
        }
        this.status = FeatureState.IN_PROGRESS;
    }

    /**
     * Completes the feature. Enforces that progress must be exactly 100% to transition to COMPLETED.
     */
    public void complete() {
        if (this.status != FeatureState.UNDER_REVIEW && this.status != FeatureState.IN_PROGRESS) {
            throw new IllegalStateException("Feature cannot be completed from " + this.status);
        }
        if (Math.abs(this.progressPercentage - 100.0) > 1e-6) {
            throw new IllegalStateException("Feature progress must be exactly 100.0% to be marked COMPLETED");
        }
        this.status = FeatureState.COMPLETED;
    }

    /**
     * Archives the feature.
     */
    public void archive() {
        this.status = FeatureState.ARCHIVED;
    }

    /**
     * Reconstructs an existing Feature from historical state.
     *
     * @param id the unique feature ID
     * @param projectId the parent project ID
     * @param title the title of the feature
     * @param priority the feature priority
     * @param status the feature status
     * @param progressPercentage the progress percentage
     * @return the reconstructed Feature
     */
    public static Feature reconstruct(
        FeatureId id,
        ProjectId projectId,
        String title,
        Priority priority,
        FeatureState status,
        double progressPercentage
    ) {
        Feature feature = new Feature(id, projectId, title, priority);
        feature.status = Objects.requireNonNull(status, "Status must not be null");
        if (progressPercentage < 0.0 || progressPercentage > 100.0) {
            throw new IllegalArgumentException("Progress percentage must be between 0.0 and 100.0");
        }
        feature.progressPercentage = progressPercentage;
        return feature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Feature feature)) return false;
        return id.equals(feature.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
