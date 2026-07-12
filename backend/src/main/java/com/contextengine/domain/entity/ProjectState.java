package com.contextengine.domain.entity;

/**
 * Represents the lifecycle and operational states of a Project.
 */
public enum ProjectState {
    /** Evaluating directories and parsing the initial workspace layout. */
    INITIALIZING,
    
    /** Actively running file monitors and processing directory updates. */
    ACTIVE,
    
    /** Pausing background optimization during periods of system inactivity. */
    IDLE,
    
    /** Throttled background thread execution during low-battery or power preservation phases. */
    SUSPENDED,
    
    /** Project registration removed and configuration caches cleaned up. */
    ARCHIVED
}
