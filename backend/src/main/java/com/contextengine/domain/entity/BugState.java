package com.contextengine.domain.entity;

/**
 * Represents the resolution lifecycle states of a tracked Bug.
 */
public enum BugState {
    /** Bug reported; error profiles logged. */
    UNRESOLVED,
    
    /** Investigation activated; linked file weights raised. */
    UNDER_INVESTIGATION,
    
    /** Regression fix verified; associated tests pass. */
    RESOLVED
}
