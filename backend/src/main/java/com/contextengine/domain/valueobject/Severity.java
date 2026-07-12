package com.contextengine.domain.valueobject;

/**
 * Categorizes the operational impact scale of tracked defects or bugs.
 */
public enum Severity {
    /** Critical impact, blocking core operations. */
    CRITICAL,
    
    /** Major impact, causing significant degradation. */
    MAJOR,
    
    /** Minor impact, minor inconvenience or cosmetic issue. */
    MINOR
}
