package com.contextengine.domain.entity;

/**
 * Represents the progress and execution states of an atomic Task.
 */
public enum TaskState {
    /** Awaiting completion of prerequisite parent tasks. */
    BLOCKED,
    
    /** Dependency constraints resolved; task available for active work. */
    READY,
    
    /** Task activated; developer focusing on linked file directories. */
    IN_PROGRESS,
    
    /** File modifications saved, indexed, and verified in the database. */
    COMPLETED
}
