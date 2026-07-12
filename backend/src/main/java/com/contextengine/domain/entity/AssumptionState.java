package com.contextengine.domain.entity;

/**
 * Represents the verification states of an engineering Assumption.
 */
public enum AssumptionState {
    /** Bounded verification pending system checks or tests. */
    UNVERIFIED,
    
    /** Verification scripts confirm the target interface meets expectations. */
    VERIFIED,
    
    /** System checks or testing fail; warning triggered. */
    INVALIDATED
}
