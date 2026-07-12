package com.contextengine.domain.entity;

/**
 * Represents the lifecycle progress states of a product Feature milestone.
 */
public enum FeatureState {
    /** Milestones registered, awaiting initial task activations. */
    BACKLOG,
    
    /** Linked tasks are being actively modified in the workspace. */
    IN_PROGRESS,
    
    /** Code changes compiled; awaiting automated checks and manual validation. */
    UNDER_REVIEW,
    
    /** Validation checks passed; functional changes are committed and active. */
    COMPLETED,
    
    /** Features retired; metrics and tracking history preserved in logs. */
    ARCHIVED
}
